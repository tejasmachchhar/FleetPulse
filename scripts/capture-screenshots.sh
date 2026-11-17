#!/usr/bin/env bash
#
# Capture real screenshots from a connected Android device or emulator.
#
# Usage:
#   ./scripts/capture-screenshots.sh docs/screenshots
#
# The script prompts you to open each screen. It never fabricates or modifies
# images. Use the synthetic demo dataset and review every image for personal
# information before committing it.
set -euo pipefail

out_dir="${1:-docs/screenshots}"

if ! command -v adb >/dev/null 2>&1; then
    echo "error: adb is not installed or not on PATH." >&2
    echo "Install Android platform-tools and connect a device/emulator." >&2
    exit 1
fi

if ! adb get-state >/dev/null 2>&1; then
    echo "error: no adb device is connected. Start an emulator or connect a phone." >&2
    exit 1
fi

mkdir -p "$out_dir"

capture_one() {
    local filename="$1"
    local screen_name="$2"

    read -r -p "Open $screen_name on the device, then press Enter to capture..."
    sleep 1
    adb exec-out screencap -p > "$out_dir/$filename"
    echo "wrote $out_dir/$filename"
}

capture_one "01-dashboard.png" "Dashboard with the sample dataset"
capture_one "02-vehicles.png" "Vehicles"
capture_one "03-usage-log.png" "Usage Log"
capture_one "04-analysis.png" "Analysis"
capture_one "05-maintenance.png" "Maintenance"
capture_one "06-settings.png" "Settings"

echo
echo "Captured screenshots in $out_dir."
echo "Review them for names, registration numbers, notes, and other personal data before committing."
