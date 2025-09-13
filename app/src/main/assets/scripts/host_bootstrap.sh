#!/bin/sh
set -e

LOG_DIR="$HOME/.samsara"
LOG="$LOG_DIR/bootstrap.log"
DEBUG_LOG="$LOG_DIR/debug.log"
mkdir -p "$LOG_DIR"

# Initialize logs with timestamp
echo "=== SamsaraServer Bootstrap Started at $(date) ===" > "$LOG"
echo "=== Debug Log Started at $(date) ===" > "$DEBUG_LOG"

ERR_NO_INTERNET=1
ERR_PKG_UPDATE=2
ERR_PKG_INSTALL=3
ERR_ALPINE_INSTALL=4
ERR_SCRIPT_COPY=5
ERR_DPKG_LOCKED=6
ERR_INSUFFICIENT_SPACE=7
ERR_SCRIPT_VALIDATION=8

SPIN_CHARS='-\\|/'
spinner_pid=0

debug_log() {
    echo "[DEBUG $(date '+%H:%M:%S')] $*" >> "$DEBUG_LOG"
    echo "[DEBUG] $*" >&2
}

error_exit() {
    stop_spinner
    local err_code=$1
    local err_msg="$2"
    echo "[!] FATAL: $err_msg" | tee -a "$LOG"
    debug_log "FATAL ERROR: $err_msg (exit code: $err_code)"
    
    # Show final error summary
    printf "\n=== ERROR SUMMARY ===\n"
    printf "Error: %s\n" "$err_msg"
    printf "Check logs: %s\n" "$LOG" 
    printf "Debug info: %s\n" "$DEBUG_LOG"
    printf "=====================\n"
    
    exit $err_code
}

validate_environment() {
    debug_log "Validating environment..."
    
    # Check if we're in Termux
    if [ -z "$PREFIX" ]; then
        error_exit $ERR_SCRIPT_VALIDATION "PREFIX environment variable not set - not running in Termux"
    fi
    
    # Check if basic commands exist
    if ! command -v pkg >/dev/null 2>&1; then
        error_exit $ERR_SCRIPT_VALIDATION "pkg command not found - Termux environment invalid"
    fi
    
    debug_log "Environment validation passed"
    echo "PREFIX: $PREFIX" >> "$LOG"
    echo "HOME: $HOME" >> "$LOG"
}

start_spinner() {
    debug_log "Starting spinner: $1"
	printf "\r[*] %s " "$1"
	(
		i=0
		while true; do
			i=$(( (i + 1) % 4 ))
			c=$(printf %s "$SPIN_CHARS" | cut -c $((i+1)))
			printf "\r[*] %s %s" "$1" "$c"
			sleep 0.1
		done
	) &
	spinner_pid=$!
}

stop_spinner() {
	if [ "$spinner_pid" -ne 0 ]; then
		kill "$spinner_pid" 2>/dev/null || true
		wait "$spinner_pid" 2>/dev/null || true
		spinner_pid=0
	fi
}

task_success() {
	stop_spinner
	printf "\r[✓] %s\n" "$1"
    debug_log "SUCCESS: $1"
}

task_fail() {
	stop_spinner
	printf "\r[!] %s\n" "$1"
    debug_log "FAILED: $1"
}

check_internet() {
    debug_log "Checking internet connectivity..."
    
    # Test multiple connectivity methods
    if ping -c 1 -W 3 8.8.8.8 >/dev/null 2>&1; then
        debug_log "Ping to 8.8.8.8 successful"
        echo "Internet: ping 8.8.8.8 OK" >> "$LOG"
    elif ping -c 1 -W 3 1.1.1.1 >/dev/null 2>&1; then
        debug_log "Ping to 1.1.1.1 successful"
        echo "Internet: ping 1.1.1.1 OK" >> "$LOG"
    else
        debug_log "Ping tests failed, checking package manager access"
        echo "Internet: ping tests failed" >> "$LOG"
    fi
    
    # Test package manager connectivity
    if command -v pkg >/dev/null 2>&1; then
        debug_log "Testing pkg connectivity"
        if pkg list-all >/dev/null 2>&1; then
            debug_log "pkg list-all successful"
            echo "Internet: pkg access OK" >> "$LOG"
            return 0
        else
            debug_log "pkg list-all failed"
            echo "Internet: pkg access failed" >> "$LOG"
        fi
    fi
    
    # Test HTTP connectivity
    if command -v curl >/dev/null 2>&1; then
        debug_log "Testing curl connectivity"
        if curl -s --connect-timeout 5 --max-time 10 "https://packages.termux.org" >/dev/null 2>&1; then
            debug_log "curl test successful"
            echo "Internet: curl test OK" >> "$LOG"
            return 0
        else
            debug_log "curl test failed"
            echo "Internet: curl test failed" >> "$LOG"
        fi
    fi
    
    if command -v wget >/dev/null 2>&1; then
        debug_log "Testing wget connectivity"
        if wget -q --timeout=10 --tries=1 "https://packages.termux.org" -O /dev/null 2>&1; then
            debug_log "wget test successful"
            echo "Internet: wget test OK" >> "$LOG"
            return 0
        else
            debug_log "wget test failed"
            echo "Internet: wget test failed" >> "$LOG"
        fi
    fi
    
    debug_log "All internet connectivity tests failed"
    echo "Internet: all tests failed" >> "$LOG"
    return 1
}

