#!/bin/bash
set -e

LOG_DIR="$HOME/.samsara"
LOG="$LOG_DIR/bootstrap.log"
mkdir -p "$LOG_DIR"

ERR_NO_INTERNET=1
ERR_PKG_UPDATE=2
ERR_PKG_INSTALL=3
ERR_DEBIAN_INSTALL=4
ERR_SCRIPT_COPY=5

SPIN_CHARS='-\\|/'
spinner_pid=0

start_spinner() {
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
}

task_fail() {
	stop_spinner
	printf "\r[!] %s\n" "$1"
}

check_internet() {
	if ping -c 1 -W 3 8.8.8.8 >/dev/null 2>&1 || ping -c 1 -W 3 1.1.1.1 >/dev/null 2>&1; then
		if command -v pkg >/dev/null 2>&1; then
			pkg list-all >/dev/null 2>&1 && return 0
		fi
		if command -v curl >/dev/null 2>&1; then
			curl -s --connect-timeout 5 --max-time 10 "https://packages.termux.org" >/dev/null 2>&1 && return 0
		fi
		if command -v wget >/dev/null 2>&1; then
			wget -q --timeout=10 --tries=1 "https://packages.termux.org" -O /dev/null 2>&1 && return 0
		fi
		return 0
	fi
	return 1
}

update_packages() {
	start_spinner "Updating package repositories"
	echo "[DEBUG] Starting package repository update..."
	if ! check_internet; then
		task_fail "No internet connection"
		exit $ERR_NO_INTERNET
	fi
	
	# Fix interrupted dpkg if needed
	echo "[DEBUG] Checking for interrupted dpkg..."
	if dpkg --configure -a >>"$LOG" 2>&1; then
		echo "[DEBUG] dpkg configuration completed"
	else
		echo "[DEBUG] dpkg configuration had issues, continuing anyway..."
	fi
	
	echo "[DEBUG] Internet connection confirmed, running: pkg update -y"
	if pkg update -y >>"$LOG" 2>&1; then
		echo "[DEBUG] Package update completed successfully"
		task_success "Package repositories updated"
	else
		echo "[DEBUG] Package update failed. Check log: $LOG"
		echo "[DEBUG] Last 10 lines of log:"
		tail -n 10 "$LOG" 2>/dev/null || echo "[DEBUG] Unable to read log file"
		task_fail "Failed to update packages"
		exit $ERR_PKG_UPDATE
	fi
}

upgrade_packages() {
	start_spinner "Upgrading packages"
	echo "[DEBUG] Starting package upgrade..."
	
	# Fix interrupted dpkg if needed
	echo "[DEBUG] Checking for interrupted dpkg before upgrade..."
	if dpkg --configure -a >>"$LOG" 2>&1; then
		echo "[DEBUG] dpkg configuration completed"
	else
		echo "[DEBUG] dpkg configuration had issues, continuing anyway..."
	fi
	
	if pkg upgrade -y >>"$LOG" 2>&1; then
		echo "[DEBUG] Package upgrade completed successfully"
		task_success "Packages upgraded"
	else
		echo "[DEBUG] Package upgrade failed. Check log: $LOG"
		echo "[DEBUG] Last 10 lines of log:"
		tail -n 10 "$LOG" 2>/dev/null || echo "[DEBUG] Unable to read log file"
		task_fail "Package upgrade failed"
		exit $ERR_PKG_INSTALL
	fi
}

