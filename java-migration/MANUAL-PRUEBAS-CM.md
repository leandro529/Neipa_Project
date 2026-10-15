# Manual de pruebas de NEIPA con datos de CM anonimizados

Este manual sirve para probar, de punta a punta, la parte de **datos de CM de NetAct** de NEIPA: catálogo de parámetros, importación del dump, reglas (con condiciones), excepciones, corridas de auditoría, plan y backup RAML, y re-auditoría después de "aplicar" el plan.

Se usa un dump **anonimizado**, sacado del CM real pero sin datos que identifiquen la red, y un **catálogo reducido**. Por eso se puede compartir con otra persona (docente, compañero, revisor). Los resultados esperados están calculados sobre esos mismos archivos, así que cada prueba tiene un número exacto para comparar.

---

## 1. Qué hay en el paquete

| Archivo | Para qué sirve |
|---|---|
| `datos/cm_anonimizado.dba` | Dump SQLite de CM anonimizado: 300 sitios (50 por RC), 1.882 celdas y 111 clases de radio. Ocupa 16 MB. |
| `datos/catalogo_reducido.csv` | Catálogo de parámetros de Nokia reducido a las clases y columnas del dump. Tiene 4.151 filas y no incluye las descripciones largas. |
| `herramientas/anonimizar_cm.py` | Genera el dump anonimizado a partir del dump real. Solo lo usa quien tiene el dump original. |
| `herramientas/reducir_catalogo.py` | Genera el catálogo reducido a partir del catálogo completo. |
| `herramientas/esperados_cm.py` | Calcula los resultados esperados de los escenarios de este manual sobre cualquier dump. |
| `herramientas/aplicar_plan_cm.py` | Simula que se aplicó el plan en la red: escribe los valores del plan RAML en una copia del dump. |
| `MANUAL-PRUEBAS-CM.md` | Este manual. |

Todas las herramientas usan solo Python 3 con su biblioteca estándar, sin paquetes extra.

---

## 2. Qué se anonimiza y qué no

El objetivo es que el dump **no permita identificar la red**, pero que siga sirviendo para auditar, porque los parámetros de radio quedan intactos.

| Dato | En el dump real | En el anonimizado |
|---|---|---|
| Sitios incluidos | ~15.700 LNBTS | 50 por RC, elegidos al azar con semilla fija (2024) |
| `MRBTS_id` / `LNBTS_id` | id real del eNB | 1001, 1002, ... (el orden no sigue al real) |
| `distName` | `PLMN-PLMN/MRBTS-13167/LNBTS-13167/LNCEL-201` | `PLMN-PLMN/MRBTS-1001/LNBTS-1001/LNCEL-201` |
| Nombres de sitio y de celda (`name`, `enbName`, `cellName`, `siteTemplateName`, ...) | ciudad + nombre del sitio | `SITIO-01001`; la celda conserva solo el sufijo de banda o sector (`SITIO-01001_M1`) |
| `mcc` / `mnc` y toda columna `*Mcc` / `*Mnc` | PLMN real | `1` / `1` (PLMN de prueba 001-01) |
| `eutraCelId` | eNB real × 256 + celda | se recalcula con el id nuevo |
| `tac` | TAC real | se renumera desde 10001 (los TAC que eran iguales siguen iguales entre sí) |
| Coordenadas y altitud de la celda | valores reales | `NULL` |
| Ids globales de vecinos (gNB, cell identity) | valores reales | `NULL` |
| Referencias a otro eNB (`CAREL.lnBtsId`) | id real | renumeradas igual que `LNBTS_id` (las de sitios fuera de la muestra pasan a 900001, ...) |
| Otros textos (`oldDN`, `btsResetRequest`, ...) | texto libre | `anon-1a2b3c4d`; las IPs se reemplazan por `10.x.y.z` |
| Vecindades (`LNREL*`, `LNADJ*`) | sí | **no se incluyen** (NEIPA tampoco las importa) |
| Transporte, seguridad, sincronismo, trazas, PM, inventario de HW y todo lo que cuelga de `FTM` (IPs, VLANs, certificados, NTP/PTP/LDAP, MME, cuentas de usuario, números de serie) | sí | **no se incluyen** |
| `PLMN_id` (rc1, rc2, rc7, rc8, rc9, rc10), `timeStamp`, `moVersion` | | se conservan |
| **Parámetros de radio** (`n310Qci1`, `t310Qci1`, `pMax`, `n310`, `qrxlevmin`, `earfcn`, `phyCellId`, ...) | | **se conservan**: son lo que se audita |

