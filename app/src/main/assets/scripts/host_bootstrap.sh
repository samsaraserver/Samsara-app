#!/bin/sh
set -e

ERR_NO_INTERNET=1
ERR_PKG_UPDATE=2
ERR_PKG_INSTALL=3
ERR_ALPINE_INSTALL=4
ERR_SCRIPT_COPY=5
ERR_DPKG_LOCKED=6
ERR_INSUFFICIENT_SPACE=7
ERR_SCRIPT_VALIDATION=8

spinner_pid=0
bootstrap_had_errors=0

debug_log() {
    echo "[DEBUG $(date '+%H:%M:%S')] $*"
}

error_exit() {
    stop_spinner
    local err_code=$1
    local err_msg="$2"
    bootstrap_had_errors=1
    echo "[!] FATAL: $err_msg"
    debug_log "FATAL ERROR: $err_msg (exit code: $err_code)"
    echo
    echo "=== ERROR SUMMARY ==="
    echo "Error: $err_msg"
    echo "====================="
    
    exit $err_code
}

validate_environment() {
    debug_log "Validating environment..."
    
    if [ -z "$PREFIX" ]; then
        error_exit $ERR_SCRIPT_VALIDATION "PREFIX environment variable not set - not running in Termux"
    fi
    
    if ! command -v pkg >/dev/null 2>&1; then
        error_exit $ERR_SCRIPT_VALIDATION "pkg command not found - Termux environment invalid"
    fi
    
    debug_log "Environment validation passed"
    echo "PREFIX: $PREFIX"
    echo "HOME: $HOME"
}

ensure_noninteractive() {
    export DEBIAN_FRONTEND=noninteractive
    export APT_LISTCHANGES_FRONTEND=none
}

ensure_ipv4_only() {
    CONF_DIR="$PREFIX/etc/apt/apt.conf.d"
    mkdir -p "$CONF_DIR"
    echo 'Acquire::ForceIPv4 "true";' > "$CONF_DIR/99force-ipv4" 2>/dev/null || true
}

ensure_apt_network_tuning() {
    CONF_DIR="$PREFIX/etc/apt/apt.conf.d"
    mkdir -p "$CONF_DIR"
    cat > "$CONF_DIR/50samsara-network" <<-'EOF'
Acquire::ForceIPv4 "true";
Acquire::Retries "1";
Acquire::http::Timeout "10";
Acquire::https::Timeout "10";
Acquire::ConnectTimeout "5";
Acquire::Languages "none";
APT::Get::Assume-Yes "true";
APT::Install-Recommends "false";
EOF
}

ensure_runtime_libs() {
    # #COMPLETION_DRIVE: Ensure apt-get can start by providing required runtime libs
    # #SUGGEST_VERIFY: After calling, run apt-get -v to confirm no missing .so errors
    if [ -n "$PREFIX" ] && [ -d "$PREFIX/lib" ]; then
        if [ ! -e "$PREFIX/lib/liblz4.so.1" ] && [ -e "$PREFIX/lib/liblz4.so" ]; then
            ln -sf "$PREFIX/lib/liblz4.so" "$PREFIX/lib/liblz4.so.1" 2>/dev/null || true
            echo "[INFO] Linked liblz4.so.1 -> liblz4.so"
        fi
    fi
}

apt_update_quick() {
    if command -v apt-get >/dev/null 2>&1; then
        if apt-get \
            -o Dpkg::Options::=--force-confnew \
            -o Dpkg::Options::=--force-confold \
            -o Acquire::http::Timeout=10 -o Acquire::https::Timeout=10 -o Acquire::ConnectTimeout=5 -o Acquire::Retries=1 \
            update; then
            return 0
        fi
        return 1
    fi
    pkg update -y
}

ensure_dpkg_clean() {
    attempts=0
    while [ $attempts -lt 2 ]; do
        attempts=$((attempts+1))
        echo "[INFO] Checking dpkg state (attempt $attempts)"
        dpkg --configure -a || true
        if command -v apt-get >/dev/null 2>&1; then
            if apt-get -o Acquire::Retries=1 -s upgrade 2>&1 | grep -q "dpkg was interrupted"; then
                echo "[INFO] Fixing interrupted dpkg: attempting -f install and reconfigure"
                apt-get -o Dpkg::Options::=--force-confnew -o Dpkg::Options::=--force-confold -f -y install || 
                dpkg --configure -a || true
                continue
            fi
        fi
        break
    done
}

