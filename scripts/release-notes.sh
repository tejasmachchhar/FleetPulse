#!/usr/bin/env bash
#
# Extract the release section for a version from CHANGELOG.md.
# Usage: scripts/release-notes.sh 4.3.0
set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "usage: $0 <version-without-v>" >&2
    exit 2
fi

version="${1#v}"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
changelog="$repo_root/CHANGELOG.md"

if [[ ! -f "$changelog" ]]; then
    echo "error: $changelog not found" >&2
    exit 1
fi

notes="$(
    awk -v ver="$version" '
        index($0, "## [" ver "]") == 1 { found = 1; next }
        found && index($0, "## [") == 1 { exit }
        found { print }
    ' "$changelog" | sed -E '/^\[[^]]+\]:[[:space:]]+https?:\/\//d'
)"

if [[ -z "${notes//[[:space:]]/}" ]]; then
    echo "error: no CHANGELOG section found for [$version]" >&2
    exit 1
fi

printf '%s\n' "$notes"