Además, 2 sitios por RC (12 en total) llevan `_test_` en el nombre (`SITIO-01251_test_`), para probar las excepciones por patrón.

`anonimizar_cm.py` termina con una **verificación automática**. Busca en todas las columnas de texto los nombres reales de los sitios de la muestra, revisa el mcc real, las IPs públicas y las columnas numéricas que contengan ids reales de eNB. Si encuentra algo, lo informa y sale con código 2. En el archivo entregado, la verificación dio OK.

> **Antes de compartir**: el dump anonimizado conserva la configuración de radio real de 300 sitios. Confirmá que tenés permiso para compartir ese tipo de dato fuera de la empresa.
>
> El mapa *real → anónimo* (opción `--mapa`) es **privado**: no se compartió y no va al repo.

---

## 3. Requisitos

- Docker Desktop corriendo y el repo con `java-migration/`.
- Python 3.8 o superior, solo para las herramientas (paso 7 y regeneración).
- PowerShell 5.1 o 7. Los comandos de este manual son de PowerShell. Todo se puede hacer también desde la web en `http://localhost:5173`.

---

## 4. Preparación

### 4.1 Copiar los datos a la carpeta que lee el backend

El backend lee los dumps y los catálogos de `external_data`, en la raíz del repo, al lado de `java-migration`. Docker la monta como `/datos/cm`.

```powershell
cd <repo>\java-migration
New-Item -ItemType Directory -Force ..\external_data | Out-Null
Copy-Item datos\cm_anonimizado.dba, datos\catalogo_reducido.csv ..\external_data\ -Force
```

### 4.2 Levantar NEIPA desde cero

Para que los números coincidan con este manual, conviene arrancar con la base limpia (solo los datos semilla):

```powershell
docker compose down -v
docker compose up -d --build
```

Esperá a que `http://localhost:8080/api/dashboard/resumen` responda.

### 4.3 Funciones de ayuda (copiar y pegar una vez en la consola)

```powershell
$Api = "http://localhost:8080"
function Api($Metodo, $Ruta, $Cuerpo) {
    $p = @{ Method = $Metodo; Uri = $Api + $Ruta; ContentType = "application/json; charset=utf-8" }
    if ($Cuerpo) { $p.Body = [Text.Encoding]::UTF8.GetBytes(($Cuerpo | ConvertTo-Json -Depth 6)) }
    Invoke-RestMethod @p
}
function Codigo($Metodo, $Ruta, $Json) {   # devuelve solo el codigo HTTP
    try { (Invoke-WebRequest -Method $Metodo -Uri ($Api + $Ruta) -Body $Json -ContentType "application/json" -UseBasicParsing).StatusCode }
    catch { [int]$_.Exception.Response.StatusCode }
}
function Resumen($IdCorrida) { Api GET "/api/cm/corridas/$IdCorrida/resumen" | Format-Table idValorEstandar, clase, parametro, conformes, desvios, exceptuados }
```

---

## 5. Pruebas

En cada prueba se indica qué hacer, cómo hacerlo (API o web) y el **resultado esperado**. Al final hay una planilla para marcar OK o Falla.

Se audita el segmento **RC10**: 50 sitios y 313 celdas. Tiene sitios `_test_` con desvíos, así que se ven todos los casos. En el Anexo A están los esperados de los otros RC.

### P1. Catálogo reducido

1. Importar el catálogo. Esto **reemplaza** el catálogo cargado.
   ```powershell
   Api POST "/api/catalogo/importaciones" @{ archivo = "catalogo_reducido.csv" }
   ```
   Web: **Catálogo → Importar → catalogo_reducido.csv**.

   **Esperado:** 4.151 parámetros importados.

