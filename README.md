# JerokySys - MS2 Cobro de Facturas

Microservicio de facturación del sistema JerokySys. Es dueño de `Invoice`,
`InvoiceDetail`, `TaxStamp` y de los enumerados `InvoiceStatus`, `SaleCondition`
y `VatType`.

La factura es un documento **autocontenido**:

- tiene un `studentId` opcional, como referencia para filtrar;
- no usa el alumno como fuente de los datos del cliente;
- guarda un snapshot del cliente (`customerName`, `customerRuc`,
  `customerAddress`);
- guarda un snapshot del emisor (`issuer*`);
- guarda un snapshot del timbrado (`stampNumber`, `stampValidFrom`,
  `stampValidUntil`).

El frontend resuelve los datos fiscales del pagador —el guardián, o el alumno
si no tiene guardián— y los envía en el `InvoiceDTO`.

## Dependencias del ecosistema

| Componente | Rol |
|---|---|
| `commons` | JAR con las entidades, los DTOs generados y las clases base (`BaseController`, `AbstractBaseService`, `Response`, `BusinessException`, `GlobalExceptionHandler`). |
| `eureka` | Registro de servicios. |
| PostgreSQL | Base compartida. El esquema lo migra `commons`. |

`Student`, `Enrollment` y `Exam` siguen existiendo en `commons` para otros
microservicios, pero `invoices` ya no los usa. `Enrollment` y `Exam` solo se
referencian con el token `[MATRICULA#12]` o `[EXAMEN#45]` dentro de
`InvoiceDetail.description`.

## Requisitos

- Java 17
- Maven
- El artefacto `com.jerokysys:commons:0.0.3-SNAPSHOT` instalado en el
  repositorio local
- PostgreSQL con el esquema de `commons` aplicado

## Configuración

```powershell
cp src/main/resources/config/private.properties.example src/main/resources/config/private.properties
```

```properties
db.url=jdbc:postgresql://<host>:<puerto>/<base>
db.username=<usuario>
db.password=<password>
JWT_SECRET=<secreto-compartido-con-auth>
```

El esquema lo migra `commons`; este servicio corre con `ddl-auto=validate` y
solo valida al arrancar.

### Variables de entorno

| Variable | Default | Para qué sirve |
|---|---|---|
| `INVOICES_SERVER_PORT` | `8083` | Puerto del servicio. |
| `LOG_LEVEL` | `INFO` | Nivel raíz del logback. |
| `CACHE_ENABLED` | `true` | Prende o apaga la caché sin tocar código. |
| `CACHE_LOG_LEVEL` | `INFO` | Nivel de los logs `Cache GET/SET/DELETE`. |
| `REDIS_HOST` | `localhost` | Host de Redis. |
| `REDIS_PORT` | `6379` | Puerto de Redis. |
| `REDIS_PASSWORD` | vacío | Password de Redis, si tiene. |

## Cómo correr

```powershell
.\mvnw.cmd spring-boot:run
```

Se registra en Eureka como `invoices`. Por detrás del gateway la ruta pública es
`/api/v1`.

- Swagger UI: `http://localhost:8083/swagger-ui.html`
- JSON de OpenAPI: `http://localhost:8083/v3/api-docs`

## Convenciones

Sigue el patrón de `academic`:

- Los controllers extienden `BaseController<Entidad, DTO, PagedFilter, Service>`
  y solo agregan endpoints de consulta por filtro.
- Los services extienden `AbstractBaseService` y sobrescriben `create`,
  `update`, `search`, `toDto` y `toEntity`.
- Los repos son interfaces que extienden `BaseRepository` y usan derived
  queries.
- El mapeo entidad/DTO es MapStruct con `componentModel = "spring"`.
- Las listas de consulta por filtro devuelven `Response<List<XDTO>>` sin
  paginar.
- Los errores de negocio son `BusinessException` con un `errorCode` estable.

## Modelo fiscal

### Precios con IVA incluido