switch_termux_mirrors_and_update() {
    start_spinner "Switching Termux mirrors"
    SRC_FILE="$PREFIX/etc/apt/sources.list"
    mkdir -p "$(dirname "$SRC_FILE")"
    [ -f "$SRC_FILE" ] && cp "$SRC_FILE" "$SRC_FILE.bak" 2>/dev/null || true

    MIRRORS="https://packages.termux.dev/apt/termux-main https://mirror.termux.dev/apt/termux-main https://grimler.se/termux/apt/termux-main"
    for MIR in $MIRRORS; do
        printf "\n[INFO] Trying Termux mirror: %s\n" "$MIR"
        printf "deb %s stable main\n" "$MIR" > "$SRC_FILE"
        if apt_update_quick; then
            task_success "Switched repo and updated"
            return 0
        fi
    done
    task_fail "All mirrors failed"
    if [ -f "$SRC_FILE.bak" ]; then
        cp "$SRC_FILE.bak" "$SRC_FILE" 2>/dev/null || true
    fi
    return 1
}

start_spinner() {
    echo "[*] $1"
}

stop_spinner() {
    :
}

task_success() {
    stop_spinner
    echo "[✓] $1"
}

task_fail() {
    stop_spinner
    echo "[!] $1"
    bootstrap_had_errors=1
}

check_internet() {
    if ping -c 1 -W 3 8.8.8.8 >/dev/null 2>&1 || ping -c 1 -W 3 1.1.1.1 >/dev/null 2>&1; then
        echo "Internet: ping test OK"
        return 0
    fi
    
    if command -v pkg >/dev/null 2>&1 && pkg list-all >/dev/null 2>&1; then
        echo "Internet: pkg access OK"
        return 0
    fi
    
    if command -v curl >/dev/null 2>&1 && curl -s --connect-timeout 5 --max-time 10 "https://packages.termux.org" >/dev/null 2>&1; then
        echo "Internet: curl test OK"
        return 0
    fi
    
    echo "Internet: all tests failed"
    return 1
}

check_dpkg_locked() {
    if [ -f /var/lib/dpkg/lock-frontend ] || [ -f /var/lib/dpkg/lock ]; then
        return 0
    fi
    
    if command -v dpkg >/dev/null 2>&1 && dpkg --audit 2>/dev/null | grep -q "unconfigured"; then
        return 0
    fi
    
    if ! pkg upgrade --dry-run >/dev/null 2>&1; then
        local pkg_error=$(pkg upgrade --dry-run 2>&1)
        if echo "$pkg_error" | grep -q "dpkg.*configure.*-a"; then
            echo "dpkg interruption detected: $pkg_error"
            return 0
        fi
    fi
    
    return 1
}

fix_dpkg_state() {
    start_spinner "Fixing dpkg state"
    
    if command -v dpkg >/dev/null 2>&1; then
        echo "Running dpkg --configure -a to fix interrupted state"
        
        if dpkg --configure -a; then
            task_success "dpkg state fixed"
            
            if pkg upgrade --dry-run >/dev/null 2>&1; then
                echo "pkg upgrade test successful after dpkg fix"
                return 0
            else
                echo "WARNING: pkg upgrade still has issues after dpkg fix"
                return 1
            fi
        else
            task_fail "Failed to fix dpkg state"
            echo "dpkg --configure -a failed"
            return 1
        fi
    else
        task_fail "dpkg command not available"
        return 1
    fi
}

update_packages() {
    start_spinner "Updating package repositories"
    

    if ! check_internet; then
        error_exit $ERR_NO_INTERNET "No internet connection available"
    fi

    if check_dpkg_locked; then
        if ! fix_dpkg_state; then
            error_exit $ERR_DPKG_LOCKED "Cannot fix dpkg locked state"
        fi
    fi
    
    ensure_ipv4_only
    ensure_noninteractive
    ensure_runtime_libs
    ensure_dpkg_clean
    ensure_apt_network_tuning
    if apt_update_quick; then
        task_success "Package repositories updated"
    else
        if check_dpkg_locked; then
            if fix_dpkg_state; then
                if apt_update_quick; then
                    task_success "Package repositories updated (after fixing dpkg)"
                    return 0
                fi
            fi
        fi
        if switch_termux_mirrors_and_update; then
            return 0
        fi
        error_exit $ERR_PKG_UPDATE "Failed to update package repositories"
    fi
}

