#!/bin/sh
set -e

cat <<'EOT'
Manual SSH bring-up (inside Alpine proot):

1) Ensure host keys exist (run once):
   ssh-keygen -A

2) Start sshd in foreground so it stays alive while this session is open:
   /usr/sbin/sshd -D -p 2222 -o StrictModes=no -o PasswordAuthentication=yes

3) From another device, connect:
   ssh samsara@<phone-ip> -p 2222  # password: server

4) Verify listening:
   ss -ltn | grep :2222 || netstat -ltn | grep :2222

Notes:
- Do not exit this shell while testing; exiting will stop sshd under proot.
- If port 2222 is busy, try -p 8022 and test again.
EOT
