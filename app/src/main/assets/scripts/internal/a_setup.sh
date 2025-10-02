#!/bin/sh
set -e

# #COMPLETION_DRIVE: Assuming this script runs inside Alpine (proot-distro login alpine) as root
# #SUGGEST_VERIFY: Run `cat /etc/alpine-release` and `id -u` should be 0 before continuing

PORT=222

info() { echo "[INFO] $*"; }
fail() { echo "[!] $*" 1>&2; exit 1; }

install_packages() {
    # #COMPLETION_DRIVE: Install required packages and OpenRC; prefer ss for port checks
    # #SUGGEST_VERIFY: Run `sshd -V`, `node -v`, `deno --version`, `rc-service --version || true`
    apk update || true
    apk add --no-cache \
        openssh nano htop nodejs npm deno curl ca-certificates \
        openrc iproute2 net-tools || fail "Package installation failed"
}

ensure_shells() {
    for s in /bin/sh /bin/bash; do
        [ -x "$s" ] || [ "$s" = "/bin/bash" ] && {
            grep -qxF "$s" /etc/shells 2>/dev/null || echo "$s" >> /etc/shells
        }
    done
}

safe_set_shell() {
    u="$1"; shpath="$2"
    ensure_shells
    if command -v chsh >/dev/null 2>&1; then
        chsh -s "$shpath" "$u" >/dev/null 2>&1 || true
    fi
    sed -i "s#^\($u:[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:\).*#\\1$shpath#" /etc/passwd || true
}

set_password() {
    u="$1"; p="$2"
    passwd -u "$u" >/dev/null 2>&1 || true
    if command -v chpasswd >/dev/null 2>&1; then
        echo "$u:$p" | chpasswd 2>/dev/null || {
            printf "%s\n%s\n" "$p" "$p" | passwd "$u" 2>/dev/null || true
        }
    else
        printf "%s\n%s\n" "$p" "$p" | passwd "$u" 2>/dev/null || true
    fi
}

setup_users() {
    info "Configuring root user"
    if set_password root server && [ -x /bin/sh ] && safe_set_shell root "/bin/sh"; then
        [ -f /etc/nologin ] && rm -f /etc/nologin || true
    else
        fail "Root user configuration failed"
    fi
}

setup_ssh() {
    info "Configuring SSH server"
    CFG=/etc/ssh/sshd_config
    mkdir -p /etc/ssh
    if [ -f /etc/ssh/sshd_config ]; then
        cp /etc/ssh/sshd_config "/etc/ssh/sshd_config.bak.$(date +%Y%m%d_%H%M%S)" 2>/dev/null || true
    fi
    cat > "$CFG" <<EOF
Port $PORT
ListenAddress 0.0.0.0
Protocol 2
HostKey /etc/ssh/ssh_host_rsa_key
StrictModes no
PubkeyAuthentication no
PasswordAuthentication yes
PermitRootLogin yes
PermitEmptyPasswords no
UseDNS no
PermitTTY yes
EOF
    if command -v ssh-keygen >/dev/null 2>&1; then
        [ -f /etc/ssh/ssh_host_rsa_key ] || ssh-keygen -t rsa -b 2048 -f /etc/ssh/ssh_host_rsa_key -N "" -q 2>/dev/null || true
    fi
    chmod 600 /etc/ssh/ssh_host_rsa_key 2>/dev/null || true
    mkdir -p /var/run/sshd /run/sshd 2>/dev/null || true
}

ensure_rc_env() {
    # #COMPLETION_DRIVE: Prepare minimal OpenRC runtime dirs under proot
    # #SUGGEST_VERIFY: `rc-status` shows default runlevel without fatal errors
    if command -v openrc >/dev/null 2>&1 || command -v rc-status >/dev/null 2>&1; then
        mkdir -p /run/openrc 2>/dev/null || true
        [ -f /run/openrc/softlevel ] || echo default > /run/openrc/softlevel 2>/dev/null || true
    fi
}

