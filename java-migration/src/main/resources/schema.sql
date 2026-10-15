-- DDL base ya presentado y verificado en la segunda entrega (Figura 11 - DER,
-- seccion "Creacion de las tablas"). Se ejecuta automaticamente al levantar la
-- aplicacion (spring.sql.init.mode=always), asi el codigo Java queda atado
-- exactamente al modelo relacional ya aprobado, sin que Hibernate infiera nada.
--
-- Ampliacion (web de configuraciones/excepciones/dashboard): se agregan las
-- tablas valores_estandar y excepciones, y la columna id_excepcion_aplicada
-- en auditorias, para poder cargar esta configuracion desde la web en vez de
-- tenerla hardcodeada en el codigo (ver EstandarProvider / ExcepcionService).
-- Como la aplicacion todavia no se compilo/corrio contra una base real (ver
-- README, "Nota importante sobre verificacion"), se agrega directamente a la
-- definicion de las tablas en vez de con ALTER TABLE.

CREATE TABLE IF NOT EXISTS segmentos (
  id_segmento INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(60) NOT NULL,
  criterio VARCHAR(120),
  -- Ampliacion "datos de CM": un segmento puede corresponder a un RC del
  -- CM de NetAct (columna PLMN_id del dump, ej "rc7") y filtrarse ademas por
  -- un patron sobre el nombre del objeto (ej "BOG*").
  rc VARCHAR(20),
  filtro_nombre VARCHAR(80)
);

CREATE TABLE IF NOT EXISTS elementos_red (
  id_elemento INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(60) NOT NULL,
  tecnologia VARCHAR(20),
  fabricante VARCHAR(30),
  id_segmento INT,
  dist_name VARCHAR(200),
  FOREIGN KEY (id_segmento) REFERENCES segmentos(id_segmento)
);

-- Regla contra la que se compara el valor vigente de cada parametro de
-- configuracion (RF02, RF04). Originalmente solo un rango (operador RANGO,
-- minimo/maximo obligatorios); se amplio (ver registro del proyecto) para
-- soportar el resto de OperadorComparacion (IGUAL, MENOR_QUE, etc.), a
-- partir de que las auditorias reales que el usuario ya tenia cargadas
-- fuera de este repo casi nunca expresan una regla como un rango, sino como
-- "el parametro debe valer X" o "debe ser menor que X" — de ahi
-- valor_referencia, y que minimo/maximo dejan de ser NOT NULL (solo se usan
-- con operador RANGO). Cargada desde la pantalla "Configuraciones" del
-- frontend.
CREATE TABLE IF NOT EXISTS valores_estandar (
  id_valor_estandar INT AUTO_INCREMENT PRIMARY KEY,
  parametro VARCHAR(60) NOT NULL,
  operador VARCHAR(20) NOT NULL DEFAULT 'RANGO',
  minimo DECIMAL(10,2),
  maximo DECIMAL(10,2),
  valor_referencia VARCHAR(120),
  unidad VARCHAR(20),
  -- Ampliacion "reglas por segmento y con condiciones": un parametro puede
  -- tener varias reglas (ya no es UNIQUE). id_segmento limita la regla a un
  -- segmento; prioridad desempata; objeto_gestionado y valor_objetivo se
  -- usan para generar el XML RAML 2.0 de reconfiguracion de los desvios.
  id_segmento INT,
  prioridad INT NOT NULL DEFAULT 0,
  objeto_gestionado VARCHAR(60),
  valor_objetivo VARCHAR(120),
  descripcion VARCHAR(255),
  INDEX idx_valores_estandar_parametro (parametro),
  FOREIGN KEY (id_segmento) REFERENCES segmentos(id_segmento) ON DELETE CASCADE
);