2. Ver la ficha de `pMax` en LNCEL:
   ```powershell
   Api GET "/api/catalogo/ficha?parametro=pMax&objetoGestionado=LNCEL" | Format-Table fuente, minimoInterno, maximoInterno, modificacion
   ```
   **Esperado:** 2 filas.
   - LTE20: rango interno 110..430.
   - SBTS19B: rango interno 0..600.

   Las dos dicen *Requires object locking*. En la web, la ficha avisa que hay que bloquear el objeto.

3. Validaciones contra el catálogo. Todas tienen que devolver **400**:
   ```powershell
   Codigo POST "/api/configuraciones" '{"parametro":"n310Qci1","objetoGestionado":"LNCEL","operador":"IGUAL","valorReferencia":"7"}'
   Codigo POST "/api/configuraciones" '{"parametro":"n310","objetoGestionado":"SIB","operador":"IGUAL","valorReferencia":"9"}'
   Codigo POST "/api/configuraciones" '{"parametro":"t310Qci1","objetoGestionado":"LNCEL","operador":"IGUAL","valorReferencia":"1000","condiciones":[{"origen":"PARAMETRO","campo":"LNBTS.actDLCAggr","operador":"IGUAL","valor":"2"}]}'
   ```
   **Esperado:** `400`, `400`, `400`:
   - `n310Qci1` acepta 1, 2, 3, 4, 6, 8, 10 o 20;
   - el `n310` del SIB es un enumerado interno de 0 a 7;
   - `actDLCAggr` solo acepta 0 o 1.

4. Web: en **Configuraciones → Nueva regla**, elegir el objeto **LNCEL** y buscar `n310`.

   **Esperado:** aparecen `n310Qci1` y `n310PubSafety`, con sus valores permitidos y el default. Al elegir un enumerado, el operador propuesto es IGUAL.

### P2. Importar el dump anonimizado

```powershell
$r = Api POST "/api/cm/importaciones" @{ archivo = "cm_anonimizado.dba" }
do { Start-Sleep 2; $imp = (Api GET "/api/cm/importaciones") | Where-Object { $_.idImportacion -eq $r.idImportacion } } while ($imp.estado -eq "EN_CURSO")
$imp | Format-List estado, objetos, valores, mensaje
Api GET "/api/cm/objetos/resumen" | Where-Object { $_.clase -in "LNBTS","LNCEL","SIB" } | Format-Table
```
Web: **Datos CM → cm_anonimizado.dba → Importar**.

**Esperado:**
- Estado **OK**. Tarda segundos.
- Avisos esperables: "La clase LNBTS no tiene el parámetro srvccDelayTimer", "La clase LNCEL no tiene el parámetro PotenciaTX_dBm", etc. Vienen de reglas semilla de la demo que no existen en el CM real.
- Por RC: LNBTS 50, LNCEL 313 y SIB 313 en RC10. En total: 300 LNBTS, 1.882 LNCEL y 1.882 SIB.
- Se crean los segmentos **RC1, RC2, RC7, RC8, RC9 y RC10**, si no existían.
- Se crean 300 elementos de red con nombres `SITIO-0xxxx`.
- No aparecen clases `LNREL` ni `LNADJ`.

```powershell
$seg = (Api GET "/api/segmentos") | Where-Object { $_.rc -eq "rc10" } | Select-Object -First 1
$seg.idSegmento   # se usa en las pruebas siguientes
```

### P3. Escenario A: cuatro reglas sin excepciones

La semilla trae la excepción **id 2** (PATRÓN `*_test_*`, todos los parámetros). Para el escenario A hay que desactivarla:
```powershell
Api PUT "/api/excepciones/2/desactivar"
```

Cargar las reglas:

| Id | Objeto | Parámetro | Regla | Significado |
|---|---|---|---|---|
| R1 | LNCEL | n310Qci1 | IGUAL 8 | N310 para QCI1 = n8 |
| R2 | LNCEL | t310Qci1 | IGUAL 1000 | T310 para QCI1 = 1000 ms |
| R3 | SIB | n310 | IGUAL 6 | valor interno 6 = **n10** (ver la ficha) |
| R4 | LNCEL | pMax | RANGO 400..460 | 40 a 46 dBm (el CM guarda el valor × 10) |