is_port_listening() {
    p="$1"
    if command -v ss >/dev/null 2>&1; then
        ss -ltn 2>/dev/null | awk '{print $4}' | grep -E "(^|:)${p}$" -q && return 0
    fi
    if command -v netstat >/dev/null 2>&1; then
        netstat -tln 2>/dev/null | awk '{print $4}' | grep -E "(^|:)${p}$" -q && return 0
    fi
    ps aux 2>/dev/null | grep -E "[s]shd( |.*-p ${p})" >/dev/null 2>&1 && return 0
    return 1
}

ensure_rc_shim() {
    # #COMPLETION_DRIVE: Provide rc-service shim if OpenRC not available
    # #SUGGEST_VERIFY: `rc-service sshd restart` works and starts sshd
    if ! command -v rc-service >/dev/null 2>&1; then
        cat > /usr/local/bin/rc-service <<'EOS'
#!/bin/sh
svc="$1"; act="$2"; shift 2 || true
case "$svc" in
    sshd)
        case "$act" in
            start)
                /usr/sbin/sshd -e "$@" 2>/dev/null || /usr/sbin/sshd "$@" ;;
            stop)
                pkill -f '[s]shd' 2>/dev/null || true ;;
            restart)
                pkill -f '[s]shd' 2>/dev/null || true
                sleep 1
                /usr/sbin/sshd -e "$@" 2>/dev/null || /usr/sbin/sshd "$@" ;;
            *) exec /usr/sbin/sshd "$@" ;;
        esac
        ;;
    *) echo "rc-service shim: unsupported service $svc" 1>&2; exit 1;;
esac
EOS
        chmod 0755 /usr/local/bin/rc-service || true
    fi
}

start_sshd() {
    info "Starting SSH server"
    if is_port_listening "$PORT"; then
        pkill -f "[s]shd.*-p $PORT" 2>/dev/null || pkill -f '[s]shd' 2>/dev/null || true
        sleep 1
    fi

    ensure_rc_env
    ensure_rc_shim

    if command -v rc-update >/dev/null 2>&1 && command -v rc-service >/dev/null 2>&1; then
        rc-update add sshd default >/dev/null 2>&1 || true
        if [ -f /etc/conf.d/sshd ]; then
            if grep -q '^SSHD_OPTS=' /etc/conf.d/sshd 2>/dev/null; then
                sed -i "s#^SSHD_OPTS=.*#SSHD_OPTS='-p $PORT'#" /etc/conf.d/sshd || true
            else
                echo "SSHD_OPTS='-p $PORT'" >> /etc/conf.d/sshd
            fi
        fi
        rc-service sshd restart >/dev/null 2>&1 || rc-service sshd start >/dev/null 2>&1 || true
        sleep 1
        if is_port_listening "$PORT"; then
            return 0
        fi
        info "OpenRC start failed, falling back to direct sshd"
    fi

    if PROOT_NO_SECCOMP=1 /usr/sbin/sshd -e -p "$PORT" -o StrictModes=no -o PasswordAuthentication=yes >/dev/null 2>&1 || \
       /usr/sbin/sshd -p "$PORT" -o StrictModes=no -o PasswordAuthentication=yes >/dev/null 2>&1; then
        sleep 1
        is_port_listening "$PORT" && return 0
        fail "SSH server not listening on port $PORT"
    else
        fail "SSH server start failed"
    fi
}

ensure_config_dir() {
    # #COMPLETION_DRIVE: Ensure config directory exists and do not overwrite user config
    # #SUGGEST_VERIFY: Check /root/.config/samsara after setup
    mkdir -p /root/.config/samsara || true
}

summary() {
    ip=""
    if command -v ip >/dev/null 2>&1; then
        ip=$(ip route get 1.1.1.1 2>/dev/null | sed -n 's/.*src \([0-9.]*\).*/\1/p')
    fi
    [ -z "$ip" ] && ip=$(hostname -i 2>/dev/null | awk '{print $1}')
    case "$ip" in 127.*|0.0.0.0|"") ip="<phone-ip>";; esac
    echo "SSH ready on port $PORT"
    echo "Connect: ssh root@${ip} -p ${PORT} (password: server)"
}

main() {
    install_packages
    setup_users
    setup_ssh
    start_sshd
    ensure_config_dir
    info "Alpine setup complete"
    summary
}

main "$@"
