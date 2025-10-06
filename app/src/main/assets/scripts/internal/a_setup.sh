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
    install -d -o root -g root -m 0755 /var/empty 2>/dev/null || true
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
            if grep -q '^command_args=' /etc/conf.d/sshd 2>/dev/null; then
                sed -i "s#^command_args=.*#command_args='-f /etc/ssh/sshd_config -p $PORT -E /var/log/sshd.log'#" /etc/conf.d/sshd || true
            else
                sed -i '/^SSHD_OPTS=/d' /etc/conf.d/sshd 2>/dev/null || true
                printf "command_args='-f /etc/ssh/sshd_config -p %s -E /var/log/sshd.log'\n" "$PORT" >> /etc/conf.d/sshd
            fi
        else
            printf "command_args='-f /etc/ssh/sshd_config -p %s -E /var/log/sshd.log'\n" "$PORT" > /etc/conf.d/sshd
        fi
        rc-update add local default >/dev/null 2>&1 || true
        mkdir -p /etc/local.d 2>/dev/null || true
        cat > /etc/local.d/samsara.start <<'EOS'
#!/bin/sh
# #COMPLETION_DRIVE: Ensure sshd is running at runlevel start
# #SUGGEST_VERIFY: rc-service sshd status returns started after login
tries=0
while [ $tries -lt 3 ]; do
    if command -v ss >/dev/null 2>&1; then
        ss -ltn 2>/dev/null | awk '{print $4}' | grep -E '(:|^)222$' -q && exit 0
    fi
    if command -v rc-service >/dev/null 2>&1; then
        rc-service sshd status >/dev/null 2>&1 || rc-service sshd start >/dev/null 2>&1 || true
    fi
    sleep 1
    tries=$((tries+1))
done
if ! ss -ltn 2>/dev/null | awk '{print $4}' | grep -E '(:|^)222$' -q; then
    nohup /usr/sbin/sshd -D -e -f /etc/ssh/sshd_config -p 222 -o StrictModes=no -o PasswordAuthentication=yes >>/var/log/samsara-sshd.log 2>&1 &
fi
EOS
        chmod 0755 /etc/local.d/samsara.start 2>/dev/null || true
        mkdir -p /etc/profile.d /run/samsara 2>/dev/null || true
        cat > /etc/profile.d/samsara_openrc.sh <<'EOS'
# #COMPLETION_DRIVE: Start OpenRC default runlevel on interactive login under proot
# #SUGGEST_VERIFY: After new login, rc-status shows services in default runlevel
if [ -z "$SAMSARA_OPENRC_BOOTED" ]; then
    export SAMSARA_OPENRC_BOOTED=1
    if command -v openrc >/dev/null 2>&1; then
        mkdir -p /run/openrc 2>/dev/null || true
        [ -f /run/openrc/softlevel ] || echo default > /run/openrc/softlevel 2>/dev/null || true
        openrc default >/dev/null 2>&1 || true
    fi
    if command -v rc-service >/dev/null 2>&1; then
        rc-service sshd status >/dev/null 2>&1 || rc-service sshd start >/dev/null 2>&1 || true
    fi
fi
EOS
        rc-service sshd restart >/dev/null 2>&1 || rc-service sshd start >/dev/null 2>&1 || true
        sleep 1
        if is_port_listening "$PORT"; then
            return 0
        fi
        info "OpenRC start failed, falling back to direct sshd"
    fi

    if nohup env PROOT_NO_SECCOMP=1 /usr/sbin/sshd -D -e -f /etc/ssh/sshd_config -p "$PORT" -o StrictModes=no -o PasswordAuthentication=yes >>/var/log/samsara-sshd.log 2>&1 & then
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

install_samsara_init() {
    cat > /usr/local/bin/samsara-init <<'EOS'
#!/bin/sh
set -e
# #COMPLETION_DRIVE: Ensure baseline services are running on every Alpine session without requiring full OpenRC boot
# #SUGGEST_VERIFY: After login, ss -ltn | grep :222 shows sshd listening; rc-service sshd status returns started if OpenRC available

PORT=222

if command -v openrc >/dev/null 2>&1; then
    mkdir -p /run/openrc 2>/dev/null || true
    [ -f /run/openrc/softlevel ] || echo default > /run/openrc/softlevel 2>/dev/null || true
    openrc default >/dev/null 2>&1 || true
fi

if command -v rc-service >/dev/null 2>&1; then
    rc-service sshd status >/dev/null 2>&1 || rc-service sshd start >/dev/null 2>&1 || true
fi

if command -v ss >/dev/null 2>&1; then
    if ! ss -ltn 2>/dev/null | awk '{print $4}' | grep -E "(:|^)${PORT}$" -q; then
        nohup /usr/sbin/sshd -D -e -f /etc/ssh/sshd_config -p "$PORT" -o StrictModes=no -o PasswordAuthentication=yes >>/var/log/samsara-sshd.log 2>&1 &
        sleep 1
    fi
fi

exec /bin/sh -l
EOS
    chmod 0755 /usr/local/bin/samsara-init || true
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
    install_samsara_init
    info "Alpine setup complete"
    summary
}

main "$@"
