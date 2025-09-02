#!/bin/sh
set -e

echo "SSH DEBUG SCRIPT"
echo "================"

# Clean slate
echo "[*] Killing any existing SSH processes..."
pkill -f sshd 2>/dev/null || true
sleep 3

echo "[*] Checking if port 2222 is free..."
ss -ltn 2>/dev/null | grep ":2222" && echo "Port 2222 is in use!" || echo "Port 2222 is free"

echo "[*] Testing the EXACT manual command that worked..."
echo "[*] Command: /usr/sbin/sshd -D -p 2222 -o StrictModes=no -o PasswordAuthentication=yes"

# Test SSH config first
echo "[*] Testing SSH configuration..."
/usr/sbin/sshd -t -f /etc/ssh/sshd_config || echo "SSH config test failed"

echo "[*] Manual foreground start recommended in this session:"
echo "/usr/sbin/sshd -D -p 2222 -o StrictModes=no -o PasswordAuthentication=yes"

echo ""
echo "MANUAL TEST:"
echo "Run this command manually: /usr/sbin/sshd -D -p 2222 -o StrictModes=no -o PasswordAuthentication=yes"
echo "Then test from another device: ssh samsara@192.168.88.47 -p 2222"