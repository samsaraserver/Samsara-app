#!/bin/sh
set -e

printf "\nSamsaraServer: Starting Termux environment...\n"
if [ ! -x "$HOME/scripts/t_setup.sh" ] && [ ! -f "$PREFIX/etc/ssh/sshd_config" ]; then
    printf "[HINT] Run: sh \"$HOME/scripts/t_setup.sh\" to configure Termux services and SSH.\n"
fi

if [ -z "$PREFIX" ] || ! command -v sh >/dev/null 2>&1; then
    printf "[ERROR] Not running inside Termux shell.\n" >&2
    exec /bin/sh -l 2>/dev/null || exit 1
fi
exec /bin/sh -l
