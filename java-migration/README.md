# neipa-java

Migracion a **Java (Spring Boot + Spring Batch) + MySQL** del nucleo ETL de NEIPA, a partir del repo original en Python/Airflow (`include/`, `vendor_utilities/`).

El codigo Python original ya no esta en este repositorio: queda en [github.com/leandro529/neipa](https://github.com/leandro529/neipa). Las rutas `.py` que se citan en este README y en los comentarios del codigo son de ese repo.

## Alcance de esta migracion

Se migro, a pedido explicito:

- El **nucleo de orquestacion ETL** (`include/ETL.py`, `include/extract.py`, `include/load.py`) → un job de Spring Batch chunk-oriented (`AuditoriaBatchConfig`), que implementa el flujo principal de **CU001 (Auditar parametros de configuracion)** ya especificado en la primera y segunda entrega del trabajo practico.
- El fabricante **NSN** (`vendor_utilities/NsnOperations.py`) → `NsnVendorExtractor`. La clase original nunca se termino de implementar (el metodo `get_default_parameters` estaba declarado sin cuerpo), asi que esta version es una **extraccion simulada** (determinista, para poder probar el flujo de punta a punta) y queda documentada como tal en el propio codigo. Reemplazarla por la extraccion real contra el OSS de NSN no cambia el contrato (`VendorExtractor`) ni el resto del flujo.
- La **persistencia propia de NEIPA en MySQL** (RNF14/15): esquema (`schema.sql`, el mismo DDL ya presentado en la segunda entrega, mas las tablas nuevas de esta ampliacion), entidades JPA y el patron Factory de operaciones SQL (`SqlDialectFactory` / `MySqlDialectOperations`, migrado directamente de `include/loaders/utilitiesModule/MySqlOperations.py`).

**No se migro** (a pedido explicito):

- `TurboLoad.py` / turbodbc.
- `domoUtilities.py` (integracion con Domo).
- GCP / BigQuery (`gcpUtilities.py`, `BigQueryOperations.py`), Google Sheets (`repository_gsheet.py`).
- Los fabricantes Huawei y Ericsson (sus clases originales tampoco tenian implementacion real: `HuaweiOperations.py` estaba vacio y `EricsonOperations.py` truncado). Quedan como puntos de extension del Factory (`VendorExtractorFactory`), lanzando `UnsupportedOperationException`.
- Los motores MsSql, Postgres y BigQuery del lado de persistencia (usados en el original para leer bases de clientes u orquestar via Airflow/bcp/sqlcmd). Quedan como puntos de extension de `SqlDialectFactory`, tambien lanzando `UnsupportedOperationException` (RF20/RF21: el diseño ya contempla poder agregarlos sin tocar la logica de auditoria).
- CU002/CU004 (validacion de impacto, autorizacion/monitoreo de cambios masivos): la tabla `cambios_masivos` esta modelada (entidad + repositorio + lectura desde el dashboard) pero sin la logica de negocio propia de esos casos de uso todavia.

## Novedades de esta ampliacion (web de configuraciones, excepciones y dashboard de control)

A partir de la migracion base (solo CU001 disparado por un unico endpoint), se agrego una API REST mas completa y un frontend React (`frontend/`) para poder operar el sistema desde la web en vez de cargar datos a mano en MySQL:

- **`ValorEstandar`** (tabla `valores_estandar`, nueva): el rango permitido de cada parametro (RF02, RF04) ahora se carga y mantiene desde la web ("Configuraciones"), en vez de vivir hardcodeado en un mapa en memoria dentro de `EstandarProvider` como en la primera version de esta migracion.
- **`Excepcion`** (tabla `excepciones`, nueva): implementa RF05-08 ("excepciones de 3 niveles" — elemento puntual, segmento, o global), con ventana de vigencia. `AuditoriaItemProcessor` ahora consulta `ExcepcionService` antes de marcar un parametro fuera de rango como `DESVIO`: si hay una excepcion vigente que lo cubre, el resultado pasa a ser `EXCEPTUADO` (nuevo valor de `ResultadoAuditoria`) y queda registrada la excepcion aplicada (`auditorias.id_excepcion_aplicada`) para trazabilidad.
- CRUD REST completo (`GET`/`POST`/`PUT`/`DELETE`) para `Segmento`, `ElementoDeRed`, `ValorEstandar` y `Excepcion`, mas lectura de `CambioMasivo` y `Auditoria` — antes solo existia el POST que disparaba el job.
- **`/api/dashboard/resumen`**: agregados para el dashboard de control (conteos de auditorias por resultado, tasa de conformidad, excepciones activas, elementos por fabricante, cambios masivos por estado, ultimas 20 auditorias).
- `CorsConfig` habilita el origen del frontend (`http://localhost:5173` por defecto) para consumir esta API.
- `GlobalExceptionHandler` devuelve errores en JSON (`{"mensaje": "..."}`) en vez del whitelabel error page default de Spring, para que el frontend pueda mostrarlos.

## Estructura

```
src/main/java/com/neipa/
  NeipaApplication.java        Main de Spring Boot
  domain/                      Entidades JPA (Segmento, ElementoDeRed, Auditoria, CambioMasivo,
                                ValorEstandar, Excepcion, AlcanceExcepcion, ResultadoAuditoria)
  repository/                  Repositorios Spring Data JPA
  service/                     ExcepcionService (RF05-08), DashboardService (agregados)
  extraction/                  Abstraccion de fabricante (equivalente a vendor_utilities/*)
  validation/                  RF04: validacion de rango permitido
  persistence/                 Abstraccion de motor SQL (equivalente a loaders/utilitiesModule/*)
  exception/                   DatabaseException / RecursoNoEncontradoException
  batch/                       Job de Spring Batch para CU001
  web/                         Controllers REST (Segmento, ElementoDeRed, ValorEstandar, Excepcion,
                                CambioMasivo, Auditoria, Dashboard), CorsConfig, GlobalExceptionHandler
src/main/resources/
  application.yml              Config de datasource MySQL/XAMPP (RNF15) y origenes CORS
  schema.sql                   DDL de la segunda entrega + valores_estandar + excepciones
  data.sql                     Semilla de valores_estandar (mismo ejemplo que antes estaba hardcodeado)
src/test/java/com/neipa/validation/
  ValidadorRangoTest.java      Reproduce el caso de prueba CPF02 (Tabla 13)
frontend/                      SPA React (Vite) — ver frontend/README.md
docker-compose.yml             Levanta MySQL + backend + frontend juntos (ver "Como correrlo")
Dockerfile                     Build multi-stage del backend (Maven -> JRE)
frontend/Dockerfile            Build multi-stage del frontend (Node -> nginx)
```

## Como correrlo

### Opcion A: Docker (recomendada — no hace falta instalar Java, Maven, Node ni MySQL)

Requiere solo Docker Desktop (o Docker Engine + el plugin `compose`) corriendo. `docker-compose.yml` levanta los 3 servicios (MySQL, backend, frontend) y compila el backend y el frontend **dentro** de las imagenes — no usa nada instalado en tu maquina:

```bash
cp .env.example .env      # y completar las dos contraseñas (en Windows: copy .env.example .env)
docker compose up --build
```

La primera vez tarda unos minutos (baja las imagenes base y las dependencias de Maven/npm dentro de los contenedores). Cuando termine:

- Web: http://localhost:5173
- API: http://localhost:8080/api/dashboard/resumen
- MySQL: `localhost:3306` (usuario `root`, la contraseña `MYSQL_ROOT_PASSWORD` de `java-migration/.env`, base `neipa`) — por si queres conectarte con un cliente SQL a mirar los datos.

`schema.sql` y `data.sql` se aplican solos al arrancar el backend, asi que el Dashboard ya va a tener los datos semilla (2 segmentos, 7 elementos, etc.) apenas levante.

Para parar todo: `Ctrl+C` y despues `docker compose down` (agregale `-v` si tambien queres borrar los datos de MySQL y arrancar de cero).

Esto es aparte de `application.yml`, que sigue apuntando a `localhost`/XAMPP para la Opcion B — `docker-compose.yml` le pisa esa configuracion por variables de entorno (`SPRING_DATASOURCE_*`) solo dentro del contenedor, sin tocar el archivo.

### Opcion B: manual (Java/Maven/Node/MySQL instalados en tu maquina)

Requiere JDK 11+ y MySQL corriendo localmente (por ejemplo via XAMPP, tal como establece RNF15). Por defecto se conecta a `jdbc:mysql://localhost:3306/neipa` con usuario `root` sin password (ajustar en `application.yml` segun tu instalacion).

```bash
mvn clean test          # corre ValidadorRangoTest
mvn spring-boot:run     # levanta la aplicacion en el puerto 8080
```

Para la web, en otra terminal (ver `frontend/README.md`):

```bash
cd frontend
npm install
npm run dev              # http://localhost:5173
```

### Flujo tipico desde la web (cualquiera de las dos opciones)

Cargar un Segmento → cargar sus ElementoDeRed → cargar los ValorEstandar de los parametros a auditar → opcionalmente cargar Excepciones → apretar "Auditar" en el segmento → revisar el resultado en el Dashboard. Con los datos semilla de `data.sql` ya cargados, se puede saltar directo a "Auditar".

Tambien se puede disparar por curl, sin la web:

```bash
curl -X POST http://localhost:8080/api/auditorias/segmentos/1
```

## Nota importante sobre verificacion

**Este proyecto no pudo compilarse/instalarse ni ejecutarse dentro del entorno donde se genero**, porque ese entorno (tanto el sandbox en la nube como la maquina puente hacia tu computadora) no tiene acceso a Maven Central ni, en esta sesion, a `registry.npmjs.org` — se probo explicitamente y ambos devuelven 403 por politica de red/egress.

Lo que **si** se pudo verificar de forma aislada, compilando y corriendo con `javac`/`java` puros (sin depender de Maven), en la migracion base original:

- La logica de validacion de rango (`ValidadorRango`) contra los 6 valores de frontera exactos de la Tabla 13 de la segunda entrega: coincide 100%.
- La generacion de SQL de `MySqlDialectOperations` (existencia de tabla, truncate, delete, upsert): genera las sentencias esperadas.

El resto del backend (entidades JPA, repositorios, controllers REST, `ExcepcionService`, `DashboardService`) y todo el frontend se escribieron con cuidado siguiendo la API estandar de Spring Boot 2.7.x / Spring Batch 4.3.x y de React 18 / Vite 5, y se revisaron a mano archivo por archivo, pero **no se compilaron/instalaron**. Antes de dar esto por terminado:

```bash
mvn clean verify                 # backend
cd frontend && npm install && npm run build   # frontend
```

**El `docker-compose.yml` y los dos `Dockerfile` tampoco se pudieron probar**: el entorno donde se generaron tiene el cliente de Docker instalado pero sin el daemon corriendo (`docker info` da "failed to connect to the docker API... daemon is running?"), y ademas los builds necesitarian la misma salida a internet que ya esta bloqueada (Maven Central / npm). Se pudo validar que `docker-compose.yml` es sintacticamente correcto (`docker compose config` lo resuelve sin errores); los `Dockerfile` siguen el patron estandar multi-stage de Maven→JRE y Node→nginx, revisados a mano, pero no se corrio `docker compose up --build` ni una sola vez.

Avisame si aparece algun error en cualquiera de estos (Maven, npm, o Docker) para corregirlo.


## Ampliación: reglas por segmento y con condiciones, excepciones por patrón y reconfiguración RAML 2.0

### Reglas (Configuraciones)

Un parámetro puede tener **varias reglas**. Cada regla puede:

- aplicarse a un **segmento** entero (`idSegmento`): se carga una sola vez la configuración de todo el segmento;
- tener **condiciones** (todas se tienen que cumplir), sobre un dato del elemento (`NOMBRE`, `TECNOLOGIA`, `FABRICANTE`) o sobre el **valor vigente de otro parámetro** del mismo elemento. Ejemplo: "si `PotenciaTX_dBm` >= 45, `AnguloTilt_grados` tiene que estar entre 0 y 12".

Al auditar, para cada parámetro de cada elemento se usa, entre las reglas que aplican, la de mayor `prioridad`; a igual prioridad, la más específica (más condiciones, y el segmento cuenta como una); si siguen empatadas, la de menor id. Si un parámetro tiene reglas pero ninguna aplica al elemento, ese parámetro no se audita para él. Cada auditoría guarda qué regla se aplicó (`idValorEstandar`, `reglaAplicada`) y, si hay desvío, el `valorObjetivo`.

### Excepciones por patrón

Nuevo alcance `PATRON`: exceptúa los elementos cuyo nombre coincide con un patrón (`*` = cualquier texto, `?` = un carácter; el `_` es literal). Ej: `*_test_*`. Con `parametro = "*"` la excepción cubre todos los parámetros. Precedencia: ELEMENTO > PATRON > SEGMENTO > GLOBAL.

### Reconfiguración (RAML 2.0 para NetAct)

Con los desvíos vigentes (la última auditoría de cada elemento y parámetro dio DESVIO) se generan planes en el mismo formato que los que ya usa el área (`<cmData type="plan" scope="changes" name="auditorias_estrategia_FECHA_GRUPO_online_">`), siempre de a dos:

- **plan** (`GRUPO_online_MM-dd-yyyy.xml`): el valor objetivo de cada parámetro;
- **backup** (`GRUPO_online_MM-dd-yyyy_backup.xml`): los mismos objetos con el valor vigente, para hacer **rollback** importándolo. Si el valor vigente estaba vacío se escribe `<p name="x"/>`.

El `distName` de cada objeto es el `distName` del elemento (si no se cargó, `PLMN-PLMN/MRBTS-{id}/LNBTS-{id}`) más el `objetoGestionado` de la regla (ej. `LNCEL-1`, `LTAC-1`); la clase del objeto es el último tramo (`LNCEL`). El valor objetivo es el `valorObjetivo` de la regla o, si está vacío, se deriva: IGUAL / MENOR_IGUAL / MAYOR_IGUAL → el valor de referencia; RANGO → el límite más cercano; EN_LISTA → el primero. Si no se puede derivar (ej. `< 500000`) el desvío no entra en el plan y queda en `pendientes.txt`.

| Endpoint | Qué devuelve |
|---|---|
| `GET /api/auditorias/desvios?idSegmento=` | desvíos vigentes con distName, valor vigente y objetivo |
| `GET /api/auditorias/reconfiguracion?idSegmento=&tipo=plan\|backup` | un XML RAML 2.0 |
| `GET /api/auditorias/reconfiguracion/zip?idSegmento=` | ZIP con plan + backup por segmento y `pendientes.txt` |

En la web: pantalla **Reconfiguración**.

### Datos de ejemplo

Segmento **Laboratorio 4G** (id 3) con `Sitio-Lab-001`, `Sitio-Lab-002`, `Sitio-Lab_test_01`, `Sitio-Lab_test_02`; regla por segmento `srvccDelayTimer = 5`; regla con condición `AnguloTilt_grados 0..12 si PotenciaTX_dBm >= 45`; excepción `PATRON *_test_*` para todos los parámetros. Resultado esperado al auditarlo: 10 CONFORME, 3 DESVIO, 3 EXCEPTUADO; plan con `srvccDelayTimer=5` (LNBTS-9001) y `AnguloTilt_grados=12` (LNCEL-1 de 9002); backup con 4 y 14.

### Importante al actualizar

El esquema cambió (tablas y columnas nuevas). Como `schema.sql` usa `CREATE TABLE IF NOT EXISTS`, una base creada con una versión anterior no se actualiza y el backend no arranca (`Unknown column ...`). Hay que recrearla: `docker compose down -v` y `docker compose up -d --build` (el script de pruebas lo hace por defecto).

## Datos de CM de NetAct (dump SQLite)

NEIPA puede auditar la red real a partir del dump SQLite del CM de NetAct (una tabla por clase de objeto: LNBTS, LNCEL, LNCEL_FDD, SIB, ...; columnas `distName`, `PLMN_id`, `MRBTS_id`, `LNBTS_id`, ..., y una por parámetro).

- **Dónde va el dump:** en `external_data/` en la raíz del repositorio (está en `.gitignore`: los datos reales nunca van a git). Docker la monta en solo lectura en `/datos/cm`.
- **Importar:** pantalla **Datos CM** → elegir el dump → *Importar* (corre en segundo plano). Se importa el inventario de LNBTS y LNCEL y, de cada clase que usan las reglas y que depende de un LNBTS, sus objetos y **solo los parámetros que tienen una regla** (o que usa una condición). No se importan las vecindades (`LNREL*`, `LNADJ*`; configurable en `neipa.cm.clases-excluidas`). Cada importación reemplaza a la anterior. Se crea un segmento por RC (`PLMN_id`) y un elemento de red por LNBTS.
- **Reglas:** el *objeto gestionado* de la regla es la clase del CM (`LNCEL`, `SIB`, `LNCEL_FDD`, ...; vacío = `LNBTS`). Una condición puede usar otro parámetro del mismo objeto (`pMax`) o de un ancestro (`LNBTS.actX`, `LNCEL.pMax`). Si se agrega una regla de un parámetro nuevo, hay que volver a importar.
- **Auditar:** en **Segmentos**, *Auditar* sobre un segmento con RC genera una **corrida**: se compara cada objeto (`distName`) con su regla y se guardan **solo los desvíos y los exceptuados** más los totales por regla. Como la red se va estandarizando, cada corrida guarda menos filas; la pantalla Datos CM muestra la evolución.
- **Reconfiguración:** los desvíos vigentes de un segmento de CM son los de su última corrida; el plan y el backup RAML usan el `distName` real de cada objeto.
- **Conexión directa a una base:** además del archivo, el CM se puede leer de una base de datos con la misma estructura que el dump (una tabla por clase, con `distName`, `PLMN_id`, `LNBTS_id` y una columna por parámetro): **MySQL/MariaDB, PostgreSQL, Oracle o SQL Server**. En **Datos CM → Conexiones a bases de CM** se da de alta la conexión (motor, URL JDBC, usuario, contraseña y esquema opcional), se prueba con *Probar* (verifica que estén LNBTS y LNCEL) y después se elige como origen al importar.
  - La conexión se abre en **solo lectura**; conviene un usuario con permiso de `SELECT` únicamente.
  - Los nombres de tablas y columnas se buscan **sin distinguir mayúsculas** (PostgreSQL guarda `lncel`/`lnbts_id` si las tablas se crearon sin comillas; Oracle, `LNCEL`/`LNBTS_ID`).
  - La contraseña se guarda **cifrada** (AES-GCM) con la clave `NEIPA_CM_CLAVE_CONEXIONES` (se define en `java-migration/.env`, que no se versiona; si cambia, hay que volver a cargar las contraseñas). La API nunca la devuelve.
  - Por seguridad se rechazan URLs con propiedades que permiten ejecutar código o leer archivos del servidor (`autoDeserialize`, `allowLoadLocalInfile`, `socketFactory`, interceptores, etc.).
  - Si la base corre en la misma PC que Docker, usar `host.docker.internal` como servidor en la URL.
  - Ejemplos ficticios: `datos/demo_cm_ficticio_mysql.sql` (lo carga `probar-neipa.ps1` en el MySQL de Docker) y `datos/demo_cm_ficticio_postgres.sql`.
- **Demo:** `datos/demo_cm_ficticio.dba` y `datos/demo_cm_ficticio_corregido.dba` son dumps chicos y **ficticios** con la misma estructura; los usa `probar-neipa.ps1`.

## Catálogo de parámetros de Nokia

Los diccionarios de parámetros de Nokia (`Parameters_LTE_*.xls`, `SBTS_Parameters_*.xls`, `5g*BTS parameter*.xlsx`) traen, por cada parámetro y clase, el rango o la enumeración, el valor por default, la fórmula del valor interno, si se modifica en línea o requiere bloquear el objeto, y los parámetros y relaciones vinculados. No traen contadores ni KPIs.

1. **Convertir a CSV** (una vez por release): con los diccionarios en `external_data/`,
   ```
   pip install xlrd openpyxl
   python java-migration/herramientas/convertir_diccionarios_nokia.py external_data
   ```
   o, sin Python instalado: `docker run --rm -v "%cd%:/w" -w /w python:3.12-slim sh -c "pip install -q xlrd openpyxl && python java-migration/herramientas/convertir_diccionarios_nokia.py external_data"`.
   Genera `external_data/catalogo_parametros_nokia.csv` (unos 17.600 parámetros con LTE 20, SBTS 19B y 5G21B).
2. **Importar:** pantalla **Catálogo** → elegir el CSV → *Importar catálogo* (reemplaza el anterior).
3. **Uso:** la pantalla Catálogo busca por parámetro o nombre y muestra la ficha completa. En **Configuraciones** se ve la ficha del parámetro que se está cargando, y al guardar una regla se valida su valor (y los de sus condiciones) contra el catálogo: un valor fuera de la enumeración o del rango devuelve 400. La validación usa el **valor interno** que guarda el CM (por ejemplo `pMax` 430 = 43 dBm, fórmula `gui_value * 10`). Si el parámetro aparece en varias releases, alcanza con que una lo acepte. Un parámetro que no está en el catálogo no se valida.