install_proot() {
	start_spinner "Installing proot-distro"
	echo "[DEBUG] Starting proot-distro installation..."
	if pkg install -y proot-distro >>"$LOG" 2>&1; then
		if command -v proot-distro >/dev/null 2>&1; then
			echo "[DEBUG] proot-distro installed successfully and command is available"
			task_success "proot-distro installed"
		else
			echo "[DEBUG] proot-distro package installed but command not found"
			task_fail "proot-distro command not available"
			exit $ERR_PKG_INSTALL
		fi
	else
		echo "[DEBUG] proot-distro installation failed. Check log: $LOG"
		echo "[DEBUG] Last 10 lines of log:"
		tail -n 10 "$LOG" 2>/dev/null || echo "[DEBUG] Unable to read log file"
		task_fail "proot-distro installation failed"
		exit $ERR_PKG_INSTALL
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

setup_debian() {
	echo "[DEBUG] Starting Debian setup..."
	echo "[DEBUG] Checking if Debian is already installed at: $PREFIX/var/lib/proot-distro/installed-rootfs/debian"
	if [ ! -d "$PREFIX/var/lib/proot-distro/installed-rootfs/debian" ]; then
		start_spinner "Installing Debian Linux"
		echo "[DEBUG] Debian not found, proceeding with fresh installation"
		if ! check_disk_space; then
			task_fail "Insufficient disk space for Debian installation"
			exit $ERR_DEBIAN_INSTALL
		fi
		echo "[DEBUG] Disk space check passed, running: proot-distro install debian"
		if proot-distro install debian >>"$LOG" 2>&1; then
			if [ -d "$PREFIX/var/lib/proot-distro/installed-rootfs/debian" ]; then
				echo "[DEBUG] Debian installation completed successfully"
				task_success "Debian Linux installed"
			else
				echo "[DEBUG] Debian installation command succeeded but directory not found"
				task_fail "Debian directory not created"
				exit $ERR_DEBIAN_INSTALL
			fi
		else
			echo "[DEBUG] Debian installation command failed. Check log: $LOG"
			echo "[DEBUG] Last 10 lines of log:"
			tail -n 10 "$LOG" 2>/dev/null || echo "[DEBUG] Unable to read log file"
			task_fail "Debian installation failed"
			exit $ERR_DEBIAN_INSTALL
		fi
	else
		start_spinner "Updating Debian packages"
		echo "[DEBUG] Debian already installed, updating packages"
		echo "[DEBUG] Running: proot-distro login debian -- sh -lc 'apt update >/dev/null 2>&1 && apt upgrade -y >/dev/null 2>&1'"
		if proot-distro login debian -- sh -lc 'apt update >/dev/null 2>&1 && apt upgrade -y >/dev/null 2>&1' >>"$LOG" 2>&1; then
			echo "[DEBUG] Debian package update completed successfully"
			task_success "Debian packages updated"
		else
			echo "[DEBUG] Debian package update failed. Check log: $LOG"
			echo "[DEBUG] Last 10 lines of log:"
			tail -n 10 "$LOG" 2>/dev/null || echo "[DEBUG] Unable to read log file"
			task_fail "Debian update failed"
			exit $ERR_DEBIAN_INSTALL
		fi
	fi
}

copy_scripts() {
	start_spinner "Installing setup scripts"
	echo "[DEBUG] Starting script copying process..."
	mkdir -p "$HOME/scripts"
	echo "[DEBUG] Created scripts directory: $HOME/scripts"
	TO_COPY=""
	for f in setup; do
		[ -f "$HOME/scripts/$f" ] && TO_COPY="$TO_COPY $f"
	done
	echo "[DEBUG] Files to copy: $TO_COPY"
	if [ -n "$TO_COPY" ]; then
		echo "[DEBUG] Copying scripts to Debian environment..."
		if tar -C "$HOME/scripts" -cf - $TO_COPY | proot-distro login debian -- sh -lc '
		set -e
		echo "[DEBUG] Inside Debian: Creating /root/scripts directory"
		mkdir -p /root/scripts
		echo "[DEBUG] Inside Debian: Extracting scripts"
		tar -C /root/scripts -xpf -
		echo "[DEBUG] Inside Debian: Fixing line endings"
		sed -i "s/\r$//" /root/scripts/setup 2>/dev/null || true
		echo "[DEBUG] Inside Debian: Setting permissions"
		chmod 0755 /root/scripts/setup || true
		echo "[DEBUG] Inside Debian: Copying to /usr/local/bin"
		mkdir -p /usr/local/bin
		cp -f /root/scripts/setup /usr/local/bin/setup || install -m 0755 /root/scripts/setup /usr/local/bin/setup
		echo "[DEBUG] Inside Debian: Script installation completed"
		' >>"$LOG" 2>&1; then
			echo "[DEBUG] Script copying completed successfully"
			task_success "Setup scripts installed"
		else
			echo "[DEBUG] Script copying failed. Check log: $LOG"
			echo "[DEBUG] Last 10 lines of log:"
			tail -n 10 "$LOG" 2>/dev/null || echo "[DEBUG] Unable to read log file"
			task_fail "Script installation failed"
			exit $ERR_SCRIPT_COPY
		fi
	else
		echo "[DEBUG] No setup scripts found to copy"
		task_fail "No setup scripts found"
		exit $ERR_SCRIPT_COPY
	fi
}

trap 'stop_spinner' EXIT INT TERM

update_packages
upgrade_packages
install_proot
setup_debian
copy_scripts

printf "\nWelcome to SamsaraServer Debian Environment\n"
printf "==========================================\n"
printf "To start SSH server and enable remote access, run:\n"
printf "  setup\n"
printf "\nThis will configure SSH on port 2222 with password 'server'\n"
exec proot-distro login debian -- /bin/bash -l