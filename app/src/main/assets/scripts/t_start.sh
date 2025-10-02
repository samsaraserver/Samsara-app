#!/bin/sh
set -e

printf "\nSamsaraServer: Starting Termux environment...\n"

if [ -z "$PREFIX" ] || ! command -v sh >/dev/null 2>&1; then
    printf "[ERROR] Not running inside Termux shell.\n" >&2
    exec /bin/sh -l 2>/dev/null || exit 1
fi

# Minimal start: just launch an interactive login shell in Termux
exec /bin/sh -l
