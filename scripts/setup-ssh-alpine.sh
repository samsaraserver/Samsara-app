#!/bin/sh
set -e

echo "[*] SSH setup starting"

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

if ! id -u samsara >/dev/null 2>&1; then
  echo "[*] Creating user samsara"
  adduser -D -s /bin/sh samsara
fi

echo "[*] Setting password for samsara"
printf "rootserver\nrootserver\n" | passwd samsara >/dev/null 2>&1 || true

CFG=/etc/ssh/sshd_config
TMP=$(mktemp)

if [ -f "$CFG" ]; then
  cp "$CFG" "$TMP"
else
  : > "$TMP"
fi

sed -i '/^Port /d' "$TMP"; echo "Port 2222" >> "$TMP"; echo "Port 8022" >> "$TMP"
grep -q "^PasswordAuthentication " "$TMP" && sed -i "s/^PasswordAuthentication .*/PasswordAuthentication yes/" "$TMP" || echo "PasswordAuthentication yes" >> "$TMP"
grep -q "^PermitRootLogin " "$TMP" && sed -i "s/^PermitRootLogin .*/PermitRootLogin no/" "$TMP" || echo "PermitRootLogin no" >> "$TMP"
sed -i "/^UsePAM /d" "$TMP"

mv "$TMP" "$CFG"

# Remove unsupported UsePAM from any included configs
if [ -d /etc/ssh/sshd_config.d ]; then
  sed -i '/^UsePAM /Id' /etc/ssh/sshd_config.d/*.conf 2>/dev/null || true
fi

echo "[*] Skipping host key generation"

if pgrep -x sshd >/dev/null 2>&1; then
  echo "[*] Restarting sshd"
  pkill -x sshd || true
fi

echo "[*] Validating sshd config"
/usr/sbin/sshd -t -f "$CFG" 2>&1 | sed 's/^/[sshd-test] /'

echo "[*] Starting sshd on port 2222"
mkdir -p /var/empty && chmod 0755 /var/empty || true
mkdir -p /run/sshd && chmod 0755 /run/sshd

if (command -v nc >/dev/null 2>&1 && (nc -z -w1 127.0.0.1 2222 || nc -z -w1 127.0.0.1 8022)) || \
  (command -v ss >/dev/null 2>&1 && ss -ltn 2>/dev/null | egrep -q ":(2222|8022)") || \
  (command -v netstat >/dev/null 2>&1 && netstat -ltn 2>/dev/null | egrep -q ":(2222|8022)") || \
   pgrep -x sshd >/dev/null 2>&1; then
  echo "[*] sshd appears already running on 0.0.0.0:2222,8022"
else
  /usr/sbin/sshd -f "$CFG" -p 2222 -o ListenAddress=0.0.0.0 -o AddressFamily=inet -o LogLevel=VERBOSE -E /var/log/sshd.log || true
fi

sleep 1
if (command -v nc >/dev/null 2>&1 && (nc -z -w1 127.0.0.1 2222 || nc -z -w1 127.0.0.1 8022)) || \
  (command -v ss >/dev/null 2>&1 && ss -ltn 2>/dev/null | egrep -q ":(2222|8022)") || \
  (command -v netstat >/dev/null 2>&1 && netstat -ltn 2>/dev/null | egrep -q ":(2222|8022)"); then
  echo "[*] sshd listening on 0.0.0.0:2222,8022"
elif grep -q 'Address in use' /var/log/sshd.log 2>/dev/null; then
  echo "[*] sshd appears already running on 0.0.0.0:2222,8022"
else
  echo "[!] sshd not listening; recent log:"
  tail -n 50 /var/log/sshd.log 2>/dev/null | sed 's/^/[sshd] /'
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
  echo "[*] Login with: ssh samsara@$IP -p 2222  # or: -p 8022"
else
  echo "[*] Login with: ssh samsara@<phone-ip> -p 2222  # or: -p 8022"
fi
