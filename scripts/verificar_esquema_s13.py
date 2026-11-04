"""Compare a migrated v1 snapshot with the current SQL schema using SQLite.
This supplements host migration tests; it does not claim the Gradle verifier ran.
"""
from pathlib import Path
import re
import sqlite3

root = Path(__file__).resolve().parents[1]
source = root / "shared/src/commonMain/sqldelight/pe/edu/upeu/pharmamobil/db"

def describe(connection):
    tables = [r[0] for r in connection.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")]
    return {name: list(connection.execute(f'PRAGMA table_info("{name}")')) for name in tables}

old = sqlite3.connect(source / "1.db")
migrated = sqlite3.connect(":memory:")
old.backup(migrated)
old.close()
current = sqlite3.connect(":memory:")
try:
    for script in sorted(source.glob("*.sqm"), key=lambda p: int(p.stem)):
        migrated.executescript(script.read_text(encoding="utf-8"))
    for file in sorted(source.glob("*.sq")):
        text = file.read_text(encoding="utf-8")
        label = re.search(r"(?m)^\w+:\s*$", text)
        current.executescript(text[:label.start()] if label else text)
    if describe(migrated) != describe(current):
        raise SystemExit("FAIL: migrated schema differs from current CREATE statements")
    for name, connection in [("migrated", migrated), ("current", current)]:
        if connection.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
            raise SystemExit(f"FAIL: integrity check {name}")
    print("PASS: v1 snapshot + migrations matches current schema; both integrity checks OK")
finally:
    migrated.close()
    current.close()
