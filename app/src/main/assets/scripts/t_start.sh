#!/bin/sh
set -e

# #COMPLETION_DRIVE: Assuming Termux environment with $PREFIX and /bin/sh available
# #SUGGEST_VERIFY: Check for PREFIX and existence of pkg before proceeding; exit with guidance if missing

printf "\nSamsaraServer: Starting Termux environment...\n"

if [ -z "$PREFIX" ] || ! command -v sh >/dev/null 2>&1; then
    printf "[ERROR] Not running inside Termux shell.\n" >&2
    exec /bin/sh -l 2>/dev/null || exit 1
fi

# Minimal start: just launch an interactive login shell in Termux
exec /bin/sh -l
