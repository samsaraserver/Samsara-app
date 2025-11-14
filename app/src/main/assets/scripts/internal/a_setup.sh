#!/bin/sh
set -e

# #COMPLETION_DRIVE: Assuming this script runs inside Alpine (proot-distro login alpine) as root
# #SUGGEST_VERIFY: Run `cat /etc/alpine-release` and `id -u` should be 0 before continuing

DEFAULT_SSH_PORT="${SAMSARA_SSH_PORT:-222}"
FALLBACK_SSH_PORT="${SAMSARA_SSH_FALLBACK_PORT:-8022}"
PORT="$DEFAULT_SSH_PORT"
# #COMPLETION_DRIVE: Assuming fallback port >=1024 is always allowed inside proot even if privileged ports are blocked
# #SUGGEST_VERIFY: When fallback triggers, run `ss -ltn | grep :$FALLBACK_SSH_PORT` to confirm listening socket
SSHD_BIN="${SSHD_BIN:-/usr/sbin/sshd}"
SSHD_LOG="/var/log/samsara-sshd.log"
SAMSARA_PORT_FILE="/etc/samsara_port"

SAMSARA_HUB_REPO="https://github.com/samsaraserver/Samsara-hub.git"
SAMSARA_HUB_BRANCH="${SAMSARA_HUB_BRANCH:-main}"
SAMSARA_HUB_ROOT="/opt"
SAMSARA_HUB_DIR="$SAMSARA_HUB_ROOT/Samsara-hub"
SAMSARA_HUB_SERVICE_BIN="/usr/local/bin/samsara-hub-service"
SAMSARA_HUB_LOCALD_SCRIPT="/etc/local.d/samsara_hub.start"
SAMSARA_HUB_LOG="/var/log/samsara-hub.log"
SAMSARA_HUB_PID="/var/run/samsara-hub.pid"

# #COMPLETION_DRIVE: Assuming /opt persists between Alpine sessions inside proot container
# #SUGGEST_VERIFY: Run "mkdir -p /opt && touch /opt/.rw-test" before setup; delete the file afterwards to confirm write access

info() { echo "[INFO] $*"; }
fail() { echo "[!] $*" 1>&2; exit 1; }

install_packages() {
    # #COMPLETION_DRIVE: Install required packages and OpenRC; prefer ss for port checks
    # #SUGGEST_VERIFY: Run `sshd -V`, `node -v`, `deno --version`, `rc-service --version || true`
    apk update || true
    apk add --no-cache \
        openssh nano htop nodejs npm deno curl ca-certificates git bash procps \
        openrc iproute2 net-tools || fail "Package installation failed"
}

ensure_sshd_binary() {
    if [ ! -x "$SSHD_BIN" ]; then
        fail "sshd binary missing at $SSHD_BIN"
    fi
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
    persist_ssh_port
}

persist_ssh_port() {
    echo "$PORT" > "$SAMSARA_PORT_FILE" 2>/dev/null || true
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
    ensure_sshd_binary
    if ! "$SSHD_BIN" -t -f /etc/ssh/sshd_config >/dev/null 2>&1; then
        diagnose_sshd_failure
        fail "sshd configuration test failed"
    fi

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
PORT_FILE="/etc/samsara_port"
if [ -f "$PORT_FILE" ]; then
        PORT=$(cat "$PORT_FILE" 2>/dev/null || echo __PORT_FALLBACK__)
else
        PORT=__PORT_FALLBACK__
fi
tries=0
while [ $tries -lt 3 ]; do
    if command -v ss >/dev/null 2>&1; then
        ss -ltn 2>/dev/null | awk '{print $4}' | grep -E "(:|^)${PORT}$" -q && exit 0
    fi
    if command -v rc-service >/dev/null 2>&1; then
        rc-service sshd status >/dev/null 2>&1 || rc-service sshd start >/dev/null 2>&1 || true
    fi
    sleep 1
    tries=$((tries+1))
done
if ! ss -ltn 2>/dev/null | awk '{print $4}' | grep -E "(:|^)${PORT}$" -q; then
    nohup /usr/sbin/sshd -D -e -f /etc/ssh/sshd_config -p "$PORT" -o StrictModes=no -o PasswordAuthentication=yes >>/var/log/samsara-sshd.log 2>&1 &
fi
EOS
        sed -i "s/__PORT_FALLBACK__/$FALLBACK_SSH_PORT/g" /etc/local.d/samsara.start
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
            persist_ssh_port
            return 0
        fi
        info "OpenRC start failed, falling back to direct sshd"
    fi

    if nohup env PROOT_NO_SECCOMP=1 "$SSHD_BIN" -D -e -f /etc/ssh/sshd_config -p "$PORT" -o StrictModes=no -o PasswordAuthentication=yes >>"$SSHD_LOG" 2>&1 & then
        sleep 1
        if is_port_listening "$PORT"; then
            persist_ssh_port
            return 0
        fi
        diagnose_sshd_failure
        if should_switch_to_fallback; then
            if switch_to_fallback_port; then
                start_sshd
                return
            fi
        fi
        fail "SSH server not listening on port $PORT"
    else
        diagnose_sshd_failure
        if should_switch_to_fallback && switch_to_fallback_port; then
            start_sshd
            return
        fi
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
# #SUGGEST_VERIFY: After login, ss -ltn | grep :${PORT} shows sshd listening; rc-service sshd status returns started if OpenRC available

PORT_FILE="/etc/samsara_port"
if [ -f "$PORT_FILE" ]; then
    PORT=$(cat "$PORT_FILE" 2>/dev/null || echo __PORT_FALLBACK__)
else
    PORT=__PORT_FALLBACK__
fi

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
    sed -i "s/__PORT_FALLBACK__/$FALLBACK_SSH_PORT/g" /usr/local/bin/samsara-init
    chmod 0755 /usr/local/bin/samsara-init || true
}

