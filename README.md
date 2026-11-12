# JerokySys - MS2 Cobro de Facturas

Microservicio de cobro del sistema JerokySys. Es dueño de `Invoice`, `InvoiceDetail` y
de los enumerados `InvoiceStatus` y `SaleCondition`.

## Dependencias del ecosistema

| Componente | Rol |
|---|---|
| `commons` | JAR con las entidades, los DTOs generados y las clases base (`BaseController`, `AbstractBaseService`, `Response`, `BusinessException`, `GlobalExceptionHandler`). Este servicio lo consume sin modificarlo. |
| `eureka` | Registro de servicios. |
| `academic` | MS hermano. Comparte la base de datos y el esquema. |
| MS Personas | Dueño de `Student`. |
| MS3 | Dueño de `Enrollment` y `Exam`. |

`Student`, `Enrollment` y `Exam` son **entidades externas**: este servicio solo escribe
el identificador y deja que la FK lo valide. Nunca lee sus tablas.

## Requisitos

- Java 17
- Maven
- El artefacto `com.jerokysys:commons:0.0.3-SNAPSHOT` instalado en el repositorio local
- PostgreSQL con el esquema de `commons` aplicado

## Configuración

Las credenciales no están en el repo. Copiá el ejemplo y completalo:

```
cp src/main/resources/config/private.properties.example src/main/resources/config/private.properties
```

```properties
db.url=jdbc:postgresql://<host>:<puerto>/<base>
db.username=<usuario>
db.password=<password>
```

El esquema lo migra `commons`; este servicio corre con `ddl-auto=validate` y solo valida
al arrancar.

### Variables de entorno

| Variable | Default | Para que sirve |
|---|---|---|
| `INVOICES_SERVER_PORT` | `8083` | Puerto del servicio. |
| `LOG_LEVEL` | `INFO` | Nivel raiz del logback. |
| `CACHE_ENABLED` | `true` | Prende o apaga la cache sin tocar codigo. |
| `CACHE_LOG_LEVEL` | `INFO` | Nivel de los logs `Cache GET/SET/DELETE`. |
| `REDIS_HOST` | `localhost` | Host de Redis. |
| `REDIS_PORT` | `6379` | Puerto de Redis. |
| `REDIS_PASSWORD` | vacio | Password de Redis, si tiene. |

## Como correr

```
./mvnw spring-boot:run
```

Se registra en Eureka como `invoices`. Por detras del gateway la ruta publica es
`/api/v1`.

- Swagger UI: `http://localhost:8083/swagger-ui.html`
- JSON de OpenAPI: `http://localhost:8083/v3/api-docs`

## Convenciones

Sigue el mismo patron que `academic`:

- Los controllers extienden `BaseController<Entidad, DTO, PagedFilter, Service>` y solo
  agregan los endpoints de consulta por filtro. Los 6 CRUD vienen heredados.
- Los services extienden `AbstractBaseService` y sobrescriben `create`, `update`,
  `search`, `toDto` y `toEntity`.
- Los repos son interfaces que extienden `BaseRepository` y usan **solo derived queries**.
  No hay `Specification` ni `@Query`.
- El mapeo entidad/DTO es MapStruct con `componentModel = "spring"`.
- Las listas de consulta por filtro devuelven `Response<List<XDTO>>` sin paginar.
- Los errores de negocio son `BusinessException` con un `errorCode` estable
  (`ENTITY_NOT_FOUND`, `VALIDATION_ERROR`, `CONFLICT`, `STUDENT_NOT_FOUND`).

La unica desviacion respecto de `academic` es que `InvoiceService.create` y `.update`
llevan `@Transactional`: guardar la cabecera, sus lineas y los totales recalculados es
una sola unidad de trabajo y depende del `CascadeType.ALL` de `Invoice.details`.

### Dos desviaciones mas que conviene conocer

1. **Los errores siempre responden HTTP 400.** El `GlobalExceptionHandler` de `commons`
   traduce toda `BusinessException` a 400 y no serializa el `errorCode` en el cuerpo, asi
   que un recurso inexistente y un dato invalido se ven iguales. Es el mismo defecto que
   tiene `academic`; queda pendiente corregirlo en `commons`.
