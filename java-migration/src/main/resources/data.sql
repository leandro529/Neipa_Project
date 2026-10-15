-- Semilla de datos de ejemplo, para que la aplicacion tenga algo para
-- mostrar (dashboard, listados) apenas se levanta, sin tener que cargar
-- todo a mano por la web antes de la primera prueba.
--
-- Se ejecuta automaticamente despues de schema.sql en cada arranque
-- (spring.sql.init.mode=always). Todos los INSERT usan IGNORE con un id
-- explicito (en vez de dejar que AUTO_INCREMENT lo asigne) para que sea
-- idempotente: si la fila con ese id ya existe, el reinicio no la duplica.
-- Los ids explicitos no chocan con las filas que despues se carguen desde
-- la web (Segmentos/Elementos/etc.), porque MySQL sigue incrementando el
-- contador de AUTO_INCREMENT a partir del mayor id ya usado.

-- ── Configuraciones (RF02/RF04) ──────────────────────────────────────────
-- PotenciaTX_dBm y AnguloTilt_grados (operador RANGO): son los dos
-- parametros originales de esta migracion. El resto es una muestra curada
-- de parametros y reglas REALES (no inventados), tomados de las auditorias
-- que el usuario ya tenia cargadas fuera de este repo (AUDIT-xx.xlsx, hoja
-- "Parametros") y del diccionario de parametros Nokia LTE
-- (Parameters_NOKLTE_FLC19_1813_02_1812_02_Alldata.xls) — ver registro del
-- proyecto para el detalle de la decision. Los 4 primeros (PotenciaTX_dBm,
-- AnguloTilt_grados, srvccDelayTimer, tacLimitGbrEmergency) son ademas los
-- que simula NsnVendorExtractor, para que el boton "Auditar" ejercite los
-- operadores RANGO, IGUAL y MENOR_QUE de punta a punta; el resto queda
-- cargado en el catalogo como ejemplo real de las reglas que un ingeniero
-- de RF cargaria desde esta pantalla, aunque hoy ningun extractor las
-- audite todavia.
INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, minimo, maximo, unidad, objeto_gestionado)
VALUES (1, 'PotenciaTX_dBm', 'RANGO', 30.00, 46.00, 'dBm', 'LNCEL-1');

INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, minimo, maximo, unidad, objeto_gestionado)
VALUES (2, 'AnguloTilt_grados', 'RANGO', 0.00, 15.00, 'grados', 'LNCEL-1');

-- AUDIT-05_4G_SRVCC-DelayTimer.xlsx (objeto LNBTS)
INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (3, 'srvccDelayTimer', 'IGUAL', '4', NULL, NULL);

-- AUDIT-22_4G_AJUSTES-UMBRALES-MINIMOS-LTAC.xlsx (objeto LTAC)
INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (4, 'tacLimitGbrEmergency', 'MENOR_QUE', '500000', NULL, 'LTAC-1');

INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (5, 'tacLimitGbrHandover', 'MENOR_QUE', '500000', NULL, 'LTAC-1');

INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (6, 'tacLimitGbrNormal', 'MENOR_QUE', '500000', NULL, 'LTAC-1');

-- AUDIT-01_4G_CARRIER-AGGREGATION.xlsx (objeto CADPR)
INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (7, 'a3Offset', 'IGUAL', '-6', NULL, 'LNCEL-1/CADPR-1');

INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (8, 'a3TimeToTrigger', 'IGUAL', '320', 'ms', 'LNCEL-1/CADPR-1');

INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (9, 'a3TriggerQuantity', 'IGUAL', '0', NULL, 'LNCEL-1/CADPR-1');

INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (10, 'enableA3Event', 'IGUAL', '1', NULL, 'LNCEL-1/CADPR-1');

-- AUDIT-11_4G_MIMO.xlsx (objetos CAREL / LNCEL_FDD)
INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (11, 'maxNumOfSuppMimoLayer', 'IGUAL', '2', NULL, 'LNCEL-1/LNCEL_FDD-1');

INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (12, 'mimoOlCqiThD', 'IGUAL', '50', NULL, 'LNCEL-1/LNCEL_FDD-1');