diagnose_sshd_failure() {
    echo "[!] sshd failed to start; diagnostics follow"
    if [ -s "$SSHD_LOG" ]; then
        echo "----- tail $SSHD_LOG -----"
        tail -n 40 "$SSHD_LOG"
        echo "----- end sshd log -----"
    else
        echo "No sshd log output captured at $SSHD_LOG"
    fi
    echo "----- sshd config test -----"
    if ! "$SSHD_BIN" -t -f /etc/ssh/sshd_config 2>&1; then
        echo "sshd -t reported errors (see above)"
    else
        echo "sshd -t returned success"
    fi
}

should_switch_to_fallback() {
    if [ "$PORT" = "$FALLBACK_SSH_PORT" ]; then
        return 1
    fi
    grep -qi "Permission denied" "$SSHD_LOG" 2>/dev/null
}

switch_to_fallback_port() {
    info "Permission denied binding to port $PORT, switching to fallback port $FALLBACK_SSH_PORT"
    PORT="$FALLBACK_SSH_PORT"
    setup_ssh
    return 0
}

install_bun_runtime() {
    if command -v bun >/dev/null 2>&1; then
        info "Bun already installed"
        return
    fi

    # #COMPLETION_DRIVE: Assuming Bun installer supports non-interactive execution inside Alpine root shell
    # #SUGGEST_VERIFY: Run `bun --version` after setup to confirm availability

    if ! command -v curl >/dev/null 2>&1; then
        fail "curl missing; cannot install Bun"
    fi

    info "Installing Bun runtime"
    if curl -fsSL https://bun.com/install | bash; then
        if [ -x /root/.bun/bin/bun ]; then
            ln -sf /root/.bun/bin/bun /usr/local/bin/bun 2>/dev/null || true
        fi
    else
        fail "Bun installation failed"
    fi
}

