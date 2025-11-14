#!/data/data/com.termux/files/usr/bin/sh
set -e

REPO_URL="https://github.com/samsaraserver/Samsara-hub.git"
BRANCH="${SAMSARA_HUB_BRANCH:-main}"
SERVICE_ROOT="$HOME/services"
SERVICE_DIR="$SERVICE_ROOT/Samsara-hub"
START_SCRIPT="start.sh"

# #COMPLETION_DRIVE: Assuming Samsara-hub exposes main as default branch with start.sh entrypoint
# #SUGGEST_VERIFY: Override SAMSARA_HUB_BRANCH env or adjust script if repository structure changes

# #COMPLETION_DRIVE: Assuming nohup exists for background execution in Termux coreutils package
# #SUGGEST_VERIFY: Run `command -v nohup` and install busybox-extras or coreutils if missing

# #COMPLETION_DRIVE: Assuming procps provides pgrep/pkill on deployed devices
# #SUGGEST_VERIFY: Run `pkg install procps` if commands are unavailable on device image

log() {
    printf "[SamsaraHub] %s\n" "$1"
}

ensure_git() {
    if command -v git >/dev/null 2>&1; then
        return 0
    fi
    if command -v pkg >/dev/null 2>&1; then
        log "Installing git dependency"
        yes | pkg install git >/dev/null 2>&1 || return 1
        return 0
    fi
    log "Missing pkg manager to install git"
    return 1
}

sync_repo() {
    mkdir -p "$SERVICE_ROOT"
    if [ -d "$SERVICE_DIR/.git" ]; then
        log "Updating Samsara-hub repository"
        git -C "$SERVICE_DIR" fetch origin "$BRANCH" --depth=1 --prune || return 1
        git -C "$SERVICE_DIR" reset --hard "origin/$BRANCH" || return 1
        git -C "$SERVICE_DIR" clean -xfd || true
    else
        log "Cloning Samsara-hub repository"
        rm -rf "$SERVICE_DIR"
        git clone --depth=1 --branch "$BRANCH" "$REPO_URL" "$SERVICE_DIR" || return 1
    fi
}

stop_existing_service() {
    if pgrep -f "$SERVICE_DIR/$START_SCRIPT" >/dev/null 2>&1; then
        log "Stopping existing Samsara-hub process"
        pkill -f "$SERVICE_DIR/$START_SCRIPT" || true
        sleep 2
    fi
}

start_service() {
    script_path="$SERVICE_DIR/$START_SCRIPT"
    if [ ! -f "$script_path" ]; then
        log "start.sh missing inside Samsara-hub"
        return 1
    fi
    chmod +x "$script_path" || true
    log "Launching start.sh"
    (cd "$SERVICE_DIR" && nohup sh "$script_path" >/dev/null 2>&1 &)
}

ensure_git && sync_repo && stop_existing_service && start_service && log "Samsara-hub service started"
