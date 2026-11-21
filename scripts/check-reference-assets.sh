#!/usr/bin/env bash
set -euo pipefail
diff -rq vehicle-reference app/src/main/assets/vehicle-reference
python3 vehicle-reference/scripts/validate_database.py
echo "Vehicle reference assets are identical and valid."