install_samsara_hub_service() {
    info "Installing Samsara Hub service controller"
    mkdir -p "$SAMSARA_HUB_ROOT" /var/log /var/run || true
    cat > "$SAMSARA_HUB_SERVICE_BIN" <<'EOS'
#!/bin/sh
set -e

REPO_URL="https://github.com/samsaraserver/Samsara-hub.git"
BRANCH="${SAMSARA_HUB_BRANCH:-main}"
SERVICE_DIR="/opt/Samsara-hub"
START_SCRIPT="start.sh"
LOG_FILE="/var/log/samsara-hub.log"
PID_FILE="/var/run/samsara-hub.pid"

# #COMPLETION_DRIVE: Assuming procps provides pgrep/pkill binaries inside Alpine base image
# #SUGGEST_VERIFY: Run `apk add procps` if service stop/start commands fail
# #COMPLETION_DRIVE: Assuming Samsara Hub start.sh expects Bun runtime on PATH
# #SUGGEST_VERIFY: Run `bun --version` before invoking this controller

log() {
    printf "[SamsaraHub] %s\n" "$1"
}

ensure_git() {
    command -v git >/dev/null 2>&1 || {
        log "git missing; install it with apk add git";
        exit 1;
    }
}

sync_repo() {
    mkdir -p "$SERVICE_DIR"
    if [ -d "$SERVICE_DIR/.git" ]; then
        log "Updating Samsara Hub repo (branch $BRANCH)"
        git -C "$SERVICE_DIR" fetch origin "$BRANCH" --depth=1 --prune
        git -C "$SERVICE_DIR" reset --hard "origin/$BRANCH"
        git -C "$SERVICE_DIR" clean -xfd || true
    else
        log "Cloning Samsara Hub repo (branch $BRANCH)"
        rm -rf "$SERVICE_DIR"
        git clone --depth=1 --branch "$BRANCH" "$REPO_URL" "$SERVICE_DIR"
    fi
}

stop_service() {
    if [ -f "$PID_FILE" ]; then
        pid=$(cat "$PID_FILE" 2>/dev/null || true)
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
            sleep 1
        fi
        rm -f "$PID_FILE"
    fi
    pkill -f "$SERVICE_DIR/$START_SCRIPT" 2>/dev/null || true
}

start_service() {
    sync_repo
    script_path="$SERVICE_DIR/$START_SCRIPT"
    if [ ! -f "$script_path" ]; then
        log "start.sh missing in $SERVICE_DIR"
        exit 1
    fi
    if ! command -v bun >/dev/null 2>&1; then
        log "Bun runtime missing; cannot start service"
        exit 1
    fi
    chmod +x "$script_path" || true
    mkdir -p /var/log /var/run
    nohup sh "$script_path" >> "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    log "Samsara Hub started (PID $(cat "$PID_FILE"))"
}

status_service() {
    if [ -f "$PID_FILE" ]; then
        pid=$(cat "$PID_FILE")
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            log "Running (PID $pid)"
            exit 0
        fi
    fi
    if pgrep -f "$SERVICE_DIR/$START_SCRIPT" >/dev/null 2>&1; then
        log "Running (pid via scan)"
        exit 0
    fi
    log "Not running"
    exit 1
}

case "$1" in
    sync)
        ensure_git
        sync_repo
        ;;
    start|"" )
        ensure_git
        stop_service
        start_service
        ;;
    stop)
        stop_service
        ;;
    restart)
        ensure_git
        stop_service
        start_service
        ;;
    status)
        status_service
        ;;
    *)
        echo "Usage: $0 {sync|start|stop|restart|status}" 1>&2
        exit 1
        ;;
esac
EOS
    chmod 0755 "$SAMSARA_HUB_SERVICE_BIN" || true
}

install_samsara_hub_locald() {
    mkdir -p /etc/local.d || true
    # #COMPLETION_DRIVE: Using OpenRC local.d hook to auto-start Samsara Hub at boot
    # #SUGGEST_VERIFY: Execute `rc-service local restart` and confirm /var/log/samsara-hub-boot.log updates
    cat > "$SAMSARA_HUB_LOCALD_SCRIPT" <<'EOS'
#!/bin/sh
if [ -x /usr/local/bin/samsara-hub-service ]; then
    /usr/local/bin/samsara-hub-service restart >/var/log/samsara-hub-boot.log 2>&1 || true
fi
EOS
    chmod 0755 "$SAMSARA_HUB_LOCALD_SCRIPT" || true
}

prepare_samsara_hub() {
    info "Preparing Samsara Hub service"
    install_bun_runtime
    install_samsara_hub_service
    "$SAMSARA_HUB_SERVICE_BIN" sync || fail "Failed to sync Samsara Hub"
    install_samsara_hub_locald
    "$SAMSARA_HUB_SERVICE_BIN" restart || fail "Failed to start Samsara Hub"
}

summary() {
    ip=""
    if command -v ip >/dev/null 2>&1; then
        ip=$(ip route get 1.1.1.1 2>/dev/null | sed -n 's/.*src \([0-9.]*\).*/\1/p')
    fi
    [ -z "$ip" ] && ip=$(hostname -i 2>/dev/null | awk '{print $1}')
    case "$ip" in 127.*|0.0.0.0|"") ip="<phone-ip>";; esac
    current_port=$(cat "$SAMSARA_PORT_FILE" 2>/dev/null || echo "$PORT")
    echo "SSH ready on port $current_port"
    echo "Connect: ssh root@${ip} -p ${current_port} (password: server)"
    if [ "$current_port" != "$DEFAULT_SSH_PORT" ]; then
        echo "Note: Fallback SSH port engaged due to privileged port restrictions."
    fi
    echo "Samsara Hub directory: $SAMSARA_HUB_DIR"
    echo "Service binary: $SAMSARA_HUB_SERVICE_BIN"
}

main() {
    install_packages
    setup_users
    setup_ssh
    start_sshd
    ensure_config_dir
    install_samsara_init
    prepare_samsara_hub
    info "Alpine setup complete"
    summary
}

main "$@"
