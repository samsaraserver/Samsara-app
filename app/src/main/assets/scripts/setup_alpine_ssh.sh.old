#!/bin/sh
set -e

echo ""
echo "SamsaraServer: Setup Alpine SSH"
echo "================================"

# Preconditions: run inside Alpine proot. OpenSSH packages should already be installed by you.
if ! command -v sshd >/dev/null 2>&1; then
  echo "[!] sshd not found. Please install OpenSSH packages first:" >&2
  echo "    apk update && apk add openssh openssh-server" >&2
  exit 1
fi

CFG=/etc/ssh/sshd_config
echo "[*] Configuring SSH server (minimal edits; keep package defaults)"

# If a saved default exists, restore it to ensure a clean base
if [ -f "$CFG.pkg-default" ]; then
  cp -f "$CFG.pkg-default" "$CFG"
fi

# 1) Comment Include lines in main config to avoid unexpected overrides (BusyBox sed compatible)
sed -i 's/^Include\b/# &/' "$CFG" || true

# 2) Neutralize Port directives in snippet dir
if [ -d /etc/ssh/sshd_config.d ]; then
  for f in /etc/ssh/sshd_config.d/*.conf; do
  [ -f "$f" ] && sed -i 's/^Port\b/# &/' "$f"
  done
fi

# 3) Ensure a single Port directive at the end
sed -i '/^Port\b/d' "$CFG" || true
printf '\nPort 2222\n' >> "$CFG"

# 4) Remove UsePAM if present (not supported in this Alpine setup)
sed -i '/^UsePAM\b/d' "$CFG" || true

# 5) Force password auth and allow root login (explicit)
sed -i '/^PasswordAuthentication\b/d' "$CFG" || true
printf 'PasswordAuthentication yes\n' >> "$CFG"
sed -i '/^PermitRootLogin\b/d' "$CFG" || true
printf 'PermitRootLogin yes\n' >> "$CFG"

# 5) Ensure Subsystem sftp path is correct on Alpine
if grep -q '^Subsystem[[:space:]]\+sftp' "$CFG"; then
  sed -i 's#^Subsystem[[:space:]]\+sftp\b.*#Subsystem sftp /usr/lib/ssh/sftp-server#' "$CFG" || true
else
  printf 'Subsystem sftp /usr/lib/ssh/sftp-server\n' >> "$CFG"
fi

echo "[*] Generating host keys (if missing)"
ssh-keygen -A >/dev/null 2>&1 || true

echo "[*] Preparing runtime directories"
mkdir -p /var/run/sshd /var/empty /run/sshd
chmod 755 /var/run/sshd /var/empty /run/sshd

echo "[*] Testing sshd configuration"
/usr/sbin/sshd -t

echo "[*] Effective ports (sshd -T):"
/usr/sbin/sshd -T 2>/dev/null | grep '^port ' || echo 'unavailable'

IP=""
if command -v ip >/dev/null 2>&1; then
  IP=$(ip route get 1.1.1.1 2>/dev/null | sed -n 's/.*src \([0-9.]*\).*/\1/p')
fi
[ -z "$IP" ] && IP=$(hostname -i 2>/dev/null | awk '{print $1}')
case "$IP" in 127.*|0.0.0.0|"") IP="<phone-ip>";; esac

cat <<EOF

==================================
SSH configured. Start it manually:
==================================
/usr/sbin/sshd -D -e -p 2222 -o StrictModes=no -o PasswordAuthentication=yes

Then connect from another device:
  ssh samsara@$IP -p 2222   # password: server (if user exists)
  ssh root@$IP -p 2222      # password: server (if you set it)

To check the port:
  ss -ltn | grep :2222 || netstat -ltn | grep :2222
EOF
