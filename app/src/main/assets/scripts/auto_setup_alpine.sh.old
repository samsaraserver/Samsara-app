#!/bin/sh
set -e

MARKER="/root/.samsara_setup_done"

echo "SamsaraServer: Auto Setup (Alpine)"
echo "==================================="

# 0) Ensure package index
apk update >/dev/null 2>&1 || true

# 1) Ensure OpenSSH packages
if ! command -v sshd >/dev/null 2>&1; then
  echo "[*] Installing OpenSSH server..."
  apk add --no-progress openssh openssh-server >/dev/null 2>&1 || apk add openssh openssh-server
fi

# 2) Users and passwords (server)
if [ -x /root/scripts/setup_alpine_users.sh ]; then
  sh /root/scripts/setup_alpine_users.sh
fi

# 3) SSH configuration
if [ -x /root/scripts/setup_alpine_ssh.sh ]; then
  sh /root/scripts/setup_alpine_ssh.sh
fi

# 4) Start sshd in background if not listening
if ! ss -ltn 2>/dev/null | grep -q ':2222 '; then
  if ! netstat -ltn 2>/dev/null | grep -q ':2222 '; then
    echo "[*] Starting sshd on port 2222 in background"
    /usr/sbin/sshd -e -p 2222 -o StrictModes=no -o PasswordAuthentication=yes || true
  fi
fi

touch "$MARKER" 2>/dev/null || true
echo "[✓] Alpine SSH setup complete. You can connect with password 'server'."