check_dpkg_locked() {
    debug_log "Checking dpkg state"
    
    # Check for lock files
    if [ -f /var/lib/dpkg/lock-frontend ] || [ -f /var/lib/dpkg/lock ]; then
        debug_log "dpkg lock files detected"
        return 0
    fi
    
    # Check for interrupted dpkg (this is the key issue!)
    if command -v dpkg >/dev/null 2>&1; then
        if dpkg --audit 2>/dev/null | grep -q "unconfigured"; then
            debug_log "dpkg has unconfigured packages"
            return 0
        fi
        
        # Test if pkg upgrade works to detect dpkg interruption
        debug_log "Testing pkg upgrade to detect dpkg issues"
        if ! pkg upgrade --dry-run >/dev/null 2>&1; then
            # Check if the error mentions dpkg configuration
            local pkg_error=$(pkg upgrade --dry-run 2>&1)
            if echo "$pkg_error" | grep -q "dpkg.*configure.*-a"; then
                debug_log "dpkg interruption detected via pkg upgrade test"
                echo "dpkg interruption detected: $pkg_error" >> "$LOG"
                return 0
            fi
        fi
    fi
    
    debug_log "dpkg state appears normal"
    return 1
}

fix_dpkg_state() {
    start_spinner "Fixing dpkg state"
    debug_log "Attempting to fix dpkg interrupted state"
    
    if command -v dpkg >/dev/null 2>&1; then
        echo "Running dpkg --configure -a to fix interrupted state" >> "$LOG"
        debug_log "Running dpkg --configure -a"
        
        if dpkg --configure -a >>"$LOG" 2>&1; then
            task_success "dpkg state fixed"
            debug_log "dpkg --configure -a succeeded"
            
            # Test if pkg commands work now
            if pkg upgrade --dry-run >/dev/null 2>&1; then
                debug_log "pkg upgrade test passed after dpkg fix"
                echo "pkg upgrade test successful after dpkg fix" >> "$LOG"
                return 0
            else
                debug_log "pkg upgrade test still failing after dpkg fix"
                echo "WARNING: pkg upgrade still has issues after dpkg fix" >> "$LOG"
                return 1
            fi
        else
            task_fail "Failed to fix dpkg state"
            debug_log "dpkg --configure -a failed"
            echo "dpkg --configure -a failed" >> "$LOG"
            return 1
        fi
    else
        task_fail "dpkg command not available"
        debug_log "dpkg command not found"
        return 1
    fi
}

update_packages() {
	start_spinner "Updating package repositories"
    debug_log "Starting package update process"
    
	if ! check_internet; then
		error_exit $ERR_NO_INTERNET "No internet connection available"
	fi
    
	if check_dpkg_locked; then
        debug_log "dpkg is locked, attempting to fix"
		if ! fix_dpkg_state; then
			error_exit $ERR_DPKG_LOCKED "Cannot fix dpkg locked state"
		fi
	fi
    
    debug_log "Running pkg update"
	if pkg update -y >>"$LOG" 2>&1; then
		task_success "Package repositories updated"
        debug_log "Package update completed successfully"
	else
        debug_log "Package update failed, checking for recovery options"
		if check_dpkg_locked; then
            debug_log "dpkg locked after update failure, attempting fix"
			if fix_dpkg_state; then
                debug_log "Retrying package update after dpkg fix"
				if pkg update -y >>"$LOG" 2>&1; then
					task_success "Package repositories updated (after fixing dpkg)"
                    debug_log "Package update succeeded after dpkg fix"
					return 0
				fi
			fi
		fi
		error_exit $ERR_PKG_UPDATE "Failed to update package repositories"
	fi
}