-- AUDIT-13_4G_ExpectedCellSize.xlsx (objeto LNCEL) — ejemplo de IGUAL sobre
-- un parametro de texto, no numerico.
INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (13, 'ExpectedCellSize', 'IGUAL', 'fixed', NULL, 'LNCEL-1');

-- AUDIT-26_PowerBackoff.xlsx (objeto LNCEL)
INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (14, 'actDlPwBackoff', 'IGUAL', '1', NULL, 'LNCEL-1');

INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (15, 'dlPwBackoffThreshold', 'IGUAL', '50', NULL, 'LNCEL-1');

INSERT IGNORE INTO valores_estandar (id_valor_estandar, parametro, operador, valor_referencia, unidad, objeto_gestionado)
VALUES (16, 'maxDlPwBackoffValue1', 'IGUAL', '10', NULL, 'LNCEL-1');

-- ── Segmentos ─────────────────────────────────────────────────────────────
INSERT IGNORE INTO segmentos (id_segmento, nombre, criterio)
VALUES (1, 'Zona Norte 4G', 'Tecnologia 4G, Zona Norte');

INSERT IGNORE INTO segmentos (id_segmento, nombre, criterio)
VALUES (2, 'Zona Sur 5G', 'Tecnologia 5G, Zona Sur');

INSERT IGNORE INTO segmentos (id_segmento, nombre, criterio)
VALUES (3, 'Laboratorio 4G', 'Sitios de laboratorio y de prueba');

-- ── Elementos de red ──────────────────────────────────────────────────────
-- Fabricante NSN en la mayoria (es el unico con extraccion implementada);
-- un elemento HUAWEI queda a proposito, para poder mostrar en la demo que
-- auditar ese fabricante todavia no esta soportado (UnsupportedOperationException,
-- RF21 - punto de extension del Factory).
INSERT IGNORE INTO elementos_red (id_elemento, nombre, tecnologia, fabricante, id_segmento)
VALUES (1, 'Sitio-Norte-001', '4G', 'NSN', 1);

INSERT IGNORE INTO elementos_red (id_elemento, nombre, tecnologia, fabricante, id_segmento)
VALUES (2, 'Sitio-Norte-002', '4G', 'NSN', 1);

INSERT IGNORE INTO elementos_red (id_elemento, nombre, tecnologia, fabricante, id_segmento)
VALUES (3, 'Sitio-Norte-003', '4G', 'NSN', 1);

INSERT IGNORE INTO elementos_red (id_elemento, nombre, tecnologia, fabricante, id_segmento)
VALUES (4, 'Antena-Norte-A1', '4G', 'NSN', 1);

INSERT IGNORE INTO elementos_red (id_elemento, nombre, tecnologia, fabricante, id_segmento)
VALUES (5, 'Sitio-Sur-001', '5G', 'NSN', 2);

INSERT IGNORE INTO elementos_red (id_elemento, nombre, tecnologia, fabricante, id_segmento)
VALUES (6, 'Sitio-Sur-002', '5G', 'NSN', 2);

INSERT IGNORE INTO elementos_red (id_elemento, nombre, tecnologia, fabricante, id_segmento)
VALUES (7, 'Core-Sur-001', '5G', 'HUAWEI', 2);

-- Segmento 3 (ampliacion): dos sitios normales y dos de prueba ("_test_"
-- en el nombre), para mostrar las reglas por segmento/condicion y la
-- excepcion por patron de nombre. Con dist_name real de NetAct.
INSERT IGNORE INTO elementos_red (id_elemento, nombre, tecnologia, fabricante, id_segmento, dist_name)
VALUES (8, 'Sitio-Lab-001', '4G', 'NSN', 3, 'PLMN-PLMN/MRBTS-9001/LNBTS-9001');

INSERT IGNORE INTO elementos_red (id_elemento, nombre, tecnologia, fabricante, id_segmento, dist_name)
VALUES (9, 'Sitio-Lab-002', '4G', 'NSN', 3, 'PLMN-PLMN/MRBTS-9002/LNBTS-9002');

INSERT IGNORE INTO elementos_red (id_elemento, nombre, tecnologia, fabricante, id_segmento, dist_name)
VALUES (10, 'Sitio-Lab_test_01', '4G', 'NSN', 3, 'PLMN-PLMN/MRBTS-9101/LNBTS-9101');

