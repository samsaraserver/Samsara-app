#!/bin/sh
set -e

echo "[*] System setup starting"

echo "[*] Installing basic packages"
apk add --no-cache curl wget git nano vim >/dev/null 2>&1 || true

echo "[*] Setting up aliases"
cat > /home/samsara/.bashrc << 'EOF'
alias ll='ls -la'
alias la='ls -A'
alias l='ls -CF'
alias ..='cd ..'
alias ...='cd ../..'
alias grep='grep --color=auto'
alias fgrep='fgrep --color=auto'
alias egrep='egrep --color=auto'

export PS1='\u@\h:\w\$ '
export EDITOR=nano

echo "Welcome to SamsaraServer Alpine Linux"
echo "Available services:"
echo "  - SSH server on port 2222"
echo "  - User: samsara (password: rootserver)"
EOF

chown samsara:samsara /home/samsara/.bashrc

echo "[*] System setup complete"