upgrade_packages() {
	start_spinner "Upgrading packages"
    debug_log "Starting package upgrade process"
    
    # Always check and fix dpkg state first
    if check_dpkg_locked; then
        debug_log "dpkg issues detected, fixing before upgrade"
        if ! fix_dpkg_state; then
            task_fail "Cannot fix dpkg state - upgrade may fail"
            debug_log "dpkg fix failed, but attempting upgrade anyway"
        fi
    fi
    
    debug_log "Running pkg upgrade"
	if pkg upgrade -y >>"$LOG" 2>&1; then
		task_success "Packages upgraded"
        debug_log "Package upgrade completed successfully"
        return 0
	else
        local upgrade_exit_code=$?
        debug_log "Package upgrade failed with exit code: $upgrade_exit_code"
		
        # Check if it's the dpkg interruption issue
        if pkg upgrade --dry-run 2>&1 | grep -q "dpkg.*configure.*-a"; then
            debug_log "dpkg interruption detected during upgrade failure"
            task_fail "dpkg interruption detected - attempting fix"
            
            if fix_dpkg_state; then
                debug_log "Retrying upgrade after dpkg fix"
                if pkg upgrade -y >>"$LOG" 2>&1; then
                    task_success "Packages upgraded (after dpkg fix)"
                    debug_log "Package upgrade succeeded after dpkg fix"
                    return 0
                fi
            fi
        fi
        
        task_fail "Package upgrade failed - continuing without upgrade"
        debug_log "WARNING: Continuing bootstrap without package upgrade"
        echo "=== UPGRADE FAILED ===" >> "$LOG"
        echo "Exit code: $upgrade_exit_code" >> "$LOG"
        echo "Continuing without upgrade" >> "$LOG"
        echo "=== END UPGRADE FAILURE ===" >> "$LOG"
        return 1
	fi
}

install_proot() {
	start_spinner "Installing proot-distro"
    debug_log "Starting proot-distro installation"
    
    # Check if already installed
    if command -v proot-distro >/dev/null 2>&1; then
        task_success "proot-distro already installed"
        debug_log "proot-distro already available"
        return 0
    fi
    
    # Check and fix dpkg state before installation
    if check_dpkg_locked; then
        debug_log "dpkg issues detected, fixing before proot-distro install"
        if ! fix_dpkg_state; then
            task_fail "Cannot fix dpkg state before installation"
            debug_log "dpkg fix failed, but attempting install anyway"
        fi
    fi
    
    debug_log "Running pkg install proot-distro"
	if pkg install -y proot-distro >>"$LOG" 2>&1; then
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
        
        # Check if it's the dpkg interruption issue
        if pkg install --dry-run proot-distro 2>&1 | grep -q "dpkg.*configure.*-a"; then
            debug_log "dpkg interruption detected during proot-distro install"
            task_fail "dpkg interruption detected - attempting fix"
            
            if fix_dpkg_state; then
                debug_log "Retrying proot-distro install after dpkg fix"
                if pkg install -y proot-distro >>"$LOG" 2>&1; then
                    if command -v proot-distro >/dev/null 2>&1; then
                        task_success "proot-distro installed (after dpkg fix)"
                        debug_log "proot-distro installation succeeded after dpkg fix"
                        return 0
                    fi
                fi
            fi
        fi
        
        # Try basic package update and retry
        debug_log "Updating package cache and retrying"
        if pkg update >>"$LOG" 2>&1 && pkg install -y proot-distro >>"$LOG" 2>&1; then
            if command -v proot-distro >/dev/null 2>&1; then
                task_success "proot-distro installed (after update)"
                debug_log "Installation succeeded after package update"
                return 0
            fi
        fi
        
        echo "=== PROOT-DISTRO INSTALL FAILED ===" >> "$LOG"
        echo "Exit code: $install_exit_code" >> "$LOG"
        echo "Last 20 lines:" >> "$LOG"
        tail -20 "$LOG" >> "$LOG" 2>/dev/null
        echo "=== END INSTALL ERROR ===" >> "$LOG"
        
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
			printf "Insufficient disk space: %d MB available, %d MB required\n" "$AVAILABLE_MB" "$REQUIRED_MB" >&2
			return 1
		fi
	fi
	return 0
}