```powershell
$R1 = Api POST "/api/configuraciones" @{ parametro="n310Qci1"; objetoGestionado="LNCEL"; operador="IGUAL"; valorReferencia="8" }
$R2 = Api POST "/api/configuraciones" @{ parametro="t310Qci1"; objetoGestionado="LNCEL"; operador="IGUAL"; valorReferencia="1000" }
$R3 = Api POST "/api/configuraciones" @{ parametro="n310";     objetoGestionado="SIB";   operador="IGUAL"; valorReferencia="6" }
$R4 = Api POST "/api/configuraciones" @{ parametro="pMax";     objetoGestionado="LNCEL"; operador="RANGO"; minimo=400; maximo=460 }
```

Como se agregaron parámetros nuevos, hay que **volver a importar** el dump (repetir P2), porque solo se traen las columnas que usan las reglas. Después, auditar:

```powershell
$cA = Api POST "/api/auditorias/segmentos/$($seg.idSegmento)"
$cA | Format-List idCorrida, comparaciones, conformes, desvios, exceptuados
Resumen $cA.idCorrida
```

**Esperado en el resumen por regla del RC10:**

| Regla | Conformes | Desvíos | Exceptuados |
|---|---:|---:|---:|
| R1 n310Qci1 | 297 | 4 | 0 |
| R2 t310Qci1 | 297 | 4 | 0 |
| R3 n310 (SIB) | 297 | 16 | 0 |
| R4 pMax | 301 | 12 | 0 |
| **Total R1 a R4** | **1.192** | **36** | **0** |

Las 1.228 comparaciones de R1 a R4 son 313 × 4 = 1.252, menos 24: hay 12 celdas sin `n310Qci1` ni `t310Qci1` en el dump. Un objeto sin valor para el parámetro no se audita.

> Los **totales de la corrida** (`comparaciones`, `desvios`, ...) también incluyen las reglas semilla que existen en el CM real: CADPR (`a3Offset`, ...), LNCEL_FDD (`maxNumOfSuppMimoLayer`, ...) y LNCEL (`actDlPwBackoff`, ...). Por eso se verifica con el **resumen por regla**: los totales de la corrida van a ser mayores que los de esta tabla.

Comprobaciones extra:
- `Api GET "/api/auditorias?idCorrida=$($cA.idCorrida)"`: solo hay filas DESVIO y EXCEPTUADO, nunca CONFORME.
- Ejemplos de desvío. Los valores son fijos, porque el dump es siempre el mismo:
  - `PLMN-PLMN/MRBTS-1258/LNBTS-1258/LNCEL-51` (`SITIO-01258_S1`): `n310Qci1` = 6, esperado 8.
  - `PLMN-PLMN/MRBTS-1254/LNBTS-1254/LNCEL-201` (`SITIO-01254_M1`): `pMax` = 370, esperado 400..460.
  - `PLMN-PLMN/MRBTS-1276/LNBTS-1276/LNCEL-101/SIB-0`: `n310` = 4, esperado 6.
- Web: en el **Dashboard**, la corrida aparece en la evolución del RC10.

### P4. Escenario B: excepción por patrón de nombre

```powershell
$X1 = Api POST "/api/excepciones" @{ alcance="PATRON"; patronNombre="*_test_*"; parametro="*"; motivo="Sitios de prueba (manual CM)" }
$cB = Api POST "/api/auditorias/segmentos/$($seg.idSegmento)"
Resumen $cB.idCorrida
```

**Esperado:** igual que A, salvo **R4 pMax: 301 conformes, 9 desvíos y 3 exceptuados**. Los 3 exceptuados son las celdas `SITIO-01252_test__S1`, `_S2` y `_S3`, con `pMax` = 478 y fuera de rango, y quedan con la excepción aplicada:

```powershell
(Api GET "/api/auditorias?idCorrida=$($cB.idCorrida)") | Where-Object { $_.resultado -eq "EXCEPTUADO" } | Format-Table nombreObjeto, parametro, valorVigente, idExcepcionAplicada
```

La cantidad de filas guardadas para R1 a R4 no cambia: sigue siendo 36. Una excepción no borra la fila, la pasa de DESVIO a EXCEPTUADO.

### P5. Escenario C: regla con condición sobre el objeto padre

R5: en los sitios **sin** carrier aggregation (`LNBTS.actDLCAggr = 0`), `n310Qci1` tiene que ser 6. R5 es más específica que R1, así que gana donde su condición se cumple.