`InvoiceDetail.unitPrice` es el precio de venta con IVA incluido.
`subtotal = quantity * unitPrice`.

```text
sales5  = suma de subtotales GRAVADA_5
sales10 = suma de subtotales GRAVADA_10
exempt  = suma de subtotales EXENTA

iva5    = round(sales5 / 21, 2)
iva10   = round(sales10 / 11, 2)

totalAmount = exempt + sales5 + sales10
totalIva    = iva5 + iva10
```

**El IVA no se suma al total**: ya está incluido en los valores de venta.
`totalIva` es informativo para la liquidación.

### Conceptos del detalle

Cada línea guarda:

- `conceptType`: `MATRICULA`, `CUOTA`, `EXAMEN` u `OTRO`;
- `conceptId`: el id de la inscripción o del examen, según el concepto;
- `description`: texto legible para el comprobante;
- `correspondingMonth`: `AAAA-MM` para las cuotas.

El backend no valida que el `conceptId` exista en Academic: es una referencia
externa. La UI es la que selecciona la inscripción.

### Timbrados y numeración

`TaxStamp` guarda:

- `businessRuc`
- `stampNumber`
- `establishmentCode`
- `expeditionPoint`
- `validFrom`, `validUntil`
- `nextSequence`

Al crear una factura, el backend:

1. bloquea el timbrado con `PESSIMISTIC_WRITE`;
2. valida que `issueDate` esté dentro de la vigencia;
3. valida que `issuerRuc` coincida con `businessRuc` del timbrado;
4. toma `nextSequence`;
5. genera `invoiceNumber = %03d-%03d-%07d`;
6. incrementa `nextSequence`;
7. guarda la factura y sus líneas en una sola transacción.

El número nunca lo genera el cliente. El correlativo consumido no se reutiliza.

### Inmutabilidad

- Solo `PENDING` se puede editar.
- En edición no cambian número, timbrado, snapshots del emisor ni `issueDate`.
- `PAID` y `CANCELLED` son inmutables.
- `DELETE` solo aplica a `PENDING` y es lógico. Las líneas también se marcan
  como borradas.
- A futuro, la UI debería usar `CANCELLED` en lugar de `DELETE`.

## Endpoints

### Facturas

Heredados de `BaseController`:

| Método | Ruta |
|---|---|
| `POST` | `/invoices` |
| `PUT` | `/invoices/{id}` |
| `GET` | `/invoices/{id}` |
| `GET` | `/invoices/all` |
| `GET` | `/invoices?page=0&size=10&sort=issueDate&direction=DESC` |
| `DELETE` | `/invoices/{id}` |

Propios:

| Método | Ruta |
|---|---|
| `PATCH` | `/invoices/{id}/status?status=PAID` |
| `GET` | `/invoices/by-student/{studentId}` |
| `GET` | `/invoices/by-status?status=PAID` |
| `GET` | `/invoices/by-sale-condition?condition=CREDITO` |
| `GET` | `/invoices/by-number?invoiceNumber=001-001-0000123` |
| `GET` | `/invoices/by-stamp-number?stampNumber=12345678` |
| `GET` | `/invoices/by-customer-ruc?ruc=80012345-6` |
| `GET` | `/invoices/by-date?date=2026-03-01` |
| `GET` | `/invoices/by-date-range?from=2026-01-01&to=2026-03-31` |
| `GET` | `/invoices/by-amount-range?min=100000&max=500000` |
| `GET` | `/invoices/search-by-customer-name?customerName=Perez` |

### Líneas de detalle

Solo lectura:

| Método | Ruta |
|---|---|
| `GET` | `/invoice-details?page=0&size=10` |
| `GET` | `/invoice-details/{id}` |
| `GET` | `/invoice-details/by-invoice/{invoiceId}` |
| `GET` | `/invoice-details/by-month?month=2026-03` |
| `GET` | `/invoice-details/by-vat-type?vatType=GRAVADA_10` |
| `GET` | `/invoice-details/by-concept?conceptType=MATRICULA&conceptId=12` |
| `GET` | `/invoice-details/by-subtotal-range?min=100000&max=500000` |
| `GET` | `/invoice-details/search-by-description?description=matricula` |

