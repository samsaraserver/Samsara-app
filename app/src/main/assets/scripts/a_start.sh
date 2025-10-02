#!/bin/sh
set -e

# #COMPLETION_DRIVE: Assuming proot-distro and Alpine rootfs are present when selecting Alpine
# #SUGGEST_VERIFY: Check for proot-distro command and Alpine install dir; if missing, print bootstrap hint and fallback to Termux

printf "\nSamsaraServer: Starting Alpine environment...\n"

if ! command -v proot-distro >/dev/null 2>&1; then
    printf "[ERROR] proot-distro not found. Run host_bootstrap.sh first.\n" >&2
    exec /bin/sh -l
fi

if [ ! -d "$PREFIX/var/lib/proot-distro/installed-rootfs/alpine" ]; then
    printf "[ERROR] Alpine not installed. Run host_bootstrap.sh to install Alpine.\n" >&2
    exec /bin/sh -l
fi

exec proot-distro login alpine -- /bin/sh -l