```powershell
$R5 = Api POST "/api/configuraciones" @{ parametro="n310Qci1"; objetoGestionado="LNCEL"; operador="IGUAL"; valorReferencia="6";
        condiciones=@(@{ origen="PARAMETRO"; campo="LNBTS.actDLCAggr"; operador="IGUAL"; valor="0" }) }
```
Web: en **Configuraciones**, la condición se elige como parámetro del **objeto padre LNBTS**.

Volver a importar el dump (P2) para traer `actDLCAggr` y auditar:
```powershell
$cC = Api POST "/api/auditorias/segmentos/$($seg.idSegmento)"
Resumen $cC.idCorrida
```

**Esperado (RC10):**

| Regla | Conformes | Desvíos | Exceptuados |
|---|---:|---:|---:|
| R1 n310Qci1 = 8 | 259 | 4 | 0 |
| R5 n310Qci1 = 6 si actDLCAggr = 0 | 0 | 35 | 3 |
| R2 t310Qci1 | 297 | 4 | 0 |
| R3 n310 (SIB) | 297 | 16 | 0 |
| R4 pMax | 301 | 9 | 3 |
| **Total** | **1.154** | **68** | **6** |

En el RC10 hay 10 sitios con `actDLCAggr = 0` y ninguno tiene `n310Qci1` = 6. Por eso R5 no tiene conformes. Ejemplo: `SITIO-01259_S1` (`.../LNBTS-1259/LNCEL-51`) tiene 8 y ahora se espera 6. Los 3 exceptuados de R5 son celdas del sitio `SITIO-01252_test_`.

### P6. Plan y backup RAML

```powershell
Invoke-WebRequest "$Api/api/auditorias/reconfiguracion?idSegmento=$($seg.idSegmento)&tipo=plan"   -OutFile plan_rc10.xml   -UseBasicParsing
Invoke-WebRequest "$Api/api/auditorias/reconfiguracion?idSegmento=$($seg.idSegmento)&tipo=backup" -OutFile backup_rc10.xml -UseBasicParsing
```
Web: en **Reconfiguración**, elegir el RC10 y descargar el plan y el backup.

**Esperado:**
- XML RAML 2.0 con `managedObject class="..." operation="update"` y el **distName anonimizado** de cada objeto, por ejemplo `PLMN-PLMN/MRBTS-1259/LNBTS-1259/LNCEL-51`.
- En el plan:
  - `SITIO-01259` (`LNBTS-1259/LNCEL-51`): `n310Qci1` = **6**, por R5.
  - `LNBTS-1258/LNCEL-51`: `n310Qci1` = **8** (R1) y `t310Qci1` = **1000** (R2).
  - `LNBTS-1254/LNCEL-201`: `pMax` = **400**, el límite más cercano de R4.
  - `.../LNCEL-101/SIB-0`: `n310` = **6**.
- **No** aparecen las celdas de `SITIO-01252_test_`, porque son exceptuados y no desvíos.
- El backup tiene los valores vigentes de esos mismos objetos (6, 200, 370, 4, ...).
- El plan también trae los desvíos de las reglas semilla que aplican (CADPR, LNCEL_FDD, ...). Es correcto: el plan sale de la última corrida.

### P7. Aplicar el plan y re-auditar (la red "se estandariza")

`aplicar_plan_cm.py` escribe los valores del plan en una **copia** del dump. Es la simulación de haber aplicado el plan en NetAct.

```powershell
python herramientas\aplicar_plan_cm.py --dump datos\cm_anonimizado.dba --plan plan_rc10.xml --salida ..\external_data\cm_anonimizado_corregido.dba
$r = Api POST "/api/cm/importaciones" @{ archivo = "cm_anonimizado_corregido.dba" }
# esperar a que termine (como en P2) y auditar
$cD = Api POST "/api/auditorias/segmentos/$($seg.idSegmento)"
Resumen $cD.idCorrida
Api GET "/api/cm/corridas?idSegmento=$($seg.idSegmento)" | Format-Table idCorrida, fecha, comparaciones, desvios, exceptuados
```