-- Condiciones de aplicacion de cada regla (todas se tienen que cumplir).
-- origen ATRIBUTO_ELEMENTO: campo = NOMBRE / TECNOLOGIA / FABRICANTE.
-- origen PARAMETRO: campo = nombre de otro parametro del mismo elemento.
CREATE TABLE IF NOT EXISTS condiciones_estandar (
  id_condicion INT AUTO_INCREMENT PRIMARY KEY,
  id_valor_estandar INT,
  origen VARCHAR(20) NOT NULL,
  campo VARCHAR(60) NOT NULL,
  operador VARCHAR(20) NOT NULL,
  valor VARCHAR(120) NOT NULL,
  FOREIGN KEY (id_valor_estandar) REFERENCES valores_estandar(id_valor_estandar) ON DELETE CASCADE
);

-- Excepciones de 3 niveles (RF05-08): evitan que un parametro fuera de rango
-- se marque como DESVIO para un elemento puntual, un segmento entero, o todo
-- el sistema, durante una ventana de vigencia. Cargada desde la pantalla
-- "Excepciones" del frontend.
CREATE TABLE IF NOT EXISTS excepciones (
  id_excepcion INT AUTO_INCREMENT PRIMARY KEY,
  alcance VARCHAR(20) NOT NULL,
  id_elemento INT,
  id_segmento INT,
  patron_nombre VARCHAR(80),
  parametro VARCHAR(60) NOT NULL,
  motivo VARCHAR(255),
  vigente_desde DATETIME,
  vigente_hasta DATETIME,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  FOREIGN KEY (id_elemento) REFERENCES elementos_red(id_elemento),
  FOREIGN KEY (id_segmento) REFERENCES segmentos(id_segmento)
);

CREATE TABLE IF NOT EXISTS auditorias (
  id_auditoria INT AUTO_INCREMENT PRIMARY KEY,
  id_elemento INT,
  parametro VARCHAR(60),
  valor_vigente VARCHAR(30),
  valor_estandar VARCHAR(120),
  resultado VARCHAR(20),
  fecha DATETIME,
  id_excepcion_aplicada INT,
  id_valor_estandar INT,
  regla_aplicada VARCHAR(255),
  valor_objetivo VARCHAR(120),
  -- Auditorias sobre datos de CM: se guardan solo desvios y exceptuados, por
  -- objeto gestionado (distName), asociados a la corrida que los genero.
  id_corrida BIGINT,
  dist_name VARCHAR(255),
  nombre_objeto VARCHAR(120),
  INDEX idx_auditorias_corrida (id_corrida, resultado),
  FOREIGN KEY (id_elemento) REFERENCES elementos_red(id_elemento),
  FOREIGN KEY (id_excepcion_aplicada) REFERENCES excepciones(id_excepcion)
);

CREATE TABLE IF NOT EXISTS cambios_masivos (
  id_cambio INT AUTO_INCREMENT PRIMARY KEY,
  id_segmento INT,
  parametro VARCHAR(60),
  pct_afectado DECIMAL(5,2),
  estado VARCHAR(20),
  autorizado_por VARCHAR(60),
  fecha DATETIME,
  FOREIGN KEY (id_segmento) REFERENCES segmentos(id_segmento)
);

-- ===========================================================================
-- Ampliacion "datos de CM" (dump SQLite de NetAct).
--
-- Del dump se importan: el inventario de LNBTS y LNCEL, y de cada clase que
-- usan las reglas (LNCEL, LNCEL_FDD, SIB, CADPR, ...) sus objetos y SOLO los
-- parametros que tienen una regla (o que usa una condicion). No se importan
-- las vecindades (LNREL*, LNADJ*).
-- ===========================================================================

CREATE TABLE IF NOT EXISTS cm_importaciones (
  id_importacion BIGINT AUTO_INCREMENT PRIMARY KEY,
  archivo VARCHAR(255) NOT NULL,
  fecha_dump VARCHAR(30),
  inicio DATETIME,
  fin DATETIME,
  estado VARCHAR(20) NOT NULL,
  objetos BIGINT DEFAULT 0,
  valores BIGINT DEFAULT 0,
  mensaje TEXT
);

