#!/bin/sh
set -e

LOG_DIR="$HOME/.samsara"
LOG="$LOG_DIR/bootstrap.log"
mkdir -p "$LOG_DIR"

check_command() {
	command -v "$1" >/dev/null 2>&1
}

verify_package_install() {
	pkg list-installed 2>/dev/null | grep -q "^$1" || return 1
}

do_bootstrap() {
	{
		pkg update -y >/dev/null 2>&1 || {
			echo "Failed to update package repositories" >&2
			return 1
		}
		pkg upgrade -y >/dev/null 2>&1 || {
			echo "Failed to upgrade packages" >&2
			return 1
		}
		pkg install -y proot-distro >/dev/null 2>&1 || {
			echo "Failed to install proot-distro" >&2
			return 1
		}
		verify_package_install "proot-distro" || {
			echo "proot-distro installation verification failed" >&2
			return 1
		}
		check_command "proot-distro" || {
			echo "proot-distro command not available after installation" >&2
			return 1
		}
		if [ ! -d "$PREFIX/var/lib/proot-distro/installed-rootfs/alpine" ]; then
			proot-distro install alpine >/dev/null 2>&1 || {
				echo "Failed to install Alpine distribution" >&2
				return 1
			}
			[ -d "$PREFIX/var/lib/proot-distro/installed-rootfs/alpine" ] || {
				echo "Alpine distribution directory not created" >&2
				return 1
			}
		else
			proot-distro login alpine -- sh -lc 'apk update >/dev/null 2>&1 && apk upgrade >/dev/null 2>&1' >/dev/null 2>&1 || {
				echo "Failed to update Alpine distribution" >&2
				return 1
			}
		fi
		mkdir -p "$HOME/scripts"
		TO_COPY=""
		for f in setup; do
			[ -f "$HOME/scripts/$f" ] && TO_COPY="$TO_COPY $f"
		done
		if [ -n "$TO_COPY" ]; then
			tar -C "$HOME/scripts" -cf - $TO_COPY | proot-distro login alpine -- sh -lc '
			set -e
			mkdir -p /root/scripts
			tar -C /root/scripts -xpf -
			sed -i "s/\r$//" /root/scripts/setup 2>/dev/null || true
			chmod 0755 /root/scripts/setup || true
			mkdir -p /usr/local/bin
			cp -f /root/scripts/setup /usr/local/bin/setup || install -m 0755 /root/scripts/setup /usr/local/bin/setup
			'
		fi
	} >"$LOG" 2>&1
}

printf "Setting up Alpine..."
do_bootstrap &
BOOT_PID=$!
if wait "$BOOT_PID"; then
	printf " done\n"
else
	printf " failed\n"
	echo "Bootstrap process failed. Check $LOG for details." >&2
	exit 1
fi

printf "Connect: ssh root@<phone-ip> -p 2222 (password: server)\n"
exec proot-distro login alpine -- /bin/sh -l