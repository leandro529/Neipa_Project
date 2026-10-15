# NEIPA – Prototipo Java (entrega 3)

Prototipo de consola de **NEIPA** (Network Element Integrity & Parameter Auditor)
para el Seminario de Práctica de Informática. Implementa en Java las clases del
diagrama de clases de diseño de la segunda entrega para el caso de uso
**CU001 Auditar parámetros de configuración**, más una versión simplificada de la
corrección de desvíos y su rollback.

En esta entrega los datos se guardan en memoria. La persistencia en MySQL y la
interfaz web (MVC) corresponden a la entrega 4.

## Requisitos

- JDK 8 o superior (probado con OpenJDK 21). No usa librerías externas.

## Cómo ejecutarlo

**Desde un IDE (NetBeans, IntelliJ IDEA, Eclipse, VS Code):** abrir la carpeta
`prototipo-java` (incluye un `pom.xml` mínimo, sin dependencias) y ejecutar la
clase `neipa.Main`.

**Desde la terminal en Windows:**

```
compilar.bat
ejecutar.bat
```

**Desde la terminal en Linux o macOS:**

```
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out neipa.Main
```

**Caso de prueba CPF02** (valores de frontera de `validarRango`):
`probar-cpf02.bat`, o `java -cp out neipa.pruebas.PruebaCPF02`.

**Prueba de la ampliación** (patrones de nombre, estándar con condición,
excepción por patrón y XML de reconfiguración): `probar-reconfiguracion.bat`, o
`java -cp out neipa.pruebas.PruebaReconfiguracion` (14 casos).

**Prueba de las fuentes de datos de CM** (patrón Factory, XML RAML, CSV y base
de datos, importación, auditoría y aplicación del plan): `probar-fuentes-cm.bat`,
o `java -cp out neipa.pruebas.PruebaFuentesCm` (19 casos). Se ejecuta desde la
carpeta `prototipo-java`, porque lee `datos-cm/`.

## Menú

| Opción | Función | Qué muestra |
|---|---|---|
| 1 | Listar elementos de red | Ordenación por inserción y polimorfismo (`describir()`) |
| 2 | Registrar elemento de red | Creación de objetos, constructores y validaciones |
| 3 | Buscar elemento por nombre | Búsqueda binaria y búsqueda secuencial |
| 4 | Ver catálogo y estándares | Parámetros numéricos y enumerados |
| 5 | Ejecutar auditoría de un segmento | CU001: cola de elementos, excepciones, rango y estándar |
| 6 | Reporte técnico | Interfaz `Reporte`, historial ordenado |
| 7 | Reporte ejecutivo | Interfaz `Reporte`, totales con arreglos |
| 8 | Registrar excepción | Excepciones por elemento, segmento, parámetro o patrón de nombre (`*_test_*`) |
| 9 | Aplicar corrección a un desvío | Pila de cambios, autorización de cambio masivo |
| 10 | Deshacer la última corrección | Rollback (desapilar) |
| 11 | Simular caída de una plataforma | Manejo de `ExtraccionException` |
| 12 | Generar XML de reconfiguración | Plan RAML 2.0 (NetAct) con el valor objetivo y backup con el valor vigente para rollback, en la carpeta `salida/` |
| 13 | Importar datos de CM de NSN | Patrón Factory (`FabricaFuentesCm`), herencia y polimorfismo (`FuenteCm`): XML RAML de NetAct, carpeta de CSV o base de datos por JDBC |
| 0 | Salir | |

## Estructura

```
src/main/java/neipa
  Main.java            punto de entrada
  modelo/              clases de entidad (ElementoDeRed, Parametro, Auditoria, ...)
  estructuras/         lista enlazada, pila y cola propias
  algoritmos/          ordenación por inserción, búsqueda binaria y secuencial
  excepciones/         excepciones propias del sistema
  datos/               repositorio en memoria y datos de prueba
  control/             ControladorAuditoria y ControladorRollback
  reportes/            interfaz Reporte y sus dos implementaciones
  vista/               menú principal y lectura validada por consola
  pruebas/             caso de prueba CPF02
```

Los archivos `.java` y los textos de la consola no llevan tildes a propósito,
para que compilen y se vean igual en cualquier terminal de Windows, Linux o macOS.

## Ampliación: estándares con condiciones, excepciones por patrón y reconfiguración

- **Estándares por segmento con condiciones.** Cada `ValorEstandar` es de un
  segmento y puede tener condiciones (`Condicion`, abstracta) que se tienen que
  cumplir todas: `CondicionPorNombre` (el nombre coincide con un patrón) y
  `CondicionPorParametro` (el valor vigente de otro parámetro está en un rango).
  Para cada elemento se aplica el estándar más específico (el de más
  condiciones). Ejemplo en los datos de prueba (segmento *Laboratorio 4G*):
  `a3Offset = -6` en general y `a3Offset = -3` si `pMax` está entre 40 y 46.
- **Excepción por patrón.** `Excepcion.porPatron("*_test_*", null, ...)`
  exceptúa todos los parámetros de los elementos cuyo nombre coincide.
  `PatronNombre` implementa la comparación con comodines (`*`, `?`) sin
  expresiones regulares.
- **XML de reconfiguración (opción 12).** `GeneradorRaml` implementa `Reporte`
  y genera, para los desvíos de la última auditoría de elementos gestionados
  por NetAct, el plan RAML 2.0 con el valor objetivo y el backup con el valor
  vigente (para rollback), con el mismo formato que los planes del área. El
  `distName` sale de `ElementoDeRed.getDistName()`, que cada subclase extiende.

## Datos de CM (opción 13)

El paquete `neipa.fuentes` lee la configuración vigente de una red NSN/Nokia
desde distintos orígenes. `FabricaFuentesCm.crear(origen)` elige la fuente:

| Origen | Fuente | Ejemplo |
|---|---|---|
| Archivo `.xml` | `FuenteCmRaml`: exportación RAML 2.0 de NetAct, o un plan generado con la opción 12 | `datos-cm/netact_demo.xml` |
| Carpeta o archivo `.csv` | `FuenteCmCsv`: un CSV por clase (LNBTS, LNCEL, CADPR...) con `distName` y una columna por parámetro | `datos-cm/csv` |
| URL `jdbc:` | `FuenteCmBaseDeDatos`: base con una tabla por clase (dump SQLite de NetAct, MySQL, PostgreSQL, Oracle o SQL Server) | `jdbc:sqlite:cm.dba` |

Los datos de `datos-cm/` son **ficticios**. Cada LNBTS se importa como sitio y
cada LNCEL como celda del segmento elegido; los valores de los parámetros que
audita NEIPA (pMax en LNCEL, a3Offset en CADPR) se cargan en el elemento del
que dependen. Después se audita el segmento con la opción 5.

La fuente de base de datos necesita el driver JDBC del motor en el classpath
(el prototipo no trae dependencias). Por ejemplo, con SQLite:
`java -cp "out;sqlite-jdbc-3.45.1.0.jar" neipa.Main` en Windows (`:` en lugar de
`;` en Linux) y el origen `jdbc:sqlite:ruta/al/dump.dba`. Sin el driver, la
opción informa el error y el menú sigue funcionando.
