#!/bin/sh
set -e

echo "[*] User configuration starting"

echo "[*] Configuring sudo access for samsara"
if ! getent group wheel >/dev/null 2>&1; then
  echo "[*] Creating wheel group"
  addgroup -S wheel >/dev/null 2>&1 || true
fi

echo "[*] Adding samsara to wheel group"
addgroup samsara wheel >/dev/null 2>&1 || true

echo "[*] Configuring sudoers"
if [ -f /etc/sudoers ]; then
  echo "[*] Backing up original sudoers"
  cp /etc/sudoers /etc/sudoers.bak
  echo "[*] Setting up wheel group sudo access"
  sed -i "s/^# %wheel ALL=(ALL) ALL/%wheel ALL=(ALL) ALL/" /etc/sudoers
  sed -i "s/^# %wheel ALL=(ALL) NOPASSWD: ALL/%wheel ALL=(ALL) NOPASSWD: ALL/" /etc/sudoers
  echo "[*] Adding explicit samsara sudo access"
  echo "samsara ALL=(ALL) ALL" >> /etc/sudoers
  echo "[*] Testing sudoers file"
  visudo -c || echo "[!] Sudoers file has issues"
else
  echo "[*] Creating new sudoers file"
  cat > /etc/sudoers << 'EOF'
root ALL=(ALL) ALL
%wheel ALL=(ALL) ALL
samsara ALL=(ALL) ALL
EOF
fi

echo "[*] Setting root password"
echo "root:server" | chpasswd || printf "server\nserver\n" | passwd root >/dev/null 2>&1 || true

echo "[*] Setting samsara password again to ensure it's correct"
echo "samsara:server" | chpasswd || printf "server\nserver\n" | passwd samsara >/dev/null 2>&1 || true

echo "[*] Verifying user configuration"
echo "User samsara details:"
id samsara
echo "Groups for samsara:"
groups samsara

echo "[*] User configuration complete"