2. **No hay seguridad.** El servicio acepta cualquier llamada. La autorizacion tendria
   que vivir en el gateway.

## Endpoints

### Facturas

Heredados de `BaseController`:

| Metodo | Ruta |
|---|---|
| `POST` | `/invoices` |
| `PUT` | `/invoices/{id}` |
| `GET` | `/invoices/{id}` |
| `GET` | `/invoices/all` |
| `GET` | `/invoices?page=0&size=10&sort=id&direction=ASC` |
| `DELETE` | `/invoices/{id}` |

Propios:

| Metodo | Ruta |
|---|---|
| `PATCH` | `/invoices/{id}/status?status=PAID` |
| `GET` | `/invoices/by-student/{studentId}` |
| `GET` | `/invoices/by-status?status=PAID` |
| `GET` | `/invoices/by-sale-condition?condition=CREDITO` |
| `GET` | `/invoices/by-number?invoiceNumber=001-001-0000123` |
| `GET` | `/invoices/by-timbrado?timbrado=001` |
| `GET` | `/invoices/by-tax-id?taxId=20123456789` |
| `GET` | `/invoices/by-date?date=2026-03-01` |
| `GET` | `/invoices/by-date-range?from=2026-01-01&to=2026-03-31` |
| `GET` | `/invoices/by-amount-range?min=100&max=500` |
| `GET` | `/invoices/search-by-business-name?businessName=Perez` |

### Lineas de detalle

Solo lectura: se escriben dentro de la factura, asi que este controller **no** extiende
`BaseController`.

| Metodo | Ruta |
|---|---|
| `GET` | `/invoice-details?page=0&size=10` |
| `GET` | `/invoice-details/{id}` |
| `GET` | `/invoice-details/by-invoice/{invoiceId}` |
| `GET` | `/invoice-details/by-month?month=2026-03` |
| `GET` | `/invoice-details/by-vat-rate?vatRate=10` |
| `GET` | `/invoice-details/by-concept?conceptType=MATRICULA&conceptId=12` |
| `GET` | `/invoice-details/by-amount-range?min=100&max=500` |
| `GET` | `/invoice-details/search-by-description?description=matricula` |

## Reglas de negocio

Al crear o actualizar una factura:

1. `invoiceNumber` tiene que matchear `AAA-AAA-NNNNNNN` y no puede estar repetido.
2. `studentId` es obligatorio. Se resuelve con un proxy de JPA, sin leer `students`.
3. La factura necesita al menos una linea de detalle.
4. `totalAmount` y `totalIva` se recalculan siempre desde las lineas; lo que mande el
   cliente se ignora. El IVA de cada linea es `amount * vatRate / 100`.
5. Las lineas de un `PUT` reemplazan por completo a las existentes.

Cambio de estado de pago: solo `PENDING` puede cambiar, y unicamente a `PAID` o
`CANCELLED`. Los dos estados finales no tienen salida.

## OpenAPI

`src/main/resources/openapi.yaml` documenta el contrato. **No genera codigo**: los
controllers se escriben a mano y los DTOs vienen del JAR de `commons`. Se comparte este
comportamiento con `academic`, donde el archivo es igualmente inerte.

## Redis cache

El servicio cachea DTOs en Redis para proteger la base de datos. Cada
microservicio usa Redis en `localhost:6379`, base `0`.

- Claves: `<key-prefix>:<domain>:<id>`, por ejemplo `invoices:invoice:5`
- Dominios y TTL: `cache.domains.*` en `src/main/resources/application.properties`
  (`0` = persistente, cualquier otro valor = segundos)
- Lecturas por id con `@Cacheable`; escrituras con `@CachePut` y `@CacheEvict`
- Listas y paginas nunca se cachean completas: cada elemento se guarda por
  separado con `DomainCache`
- Cada operacion se loguea como `Cache GET|SET|DELETE <clave>` con HIT/MISS y TTL;
  el nivel se controla con `logging.level.com.jerokysys.invoices.config.LoggingCache`
- Si Redis no esta disponible, la request sigue contra PostgreSQL y el fallo se loguea