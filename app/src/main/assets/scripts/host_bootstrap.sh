#!/bin/sh
set -e

# Termux-side bootstrap with spinner. Installs/updates proot-distro and Alpine quietly,
# copies Alpine scripts into /root/scripts, then opens an Alpine shell. No in-Alpine scripts are auto-run.

LOG_DIR="$HOME/.samsara"
LOG="$LOG_DIR/bootstrap.log"
mkdir -p "$LOG_DIR"

SPIN_CHARS='-\\|/'
spin() {
	i=0
	while kill -0 "$1" 2>/dev/null; do
		i=$(( (i + 1) % 4 ))
		c=$(printf %s "$SPIN_CHARS" | cut -c $((i+1)))
		printf "\r[%%] Setting up Alpine and tools... %s" "$c"
		sleep 0.1
	done
	printf "\r[✓] Setup tasks completed.             \n"
}

do_bootstrap() {
	{
		echo "[*] Updating Termux package repositories..."
		pkg update -y
		echo "[*] Upgrading existing packages..."
		pkg upgrade -y
		echo "[*] Installing/upgrading proot-distro..."
		pkg install -y proot-distro
		echo "[*] Checking Alpine installation..."
		if [ ! -d "$PREFIX/var/lib/proot-distro/installed-rootfs/alpine" ]; then
			echo "[*] Installing Alpine Linux (first time setup)..."
			proot-distro install alpine
		else
			echo "[*] Alpine installed. Updating packages inside Alpine..."
			proot-distro login alpine -- sh -lc 'apk update && apk upgrade'
		fi
		echo "[*] Copying scripts into Alpine /root/scripts..."
		mkdir -p "$HOME/scripts"
			# Copy unified setup script if present
			TO_COPY=""
			for f in setup; do
				[ -f "$HOME/scripts/$f" ] && TO_COPY="$TO_COPY $f"
			done
		if [ -n "$TO_COPY" ]; then
			tar -C "$HOME/scripts" -cf - $TO_COPY | proot-distro login alpine -- sh -lc '
			set -e
			mkdir -p /root/scripts
			tar -C /root/scripts -xpf -
			# Normalize Windows CRLF newlines to LF and ensure executable
			sed -i "s/\r$//" /root/scripts/setup 2>/dev/null || true
			chmod 0755 /root/scripts/setup || true
			# Install globally so `setup` works from anywhere
			mkdir -p /usr/local/bin
			cp -f /root/scripts/setup /usr/local/bin/setup || install -m 0755 /root/scripts/setup /usr/local/bin/setup
		'
		else
			echo "[!] No scripts found in $HOME/scripts to copy."
		fi
	} >"$LOG" 2>&1
}

echo "[*] Starting hidden setup. Logs: $LOG"
do_bootstrap &
BOOT_PID=$!
spin "$BOOT_PID"
wait "$BOOT_PID" || true

echo "[*] Finished. Review logs at: $LOG"

cat <<'EOM'

Manual steps inside Alpine (run as needed):
	# Run root-only setup (defaults: port 2222, background sshd)
	setup

Connect from another device:
	ssh root@<phone-ip> -p 2222      # password: server
EOM

echo "[*] Opening Alpine shell as root..."
exec proot-distro login alpine -- /bin/sh -l