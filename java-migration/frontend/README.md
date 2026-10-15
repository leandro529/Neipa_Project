# neipa-frontend

SPA en React (Vite) para NEIPA: cargar configuraciones (RF02/RF04) y
excepciones (RF05-08), y ver el dashboard de control. Consume la API REST
del backend Spring Boot (`java-migration/`, puerto 8080 por defecto).

## Pantallas

- **Dashboard** (`/`): KPIs (segmentos, elementos, auditorías, tasa de
  conformidad, excepciones activas), un gráfico de resultados de auditoría
  (conforme/desvío/exceptuado) y otro de elementos por fabricante, y una
  tabla con las últimas 20 auditorías.
- **Segmentos** (`/segmentos`): alta/edición/baja de Segmentos, y botón para
  disparar una auditoría (CU001) sobre todos sus elementos.
- **Elementos de red** (`/elementos`): alta/edición/baja de ElementoDeRed
  (nombre, tecnología, fabricante, segmento).
- **Configuraciones** (`/configuraciones`): alta/edición/baja de
  ValorEstandar (parámetro, rango mínimo/máximo, unidad) — reemplaza el mapa
  hardcodeado que tenía `EstandarProvider` antes de esta ampliación.
- **Excepciones** (`/excepciones`): alta/edición/baja/desactivación de
  Excepcion, con selector de alcance (elemento puntual, segmento, o global) y
  ventana de vigencia.

## Como correrlo

Requiere Node.js 18+ y el backend (`java-migration/`) corriendo en
`localhost:8080` (o el host que configures en `.env.local`).

```bash
npm install
cp .env.example .env.local   # ajustar VITE_API_BASE_URL si hace falta
npm run dev                  # http://localhost:5173
```

Para un build de producción (archivos estáticos en `dist/`):

```bash
npm run build
npm run preview
```

## Nota importante sobre verificación

**Este proyecto no se pudo instalar ni compilar dentro del entorno donde se
generó**: tanto el sandbox en la nube como el puente hacia la computadora del
usuario no tienen acceso a `registry.npmjs.org` en este caso (a diferencia de
otros proyectos donde sí estuvo disponible) — se probó explícitamente y da
403 "Host not in allowlist". El código se escribió con cuidado siguiendo la
API estándar de React 18 / React Router 6 / Vite 5, y se revisó a mano
componente por componente, pero **no se corrió `npm install` ni `npm run
build`**. Antes de darlo por terminado, corré `npm install && npm run build`
en un entorno con acceso normal a internet y avisame si aparece algún error
para corregirlo — igual que quedó pendiente para el backend con
`mvn clean verify`.
