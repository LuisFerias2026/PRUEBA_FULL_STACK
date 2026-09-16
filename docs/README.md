# FlashReserve

FlashReserve es una rebanada vertical de reserva de inventario para campanas de alta demanda.

## Stack

- Frontend: Angular 19, TypeScript, RxJS y polling de estado.
- Backend: Java 21, Spring Boot 3.5.3 y Spring WebFlux.
- Persistencia: MongoDB reactivo.
- Operacion: Docker Compose, Nginx, Kubernetes, Terraform y GitHub Actions.

## Flujo principal

1. El cliente crea una reserva con `Idempotency-Key`.
2. El backend descuenta inventario atomico y devuelve la reserva.
3. El proveedor envia un evento `CONFIRMED` o `REJECTED` con HMAC.
4. La UI consulta el estado cada 2 segundos mientras permanece `PENDING`.
5. Eventos duplicados o antiguos no repiten efectos ni revierten un estado mas reciente.

El proyecto incluye una ruta local para simular eventos del proveedor. Esa ruta esta desactivada en Kubernetes.

## Ejecucion

La opcion recomendada es Docker Compose:

```bash
docker compose -f infra/docker/docker-compose.yml up --build
```

Puertos locales habituales:

- Frontend: `4200` (o el puerto publicado configurado).
- Backend: `8080` (o el puerto publicado configurado).
- MongoDB: `27017`.

Para desarrollo separado se requiere JDK 21, Node 22 y MongoDB.

## Validacion

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
npm install
npm test
```

Las pruebas cubren idempotencia, concurrencia anti-sobreventa, secuencias de proveedor, HMAC, liberacion de inventario y el flujo de polling.

## Infraestructura

- `infra/docker/`: imagenes, Compose y Nginx.
- `infra/k8s/`: manifiesto representativo con probes de Actuator.
- `infra/terraform/`: infraestructura SQS de referencia; no se aplica automaticamente.
- `.github/workflows/ci.yml`: compilacion y pruebas.