upgrade_packages() {
    start_spinner "Upgrading packages"
    
    if check_dpkg_locked; then
        if ! fix_dpkg_state; then
            task_fail "Cannot fix dpkg state before upgrade"
            return 1
        fi
    fi
    

    if pkg upgrade -y; then
        task_success "Packages upgraded"
        return 0
    else
        if pkg upgrade --dry-run 2>&1 | grep -q "dpkg.*configure.*-a"; then
            task_fail "dpkg interruption detected - attempting fix"
            
            if fix_dpkg_state; then
                if pkg upgrade -y; then
                    task_success "Packages upgraded (after dpkg fix)"
                    return 0
                fi
            fi
        fi
        
        task_fail "Package upgrade failed - continuing without upgrade"
        return 1
    fi
}

install_proot() {
    start_spinner "Installing proot-distro"
    debug_log "Starting proot-distro installation"
    
    if command -v proot-distro >/dev/null 2>&1; then
        task_success "proot-distro already installed"
        debug_log "proot-distro already available"
        return 0
    fi
    
    ensure_noninteractive
    ensure_dpkg_clean
    if check_dpkg_locked; then
        debug_log "dpkg issues detected, fixing before proot-distro install"
        if ! fix_dpkg_state; then
            task_fail "Cannot fix dpkg state before installation"
            debug_log "dpkg fix failed, but attempting install anyway"
        fi
    fi
    
    debug_log "Running pkg install proot-distro"
    if pkg install -y proot-distro; then
        if command -v proot-distro >/dev/null 2>&1; then
            task_success "proot-distro installed"
            debug_log "proot-distro installation successful"
            return 0
        else
            debug_log "proot-distro package installed but command not available"
            task_fail "proot-distro command not available after installation"
        fi
    else
        local install_exit_code=$?
        debug_log "proot-distro installation failed with exit code: $install_exit_code"
        
        if apt-get -s install proot-distro 2>&1 | grep -q "dpkg was interrupted"; then
            debug_log "dpkg interruption detected during proot-distro install"
            task_fail "dpkg interruption detected - attempting fix"
            
            ensure_dpkg_clean
            if fix_dpkg_state; then
                debug_log "Retrying proot-distro install after dpkg fix"
                if pkg install -y proot-distro; then
                    if command -v proot-distro >/dev/null 2>&1; then
                        task_success "proot-distro installed (after dpkg fix)"
                        debug_log "proot-distro installation succeeded after dpkg fix"
                        return 0
                    fi
                fi
            fi
        fi
        
        debug_log "Updating package cache and retrying"
        if apt_update_quick && pkg install -y proot-distro; then
            if command -v proot-distro >/dev/null 2>&1; then
                task_success "proot-distro installed (after update)"
                debug_log "Installation succeeded after package update"
                return 0
            fi
        fi

        error_exit $ERR_PKG_INSTALL "proot-distro installation failed - check dpkg state manually"
    fi
}

check_disk_space() {
    REQUIRED_MB=1024
    if command -v df >/dev/null 2>&1; then
        AVAILABLE_KB=$(df "$PREFIX" 2>/dev/null | awk 'NR==2 {print $4}' 2>/dev/null)
        if [ -n "$AVAILABLE_KB" ] && [ "$AVAILABLE_KB" -gt 0 ]; then
            AVAILABLE_MB=$((AVAILABLE_KB / 1024))
            [ "$AVAILABLE_MB" -ge "$REQUIRED_MB" ] && return 0
            echo "Insufficient disk space: $AVAILABLE_MB MB available, $REQUIRED_MB MB required" >&2
            return 1
        fi
    fi
    return 0
}

setup_alpine() {
    debug_log "Starting Alpine Linux setup"

    alpine_installed=0
    if command -v proot-distro >/dev/null 2>&1; then
        if proot-distro list 2>/dev/null | grep -E '^alpine\b' >/dev/null 2>&1; then
            alpine_installed=1
        else
            # Probe by attempting a no-op login
            if proot-distro login alpine -- true >/dev/null 2>&1; then
                alpine_installed=1
            fi
        fi
    fi

    if [ "$alpine_installed" -eq 0 ]; then
        start_spinner "Installing Alpine Linux"
        debug_log "Alpine not found, performing fresh installation"

        if ! check_disk_space; then
            debug_log "Insufficient disk space for Alpine installation"
            error_exit $ERR_ALPINE_INSTALL "Insufficient disk space for Alpine installation"
        fi

        debug_log "Running proot-distro install alpine"
        if proot-distro install alpine; then
            # Verify with list or login instead of path-only checks
            if proot-distro list 2>/dev/null | grep -E '^alpine\b' >/dev/null 2>&1 || \
               proot-distro login alpine -- true >/dev/null 2>&1; then
                task_success "Alpine Linux installed"
                debug_log "Alpine Linux installation and verification successful"
            else
                debug_log "Alpine installation succeeded but verification failed"
                error_exit $ERR_ALPINE_INSTALL "Alpine verification failed after installation"
            fi
        else
            debug_log "Alpine installation failed"
            error_exit $ERR_ALPINE_INSTALL "Alpine installation failed"
        fi
    else
        start_spinner "Updating Alpine packages"
        debug_log "Alpine found, updating existing installation"

        if proot-distro login alpine -- sh -lc 'apk update && apk upgrade -U -a'; then
            task_success "Alpine packages updated"
            debug_log "Alpine packages updated successfully"
        else
            debug_log "Alpine update failed"
            error_exit $ERR_ALPINE_INSTALL "Alpine update failed"
        fi
    fi
}

