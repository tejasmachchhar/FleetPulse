#!/usr/bin/env python3
"""CI validator for FleetPulse vehicle-reference repository."""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def main():
    errors = []
    manifest = json.loads((ROOT / "manifest.json").read_text(encoding="utf-8"))
    schema = json.loads((ROOT / "schema" / "vehicle.schema.json").read_text(encoding="utf-8"))
    cat_tax = json.loads((ROOT / "taxonomy" / "categories.json").read_text(encoding="utf-8"))
    prop_tax = json.loads((ROOT / "taxonomy" / "propulsion-types.json").read_text(encoding="utf-8"))
    fuel_tax = json.loads((ROOT / "taxonomy" / "fuel-types.json").read_text(encoding="utf-8"))
    src_idx = json.loads((ROOT / "sources" / "source-index.json").read_text(encoding="utf-8"))

    valid_cats = {c["id"] for c in cat_tax["categories"]}
    valid_props = {p["id"] for p in prop_tax["propulsion_types"]}
    valid_fuels = {f["id"] for f in fuel_tax["fuel_types"]}
    valid_srcs = {s["source_id"] for s in src_idx["sources"]}

    try:
        import jsonschema
        v = jsonschema.Draft202012Validator(schema)
    except ImportError:
        v = None

    seen_ids = set()
    total_records = 0

    for idx_file in sorted((ROOT / "indexes").glob("*.json")):
        idx_data = json.loads(idx_file.read_text(encoding="utf-8"))
        for item in idx_data["records"]:
            rec_path = ROOT / item["record_path"]
            if not rec_path.exists():
                errors.append(f"Missing vehicle file referenced by index: {item['record_path']}")
                continue
            rec = json.loads(rec_path.read_text(encoding="utf-8"))
            total_records += 1
            if rec["id"] in seen_ids:
                errors.append(f"Duplicate ID: {rec['id']}")
            seen_ids.add(rec["id"])
            if rec["classification"]["category"] not in valid_cats:
                errors.append(f"Invalid category in {rec['id']}")
            if rec["powertrain"]["propulsion"] not in valid_props:
                errors.append(f"Invalid propulsion in {rec['id']}")
            if rec["powertrain"]["fuel_type"] not in valid_fuels:
                errors.append(f"Invalid fuel_type in {rec['id']}")
            for s in rec["sources"]:
                if s["source_id"] not in valid_srcs:
                    errors.append(f"Unknown source_id {s['source_id']} in {rec['id']}")
            if v:
                for e in v.iter_errors(rec):
                    errors.append(f"Schema error in {rec['id']} at {list(e.path)}: {e.message}")

    if total_records != manifest["total_records"]:
        errors.append(f"Manifest total_records ({manifest['total_records']}) != actual ({total_records})")

    if errors:
        print(f"VALIDATION FAILED with {len(errors)} errors:")
        for e in errors:
            print(" -", e)
        sys.exit(1)
    print(f"VALIDATION PASSED: {total_records} vehicle records, {len(valid_srcs)} sources, 0 errors.")

if __name__ == "__main__":
    main()
