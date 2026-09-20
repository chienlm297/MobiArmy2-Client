#!/usr/bin/env bash
set -euo pipefail
umask 077
cd "$(dirname "${BASH_SOURCE[0]}")/.."
: "${AUTO_USER_A:?Set AUTO_USER_A}"
: "${AUTO_PASSWORD_A:?Set AUTO_PASSWORD_A}"
: "${AUTO_USER_B:?Set AUTO_USER_B}"
: "${AUTO_PASSWORD_B:?Set AUTO_PASSWORD_B}"
if [[ "$AUTO_USER_A" == "$AUTO_USER_B" ]]; then echo 'Use two different accounts' >&2; exit 1; fi
bash gradlew :desktop:autoplayClasspath --offline --console=plain
if [[ "${AUTO_RELOGIN:-true}" != true ]]; then echo "Acceptance requires AUTO_RELOGIN=true" >&2; exit 1; fi
client_cp=$(cat desktop/build/autoplay-classpath.txt)
mkdir -p test-results
if [[ -n "${AUTO_RUN_DIR:-}" ]]; then
 mkdir -p "$AUTO_RUN_DIR"
 if [[ -e "$AUTO_RUN_DIR/A-room" || -e "$AUTO_RUN_DIR/client-A.jsonl" ]]; then echo 'Use a fresh AUTO_RUN_DIR' >&2; exit 1; fi
else
 AUTO_RUN_DIR=$(mktemp -d "$PWD/test-results/autoplay-XXXXXX")
fi
export AUTO_RUN_DIR
AUTO_RUN_DIR=$(realpath "$AUTO_RUN_DIR")
echo "Evidence: $AUTO_RUN_DIR"
pids=()
cleanup(){ for pid in "${pids[@]}"; do kill "$pid" 2>/dev/null || true; done; for pid in "${pids[@]}"; do wait "$pid" 2>/dev/null || true; done; }
trap cleanup EXIT INT TERM
(
 cd desktop/assets
 exec env AUTO_ENABLED=true AUTO_ROLE=A AUTO_USERNAME="$AUTO_USER_A" AUTO_PASSWORD="$AUTO_PASSWORD_A" \
 AUTO_PARTNER="${AUTO_NAME_B:-$AUTO_USER_B}" AUTO_PROFILE_DIR="$AUTO_RUN_DIR/profile-A" \
 java -cp "$client_cp" com.mygdx.game.DesktopLauncher > "$AUTO_RUN_DIR/client-A.console.log" 2>&1
) & pids+=("$!")
(
 cd desktop/assets
 exec env AUTO_ENABLED=true AUTO_ROLE=B AUTO_USERNAME="$AUTO_USER_B" AUTO_PASSWORD="$AUTO_PASSWORD_B" \
 AUTO_PARTNER="${AUTO_NAME_A:-$AUTO_USER_A}" AUTO_PROFILE_DIR="$AUTO_RUN_DIR/profile-B" \
 java -cp "$client_cp" com.mygdx.game.DesktopLauncher > "$AUTO_RUN_DIR/client-B.console.log" 2>&1
) & pids+=("$!")
deadline=$((SECONDS + ${AUTO_TOTAL_TIMEOUT_SECONDS:-1800} + 60))
while (( SECONDS < deadline )); do
 if [[ -f "$AUTO_RUN_DIR/client-A.summary" && -f "$AUTO_RUN_DIR/client-B.summary" ]]; then break; fi
 dead=false
 for pid in "${pids[@]}"; do if ! kill -0 "$pid" 2>/dev/null; then dead=true; fi; done
 if $dead; then echo 'Client exited; inspect console log' >&2; break; fi
 if grep -Eq '^status=(FAILED|OFF)' "$AUTO_RUN_DIR"/*.summary 2>/dev/null; then break; fi
 sleep 1
done
python3 scripts/autoplay-report.py "$AUTO_RUN_DIR" "${AUTO_MATCHES:-3}"