setup_alpine() {
    debug_log "Starting Alpine Linux setup"
    
	if [ ! -d "$PREFIX/var/lib/proot-distro/installed-rootfs/alpine" ]; then
		start_spinner "Installing Alpine Linux"
        debug_log "Alpine not found, performing fresh installation"
        
		if ! check_disk_space; then
            debug_log "Insufficient disk space for Alpine installation"
			error_exit $ERR_ALPINE_INSTALL "Insufficient disk space for Alpine installation"
		fi
        
        debug_log "Running proot-distro install alpine"
		if proot-distro install alpine >>"$LOG" 2>&1; then
			if [ -d "$PREFIX/var/lib/proot-distro/installed-rootfs/alpine" ]; then
				task_success "Alpine Linux installed"
                debug_log "Alpine Linux installation and verification successful"
			else
                debug_log "Alpine installation succeeded but directory not created"
				error_exit $ERR_ALPINE_INSTALL "Alpine directory not created after installation"
			fi
		else
            debug_log "Alpine installation failed"
			error_exit $ERR_ALPINE_INSTALL "Alpine installation failed"
		fi
	else
		start_spinner "Updating Alpine packages"
        debug_log "Alpine found, updating existing installation"
        
		if proot-distro login alpine -- sh -lc 'apk update >/dev/null 2>&1 && apk upgrade >/dev/null 2>&1' >>"$LOG" 2>&1; then
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
	for f in setup; do
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
		sed -i "s/\r$//" /root/scripts/setup 2>/dev/null || true
		chmod 0755 /root/scripts/setup || true
		mkdir -p /usr/local/bin
		cp -f /root/scripts/setup /usr/local/bin/setup || install -m 0755 /root/scripts/setup /usr/local/bin/setup
		' >>"$LOG" 2>&1; then
			task_success "Setup scripts installed"
            debug_log "Scripts successfully installed to Alpine"
		else
            debug_log "Script installation to Alpine failed"
			error_exit $ERR_SCRIPT_COPY "Script installation to Alpine failed"
		fi
	else
        debug_log "No setup scripts found to copy"
		error_exit $ERR_SCRIPT_COPY "No setup scripts found in $HOME/scripts"
	fi
}

trap 'stop_spinner; debug_log "Script terminated by signal"' EXIT INT TERM

# Start with environment validation
printf "\n=== SamsaraServer Alpine Bootstrap ===\n"
debug_log "Bootstrap script starting"
validate_environment

update_packages

# Try upgrade, but continue if it fails (since update succeeded)
upgrade_success=1
if ! upgrade_packages; then
    debug_log "Package upgrade failed, but continuing with current packages"
    printf "\n[WARNING] Package upgrade failed, continuing with current packages\n"
    echo "WARNING: Proceeding without package upgrade" >> "$LOG"
    upgrade_success=0
fi

# If upgrade failed, try installing essential packages individually
if [ "$upgrade_success" -eq 0 ]; then
    debug_log "Installing essential packages individually due to upgrade failure"
    printf "[*] Installing essential packages individually...\n"
    
    essential_packages="wget curl tar gzip"
    for pkg_name in $essential_packages; do
        if ! command -v "$pkg_name" >/dev/null 2>&1; then
            debug_log "Installing essential package: $pkg_name"
            if pkg install -y "$pkg_name" >>"$LOG" 2>&1; then
                debug_log "Successfully installed: $pkg_name"
            else
                debug_log "Failed to install: $pkg_name (continuing anyway)"
                echo "WARNING: Failed to install $pkg_name" >> "$LOG"
            fi
        else
            debug_log "Essential package already available: $pkg_name"
        fi
    done
fi

install_proot
setup_alpine
copy_scripts

debug_log "Bootstrap completed successfully"
printf "\n=== Bootstrap Complete ===\n"

show_logs() {
	printf "\n=== SAMSARA SERVER BOOTSTRAP LOG ===\n"
	if [ -f "$LOG" ]; then
		tail -30 "$LOG" 2>/dev/null || cat "$LOG" 2>/dev/null || echo "Cannot read log file"
	else
		echo "Log file not found: $LOG"
	fi
	printf "=== END LOG ===\n"
}

printf "Connect: ssh root@<phone-ip> -p 2222 (password: server)\n"
printf "To view bootstrap logs: cat %s\n" "$LOG"
printf "Debug logs: cat %s\n" "$DEBUG_LOG"
printf "Quick log view: tail -20 %s\n" "$LOG"

# If we reach here with proot-distro, launch Alpine
if command -v proot-distro >/dev/null 2>&1 && [ -d "$PREFIX/var/lib/proot-distro/installed-rootfs/alpine" ]; then
    printf "\n[*] Launching Alpine Linux...\n"
    exec proot-distro login alpine -- /bin/sh -l
else
    printf "\n[!] Alpine not available - staying in Termux shell\n"
    printf "You can manually install proot-distro with: pkg install proot-distro\n"
    printf "Then install Alpine with: proot-distro install alpine\n"
    exec /bin/sh -l
fi