#!/bin/sh
set -e

LOG_DIR="$HOME/.samsara"
LOG="$LOG_DIR/bootstrap.log"
mkdir -p "$LOG_DIR"

ERR_NO_INTERNET=1
ERR_PKG_UPDATE=2
ERR_PKG_INSTALL=3
ERR_ALPINE_INSTALL=4
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
	if ! check_internet; then
		task_fail "No internet connection"
		exit $ERR_NO_INTERNET
	fi
	if pkg update -y >>"$LOG" 2>&1; then
		task_success "Package repositories updated"
	else
		task_fail "Failed to update packages"
		exit $ERR_PKG_UPDATE
	fi
}

upgrade_packages() {
	start_spinner "Upgrading packages"
	if pkg upgrade -y >>"$LOG" 2>&1; then
		task_success "Packages upgraded"
	else
		task_fail "Package upgrade failed"
		exit $ERR_PKG_INSTALL
	fi
}

install_proot() {
	start_spinner "Installing proot-distro"
	if pkg install -y proot-distro >>"$LOG" 2>&1; then
		if command -v proot-distro >/dev/null 2>&1; then
			task_success "proot-distro installed"
		else
			task_fail "proot-distro command not available"
			exit $ERR_PKG_INSTALL
		fi
	else
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

setup_alpine() {
	if [ ! -d "$PREFIX/var/lib/proot-distro/installed-rootfs/alpine" ]; then
		start_spinner "Installing Alpine Linux"
		if ! check_disk_space; then
			task_fail "Insufficient disk space for Alpine installation"
			exit $ERR_ALPINE_INSTALL
		fi
		if proot-distro install alpine >>"$LOG" 2>&1; then
			if [ -d "$PREFIX/var/lib/proot-distro/installed-rootfs/alpine" ]; then
				task_success "Alpine Linux installed"
			else
				task_fail "Alpine directory not created"
				exit $ERR_ALPINE_INSTALL
			fi
		else
			task_fail "Alpine installation failed"
			exit $ERR_ALPINE_INSTALL
		fi
	else
		start_spinner "Updating Alpine packages"
		if proot-distro login alpine -- sh -lc 'apk update >/dev/null 2>&1 && apk upgrade >/dev/null 2>&1' >>"$LOG" 2>&1; then
			task_success "Alpine packages updated"
		else
			task_fail "Alpine update failed"
			exit $ERR_ALPINE_INSTALL
		fi
	fi
}

copy_scripts() {
	start_spinner "Installing setup scripts"
	mkdir -p "$HOME/scripts"
	TO_COPY=""
	for f in setup; do
		[ -f "$HOME/scripts/$f" ] && TO_COPY="$TO_COPY $f"
	done
	if [ -n "$TO_COPY" ]; then
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
		else
			task_fail "Script installation failed"
			exit $ERR_SCRIPT_COPY
		fi
	else
		task_fail "No setup scripts found"
		exit $ERR_SCRIPT_COPY
	fi
}

trap 'stop_spinner' EXIT INT TERM

update_packages
upgrade_packages
install_proot
setup_alpine
copy_scripts

printf "\nWelcome to SamsaraServer Alpine Environment\n"
printf "==========================================\n"
printf "To start SSH server and enable remote access, run:\n"
printf "  setup\n"
printf "\nThis will configure SSH on port 2222 with password 'server'\n"
exec proot-distro login alpine -- /bin/sh -l