#!/bin/sh
set -e

echo "SamsaraServer: Setup Alpine Users"
echo "================================="

# Ensure bash if available; fallback to /bin/sh
USER_SHELL="/bin/sh"
if command -v bash >/dev/null 2>&1; then
  USER_SHELL="/bin/bash"
fi

# Create samsara user if missing, with home dir
if ! id -u samsara >/dev/null 2>&1; then
  adduser -D -s "$USER_SHELL" samsara
fi

# Ensure wheel group and add samsara to it
addgroup -S wheel 2>/dev/null || true
addgroup samsara wheel 2>/dev/null || true

# Set passwords to 'server'
echo "root:server" | chpasswd
echo "samsara:server" | chpasswd

# Ensure correct shell for samsara
sed -i "s#^samsara:[^:]*:[0-9]*:[0-9]*:[^:]*:/home/samsara:[^:]*#samsara:x:$(id -u samsara):$(id -g samsara):samsara:/home/samsara:$USER_SHELL#" /etc/passwd || true

echo "Users ready. Try: ssh root@<phone-ip> -p 2222 (pass: server) or ssh samsara@<phone-ip> -p 2222 (pass: server)"
