# Fall Ouch Metrics Backend

Минимальный backend без внешних Python-зависимостей: HTTP API + SQLite.

## API

- `GET /health` - проверка, что сервис жив.
- `POST /v1/events` - приём событий из приложения. Нужен header `X-Fall-Ouch-Write-Key`.
- `GET /v1/summary` - агрегированная статистика. Нужен header `X-Fall-Ouch-Admin-Key`.
- `GET /v1/events?limit=100` - последние события. Нужен header `X-Fall-Ouch-Admin-Key`.

## События

- `monitoring_enabled`
- `monitoring_disabled` с `active_duration_ms`
- `custom_sound_added` с `added_count` и `current_custom_sounds_count`
- `widget_installed`
- `widget_removed`
- `widget_pin_requested`
- `widget_toggle`
- `widget_open`
- `language_selected`

## Ручная установка на сервере

```bash
sudo useradd --system --home /opt/fall-ouch-metrics --shell /usr/sbin/nologin fallouch || true
sudo mkdir -p /opt/fall-ouch-metrics
sudo cp server.py /opt/fall-ouch-metrics/server.py
sudo chown -R fallouch:fallouch /opt/fall-ouch-metrics

sudo tee /etc/fall-ouch-metrics.env >/dev/null <<'EOF'
FALL_OUCH_HOST=0.0.0.0
FALL_OUCH_PORT=8787
FALL_OUCH_DB=/opt/fall-ouch-metrics/fall_ouch_metrics.sqlite3
FALL_OUCH_WRITE_KEY=replace-with-client-write-key
FALL_OUCH_ADMIN_KEY=replace-with-private-admin-key
EOF

sudo cp fall-ouch-metrics.service /etc/systemd/system/fall-ouch-metrics.service
sudo systemctl daemon-reload
sudo systemctl enable --now fall-ouch-metrics
```

## Как смотреть метрики

```bash
curl -H "X-Fall-Ouch-Admin-Key: $FALL_OUCH_ADMIN_KEY" \
  http://127.0.0.1:8787/v1/summary

sqlite3 /opt/fall-ouch-metrics/fall_ouch_metrics.sqlite3 \
  "select event_name, count(*) from events group by event_name order by count(*) desc;"
```

С локального Mac удобнее так, чтобы не копировать admin-key:

```bash
ssh root@89.125.81.120 \
  'set -a; . /etc/fall-ouch-metrics.env; curl -s -H "X-Fall-Ouch-Admin-Key: $FALL_OUCH_ADMIN_KEY" http://127.0.0.1:8787/v1/summary'

ssh root@89.125.81.120 \
  'sqlite3 /opt/fall-ouch-metrics/fall_ouch_metrics.sqlite3 "select event_name, count(*) from events group by event_name order by count(*) desc;"'
```

Для Google Play лучше поставить домен и HTTPS через nginx/Caddy. Для текущей debug-сборки Android отправляет события на `http://89.125.81.120:8787/v1/events`.
