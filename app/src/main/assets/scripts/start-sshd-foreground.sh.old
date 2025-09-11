#!/bin/sh
set -e

# Start sshd in foreground and write logs to /tmp/sshd-debug.log
LOG=/tmp/sshd-debug.log
echo "[*] Starting sshd in foreground. Logs: $LOG"
# Ensure host keys exist
ssh-keygen -A >/dev/null 2>&1 || true
# Clean previous log
: > "$LOG"

# Start sshd in foreground and tee the output
/usr/sbin/sshd -D -e -p 2222 -o StrictModes=no -o PasswordAuthentication=yes 2>&1 | tee -a "$LOG"

# If sshd exits, print last lines
echo "[*] sshd exited, last 50 lines of log:"
tail -n 50 "$LOG" || true
