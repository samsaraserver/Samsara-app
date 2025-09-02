#!/bin/sh

echo "=== Proot SSH Workaround Tests ==="
echo ""

echo "[*] Test 1: SSH on high port (8022)"
echo "Trying SSH on port 8022 instead of 2222..."
pkill -x sshd 2>/dev/null || true
/usr/sbin/sshd -D -p 8022 -o PasswordAuthentication=yes -o PermitRootLogin=no &
sleep 2
if pgrep -x sshd >/dev/null; then
  echo "SUCCESS: SSH running on port 8022"
  pkill -x sshd
else
  echo "FAILED: SSH still won't start on 8022"
fi

echo ""
echo "[*] Test 2: SSH with explicit IP binding"
echo "Trying to bind to 127.0.0.1 specifically..."
/usr/sbin/sshd -D -p 2222 -o ListenAddress=127.0.0.1 -o PasswordAuthentication=yes -o PermitRootLogin=no &
sleep 2
if pgrep -x sshd >/dev/null; then
  echo "SUCCESS: SSH running on 127.0.0.1:2222"
  pkill -x sshd
else
  echo "FAILED: SSH won't bind to 127.0.0.1"
fi

echo ""
echo "[*] Test 3: Dropbear SSH alternative"
echo "Checking if we can install dropbear instead..."
if command -v dropbear >/dev/null; then
  echo "Dropbear already installed"
else
  echo "Installing dropbear..."
  apk add --no-cache dropbear 2>/dev/null && echo "Dropbear installed" || echo "Dropbear install failed"
fi

if command -v dropbear >/dev/null; then
  echo "Testing dropbear on port 2222..."
  dropbear -p 2222 -F &
  sleep 2
  if pgrep dropbear >/dev/null; then
    echo "SUCCESS: Dropbear running!"
    pkill dropbear
  else
    echo "FAILED: Dropbear won't start either"
  fi
fi

echo ""
echo "[*] Test 4: Simple telnet server"
echo "Testing if we can at least get a basic network service..."
if command -v nc >/dev/null; then
  echo "Starting netcat listener on port 2222..."
  echo "test server" | nc -l -p 2222 &
  sleep 1
  if netstat -ln 2>/dev/null | grep -q 2222; then
    echo "SUCCESS: Basic network service works"
    pkill nc
  else
    echo "FAILED: Even basic networking is blocked"
  fi
fi

echo ""
echo "[*] Test 5: Check proot capabilities"
echo "Proot mount info:"
mount | head -5
echo ""
echo "Network namespace info:"
ls -la /proc/net/ 2>/dev/null | head -5 || echo "Cannot access /proc/net"
echo ""
echo "Available network interfaces:"
ip link show 2>/dev/null || ifconfig 2>/dev/null || echo "No network interface commands work"

echo ""
echo "=== Alternative Suggestions ==="
echo "1. Try Termux native SSH (outside proot)"
echo "2. Use port forwarding: ssh -L 2222:localhost:22"
echo "3. Use HTTP-based remote access instead"
echo "4. Use a reverse tunnel to external server"
echo "========================="
