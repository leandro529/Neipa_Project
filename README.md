# NEIPA – Network Element Integrity & Parameter Auditor

Sistema para **auditar y estandarizar los parámetros de configuración** de una red
móvil multivendor (Nokia/NSN, Ericsson, Huawei): compara el valor vigente de cada
parámetro con el estándar definido por segmento, admite excepciones, detecta los
desvíos y genera los archivos de reconfiguración (plan RAML 2.0 de NetAct y su
backup para rollback). Trabajo práctico del Seminario de Práctica de Informática.

## Contenido del repositorio

| Carpeta | Qué es |
|---|---|
| `java-migration/` | Aplicación web: Spring Boot + Spring Batch + MySQL (backend) y React (frontend), con Docker Compose. Ver su [README](java-migration/README.md). |
| `java-migration/MANUAL-PRUEBAS-CM.md` | Manual de pruebas de punta a punta con datos de CM **anonimizados** (resultados esperados exactos). |
| `java-migration/datos/` | Datos de prueba: dump de CM anonimizado, catálogo reducido de parámetros y demos ficticias. |
| `prototipo-java/` | Prototipo de consola en Java 8 (tercera entrega), sin dependencias. Ver su [README](prototipo-java/README.md). |
| `java-migration/herramientas/` | Scripts de Python (solo biblioteca estándar) para generar el catálogo y los datos del manual de pruebas. No hacen falta para levantar la aplicación. |

El código original en Python/Airflow del que parte la migración (ETL, fábricas de
fabricantes y motores SQL) está en [github.com/leandro529/neipa](https://github.com/leandro529/neipa).

`external_data/` (no se versiona) es la carpeta local para los datos **reales** de
la red: el dump de CM de NetAct y los diccionarios de parámetros de Nokia.

## Inicio rápido

**Aplicación web** (requiere Docker Desktop):

```
cd java-migration
docker compose up -d --build
```

- Web: http://localhost:5173 · API: http://localhost:8080/api/dashboard/resumen
- Para probar con datos de CM sin tener los reales, copiar
  `java-migration/datos/cm_anonimizado.dba` y `java-migration/datos/catalogo_reducido.csv`
  a `external_data/` (en la raíz del repositorio) y seguir el
  [manual de pruebas](java-migration/MANUAL-PRUEBAS-CM.md): importar el catálogo
  (pantalla **Catálogo**), importar el dump (pantalla **Datos CM**), cargar las
  reglas y auditar los segmentos por RC.
- Tests unitarios del backend: `mvn test` dentro de `java-migration`.

**Prototipo de consola** (JDK 8 o superior):

```
cd prototipo-java
compilar.bat        (o: javac -d out $(find src -name "*.java"))
ejecutar.bat        (o: java -cp out neipa.Main)
```

## Datos de prueba

| Archivo | Contenido |
|---|---|
| `java-migration/datos/cm_anonimizado.dba` | Dump SQLite de CM de NetAct **anonimizado**: 300 sitios (50 por RC), 1.882 celdas, 111 clases. Nombres `SITIO-0xxxx`, IDs renumerados, MCC/MNC 1/1, sin ubicación ni IPs reales; los parámetros de radio quedan intactos. Lo genera `herramientas/anonimizar_cm.py`. |
| `java-migration/datos/catalogo_reducido.csv` | Catálogo de parámetros (clase, rango o enumeración, default, modificación) reducido a las clases del dump, sin descripciones. Lo genera `herramientas/reducir_catalogo.py`. |
| `java-migration/datos/demo_cm_ficticio*.dba`, `*.sql`, `catalogo_demo.csv` | Datos ficticios mínimos que usa la prueba automática. |
| `prototipo-java/datos-cm/` | Exportación RAML y CSV ficticia para la opción 13 del prototipo. |
