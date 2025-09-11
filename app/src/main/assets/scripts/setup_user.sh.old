#!/bin/sh
set -e

echo "[*] Setting up user and system packages..."

# Update package manager
echo "[*] Updating package manager..."
apk update >/dev/null 2>&1

# Install essential packages (matching your working setup)
echo "[*] Installing OpenSSH server and essentials..."
apk add openssh openssh-server sudo nano bash >/dev/null 2>&1

# Verify bash installation
if ! command -v bash >/dev/null 2>&1; then
    echo "[*] Bash not available, using /bin/sh for user shell"
    USER_SHELL="/bin/sh"
else
    echo "[*] Bash installed successfully"
    USER_SHELL="/bin/bash"
fi

# Create user account (non-interactive)
echo "[*] Creating user 'samsara'..."
if ! id -u samsara >/dev/null 2>&1; then
    # Create user with home directory and proper shell
    adduser -D -s "$USER_SHELL" samsara
    # Set password non-interactively
    echo "samsara:server" | chpasswd
else
    echo "[*] User samsara already exists"
    echo "samsara:server" | chpasswd
    # Make sure the shell is set correctly (Alpine Linux compatible)
    sed -i "s|^samsara:.*|samsara:x:$(id -u samsara):$(id -g samsara):samsara:/home/samsara:$USER_SHELL|" /etc/passwd
fi

# Add user to sudo group
echo "[*] Adding user to wheel group..."
addgroup samsara wheel 2>/dev/null || true

# Configure sudoers safely using sudoers.d to avoid permissions issues
echo "[*] Configuring sudo permissions..."
mkdir -p /etc/sudoers.d
cat > /etc/sudoers.d/samsara <<'EOF'
Defaults:samsara !authenticate
Defaults secure_path=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
samsara ALL=(ALL) ALL
%wheel ALL=(ALL) ALL
EOF
chmod 0440 /etc/sudoers.d/samsara

# Ensure main sudoers has correct permissions
chmod 0440 /etc/sudoers 2>/dev/null || true

# Set root password too
echo "root:server" | chpasswd

echo "[*] User setup complete!"
echo "    - Username: samsara" 
echo "    - Password: server"
echo "    - Root password: server"
echo "    - User shell: $USER_SHELL"
echo "    - Sudo access: enabled"