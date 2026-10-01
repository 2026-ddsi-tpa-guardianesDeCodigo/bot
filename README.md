# DonaTrack — Bot de Telegram

Quinto componente del TP "DonaTrack" (cátedra Diseño de Sistemas de Información, UTN.BA).
Capa de presentación por chat: recibe un comando, muestra un menú de botones, pide los datos
que hagan falta y llama a la API REST del componente correspondiente. No tiene lógica de
dominio propia ni base de datos.

Desde la Entrega 5 habla con los **4 componentes** (Donadores y Entidades, Donaciones,
Logística e Incentivos) — antes (Entrega 4) solo hablaba con Donadores y Entidades.

Basado en el patrón de la práctica de cátedra "Copia.me" (librería `org.telegram:telegrambots`,
`TelegramLongPollingBot`, token por variable de entorno).

**El bot corre siempre local** (long polling desde la PC) — nunca se despliega a Render. Para la
instancia presencial, tiene que estar corriendo en una sola máquina.

## Requisitos

- Java 21, Maven.
- Un bot creado con [`@BotFather`](https://t.me/BotFather) en Telegram (comando `/newbot`), que
  da un token y un username.
- Los 4 backends corriendo (local o en Render) — con cold start de 30-90s si es Render free tier.

## Variables de entorno

| Variable | Obligatoria | Descripción |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | Sí | Token que da `@BotFather`. **Nunca lo commitees** — quien tiene el token, tiene el bot. |
| `TELEGRAM_BOT_USERNAME` | Sí | Username del bot (sin `@`), el que elegiste al crearlo. |
| `DONADORES_URL` | No | URL de Donadores y Entidades. Default: el deploy de Render. |
| `DONACIONES_URL` | No | URL de Donaciones. Default: el deploy de Render. |
| `LOGISTICA_URL` | No | URL de Logística. Default: el deploy de Render. |
| `INCENTIVOS_URL` | No | URL de Incentivos. Default: el deploy de Render. |
| `PORT` | No | Puerto del bot (solo expone `/actuator/health`, no hay endpoints propios). Default `8085`. |

## Correr

```bash
export TELEGRAM_BOT_TOKEN=tu_token
export TELEGRAM_BOT_USERNAME=tu_bot_username
# opcionales, si querés probar contra local en vez de Render:
export DONADORES_URL=http://localhost:8081
export DONACIONES_URL=http://localhost:8083
export LOGISTICA_URL=http://localhost:8084
export INCENTIVOS_URL=http://localhost:8082
mvn spring-boot:run
```

Alternativa más cómoda para uso local: creá un `.env` (ya está en `.gitignore`, **nunca se
commitea**) con

```
TELEGRAM_BOT_TOKEN=tu_token
TELEGRAM_BOT_USERNAME=tu_bot_username
```

y corré `./correr-local.sh`, que lo carga y levanta el bot. Como el bot nunca se despliega a
Render, no hace falta configurar estas credenciales ahí.

## Uso

En Telegram, hablarle al bot y mandar `/start`. Elegí con qué módulo querés hablar (Donadores y
Entidades, Donaciones, Logística o Incentivos) y a partir de ahí navegá por menú. Comandos:

- `/start` — reinicia la conversación desde cero (vuelve a preguntar el módulo).
- `/menu` — vuelve al menú del módulo actual sin perderlo.
- `/cancelar` — cancela la acción en curso (por ejemplo, si empezaste a cargar una necesidad a
  mitad de camino).

### Funciones disponibles

**Donadores y Entidades** (con submenú Donador / Admin, como en la Entrega 4):
- Donador: registrarme, consultar mis estadísticas, buscar/listar donadores.
- Admin: crear/editar/buscar/listar entidades; alta/modificar/borrar/buscar necesidades.

**Donaciones:**
- Registrar donación, consultar por ID, listar todas / por donador.
- Cambiar estado de una donación, registrar queja sobre una donación.
- Crear producto, categoría e identificador; listar productos.

**Logística:**
- Crear/listar/buscar depósito, configurar su algoritmo de matchmaking.
- Consultar stock de un producto, listar asignaciones.
- Reportar entrega de un paquete.

**Incentivos:**
- Crear misión, crear insignia, asignar misión a un donador.
- Procesar donador (dispara la evaluación de su misión en curso).
- Consultar el progreso (misión en curso + insignias) de un donador.

## Diseño

- `DonaTrackBot` — único punto de entrada de Telegram (`onUpdateReceived`), arma menús, valida
  campos de forma genérica y despacha la ejecución de cada acción al handler del módulo que
  corresponda (`Accion.getModulo()`). La conversación por chat vive en
  `SesionManager`/`SesionUsuario` (en memoria, sin persistencia).
- `Accion` — enum que define, para cada flujo, a qué módulo pertenece y la secuencia ordenada de
  campos que hay que pedirle al usuario antes de llamar a la fachada.
- `handler/*Handler` — uno por módulo (`DonadoresHandler`, `DonacionesHandler`,
  `LogisticaHandler`, `IncentivosHandler`): arma el DTO con las respuestas de la sesión, llama al
  cliente HTTP correspondiente y formatea la respuesta para el chat. No duplican reglas de
  negocio — dejan que la fachada de cada componente valide y devuelvan el error tal cual si falla.
- `client/*Client` — un cliente HTTP por componente, mismo patrón: `JdkClientHttpRequestFactory`
  (soporta `PATCH`, a diferencia del `SimpleClientHttpRequestFactory` por default) + timeout
  explícito para no colgarse si el servicio no responde o está en cold start.

## Pendiente (Entrega 5, ver `CONTEXTO_E5.md` §8-D)

- Logging ECS + `trace.id` propagado + `chat_id_hash` (el bot hoy no loguea nada estructurado).
- Tests (el bot no tiene ninguno).
