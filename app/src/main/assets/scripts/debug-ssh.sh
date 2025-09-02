#!/bin/sh
set -e

echo "[*] SSH Debug Information"
echo "========================="

echo "[*] Checking SSH processes:"
ps aux | grep sshd | grep -v grep || echo "No sshd processes found"

echo "[*] Checking listening ports:"
netstat -ltn 2>/dev/null | grep ":2222" || echo "Nothing listening on port 2222"
echo "[*] Checking what processes might be using port 2222:"
fuser 2222/tcp 2>/dev/null && echo "Found process using port 2222" || echo "No process using port 2222"
echo "[*] All listening ports:"
netstat -ltn 2>/dev/null | head -10

echo "[*] Checking SSH config:"
if [ -f /etc/ssh/sshd_config ]; then
  echo "SSH config exists, key settings:"
  grep -E "^(Port|PasswordAuthentication|PermitRootLogin|PubkeyAuthentication)" /etc/ssh/sshd_config || echo "No key settings found"
else
  echo "No SSH config file found"
fi

echo "[*] Checking SSH host keys:"
ls -la /etc/ssh/ssh_host_* 2>/dev/null || echo "No SSH host keys found"

echo "[*] Checking SSH daemon binary:"
which sshd || echo "sshd not found in PATH"
/usr/sbin/sshd -V 2>&1 | head -n1 || echo "Cannot check sshd version"

echo "[*] Checking if SSH can start:"
echo "Testing SSH daemon startup..."
/usr/sbin/sshd -t -f /etc/ssh/sshd_config 2>&1 | sed 's/^/[test] /' || echo "SSH config test failed"

echo "[*] Manual SSH startup attempt:"
echo "Trying to start SSH manually..."
timeout 5 /usr/sbin/sshd -D -p 2222 -o PasswordAuthentication=yes -o PermitRootLogin=no -o LogLevel=VERBOSE 2>&1 | sed 's/^/[manual] /' &
sleep 2
if pgrep -x sshd >/dev/null 2>&1; then
  echo "SSH started successfully!"
  pkill -x sshd || true
else
  echo "SSH failed to start"
fi

echo "========================="
echo "[*] Debug complete"
