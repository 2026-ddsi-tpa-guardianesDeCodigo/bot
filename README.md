# DonaTrack — Bot de Telegram

Quinto componente del TP "DonaTrack" (cátedra Diseño de Sistemas de Información, UTN.BA,
Entrega 4). Capa de presentación por chat: recibe un comando, muestra un menú de botones,
pide los datos que hagan falta y llama a la API REST de **Donadores y Entidades**. No tiene
lógica de dominio propia, ni base de datos, ni se comunica con los otros 3 componentes.

Basado en el patrón de la práctica de cátedra "Copia.me" (librería `org.telegram:telegrambots`,
`TelegramLongPollingBot`, token por variable de entorno).

**El bot corre siempre local** (long polling desde la PC) — nunca se despliega a Render. Para la
instancia presencial, tiene que estar corriendo en una sola máquina.

## Requisitos

- Java 21, Maven.
- Un bot creado con [`@BotFather`](https://t.me/BotFather) en Telegram (comando `/newbot`), que
  da un token y un username.
- La API de Donadores y Entidades corriendo (local o en Render).

## Variables de entorno

| Variable | Obligatoria | Descripción |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | Sí | Token que da `@BotFather`. **Nunca lo commitees** — quien tiene el token, tiene el bot. |
| `TELEGRAM_BOT_USERNAME` | Sí | Username del bot (sin `@`), el que elegiste al crearlo. |
| `DONADORES_URL` | No | URL de Donadores y Entidades. Default: el deploy de Render. |
| `PORT` | No | Puerto del bot (solo expone `/actuator/health`, no hay endpoints propios). Default `8085`. |

## Correr

```bash
export TELEGRAM_BOT_TOKEN=tu_token
export TELEGRAM_BOT_USERNAME=tu_bot_username
export DONADORES_URL=http://localhost:8081   # opcional, si querés probar contra local
mvn spring-boot:run
```

## Uso

En Telegram, hablarle al bot y mandar `/start`. Elige tipo de usuario (Donador / Admin) con
botones, y a partir de ahí navega por menú. Comandos:

- `/start` — reinicia la conversación desde cero (vuelve a preguntar el tipo de usuario).
- `/menu` — vuelve al menú principal sin perder el tipo de usuario elegido.
- `/cancelar` — cancela la acción en curso (por ejemplo, si empezaste a cargar una necesidad a
  mitad de camino).

### Funciones disponibles

**Donador:**
- Registrarme
- Consultar mis estadísticas
- Buscar donador por ID
- Listar donadores

**Admin:**
- Crear / editar / buscar / listar entidades
- Alta / modificar / borrar / buscar necesidades (por ID)

## Diseño

- `DonaTrackBot` — único punto de entrada de Telegram (`onUpdateReceived`), delega la
  conversación a `SesionManager`/`SesionUsuario` (estado por chat, en memoria, sin persistencia)
  y las llamadas salientes a `DonadoresClient`.
- `Accion` — enum que define, para cada flujo, la secuencia ordenada de campos que hay que
  pedirle al usuario antes de llamar a la fachada.
- `DonadoresClient` — único cliente HTTP saliente del bot, con timeout explícito (no confiar en
  el default de `RestClient`, que no tiene).
