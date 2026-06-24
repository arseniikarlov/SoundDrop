#!/usr/bin/env python3
import json
import os
import sqlite3
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse


HOST = os.getenv("FALL_OUCH_HOST", "0.0.0.0")
PORT = int(os.getenv("FALL_OUCH_PORT", "8787"))
DB_PATH = Path(os.getenv("FALL_OUCH_DB", "/opt/fall-ouch-metrics/fall_ouch_metrics.sqlite3"))
WRITE_KEY = os.getenv("FALL_OUCH_WRITE_KEY", "")
ADMIN_KEY = os.getenv("FALL_OUCH_ADMIN_KEY", "")
MAX_BODY_BYTES = 128 * 1024


def now_ms() -> int:
    return int(time.time() * 1000)


def db_connect() -> sqlite3.Connection:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    connection = sqlite3.connect(DB_PATH)
    connection.row_factory = sqlite3.Row
    return connection


def init_db() -> None:
    with db_connect() as connection:
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                received_at_ms INTEGER NOT NULL,
                app_instance_id TEXT NOT NULL,
                event_name TEXT NOT NULL,
                event_time_ms INTEGER NOT NULL,
                platform TEXT NOT NULL,
                app_version TEXT NOT NULL,
                params_json TEXT NOT NULL
            )
            """
        )
        connection.execute("CREATE INDEX IF NOT EXISTS idx_events_name ON events(event_name)")
        connection.execute("CREATE INDEX IF NOT EXISTS idx_events_instance ON events(app_instance_id)")
        connection.execute("CREATE INDEX IF NOT EXISTS idx_events_received ON events(received_at_ms)")


def unauthorized(handler: BaseHTTPRequestHandler) -> None:
    handler.send_json({"error": "unauthorized"}, status=401)


class MetricsHandler(BaseHTTPRequestHandler):
    server_version = "FallOuchMetrics/1.0"

    def do_GET(self) -> None:
        route = urlparse(self.path)
        if route.path == "/health":
            self.send_json({"ok": True})
            return
        if route.path == "/v1/summary":
            if not self.has_admin_access():
                unauthorized(self)
                return
            self.send_json(summary())
            return
        if route.path == "/v1/events":
            if not self.has_admin_access():
                unauthorized(self)
                return
            params = parse_qs(route.query)
            limit = int(params.get("limit", ["100"])[0])
            self.send_json({"events": recent_events(limit=min(max(limit, 1), 500))})
            return
        self.send_json({"error": "not_found"}, status=404)

    def do_POST(self) -> None:
        route = urlparse(self.path)
        if route.path != "/v1/events":
            self.send_json({"error": "not_found"}, status=404)
            return
        if WRITE_KEY and self.headers.get("X-Fall-Ouch-Write-Key") != WRITE_KEY:
            unauthorized(self)
            return
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0 or length > MAX_BODY_BYTES:
            self.send_json({"error": "bad_body_size"}, status=400)
            return
        try:
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
            inserted = insert_events(payload)
        except Exception as error:
            self.send_json({"error": "bad_request", "details": str(error)}, status=400)
            return
        self.send_json({"ok": True, "inserted": inserted}, status=201)

    def has_admin_access(self) -> bool:
        return bool(ADMIN_KEY) and self.headers.get("X-Fall-Ouch-Admin-Key") == ADMIN_KEY

    def send_json(self, payload: dict, status: int = 200) -> None:
        encoded = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def log_message(self, format: str, *args) -> None:
        print("%s - %s" % (self.address_string(), format % args))


def insert_events(payload: dict) -> int:
    app_instance_id = str(payload.get("app_instance_id", "")).strip()
    platform = str(payload.get("platform", "android")).strip()[:32]
    app_version = str(payload.get("app_version", "")).strip()[:64]
    events = payload.get("events", [])
    if not app_instance_id:
        raise ValueError("app_instance_id is required")
    if not isinstance(events, list) or not events:
        raise ValueError("events must be a non-empty array")

    rows = []
    received_at_ms = now_ms()
    for event in events[:50]:
        if not isinstance(event, dict):
            continue
        name = str(event.get("name", "")).strip()
        if not name:
            continue
        event_time_ms = int(event.get("timestamp_ms") or received_at_ms)
        params = event.get("params") if isinstance(event.get("params"), dict) else {}
        rows.append(
            (
                received_at_ms,
                app_instance_id[:128],
                name[:96],
                event_time_ms,
                platform,
                app_version,
                json.dumps(params, ensure_ascii=False, separators=(",", ":")),
            )
        )
    if not rows:
        raise ValueError("no valid events")

    with db_connect() as connection:
        connection.executemany(
            """
            INSERT INTO events (
                received_at_ms,
                app_instance_id,
                event_name,
                event_time_ms,
                platform,
                app_version,
                params_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            rows,
        )
    return len(rows)


def summary() -> dict:
    with db_connect() as connection:
        total_events = connection.execute("SELECT COUNT(*) AS c FROM events").fetchone()["c"]
        total_devices = connection.execute("SELECT COUNT(DISTINCT app_instance_id) AS c FROM events").fetchone()["c"]
        rows = connection.execute(
            "SELECT event_name, COUNT(*) AS c FROM events GROUP BY event_name ORDER BY c DESC"
        ).fetchall()
        active_rows = connection.execute(
            "SELECT params_json FROM events WHERE event_name = 'monitoring_disabled'"
        ).fetchall()
        custom_rows = connection.execute(
            "SELECT params_json FROM events WHERE event_name = 'custom_sound_added'"
        ).fetchall()

    total_active_ms = 0
    total_custom_added = 0
    max_custom_sounds = 0
    for row in active_rows:
        params = safe_json(row["params_json"])
        total_active_ms += int(params.get("active_duration_ms") or 0)
    for row in custom_rows:
        params = safe_json(row["params_json"])
        total_custom_added += int(params.get("added_count") or 0)
        max_custom_sounds = max(max_custom_sounds, int(params.get("current_custom_sounds_count") or 0))

    return {
        "total_events": total_events,
        "total_devices": total_devices,
        "events_by_name": {row["event_name"]: row["c"] for row in rows},
        "total_active_ms": total_active_ms,
        "total_active_hours": round(total_active_ms / 1000 / 60 / 60, 2),
        "total_custom_sounds_added": total_custom_added,
        "max_custom_sounds_on_device": max_custom_sounds,
    }


def recent_events(limit: int) -> list[dict]:
    with db_connect() as connection:
        rows = connection.execute(
            """
            SELECT id, received_at_ms, app_instance_id, event_name, event_time_ms, platform, app_version, params_json
            FROM events
            ORDER BY id DESC
            LIMIT ?
            """,
            (limit,),
        ).fetchall()
    return [
        {
            "id": row["id"],
            "received_at_ms": row["received_at_ms"],
            "app_instance_id": row["app_instance_id"],
            "event_name": row["event_name"],
            "event_time_ms": row["event_time_ms"],
            "platform": row["platform"],
            "app_version": row["app_version"],
            "params": safe_json(row["params_json"]),
        }
        for row in rows
    ]


def safe_json(raw: str) -> dict:
    try:
        parsed = json.loads(raw or "{}")
        return parsed if isinstance(parsed, dict) else {}
    except json.JSONDecodeError:
        return {}


if __name__ == "__main__":
    init_db()
    print(f"Fall Ouch metrics backend listening on {HOST}:{PORT}, db={DB_PATH}")
    ThreadingHTTPServer((HOST, PORT), MetricsHandler).serve_forever()