**Esperado:**
- `aplicar_plan_cm.py` informa que aplicó todos los valores del plan, sin líneas "sin aplicar".
- Resumen por regla del RC10:

| Regla | Conformes | Desvíos | Exceptuados |
|---|---:|---:|---:|
| R1 | 263 | 0 | 0 |
| R5 | 35 | 0 | 3 |
| R2 | 301 | 0 | 0 |
| R3 | 313 | 0 | 0 |
| R4 | 310 | 0 | 3 |
| **Total** | **1.222** | **0** | **6** |

- Solo quedan 6 filas guardadas para R1 a R5, que son los exceptuados (en C eran 74). Cada corrida guarda menos filas a medida que la red se estandariza.
- La evolución de corridas del RC10 muestra los desvíos bajando en A, B, C y D.

### P8. (Opcional) Otros RC y segmentos con filtro

- Auditar cualquier otro RC y comparar con el **Anexo A**. Vale para los segmentos **originales**: el dump corregido del paso 7 solo cambió el RC10.
- Crear un segmento de CM con RC `rc10` y filtro de nombre `*_test_*` y auditarlo. Solo se evalúan las celdas de `SITIO-01251_test_` y `SITIO-01252_test_`.

### P9. Limpieza

```powershell
foreach ($id in $R1.idValorEstandar,$R2.idValorEstandar,$R3.idValorEstandar,$R4.idValorEstandar,$R5.idValorEstandar) { Codigo DELETE "/api/configuraciones/$id" $null }
Codigo DELETE "/api/excepciones/$($X1.idExcepcion)" $null
Api POST "/api/catalogo/importaciones" @{ archivo = "catalogo_parametros_nokia.csv" }   # si se tiene el catalogo completo
```
- Borrar una regla que ya tiene auditorías devuelve **409**. Es correcto, porque las auditorías la referencian.
- Para volver a la semilla completa, incluida la excepción id 2 activa: `docker compose down -v` y `docker compose up -d`.

---

## 6. Planilla de resultados

| # | Prueba | Esperado | OK / Falla |
|---|---|---|---|
| P1.1 | Importar catálogo reducido | 4.151 parámetros | |
| P1.2 | Ficha pMax | 110..430 (LTE20) y 0..600 (SBTS19B) | |
| P1.3 | Validaciones | 400, 400, 400 | |
| P2 | Importar dump | OK; 300 LNBTS, 1.882 LNCEL, 1.882 SIB; segmentos RC1..RC10; sin LNREL | |
| P3 | Escenario A (RC10) | R1 297/4/0, R2 297/4/0, R3 297/16/0, R4 301/12/0 | |
| P4 | Escenario B | R4 301/9/3 (SITIO-01252_test_) | |
| P5 | Escenario C | R1 259/4/0, R5 0/35/3, R4 301/9/3 | |
| P6 | Plan y backup | distName anonimizado; sin sitios `_test_`; valores objetivo 6, 8, 1000, 400 y 6 | |
| P7 | Corrida después del plan | 0 desvíos en R1..R5; 6 exceptuados | |
| P9 | Limpieza | 204 o 409 al borrar | |

(Los números son conformes/desvíos/exceptuados del resumen por regla.)

---

## 7. Regenerar los archivos (solo quien tiene el dump real)

Desde `java-migration\herramientas`:

```powershell
# 1. Dump anonimizado (~25 s sobre el dump de 9 GB). El original se abre en solo lectura.
python anonimizar_cm.py --origen ..\..\external_data\20240124_sqlite.dba --destino ..\datos\cm_anonimizado.dba --sitios-por-rc 50 --sitios-test 2 --forzar

# 2. Catálogo reducido
python reducir_catalogo.py --catalogo ..\..\external_data\catalogo_parametros_nokia.csv --dump ..\datos\cm_anonimizado.dba --salida ..\datos\catalogo_reducido.csv

# 3. Resultados esperados (para actualizar las tablas de este manual)
python esperados_cm.py --dump ..\datos\cm_anonimizado.dba --rc rc10
```

Opciones útiles de `anonimizar_cm.py`:

