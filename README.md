# FlashReserve

Rebanada vertical de reserva de inventario para campañas de alta demanda.

Prueba técnica IAS: **TypeScript/Angular + Java 21 / Spring Boot WebFlux + MongoDB**.

Arquitectura **hexagonal** (puertos y adaptadores): el dominio no depende de Spring ni Mongo; HTTP, HMAC y Mongo son adaptadores.

```
domain/            modelo + excepciones + puertos (entrada/salida)
application/       casos de uso (solo hablan con puertos)
adapter/in/web     REST + DTOs HTTP
adapter/in/provider webhook HMAC del proveedor
adapter/in/scheduler worker de outbox
adapter/out/mongo  persistencia reactiva
adapter/out/audit  publicación de auditoría (log local o SQS)
config/            índices, semilla, propiedades
```

## Flujo

1. El cliente crea una reserva (`clientId`, `sku`, cantidad 1–5) con header `Idempotency-Key`.
2. El backend descuenta inventario de forma atómica y responde **sin esperar** auditoría.
3. Un proveedor confirma o rechaza con `POST /api/provider/events` firmado (HMAC-SHA256).
4. La UI consulta el estado y hace **polling cada 2 s** mientras esté `PENDING` (sin recargar la página).

SKUs de prueba: `SKU-FLASH-A` (10 uds) y `SKU-FLASH-B` (3 uds).

## Cómo ejecutar

### Docker Compose (recomendado)

```bash
docker compose -f infra/docker/docker-compose.yml up --build
```

- UI: http://localhost:4200
- API: http://localhost:8080
- MongoDB: localhost:27017

### Dev separado

Requisitos: **JDK 21** (`JAVA_HOME`), Node 22, Docker para Mongo.

```bash
docker run --name flashreserve-mongo -p 27017:27017 -d mongo:7.0
cd backend && ./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
cd frontend && npm install && npm start       # proxy a localhost:8080
```

Variables (ningún secreto real en el repo):

| Variable | Default local |
|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/flashreserve` |
| `PROVIDER_HMAC_SECRET` | `dev-secret-change-me` |
| `CORS_ORIGINS` | `http://localhost:4200,http://localhost:8080` |
| `APP_DEMO_ENABLED` | `true` (local). `false` en K8s. |
| `OUTBOX_SQS_QUEUE_URL` | vacío = log local. URL de SQS = publica a la cola. |

Plantilla: `.env.example`.

## Cómo probar el flujo principal

### UI

1. Abrir http://localhost:4200. Deben verse los dos SKUs.
2. Reservar 2 unidades de `SKU-FLASH-A` → estado `PENDING` y el stock baja.
3. Pulsar **Reservar** otra vez con la misma `Idempotency-Key` → misma reserva, stock igual.
4. En **Simular proveedor**, secuencia `1`, `CONFIRMED`. La UI pasa a `CONFIRMED` en ≤ 5 s.
5. Reenviar el evento o una secuencia menor con `REJECTED` → no revierte.
6. Una reserva `REJECTED` **devuelve el stock**. `CONFIRMED` lo deja comprometido.

El panel de simulación llama a `/api/demo/provider-events` (solo local). El webhook real `POST /api/provider/events` exige HMAC; el secreto no vive en el navegador. En Kubernetes el demo va desactivado (`APP_DEMO_ENABLED=false`).

### HTTP

```bash
curl -s http://localhost:8080/api/inventory

curl -s -H "Content-Type: application/json" -H "Idempotency-Key: demo-key-1" \
  -d '{"clientId":"c1","sku":"SKU-FLASH-A","quantity":2}' \
  http://localhost:8080/api/reservations
```

Firma del evento (Python 3). El body firmado debe ser **exactamente** el que se envía:

```python
import hmac, hashlib, json, urllib.request
secret = b"dev-secret-change-me"
body = json.dumps({
  "eventId": "evt-1",
  "reservationId": "RESERVA_ID",
  "sequence": 1,
  "status": "CONFIRMED",
  "occurredAt": "2026-09-16T12:00:00Z"
}, separators=(",", ":"))
sig = hmac.new(secret, body.encode(), hashlib.sha256).hexdigest()
req = urllib.request.Request(
  "http://localhost:8080/api/provider/events",
  data=body.encode(),
  headers={"Content-Type": "application/json", "X-Provider-Signature": sig},
  method="POST",
)
print(urllib.request.urlopen(req).read().decode())
```

## API

| Método | Ruta | Notas |
|---|---|---|
| `POST` | `/api/reservations` | Header `Idempotency-Key`. Body: `clientId`, `sku`, `quantity` (1–5). |
| `GET` | `/api/reservations/{id}` | Estado actual. |
| `GET` | `/api/inventory` | SKUs y stock. |
| `POST` | `/api/provider/events` | `eventId`, `reservationId`, `sequence`, `status`, `occurredAt` + `X-Provider-Signature`. |
| `POST` | `/api/demo/provider-events` | Mismo body, **sin** HMAC. Solo si `APP_DEMO_ENABLED=true`. |
| `GET` | `/actuator/health` | También `/actuator/health/liveness` y `/readiness`. |

Errores: 400 validación, 401 firma, 404 SKU/reserva, 409 sin stock.

Repetición de solicitud: el cliente envía `Idempotency-Key` (UUID). Índice único en Mongo. El reintento devuelve la reserva original y no vuelve a descontar inventario.

## Pruebas

```bash
cd backend && ./mvnw test     # Windows: $env:JAVA_HOME debe ser JDK 21; .\mvnw.cmd test
cd frontend && npm test
```

Cubre: descuento de stock, liberación si `REJECTED`, idempotencia, concurrencia anti-sobreventa, duplicados/secuencia, HMAC, demo local y polling de 2 s en el cliente.

## Artefactos operativos

- Docker: `infra/docker/`
- CI: `.github/workflows/ci.yml` (compila y ejecuta tests; no se despliega)
- Kubernetes: `infra/k8s/flashreserve.yaml` (representativo, probes Actuator)
- Terraform SQS: `infra/terraform/` — `terraform plan` only, no aplicar

## Decisiones

- ADRs: `docs/adr/`
- Supuestos de negocio: `docs/supuestos.md`
- Deuda y recortes: `docs/deuda.md`
- Uso de IA: `docs/uso-ia.md`
