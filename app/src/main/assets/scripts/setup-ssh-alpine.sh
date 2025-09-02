#!/bin/sh
set -e

echo "[*] SSH setup starting"

echo "[*] Installing required packages for SSH"
apk add --no-cache psmisc >/dev/null 2>&1 || true

if ! apk info -e openssh >/dev/null 2>&1; then
  echo "[*] Installing openssh"
  apk update >/dev/null 2>&1 || true
  apk add --no-cache openssh >/dev/null 2>&1
else
  echo "[*] openssh already installed"
fi

if ! apk info -e openrc >/dev/null 2>&1; then
  echo "[*] Installing openrc"
  apk add --no-cache openrc >/dev/null 2>&1 || true
fi

if ! command -v ip >/dev/null 2>&1; then
  echo "[*] Installing iproute2"
  apk add --no-cache iproute2 >/dev/null 2>&1 || true
fi

echo "[*] Creating and configuring samsara user FIRST"
if ! id -u samsara >/dev/null 2>&1; then
  echo "[*] Creating user samsara"
  adduser -D -s /bin/sh samsara
else
  echo "[*] User samsara already exists"
fi

echo "[*] Setting password for samsara"
echo "samsara:server" | chpasswd || printf "server\nserver\n" | passwd samsara >/dev/null 2>&1 || true

echo "[*] Ensuring samsara user home directory exists and has correct permissions"
mkdir -p /home/samsara
chown samsara:samsara /home/samsara
chmod 755 /home/samsara

echo "[*] Verifying user setup"
id samsara || echo "[!] User verification failed"

CFG=/etc/ssh/sshd_config
TMP=$(mktemp)

echo "[*] Creating clean SSH config"
cat > "$TMP" << 'EOF'
Port 2222
PasswordAuthentication yes
PermitRootLogin no
PubkeyAuthentication no
ChallengeResponseAuthentication no
PrintMotd no
Subsystem sftp /usr/lib/openssh/sftp-server
EOF

mv "$TMP" "$CFG"

if [ -d /etc/ssh/sshd_config.d ]; then
  sed -i '/^UsePAM /Id' /etc/ssh/sshd_config.d/*.conf 2>/dev/null || true
fi

if pgrep -x sshd >/dev/null 2>&1; then
  echo "[*] Stopping existing sshd processes"
  pkill -x sshd || true
  sleep 3
  echo "[*] Killing any remaining SSH processes on port 2222"
  fuser -k 2222/tcp 2>/dev/null || true
  sleep 1
fi

echo "[*] Ensuring host keys exist"
if [ ! -f /etc/ssh/ssh_host_rsa_key ]; then
  ssh-keygen -t rsa -f /etc/ssh/ssh_host_rsa_key -N "" -q
fi
if [ ! -f /etc/ssh/ssh_host_ed25519_key ]; then
  ssh-keygen -t ed25519 -f /etc/ssh/ssh_host_ed25519_key -N "" -q
fi

echo "[*] Validating sshd config"
/usr/sbin/sshd -t -f "$CFG" 2>&1 | sed 's/^/[sshd-test] /'

echo "[*] Starting sshd on port 2222"
mkdir -p /var/empty && chmod 0755 /var/empty || true
mkdir -p /run/sshd && chmod 0755 /run/sshd || true

echo "[*] Starting sshd daemon"
echo "[*] Final check - killing anything on port 2222"
fuser -k 2222/tcp 2>/dev/null || true
sleep 1

if /usr/sbin/sshd -f "$CFG" -D &
then
  echo "[*] sshd started in background"
  sleep 2
else
  echo "[*] Background start failed, trying explicit options"
  /usr/sbin/sshd -p 2222 -o PasswordAuthentication=yes -o PermitRootLogin=no -o UsePAM=no -D &
  sleep 2
fi

sleep 2
echo "[*] Checking if sshd is running"
if pgrep -x sshd >/dev/null 2>&1; then
  echo "[*] sshd process is running"
  if (command -v nc >/dev/null 2>&1 && nc -z -w1 127.0.0.1 2222) || \
    (command -v netstat >/dev/null 2>&1 && netstat -ltn 2>/dev/null | grep -q ":2222"); then
    echo "[*] sshd listening on port 2222"
  else
    echo "[!] sshd running but not listening on port 2222"
    echo "[*] Checking what's using port 2222:"
    netstat -ltnp 2>/dev/null | grep ":2222" || echo "[*] No process found on port 2222"
  fi
else
  echo "[!] sshd not running"
  echo "[*] Checking sshd logs:"
  if [ -f /var/log/sshd.log ]; then
    tail -n 10 /var/log/sshd.log | sed 's/^/[sshd-log] /'
  else
    echo "[*] No sshd log file found"
  fi
  echo "[*] Trying to start sshd in foreground for debugging:"
  timeout 3 /usr/sbin/sshd -D -p 2222 -o PasswordAuthentication=yes -o PermitRootLogin=no -o LogLevel=DEBUG 2>&1 | sed 's/^/[sshd-debug] /' &
fi

echo "[*] SSH ready on port 2222"
IP=""
if command -v ip >/dev/null 2>&1; then
  IP=$(ip -4 route get 1.1.1.1 2>/dev/null | sed -n 's/.* src \([0-9.]*\).*/\1/p')
  if [ -z "$IP" ]; then
    IFACE=$(ip route 2>/dev/null | sed -n 's/^default .* dev \([^ ]*\).*/\1/p' | head -n1)
    if [ -n "$IFACE" ]; then
      IP=$(ip -o -4 addr show dev "$IFACE" scope global 2>/dev/null | sed -n 's/.* inet \([0-9.]*\)\/.*/\1/p' | head -n1)
    fi
  fi
fi
case "$IP" in 127.*|0.0.0.0|"") IP="";; esac
if [ -z "$IP" ] && command -v ifconfig >/dev/null 2>&1; then
  IP=$(ifconfig 2>/dev/null | sed -n 's/.*inet \([0-9.]*\).*/\1/p' | grep -v '^127\.' | head -n1)
fi
if [ -n "$IP" ]; then
  echo "[*] Device LAN IP (host): $IP"
  echo "[*] Login with: ssh samsara@$IP -p 2222"
else
  echo "[*] Login with: ssh samsara@<phone-ip> -p 2222"
fi
