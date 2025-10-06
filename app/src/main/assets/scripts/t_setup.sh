#!/data/data/com.termux/files/usr/bin/sh
set -e

# #COMPLETION_DRIVE: Assuming this runs inside Termux with pkg available
# #SUGGEST_VERIFY: Check `$PREFIX` is set and `pkg --version` works

PORT=333
SV_DIR="$PREFIX/var/service"

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
    # #COMPLETION_DRIVE: Install default packages per SERVERSYSTEM.md
    # #SUGGEST_VERIFY: Verify commands exist: sshd, nano, htop, node, deno
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
PubkeyAuthentication no
PermitEmptyPasswords no
UseDNS no
PermitTTY yes
EOF
    [ -f "$PREFIX/etc/ssh/ssh_host_rsa_key" ] || ssh-keygen -t rsa -b 2048 -f "$PREFIX/etc/ssh/ssh_host_rsa_key" -N "" -q || true
}

# #COMPLETION_DRIVE: On Termux, create a passwd alias so username 'samsara' maps to current UID/GID
# #SUGGEST_VERIFY: After running, `grep '^samsara:' "$PREFIX/etc/passwd"` and SSH as samsara with password 'server'
ensure_default_user() {
    curu=$(whoami)
    uid=$(id -u)
    gid=$(id -g)
    home="$HOME"
    shell="$PREFIX/bin/login"
    PASSWD_FILE="$PREFIX/etc/passwd"
    [ -f "$PASSWD_FILE" ] || touch "$PASSWD_FILE"
    if ! grep -q '^samsara:' "$PASSWD_FILE" 2>/dev/null; then
        # Duplicate current user's line if available; else synthesize one
        cur_line=$(grep "^$curu:" "$PASSWD_FILE" 2>/dev/null || true)
        if [ -n "$cur_line" ]; then
            echo "$cur_line" | sed "s/^$curu:/samsara:/" >> "$PASSWD_FILE"
        else
            echo "samsara:x:$uid:$gid:Samsara User:$home:$shell" >> "$PASSWD_FILE"
        fi
    fi
    # Set password for samsara or fallback to current user
    if printf "server\nserver\n" | passwd samsara >/dev/null 2>&1; then
        echo "user:samsara" > "$HOME/.samsara-user" 2>/dev/null || true
    else
        printf "server\nserver\n" | passwd "$curu" >/dev/null 2>&1 || true
        echo "user:$curu" > "$HOME/.samsara-user" 2>/dev/null || true
    fi
}

ensure_services() {
    info "Ensuring termux-services"
    sv_enable="${PREFIX}/bin/sv-enable"
    [ -x "$sv_enable" ] || fail "termux-services not installed properly"
    mkdir -p "$SV_DIR"
    if [ ! -d "$SV_DIR/sshd" ]; then
        ln -sf "$PREFIX/var/service-available/sshd" "$SV_DIR/sshd" 2>/dev/null || true
    fi
    "$sv_enable" sshd || true
    sv up sshd || true
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

summary() {
    echo "Termux setup complete"
    userLabel="$(cut -d: -f2 "$HOME/.samsara-user" 2>/dev/null | sed -n 's/^user://p' || true)"
    [ -z "$userLabel" ] && userLabel="$(whoami)"
    echo "SSH:    ssh ${userLabel}@<phone-ip> -p $PORT (password: server)"
}

main() {
    install_packages
    configure_ssh
    ensure_default_user
    ensure_services
    ensure_config
    summary
}

main "$@"
