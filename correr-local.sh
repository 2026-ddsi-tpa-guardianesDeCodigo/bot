#!/usr/bin/env bash
# Levanta el bot local cargando las credenciales desde .env (gitignored, nunca se commitea).
# La URL del gateway se puede pisar antes de llamar a este script, p. ej.:
#   GATEWAY_URL=http://localhost:8087 ./correr-local.sh
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "Falta .env (TELEGRAM_BOT_TOKEN y TELEGRAM_BOT_USERNAME) - ver README.md." >&2
  exit 1
fi

set -a
source .env
set +a

mvn spring-boot:run
