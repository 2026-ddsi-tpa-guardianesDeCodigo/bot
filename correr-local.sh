#!/usr/bin/env bash
# Levanta el bot local cargando las credenciales desde .env (gitignored, nunca se commitea).
# Las URLs de los 4 backends se pueden pisar antes de llamar a este script, p. ej.:
#   DONADORES_URL=http://localhost:8081 ./correr-local.sh
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
