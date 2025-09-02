#!/bin/sh
set -e

LOG=/tmp/sshd-debug.log
echo "[*] Starting sshd in foreground. Logs: $LOG"
ssh-keygen -A >/dev/null 2>&1 || true
: > "$LOG"

/usr/sbin/sshd -D -e -p 2222 -o StrictModes=no -o PasswordAuthentication=yes 2>&1 | tee -a "$LOG"

echo "[*] sshd exited, last 50 lines of log:"
tail -n 50 "$LOG" || true
