#!/usr/bin/env bash
#
# Run the same verification tasks as the Android CI workflow.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

if [[ -z "${JAVA_HOME:-}" ]] && ! command -v java >/dev/null 2>&1; then
    echo "error: Java 17 is required (set JAVA_HOME or add java to PATH)." >&2
    exit 1
fi

if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
    echo "warning: ANDROID_HOME/ANDROID_SDK_ROOT is not set; Gradle may fail to find the Android SDK." >&2
fi

chmod +x ./gradlew

./gradlew --no-daemon \
    :app:testDebugUnitTest \
    :app:lintDebug \
    :app:assembleDebug

echo
echo "Verification complete."
echo "  Tests:  app/build/reports/tests/testDebugUnitTest/index.html"
echo "  Lint:   app/build/reports/lint-results-debug.html"
echo "  APK:    app/build/outputs/apk/debug/app-debug.apk"
