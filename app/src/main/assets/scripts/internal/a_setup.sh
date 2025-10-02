#!/bin/sh
set -e

# #COMPLETION_DRIVE: Assuming this script runs inside Alpine (proot-distro login alpine) as root
# #SUGGEST_VERIFY: Run `cat /etc/alpine-release` and `id -u` should be 0 before continuing

PORT=222

info() { echo "[INFO] $*"; }
fail() { echo "[!] $*" 1>&2; exit 1; }

install_packages() {
  # #COMPLETION_DRIVE: Install required packages without enabling services
  # #SUGGEST_VERIFY: Run `sshd -V` and `node -v` and `deno --version`
  apk update || true
  apk add --no-cache openssh openssh-server nano htop nodejs npm deno curl ca-certificates || fail "Package installation failed"
}

ensure_shells() {
  for s in /bin/sh /bin/bash; do
    [ -x "$s" ] || [ "$s" = "/bin/bash" ] && {
      grep -qxF "$s" /etc/shells 2>/dev/null || echo "$s" >> /etc/shells
    }
  done
}

safe_set_shell() {
  u="$1"; shpath="$2"
  ensure_shells
  if command -v chsh >/dev/null 2>&1; then
    chsh -s "$shpath" "$u" >/dev/null 2>&1 || true
  fi
  sed -i "s#^\($u:[^:]*:[^:]*:[^:]*:[^:]*:[^:]*:\).*#\\1$shpath#" /etc/passwd || true
}

set_password() {
  u="$1"; p="$2"
  passwd -u "$u" >/dev/null 2>&1 || true
  if command -v chpasswd >/dev/null 2>&1; then
    echo "$u:$p" | chpasswd 2>/dev/null || {
      printf "%s\n%s\n" "$p" "$p" | passwd "$u" 2>/dev/null || true
    }
  else
    printf "%s\n%s\n" "$p" "$p" | passwd "$u" 2>/dev/null || true
  fi
}

setup_users() {
  info "Configuring root user"
  if set_password root server && [ -x /bin/sh ] && safe_set_shell root "/bin/sh"; then
    [ -f /etc/nologin ] && rm -f /etc/nologin || true
  else
    fail "Root user configuration failed"
  fi
}

setup_ssh() {
  info "Configuring SSH server"
  CFG=/etc/ssh/sshd_config
  mkdir -p /etc/ssh
  rm -f /etc/ssh/ssh_host_rsa_key /etc/ssh/ssh_host_rsa_key.pub 2>/dev/null || true
  cat > "$CFG" <<EOF
Port $PORT
ListenAddress 0.0.0.0
Protocol 2
HostKey /etc/ssh/ssh_host_rsa_key
StrictModes no
PubkeyAuthentication no
PasswordAuthentication yes
PermitRootLogin yes
PermitEmptyPasswords no
UseDNS no
PermitTTY yes
EOF
  if command -v ssh-keygen >/dev/null 2>&1; then
    ssh-keygen -t rsa -b 2048 -f /etc/ssh/ssh_host_rsa_key -N "" -q 2>/dev/null || true
  fi
  [ -f /etc/ssh/ssh_host_rsa_key ] || {
    echo "-----BEGIN RSA PRIVATE KEY-----" > /etc/ssh/ssh_host_rsa_key
    echo "dummy" >> /etc/ssh/ssh_host_rsa_key
    echo "-----END RSA PRIVATE KEY-----" >> /etc/ssh/ssh_host_rsa_key
  }
  chmod 600 /etc/ssh/ssh_host_rsa_key 2>/dev/null || true
  mkdir -p /var/run/sshd /run/sshd 2>/dev/null || true
}

start_sshd() {
  info "Starting SSH server"
  if netstat -tln 2>/dev/null | grep -q ":$PORT "; then
    pkill -f "sshd.*-p $PORT" 2>/dev/null || true
    sleep 1
  fi
  if PROOT_NO_SECCOMP=1 /usr/sbin/sshd -e -p "$PORT" -o StrictModes=no -o PasswordAuthentication=yes >/dev/null 2>&1; then
    sleep 1
    if netstat -tln 2>/dev/null | grep -q ":$PORT "; then
      return 0
    fi
    fail "SSH server not listening on port $PORT"
  else
    fail "SSH server start failed"
  fi
}

ensure_config_dir() {
  # #COMPLETION_DRIVE: Ensure config directory exists and do not overwrite user config
  # #SUGGEST_VERIFY: Check /root/.config/samsara after setup
  mkdir -p /root/.config/samsara || true
}

summary() {
  ip=""
  if command -v ip >/dev/null 2>&1; then
    ip=$(ip route get 1.1.1.1 2>/dev/null | sed -n 's/.*src \([0-9.]*\).*/\1/p')
  fi
  [ -z "$ip" ] && ip=$(hostname -i 2>/dev/null | awk '{print $1}')
  case "$ip" in 127.*|0.0.0.0|"") ip="<phone-ip>";; esac
  echo "SSH ready on port $PORT"
  echo "Connect: ssh root@${ip} -p ${PORT} (password: server)"
}

main() {
  install_packages
  setup_users
  setup_ssh
  start_sshd
  ensure_config_dir
  info "Alpine setup complete"
  summary
}

main "$@"
