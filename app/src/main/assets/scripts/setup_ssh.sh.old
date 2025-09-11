#!/bin/sh
set -e

echo "[*] Setting up SSH server..."

# Stop any existing SSH processes
echo "[*] Cleaning up existing SSH processes..."
pkill -f sshd 2>/dev/null || true
sleep 2

# Configure SSH Server using the package default; apply minimal edits only
echo "[*] Configuring SSH server (keep package default; apply minimal edits)..."

CFG=/etc/ssh/sshd_config

# If we saved a package default earlier, restore it
if [ -f "$CFG.pkg-default" ]; then
    echo "[*] Restoring package default sshd_config"
    cp -f "$CFG.pkg-default" "$CFG"
fi

# Apply minimal changes: enforce port to 2222 and drop UsePAM if present
# 1) Comment out any Include lines to avoid overrides from snippets
sed -i 's/^\s*Include\b/# &/I' "$CFG" || true

# 2) Neutralize Port directives in included snippets if any exist
if [ -d /etc/ssh/sshd_config.d ]; then
    for f in /etc/ssh/sshd_config.d/*.conf; do
        [ -f "$f" ] && sed -i 's/^\s*Port\b/# &/I' "$f"
    done
fi

# 3) Ensure a single Port 2222 at the end so it wins
sed -i 's/^\s*#\?\s*Port\b.*/# Port overridden by setup_ssh.sh/I' "$CFG" || true
printf '\nPort 2222\n' >> "$CFG"

# Remove UsePAM if present to avoid unsupported directive errors
sed -i '/^\s*UsePAM\b/I d' "$CFG" || true

echo "[*] Using sshd_config with minimal changes:"
grep -iE '^(Include|Port|PasswordAuthentication|PubkeyAuthentication|PermitRootLogin|Subsystem)' "$CFG" | sed 's/^/[conf] /' || true

echo "[*] Effective ports according to sshd -T:"
/usr/sbin/sshd -T 2>/dev/null | grep '^port ' | sed 's/^/[sshd -T] /' || echo '[sshd -T] unavailable'

# Generate SSH host keys
echo "[*] Generating SSH host keys..."
ssh-keygen -A >/dev/null 2>&1

# Create necessary directories
echo "[*] Creating SSH runtime directories..."
mkdir -p /var/run/sshd /var/empty /run/sshd
chmod 755 /var/run/sshd /var/empty /run/sshd

# Test the SSH configuration
echo "[*] Testing SSH configuration..."
/usr/sbin/sshd -t

# Do not start SSH here to avoid proot killing background daemons on session exit.
echo "[*] Not starting sshd automatically to avoid proot session teardown issues."
echo "[*] To start sshd in foreground (recommended inside this session):"
echo "    /usr/sbin/sshd -D -p 2222 -o StrictModes=no -o PasswordAuthentication=yes"
echo "[*] To verify port listening:"
echo "    ss -ltn | grep :2222 || netstat -ltn | grep :2222"

# Get device IP
IP=""
if command -v ip >/dev/null 2>&1; then
    IP=$(ip route get 1.1.1.1 2>/dev/null | sed -n 's/.*src \([0-9.]*\).*/\1/p')
fi
if [ -z "$IP" ]; then
    IP=$(hostname -i 2>/dev/null | cut -d' ' -f1)
fi
case "$IP" in 127.*|0.0.0.0|"") IP="<your-phone-ip>";; esac

echo ""
echo "=================================="
echo "SSH SERVER IS READY!"
echo "=================================="
echo "Connection test:"
echo "The SSH server is running and accepting connections."
echo ""
echo "Connect from another device:"
echo ""
echo "ssh samsara@$IP -p 2222"
echo "Password: server"
echo ""
echo "Or as root:"
echo "ssh root@$IP -p 2222" 
echo "Password: server"
echo ""
echo "Note: User shell is automatically configured."
echo "If connection issues persist, check /tmp/sshd.log"
echo "=================================="