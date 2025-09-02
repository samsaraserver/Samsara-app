#!/bin/sh

echo "=== Port Investigation ==="
echo ""

echo "[*] Checking what's using port 2222:"
echo "Trying to find the process..."

# Check if anything is listening
echo "Netstat check (may fail due to proot):"
netstat -ltn 2>/dev/null | grep 2222 || echo "Netstat failed or nothing found"

echo ""
echo "[*] Process check:"
ps aux | grep -E "(2222|ssh)" | grep -v grep || echo "No obvious processes found"

echo ""
echo "[*] Testing current SSH on port 8022:"
if pgrep -x sshd >/dev/null; then
  echo "SSH daemon is running!"
  echo "Trying to connect to SSH on port 8022..."
  echo "Run this command to test:"
  echo "ssh samsara@localhost -p 8022"
  echo "(password: server)"
else
  echo "SSH daemon not running"
fi

echo ""
echo "[*] Testing netcat on port 9999:"
if pgrep nc >/dev/null; then
  echo "Netcat is running on port 9999"
  echo "Test with: echo 'hello' | nc localhost 9999"
else
  echo "Netcat not running"
fi

echo ""
echo "[*] Killing test processes:"
pkill nc 2>/dev/null && echo "Killed netcat" || echo "No netcat to kill"

echo ""
echo "[*] What might be using port 2222:"
echo "Possibilities:"
echo "1. Previous SSH attempt still running"
echo "2. Another service in Alpine"
echo "3. Something in Termux host system"
echo "4. Port forwarding from Android"

echo ""
echo "[*] Let's try to free port 2222:"
fuser -k 2222/tcp 2>/dev/null && echo "Killed process on 2222" || echo "Nothing to kill on 2222"

echo ""
echo "=== Try SSH on 8022 now ==="
echo "ssh samsara@localhost -p 8022"
echo "========================="