| Opción | Efecto |
|---|---|
| `--sitios-por-rc N` | Tamaño de la muestra; 0 = todos los sitios (varios cientos de MB). |
| `--rc rc1,rc7` | Solo esos RC. |
| `--clases LNBTS,LNCEL,SIB` | Solo esas clases (dump más chico). |
| `--semilla N` | Otra muestra y otros seudónimos. **Cambia los números esperados**: hay que volver a correr `esperados_cm.py`. |
| `--sitios-test N` | Sitios por RC marcados con `_test_`. |
| `--mapa archivo.csv` | Guarda la tabla real → anónimo. **Privada: no compartir.** |
| `--incluir-sensibles` | No excluye transporte, seguridad ni trazas; los textos se anonimizan igual. No se recomienda para compartir. |

Con la misma semilla, el resultado es idéntico en cada corrida.

---

## 8. Problemas frecuentes

| Síntoma | Causa / solución |
|---|---|
| `/api/cm/archivos` no muestra `cm_anonimizado.dba` | No se copió a `external_data` (4.1), o el backend se levantó antes de crear la carpeta: `docker compose restart backend`. |
| Una regla nueva no da comparaciones | No se reimportó el dump después de crearla: solo se importan las columnas que usan las reglas. |
| Aparecen exceptuados en el escenario A | Falta desactivar la excepción semilla id 2 (`*_test_*`). |
| Los totales de la corrida no coinciden con las tablas | Es esperable: incluyen las reglas semilla. Hay que comparar el **resumen por regla**. |
| Guardar una regla devuelve 400 con un valor que parece válido | El CM y el catálogo usan valores **internos**: pMax 43 dBm = 430; SIB n310 "n10" = 6. Hay que ver la ficha. |
| `aplicar_plan_cm.py` informa objetos "sin aplicar" | El plan es de otro dump o de otra semilla. Hay que bajar el plan después de auditar con este dump. |

---

## Anexo A. Esperados por RC (resumen por regla, C/D/E)

Para el dump entregado (semilla 2024, 50 sitios por RC). Los calcula `esperados_cm.py`.

**Escenario A** (R1 a R4, sin excepción)

| RC | R1 n310Qci1 | R2 t310Qci1 | R3 n310 | R4 pMax | Total C/D/E |
|---|---|---|---|---|---|
| rc1 | 304/16/0 | 304/16/0 | 304/16/0 | 288/32/0 | 1200/80/0 |
| rc2 | 287/12/0 | 287/12/0 | 287/12/0 | 294/5/0 | 1155/41/0 |
| rc7 | 324/37/0 | 324/37/0 | 324/37/0 | 351/10/0 | 1323/121/0 |
| rc8 | 331/1/0 | 331/1/0 | 331/2/0 | 299/34/0 | 1292/38/0 |
| rc9 | 248/8/0 | 248/8/0 | 248/8/0 | 195/61/0 | 939/85/0 |
| rc10 | 297/4/0 | 297/4/0 | 297/16/0 | 301/12/0 | 1192/36/0 |

**Escenario B** (A + excepción `*_test_*`): cambia solo R4 en rc1 (288/29/3), rc9 (195/60/1) y rc10 (301/9/3).

**Escenario C** (B + R5)

| RC | R1 | R5 | R2 | R3 | R4 | Total C/D/E |
|---|---|---|---|---|---|---|
| rc1 | 287/12/0 | 4/17/0 | 304/16/0 | 304/16/0 | 288/29/3 | 1187/90/3 |
| rc2 | 261/9/0 | 3/26/0 | 287/12/0 | 287/12/0 | 294/5/0 | 1132/64/0 |
| rc7 | 308/37/0 | 0/16/0 | 324/37/0 | 324/37/0 | 351/10/0 | 1307/137/0 |
| rc8 | 311/0/0 | 1/20/0 | 331/1/0 | 331/2/0 | 299/34/0 | 1273/57/0 |
| rc9 | 226/8/0 | 0/20/2 | 248/8/0 | 248/8/0 | 195/60/1 | 917/104/3 |
| rc10 | 259/4/0 | 0/35/3 | 297/4/0 | 297/16/0 | 301/9/3 | 1154/68/6 |

Después de aplicar el plan de un RC y re-auditar: 0 desvíos en R1 a R5. Los conformes pasan a ser conformes + desvíos de la tabla C y los exceptuados no cambian.
