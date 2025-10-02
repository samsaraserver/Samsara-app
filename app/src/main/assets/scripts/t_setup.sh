#!/data/data/com.termux/files/usr/bin/sh
set -e

# #COMPLETION_DRIVE: Assuming this runs inside Termux with pkg available
# #SUGGEST_VERIFY: Check `$PREFIX` is set and `pkg --version` works

PORT=333
DASHBOARD_PORT=8765
SV_DIR="$PREFIX/var/service"
SVC_NAME="samsara-dashboard"
SVC_PATH="$SV_DIR/$SVC_NAME"
HTML_PATH="$PREFIX/share/samsara/samsara_dashboard.html"
TS_PATH="$PREFIX/bin/samsara_dashboard.ts"

info(){ echo "[INFO] $*"; }
fail(){ echo "[!] $*" 1>&2; exit 1; }

ensure_runtime_libs() {
    if [ -n "$PREFIX" ] && [ -d "$PREFIX/lib" ]; then
        [ -e "$PREFIX/lib/liblz4.so.1" ] || [ ! -e "$PREFIX/lib/liblz4.so" ] || ln -sf "$PREFIX/lib/liblz4.so" "$PREFIX/lib/liblz4.so.1" || true
    fi
}

install_packages() {
    info "Updating and installing Termux packages"
    ensure_runtime_libs
    yes | pkg update || true
    yes | pkg upgrade || true
    yes | pkg install termux-services openssh nano htop nodejs deno || fail "Package install failed"
}

configure_ssh() {
    info "Configuring sshd on port $PORT"
    mkdir -p "$PREFIX/etc/ssh" "$PREFIX/var/run/sshd"
    if [ -f "$PREFIX/etc/ssh/sshd_config" ]; then
        cp "$PREFIX/etc/ssh/sshd_config" "$PREFIX/etc/ssh/sshd_config.bak.$(date +%Y%m%d_%H%M%S)" || true
    fi
    cat > "$PREFIX/etc/ssh/sshd_config" <<EOF
Port $PORT
ListenAddress 0.0.0.0
HostKey $PREFIX/etc/ssh/ssh_host_rsa_key
PasswordAuthentication yes
PermitRootLogin yes
PermitEmptyPasswords no
UseDNS no
PermitTTY yes
EOF
    [ -f "$PREFIX/etc/ssh/ssh_host_rsa_key" ] || ssh-keygen -t rsa -b 2048 -f "$PREFIX/etc/ssh/ssh_host_rsa_key" -N "" -q || true
    # #COMPLETION_DRIVE: Set default password 'server' for the current Termux user
    # #SUGGEST_VERIFY: Run `whoami` then `ssh -p 333 <user>@<ip>` and login with 'server'
    u=$(whoami)
    printf "server\nserver\n" | passwd "$u" >/dev/null 2>&1 || true
}

ensure_services() {
    info "Ensuring termux-services"
    sv_enable="${PREFIX}/bin/sv-enable"
    [ -x "$sv_enable" ] || fail "termux-services not installed properly"
    mkdir -p "$SV_DIR"
    # Enable sshd service (provided by termux-services)
    if [ ! -d "$SV_DIR/sshd" ]; then
        ln -sf "$PREFIX/var/service-available/sshd" "$SV_DIR/sshd" 2>/dev/null || true
    fi
    "$sv_enable" sshd || true
    sv up sshd || true
}

write_dashboard_ts() {
    info "Writing dashboard server"
    mkdir -p "$PREFIX/bin" "$PREFIX/share/samsara"
    cat > "$TS_PATH" <<'TS'
// #COMPLETION_DRIVE: Bind to localhost-only in Termux
// #SUGGEST_VERIFY: curl http://127.0.0.1:8765/ inside Termux
const hostname = "127.0.0.1";
const port = 8765;

async function root() {
  try {
    const html = await Deno.readTextFile("$PREFIX/share/samsara/samsara_dashboard.html".replace("$PREFIX", Deno.env.get("PREFIX") ?? "/data/data/com.termux/files/usr"));
    return new Response(html, { headers: { "content-type": "text/html; charset=utf-8" } });
  } catch {
    return new Response("<h1>Samsara Dashboard</h1>");
  }
}

ensure_config() {
    # #COMPLETION_DRIVE: Copy config JSON into Termux config directory if present
    # #SUGGEST_VERIFY: Check $HOME/.config/samsara/samsara_config.json after running setup
    mkdir -p "$HOME/.config/samsara"
    if [ -f "$HOME/scripts/samsara_config.json" ] && [ ! -f "$HOME/.config/samsara/samsara_config.json" ]; then
        cp -f "$HOME/scripts/samsara_config.json" "$HOME/.config/samsara/samsara_config.json" || true
        chmod 0644 "$HOME/.config/samsara/samsara_config.json" || true
    fi
}

function readFirst(paths) {
  for (const p of paths) {
    try { const t = Deno.readTextFileSync(p).trim(); if (t) return t; } catch {}
  }
  return "";
}

function temp() {
  const paths = [
    "/sys/class/thermal/thermal_zone0/temp",
    "/sys/devices/virtual/thermal/thermal_zone0/temp",
  ];
  const raw = readFirst(paths);
  if (!raw) return new Response(JSON.stringify({ celsius: null }), { headers: { "content-type": "application/json" } });
  const n = Number(raw);
  const c = Number.isNaN(n) ? null : (n > 1000 ? Math.round(n) / 1000 : n);
  return new Response(JSON.stringify({ celsius: c }), { headers: { "content-type": "application/json" } });
}

Deno.serve({ hostname, port }, (req) => {
  const u = new URL(req.url);
  if (u.pathname === "/") return root();
  if (u.pathname === "/api/temperature") return temp();
  if (u.pathname === "/healthz") return new Response("ok");
  return new Response("Not found", { status: 404 });
});
TS
    chmod 0755 "$TS_PATH" || true
}

ensure_dashboard_html() {
    mkdir -p "$(dirname "$HTML_PATH")"
    if [ -f "$HTML_PATH" ]; then return 0; fi
    if [ -f "$HOME/scripts/samsara_dashboard.html" ]; then
        cp -f "$HOME/scripts/samsara_dashboard.html" "$HTML_PATH" || true
    else
        echo "<h1>Samsara Dashboard</h1>" > "$HTML_PATH"
    fi
}

install_runit_service() {
    info "Installing runit service for dashboard"
    mkdir -p "$SVC_PATH/log"
    cat > "$SVC_PATH/run" <<EOF
#!/data/data/com.termux/files/usr/bin/sh
exec 2>&1
exec deno run --no-prompt --allow-read=$PREFIX/share/samsara,$PREFIX/bin --allow-net=127.0.0.1:$DASHBOARD_PORT "$TS_PATH"
EOF
    chmod 0755 "$SVC_PATH/run"
    cat > "$SVC_PATH/log/run" <<'EOF'
#!/data/data/com.termux/files/usr/bin/sh
exec svlogd -tt ./
EOF
    chmod 0755 "$SVC_PATH/log/run"
    sv up "$SVC_NAME" || true
}

summary() {
    echo "Termux setup complete"
    echo "SSH:    port $PORT (service: sshd)"
    echo "UI:     http://127.0.0.1:$DASHBOARD_PORT/ (localhost only)"
}

main() {
    install_packages
    configure_ssh
    ensure_services
    ensure_config
    ensure_dashboard_html
    write_dashboard_ts
    install_runit_service
    summary
}

main "$@"
