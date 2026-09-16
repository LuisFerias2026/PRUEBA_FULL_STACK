# Uso acotado de IA

La IA se utilizo puntualmente como apoyo para revisar el scaffolding, proponer alternativas y mejorar la redaccion del README. No sustituyo el criterio tecnico ni la implementacion.

La logica de negocio, las decisiones de arquitectura, los tests y la validacion final fueron revisados y ejecutados por el autor. Los requisitos y reglas de negocio se tomaron del PDF de la prueba.

## Validacion realizada

- Backend con `./mvnw test` usando Java 21.
- Frontend con `npm test` y ChromeHeadless.
- Docker Compose con llamadas HTTP al flujo de reserva, repeticion de `Idempotency-Key` y eventos del proveedor.

No se compartieron secretos reales con herramientas externas.