CREATE TABLE IF NOT EXISTS cm_objetos (
  dist_name VARCHAR(255) NOT NULL PRIMARY KEY,
  clase VARCHAR(60) NOT NULL,
  rc VARCHAR(20),
  mrbts_id INT,
  lnbts_id INT,
  lncel_id INT,
  nombre VARCHAR(120),
  version VARCHAR(60),
  id_importacion BIGINT,
  INDEX idx_cm_objetos_clase_rc (clase, rc)
);

CREATE TABLE IF NOT EXISTS cm_valores (
  dist_name VARCHAR(255) NOT NULL,
  parametro VARCHAR(80) NOT NULL,
  clase VARCHAR(60) NOT NULL,
  rc VARCHAR(20),
  valor VARCHAR(255),
  PRIMARY KEY (dist_name, parametro),
  INDEX idx_cm_valores_clase_rc_param (clase, rc, parametro)
);

-- Una corrida = una auditoria de un segmento sobre los datos de CM.
CREATE TABLE IF NOT EXISTS corridas (
  id_corrida BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_segmento INT,
  id_importacion BIGINT,
  fecha DATETIME,
  objetos_evaluados BIGINT DEFAULT 0,
  comparaciones BIGINT DEFAULT 0,
  conformes BIGINT DEFAULT 0,
  desvios BIGINT DEFAULT 0,
  exceptuados BIGINT DEFAULT 0,
  sin_regla BIGINT DEFAULT 0,
  duracion_ms BIGINT DEFAULT 0,
  INDEX idx_corridas_segmento (id_segmento, id_corrida)
);

-- Resumen por regla de cada corrida (los conformes no se guardan fila por fila).
CREATE TABLE IF NOT EXISTS corrida_resumen (
  id_corrida BIGINT NOT NULL,
  id_valor_estandar INT NOT NULL,
  clase VARCHAR(60),
  parametro VARCHAR(80),
  conformes BIGINT DEFAULT 0,
  desvios BIGINT DEFAULT 0,
  exceptuados BIGINT DEFAULT 0,
  PRIMARY KEY (id_corrida, id_valor_estandar)
);

-- Catalogo de parametros (diccionarios de Nokia convertidos a CSV con
-- herramientas/convertir_diccionarios_nokia.py). Se usa para consultar un
-- parametro y para validar los valores de las reglas. Los rangos "_interno"
-- estan en el valor que guarda el CM (por ejemplo pMax 430 = 43 dBm).
CREATE TABLE IF NOT EXISTS catalogo_parametros (
  id_catalogo BIGINT AUTO_INCREMENT PRIMARY KEY,
  fuente VARCHAR(120) NOT NULL,
  release_doc VARCHAR(40),
  tecnologia VARCHAR(20),
  clase VARCHAR(60) NOT NULL,
  ruta_clase VARCHAR(200),
  parametro VARCHAR(120) NOT NULL,
  estructura VARCHAR(120),
  nombre VARCHAR(255),
  tipo VARCHAR(40),
  unidad VARCHAR(40),
  rango TEXT,
  minimo_interno DOUBLE,
  maximo_interno DOUBLE,
  enumeracion TEXT,
  formula VARCHAR(255),
  default_valor VARCHAR(400),
  default_interno VARCHAR(60),
  modificacion VARCHAR(60),
  funciones TEXT,
  relacionados TEXT,
  relaciones TEXT,
  descripcion TEXT,
  INDEX idx_catalogo_param (parametro, clase),
  INDEX idx_catalogo_clase (clase)
);

-- Conexiones a bases con la estructura del CM de NetAct (NSN): MySQL/MariaDB,
-- PostgreSQL, Oracle o SQL Server. La contrasena se guarda cifrada (AES-GCM,
-- clave neipa.cm.clave-conexiones) y la API nunca la devuelve.
CREATE TABLE IF NOT EXISTS cm_conexiones (
  id_conexion BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL UNIQUE,
  motor VARCHAR(20) NOT NULL,
  url VARCHAR(500) NOT NULL,
  usuario VARCHAR(120),
  contrasena_cifrada VARCHAR(700),
  esquema VARCHAR(120),
  creada DATETIME,
  ultima_prueba DATETIME,
  ultimo_resultado VARCHAR(500)
);