### Timbrados

| Método | Ruta | Rol |
|---|---|---|
| `GET` | `/tax-stamps` | ADMIN, SECRETARY |
| `GET` | `/tax-stamps/all` | ADMIN, SECRETARY |
| `GET` | `/tax-stamps/{id}` | ADMIN, SECRETARY |
| `GET` | `/tax-stamps/valid?date=2026-03-01` | ADMIN, SECRETARY |
| `POST` | `/tax-stamps` | ADMIN |
| `PUT` | `/tax-stamps/{id}` | ADMIN |
| `DELETE` | `/tax-stamps/{id}` | ADMIN |

## Reglas de negocio

Al crear o actualizar una factura:

1. `taxStampId` es obligatorio al crear.
2. `studentId` es opcional, pero la UI siempre lo envía; la factura queda
   vinculada al alumno para filtrar.
3. `customerName` y `customerRuc` son obligatorios. La factura solo acepta RUC,
   no CI.
3. `issuerName`, `issuerRuc`, `issuerActivity` e `issuerAddress` son
   obligatorios al crear.
4. La factura necesita al menos una línea.
5. Cada línea necesita `quantity > 0`, `unitPrice >= 0` y `vatType`.
6. `subtotal`, totales y numeración los calcula siempre el servidor.
7. El timbrado debe estar vigente en `issueDate`.
8. `issuerRuc` debe coincidir con el `businessRuc` del timbrado.
9. Las líneas de un `PUT` reemplazan por completo a las existentes.

Cambio de estado de pago: solo `PENDING` puede cambiar, y únicamente a `PAID` o
`CANCELLED`. Los dos estados finales no tienen salida.

## OpenAPI

`src/main/resources/openapi.yaml` documenta el contrato. **No genera código**:
los controllers se escriben a mano y los DTOs vienen del JAR de `commons`.

## Redis cache

El servicio cachea DTOs en Redis para proteger la base de datos. Cada
microservicio usa Redis en `localhost:6379`, base `0`.

- Claves: `<key-prefix>:<domain>:<id>`, por ejemplo `invoices:invoice:5`
- Dominios y TTL: `cache.domains.*` en `application.properties`
  (`invoice`, `invoicedetail`, `taxstamp`)
- Lecturas por id con `@Cacheable`; escrituras con `@CacheEvict`
- Listas y páginas nunca se cachean completas: cada elemento se guarda por
  separado con `DomainCache`
- Si Redis no está disponible, la request sigue contra PostgreSQL y el fallo se
  loguea

## Migración

El cambio de esquema está en `commons`:

```powershell
cd jerokysys-commons
.\mvnw.cmd -Pliquibase process-resources liquibase:update
```

Changesets relevantes:

- `18-create-tax-stamps`
- `19-drop-legacy-invoices`
- `20-create-invoices`
- `21-create-invoice-details`
- `22-seed-tax-stamps`

El changeset `19` elimina las tablas de facturas anteriores. Fue una decisión
deliberada porque los datos existentes eran de prueba.

Después de migrar:

1. reiniciar `invoices`;
2. limpiar Redis (`invoices:*`) porque cambió la forma de los DTOs;
3. verificar que `ddl-auto=validate` pase al arrancar.

## Notas

- `TaxStamp` es una entidad nueva en `commons`; todos los servicios que escanean
  `com.jerokysys.commons.domain.entity` la validan al arrancar. Migrar antes de
  reiniciar cualquier servicio.
- El `GlobalExceptionHandler` de `commons` responde HTTP 400 para toda
  `BusinessException`, incluso cuando el error de negocio sería 404 o 409.
- La UI actual todavía usa el contrato anterior. Queda para la próxima
  iteración.