copy_scripts() {
    start_spinner "Installing setup scripts"
    debug_log "Starting script installation to Alpine"
    
    mkdir -p "$HOME/scripts"
    TO_COPY=""
    for f in a_setup.sh samsara_config.json; do
        debug_log "Checking for script: $HOME/scripts/$f"
        if [ -f "$HOME/scripts/$f" ]; then
            TO_COPY="$TO_COPY $f"
            debug_log "Found script: $f"
        else
            debug_log "Script not found: $f"
        fi
    done

    if [ -n "$TO_COPY" ]; then
        debug_log "Copying scripts to Alpine: $TO_COPY"
        if tar -C "$HOME/scripts" -cf - $TO_COPY | proot-distro login alpine -- sh -lc '
            set -e
            mkdir -p /root/scripts
            tar -C /root/scripts -xpf -
            if [ -f /root/scripts/a_setup.sh ]; then
                sed -i "s/\r$//" /root/scripts/a_setup.sh 2>/dev/null || true
                chmod 0755 /root/scripts/a_setup.sh || true
                mkdir -p /usr/local/bin
                cp -f /root/scripts/a_setup.sh /usr/local/bin/a_setup || install -m 0755 /root/scripts/a_setup.sh /usr/local/bin/a_setup
            fi
            mkdir -p /root/.config/samsara
            if [ -f /root/scripts/samsara_config.json ]; then
                if [ ! -f /root/.config/samsara/samsara_config.json ]; then
                    cp -f /root/scripts/samsara_config.json /root/.config/samsara/samsara_config.json || true
                    chmod 0644 /root/.config/samsara/samsara_config.json || true
                fi
            fi
        ' ; then
            task_success "Setup scripts installed"
            debug_log "Scripts successfully installed to Alpine"
        else
            debug_log "Script installation to Alpine failed"
            error_exit $ERR_SCRIPT_COPY "Script installation to Alpine failed"
        fi
    else
        debug_log "No setup assets found to copy"
        error_exit $ERR_SCRIPT_COPY "No setup assets found in $HOME/scripts"
    fi
}

trap 'stop_spinner' EXIT INT TERM

echo
echo "======================================"
echo "    SamsaraServer Alpine Bootstrap    "
echo "======================================"
echo
validate_environment
ensure_noninteractive
ensure_runtime_libs

update_packages

if ! upgrade_packages; then
    echo
    echo "[WARNING] Package upgrade failed, continuing with current packages"
fi

install_proot
setup_alpine
copy_scripts

debug_log "Bootstrap completed successfully"
echo
echo "======================================"
echo "      Bootstrap Process Complete      "
echo "======================================"

if [ "$bootstrap_had_errors" -eq 0 ]; then
    echo
    echo "[SYSTEM] Bootstrap successful - preparing environment..."
    clear
    echo
    echo "╔══════════════════════════════════════╗"
    echo "║          SamsaraServer Ready         ║"
    echo "╚══════════════════════════════════════╝"
    echo
    echo "NEXT STEP: Inside Alpine, run 'a_setup' to install packages and configure SSH"
    echo "          and complete server initialization"
    echo
else
    echo
    echo "[WARNING] Bootstrap completed with warnings or errors"
    echo
    echo "Please review logs before proceeding"
fi

if command -v proot-distro >/dev/null 2>&1 && [ -d "$PREFIX/var/lib/proot-distro/installed-rootfs/alpine" ]; then
    echo "[SYSTEM] Initializing Alpine Linux environment..."
    echo "────────────────────────────────────────"
    exec proot-distro login alpine -- /bin/sh -l
else
    echo
    echo "[ERROR] Alpine Linux environment unavailable"
    echo "────────────────────────────────────────"
    echo "Manual Recovery Steps:"
    echo "  1. Install proot-distro: pkg install proot-distro"
    echo "  2. Install Alpine: proot-distro install alpine"
    echo "  3. Restart SamsaraServer application"
    echo
    exec /bin/sh -l
fi