INSERT IGNORE INTO elementos_red (id_elemento, nombre, tecnologia, fabricante, id_segmento, dist_name)
VALUES (11, 'Sitio-Lab_test_02', '4G', 'NSN', 3, 'PLMN-PLMN/MRBTS-9102/LNBTS-9102');

-- ── Reglas por segmento y con condiciones (ampliacion) ───────────────────
-- Ids desde 100 para no chocar con las reglas generales de arriba. Van
-- despues de los segmentos porque la regla por segmento los referencia.
--
-- 1) Regla por SEGMENTO: en el segmento 3 (Laboratorio 4G) el estandar de
--    srvccDelayTimer es 5 en vez de 4. Se carga una sola vez para todo el
--    segmento; le gana a la regla general porque es mas especifica.
INSERT IGNORE INTO valores_estandar
  (id_valor_estandar, parametro, operador, valor_referencia, id_segmento, objeto_gestionado, descripcion)
VALUES
  (100, 'srvccDelayTimer', 'IGUAL', '5', 3, NULL, 'Laboratorio: SRVCC con delay 5');

-- 2) Regla que depende de OTRO PARAMETRO: si la potencia es alta
--    (PotenciaTX_dBm >= 45), el tilt tiene que estar entre 0 y 12 en vez de
--    0 y 15. Vale para cualquier segmento.
INSERT IGNORE INTO valores_estandar
  (id_valor_estandar, parametro, operador, minimo, maximo, unidad, objeto_gestionado, descripcion)
VALUES
  (101, 'AnguloTilt_grados', 'RANGO', 0.00, 12.00, 'grados', 'LNCEL-1', 'Con potencia alta, tilt maximo 12');

INSERT IGNORE INTO condiciones_estandar (id_condicion, id_valor_estandar, origen, campo, operador, valor)
VALUES (1, 101, 'PARAMETRO', 'PotenciaTX_dBm', 'MAYOR_IGUAL', '45');

-- ── Excepcion de ejemplo (RF05-08) ────────────────────────────────────────
-- Alcance SEGMENTO: cubre PotenciaTX_dBm para todo el segmento 1 durante
-- los proximos 90 dias, para poder ver en la demo una auditoria EXCEPTUADA
-- en vez de DESVIO cuando el valor simulado cae fuera de rango.
INSERT IGNORE INTO excepciones
  (id_excepcion, alcance, id_elemento, id_segmento, parametro, motivo, vigente_desde, vigente_hasta, activa)
VALUES
  (1, 'SEGMENTO', NULL, 1, 'PotenciaTX_dBm',
   'Ajuste de potencia en curso en Zona Norte, autorizado por ingenieria RF',
   NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), TRUE);

-- Excepcion por PATRON (ampliacion): todos los sitios de prueba (nombre con
-- "_test_") quedan exceptuados en todos sus parametros ("*"), sin vencimiento.
INSERT IGNORE INTO excepciones
  (id_excepcion, alcance, id_elemento, id_segmento, patron_nombre, parametro, motivo, vigente_desde, vigente_hasta, activa)
VALUES
  (2, 'PATRON', NULL, NULL, '*_test_*', '*',
   'Sitios de prueba: no se reconfiguran', NULL, NULL, TRUE);

-- ── Cambios masivos de ejemplo ────────────────────────────────────────────
-- Solo para que el dashboard tenga algo que mostrar en "cambios masivos
-- por estado"; la logica de negocio de CU004 (autorizar/monitorear) sigue
-- fuera de alcance.
INSERT IGNORE INTO cambios_masivos (id_cambio, id_segmento, parametro, pct_afectado, estado, autorizado_por, fecha)
VALUES (1, 1, 'PotenciaTX_dBm', 12.50, 'PENDIENTE', NULL, NOW());

INSERT IGNORE INTO cambios_masivos (id_cambio, id_segmento, parametro, pct_afectado, estado, autorizado_por, fecha)
VALUES (2, 2, 'AnguloTilt_grados', 5.00, 'APROBADO', 'jefe.rf@neipa.local', NOW());
