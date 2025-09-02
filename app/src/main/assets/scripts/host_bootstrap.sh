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
		# Build tar of known script names if present
		TO_COPY=""
		for f in setup_alpine_users.sh setup_alpine_ssh.sh start-sshd-foreground.sh; do
			[ -f "$HOME/scripts/$f" ] && TO_COPY="$TO_COPY $f"
		done
		if [ -n "$TO_COPY" ]; then
			tar -C "$HOME/scripts" -cf - $TO_COPY | proot-distro login alpine -- sh -lc 'mkdir -p /root/scripts && tar -C /root/scripts -xpf - && chmod +x /root/scripts/*.sh || true'
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

# Determine default Alpine login user: prefer samsara if present
LOGIN_USER=$(proot-distro login alpine -- sh -lc 'id -u samsara >/dev/null 2>&1 && echo samsara || echo root')

cat <<'EOM'

Manual steps inside Alpine (run as needed):
	# Create users (root/samsara passwords: server)
	sh /root/scripts/setup_alpine_users.sh

	# Configure SSH to listen on port 2222
	sh /root/scripts/setup_alpine_ssh.sh

	# Start sshd in foreground for debugging
	sh /root/scripts/start-sshd-foreground.sh

Connect from another device:
	ssh samsara@<phone-ip> -p 2222   # password: server
	ssh root@<phone-ip> -p 2222      # password: server
EOM

echo "[*] Opening Alpine shell as $LOGIN_USER..."
# Always enter as root, then switch to samsara with a safe shell if present.
exec proot-distro login alpine -- /bin/sh -lc '
	if id -u samsara >/dev/null 2>&1; then
		exec su -s /bin/sh -l samsara
	else
		exec /bin/sh -l
	fi
'