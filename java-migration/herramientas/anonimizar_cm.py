#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Anonimiza un dump SQLite de CM de NetAct para poder compartirlo y probar NEIPA
sin exponer datos reales de la red.

Que hace
--------
1. Toma una MUESTRA de sitios (LNBTS) por RC (PLMN_id): por defecto 50 por RC,
   elegidos al azar con una semilla fija (el resultado es reproducible).
2. Copia solo el SUBCONJUNTO que usa NEIPA: las tablas que dependen de un LNBTS
   (tienen la columna LNBTS_id), salvo:
     - las vecindades LNREL* / LNADJ* (NEIPA no las importa), y
     - las clases de transporte, seguridad, sincronismo, trazas y gestion del
       sitio (IPs, VLANs, certificados, servidores NTP/PTP/LDAP, MME, cuentas
       de usuario, numeros de serie de HW, todo lo que cuelga de FTM...),
       que no son parametros de radio y son las mas sensibles.
   Las tablas conservan exactamente las mismas columnas y tipos que el dump
   original, asi que NEIPA las lee igual (pantalla "Datos CM").
3. Anonimiza los valores:
     - MRBTS_id / LNBTS_id: se renumeran (1001, 1002, ...) y se reescribe el
       distName ("PLMN-PLMN/MRBTS-1001/LNBTS-1001/LNCEL-11").
     - Nombres (name, siteTemplateName, enbName, cellName, heNBName, ...):
       "SITIO-01001" para el sitio; las celdas conservan solo el sufijo de
       banda/sector ("SITIO-01001_M1").
     - mcc / mnc (y toda columna que termine en Mcc/Mnc): PLMN de prueba 001/01.
     - eutraCelId: se recalcula con el nuevo id (eNB x 256 + id local).
     - tac: se renumera de forma consistente (los mismos tac reales quedan
       iguales entre si; ej. 10001, 10002...).
     - Coordenadas y altitud de la celda: se borran (NULL).
     - Ids globales de vecinos 5G (gNB/cell id): se borran (NULL).
     - Cualquier otro texto que no sea un numero: se reemplaza por un token
       ("anon-1a2b3c4d"); las IPs, por una IP privada 10.x.y.z.
   NO se tocan los parametros de configuracion de radio (n310Qci1, pMax,
   t310, qrxlevmin, earfcn, phyCellId, ...): son justamente lo que se audita.
4. Verifica el resultado: busca en todas las columnas de texto los nombres
   reales de los sitios de la muestra, el mcc/mnc reales e IPs no privadas.
   Si encuentra algo, lo informa y termina con codigo 2.

Opcional: --sitios-test N marca N sitios por RC con "_test_" en el nombre,
para probar las excepciones por patron (ej. patron "*_test_*").

Uso
---
  python anonimizar_cm.py --origen ../../external_data/20240124_sqlite.dba \
                          --destino ../datos/cm_anonimizado.dba

  python anonimizar_cm.py --origen DUMP.dba --destino SALIDA.dba \
      --sitios-por-rc 20 --sitios-test 2 --semilla 7 --rc rc1,rc7

Opciones utiles:
  --sitios-por-rc 0       todos los sitios (archivo de varios cientos de MB)
  --rc rc1,rc7            solo esos RC
  --clases LNBTS,LNCEL,SIB,LNCEL_FDD   solo esas clases
  --mapa mapa_privado.csv guarda la tabla real -> anonimo. ES PRIVADA:
                          no compartirla ni subirla al repo.

Solo usa la biblioteca estandar de Python 3 (sqlite3). El dump original se
abre en solo lectura y nunca se modifica.
"""

import argparse
import csv
import hashlib
import ipaddress
import os
import pathlib
import random
import re
import sqlite3
import sys
import time

# ---------------------------------------------------------------- configuracion

# Vecindades: NEIPA no las importa (neipa.cm.clases-excluidas).
EXCLUIDAS_PATRONES = [r"^LNREL", r"^LNADJ"]

# Transporte, seguridad, sincronismo, trazas, PM e inventario de HW: no son
# parametros de radio y concentran IPs, certificados, servidores y seriales.
EXCLUIDAS_SENSIBLES = [
    r"^BFDGRP", r"^BTOOTH", r"^CACERT", r"^CBRSCERTH", r"^CERTH", r"^CTRLTS",
    r"^ETHLK", r"^FTM$", r"^GTPU", r"^IDNS", r"^IEIF", r"^IHCP", r"^INTP",
    r"^IPNO", r"^IPRM", r"^IPRT", r"^IPSECC", r"^IVIF", r"^L2SWI", r"^LNMME",
    r"^LTRACE", r"^MTRACE", r"^NTPS", r"^PMCCP", r"^PMPLM", r"^PMQAP",
    r"^PMRNL", r"^PMRPQH", r"^PMTNL", r"^POE$", r"^PPTT", r"^QOS", r"^RTPOL",
    r"^SCTP", r"^SECPRM", r"^STPG", r"^SYNC", r"^TOPB", r"^TOPF", r"^TOPP",
    r"^TRSNW", r"^TWAMP", r"^UNIT$", r"^WIFICNF", r"^LNSENB",
    r"^AMGR", r"^LUAC", r"^PNASC",  # cuentas de usuario, LDAP, acceso local
]
# Ademas se omite todo objeto que cuelgue de FTM (gestion del sitio: usuarios,
# LDAP, admision de transporte); se filtra por el distName.
RAMAS_EXCLUIDAS = ["/FTM-"]

# Columnas de texto que se dejan como estan.
TEXTO_SEGURO = {"timestamp", "moversion", "plmn_id"}

# Columnas con nombres de sitio/celda.
RE_NOMBRE = re.compile(r"(^name$|name$|label$|^sitetemplatename$|presentationname)", re.I)
# Columnas con un distName (texto).
RE_DN = re.compile(r"(^distname$|dn$|dnlist$|^olddn$)", re.I)
# PLMN real.
RE_MCC = re.compile(r"mcc$", re.I)
RE_MNC = re.compile(r"mnc$", re.I)
# Ubicacion de la celda.
RE_UBICACION = re.compile(
    r"(latitud|longitud|altitude|uncertainty|confidence|orientationofmajoraxis|latitudesign)", re.I)
# Identificadores globales (vecinos 5G, etc.).
RE_ID_GLOBAL = re.compile(r"(gnbcellid|gnbid|enbid|cellidentity|nrcgi|^eci)$", re.I)
# Referencias numericas a otro eNB/MRBTS (no son parametros de radio).
RE_REF_ENB = re.compile(r"^(lnbtsid|mrbtsid|enbidref|targetlnbtsid)$", re.I)
RE_TAC = re.compile(r"^(tac|tacvalue|trackingareacode)$", re.I)

RE_MRBTS = re.compile(r"MRBTS-(\d+)")
RE_LNBTS = re.compile(r"LNBTS-(\d+)")
RE_IPV4 = re.compile(r"\b(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})\b")
RE_NUMERO = re.compile(r"^\s*-?\d+(\.\d+)?\s*$")
RE_SUFIJO_CELDA = re.compile(r"_([A-Za-z0-9]{1,6})$")
ENUM_TEXTO = {"true", "false", "on", "off", "enabled", "disabled", "yes", "no"}

PRIMER_ID = 1001          # primer MRBTS/LNBTS anonimo
PRIMER_ID_EXTERNO = 900001  # ids de otros sitios referenciados en un texto
PRIMER_TAC = 10001
PLMN_PRUEBA_MCC = 1       # 001
PLMN_PRUEBA_MNC = 1       # 01


def q(nombre):
    """Identificador SQL entre comillas dobles."""
    return '"' + nombre.replace('"', '""') + '"'


def excluida(tabla, sin_filtro_sensible):
    patrones = list(EXCLUIDAS_PATRONES)
    if not sin_filtro_sensible:
        patrones += EXCLUIDAS_SENSIBLES
    return any(re.search(p, tabla, re.I) for p in patrones)


def es_texto(tipo):
    t = (tipo or "").lower()
    return "char" in t or "text" in t or "clob" in t


# ---------------------------------------------------------------- anonimizador

class Anonimizador:
    def __init__(self, sal):
        self.sal = sal
        self.ids = {}            # (mrbts_real, lnbts_real) -> id anonimo
        self.id_por_mrbts = {}   # mrbts_real -> id anonimo
        self.id_por_lnbts = {}   # lnbts_real -> id anonimo
        self.externos = {}       # id real no muestreado -> id anonimo externo
        self.nombres = {}        # id anonimo -> nombre anonimo del sitio
        self.tacs = {}           # tac real -> tac anonimo
        self.nombres_reales = set()
        self.ids_reales = set()

    # ids
    def registrar(self, mrbts, lnbts, id_nuevo, nombre_nuevo, nombre_real):
        self.ids[(mrbts, lnbts)] = id_nuevo
        self.ids_reales.update((mrbts, lnbts))
        self.id_por_mrbts[mrbts] = id_nuevo
        self.id_por_lnbts[lnbts] = id_nuevo
        self.nombres[id_nuevo] = nombre_nuevo
        if nombre_real:
            self.nombres_reales.add(nombre_real)

    def _externo(self, real):
        if real not in self.externos:
            self.externos[real] = PRIMER_ID_EXTERNO + len(self.externos)
        return self.externos[real]

    def mrbts(self, real):
        if real is None:
            return None
        real = int(real)
        return self.id_por_mrbts.get(real) or self._externo(("M", real))

    def lnbts(self, real):
        if real is None:
            return None
        real = int(real)
        return self.id_por_lnbts.get(real) or self._externo(("L", real))

    # textos
    def hash(self, texto, largo=8):
        return hashlib.sha256((self.sal + "|" + str(texto)).encode("utf-8")).hexdigest()[:largo]

    def dn(self, valor):
        if valor is None:
            return None
        s = str(valor)
        if "MRBTS-" not in s and "LNBTS-" not in s:
            # oldDN y similares a veces traen el nombre del sitio, no un distName
            return self.texto(s)
        s = RE_MRBTS.sub(lambda m: "MRBTS-%d" % self.mrbts(m.group(1)), s)
        s = RE_LNBTS.sub(lambda m: "LNBTS-%d" % self.lnbts(m.group(1)), s)
        return s

    def ip(self, valor):
        def reemplazo(m):
            try:
                ipaddress.ip_address(m.group(0))
            except ValueError:
                return m.group(0)
            h = int(self.hash(m.group(0), 6), 16)
            return "10.%d.%d.%d" % ((h >> 16) & 255, (h >> 8) & 255, max(1, h & 255))
        return RE_IPV4.sub(reemplazo, valor)

    def nombre(self, valor, mrbts_real, lnbts_real, es_sitio):
        if valor is None:
            return None
        base = None
        if lnbts_real is not None:
            base = self.nombres.get(self.lnbts(lnbts_real))
        if base is None and mrbts_real is not None:
            base = self.nombres.get(self.mrbts(mrbts_real))
        if base is None:
            base = "OBJ-" + self.hash(valor)
        if es_sitio:
            return base
        m = RE_SUFIJO_CELDA.search(str(valor))
        return base + ("_" + m.group(1) if m else "")

    def texto(self, valor):
        if valor is None:
            return None
        s = str(valor)
        if s.strip() == "" or RE_NUMERO.match(s) or s.strip().lower() in ENUM_TEXTO:
            return valor
        if "MRBTS-" in s or "LNBTS-" in s:
            return self.dn(self.ip(s))
        if RE_IPV4.search(s):
            t = self.ip(s)
            # solo IPs (y separadores): se conserva el formato; si hay mas texto, token
            return t if re.fullmatch(r"[\d.\s,;:/\[\]]+", t) else "anon-" + self.hash(s)
        return "anon-" + self.hash(s)

    def tac(self, valor):
        if valor is None:
            return None
        try:
            real = int(valor)
        except (TypeError, ValueError):
            return valor
        if real not in self.tacs:
            self.tacs[real] = None  # se numera al final, ordenado
        return real

    def numerar_tacs(self):
        for i, real in enumerate(sorted(self.tacs)):
            self.tacs[real] = PRIMER_TAC + i


# ---------------------------------------------------------------- proceso

def log(msg):
    print(time.strftime("%H:%M:%S"), msg, flush=True)


def elegir_muestra(con, args):
    filas = con.execute(
        "SELECT PLMN_id, MRBTS_id, LNBTS_id, name FROM src.LNBTS "
        "WHERE MRBTS_id IS NOT NULL AND LNBTS_id IS NOT NULL").fetchall()
    por_rc = {}
    for rc, m, l, n in filas:
        por_rc.setdefault(rc or "sin_rc", []).append((int(m), int(l), n))
    rcs = sorted(por_rc, key=lambda r: (len(r), r))
    if args.rc:
        pedidos = [r.strip().lower() for r in args.rc.split(",") if r.strip()]
        rcs = [r for r in rcs if r.lower() in pedidos]
    rnd = random.Random(args.semilla)
    muestra = []
    for rc in rcs:
        sitios = sorted(por_rc[rc])
        if args.sitios_por_rc and len(sitios) > args.sitios_por_rc:
            sitios = sorted(rnd.sample(sitios, args.sitios_por_rc))
        rnd.shuffle(sitios)  # que el orden de ids no siga el real
        muestra.append((rc, sitios))
    return muestra


def crear_tablas(con, tablas):
    for t in tablas:
        sql = con.execute("SELECT sql FROM src.sqlite_master WHERE type='table' AND name=?", (t,)).fetchone()[0]
        con.execute("DROP TABLE IF EXISTS main." + q(t))
        con.execute(sql)  # se crea en main (la base destino)


def copiar(con, tabla, columnas, sin_ramas_sensibles=True):
    nombres = [c[0] for c in columnas]
    tiene_mrbts = "MRBTS_id" in nombres
    cond = "m.l = s.LNBTS_id" + (" AND m.m = s.MRBTS_id" if tiene_mrbts else "")
    if sin_ramas_sensibles and "distName" in nombres:
        for rama in RAMAS_EXCLUIDAS:
            cond += " AND s.distName NOT LIKE '%%%s%%'" % rama
    lista = ", ".join("s." + q(c) for c in nombres)
    con.execute("INSERT INTO main.%s (%s) SELECT %s FROM src.%s s JOIN temp.muestra m ON %s"
                % (q(tabla), ", ".join(q(c) for c in nombres), lista, q(tabla), cond))
    return con.execute("SELECT count(*) FROM main." + q(tabla)).fetchone()[0]


def anonimizar_tabla(con, an, tabla, columnas):
    nombres = {c[0] for c in columnas}
    sets = []
    mrbts_col = "MRBTS_id" if "MRBTS_id" in nombres else "NULL"
    lnbts_col = "LNBTS_id" if "LNBTS_id" in nombres else "NULL"
    for col, tipo in columnas:
        bajo = col.lower()
        if bajo in TEXTO_SEGURO:
            continue
        if col == "MRBTS_id":
            sets.append('%s = an_mrbts(%s)' % (q(col), q(col)))
        elif col == "LNBTS_id":
            sets.append('%s = an_lnbts(%s)' % (q(col), q(col)))
        elif RE_REF_ENB.search(col):
            # referencia a otro eNB (ej. CAREL.lnBtsId): mismo renumerado que LNBTS_id
            sets.append('%s = an_lnbts(%s)' % (q(col), q(col)))
        elif bajo == "eutracelid":
            sets.append('%s = CASE WHEN %s IS NULL THEN NULL ELSE an_lnbts(%s) * 256 + (CAST(%s AS INTEGER) %% 256) END'
                        % (q(col), q(col), lnbts_col, q(col)))
        elif RE_MCC.search(col):
            sets.append('%s = CASE WHEN %s IS NULL THEN NULL ELSE %d END' % (q(col), q(col), PLMN_PRUEBA_MCC))
        elif RE_MNC.search(col):
            sets.append('%s = CASE WHEN %s IS NULL THEN NULL ELSE %d END' % (q(col), q(col), PLMN_PRUEBA_MNC))
        elif RE_UBICACION.search(col) or RE_ID_GLOBAL.search(col):
            sets.append('%s = NULL' % q(col))
        elif RE_TAC.match(col):
            sets.append('%s = an_tac(%s)' % (q(col), q(col)))
        elif es_texto(tipo):
            if RE_DN.search(col):
                sets.append('%s = an_dn(%s)' % (q(col), q(col)))
            elif RE_NOMBRE.search(col):
                es_sitio = 1 if tabla.upper() == "LNBTS" else 0
                sets.append('%s = an_nombre(%s, %s, %s, %d)' % (q(col), q(col), mrbts_col, lnbts_col, es_sitio))
            else:
                sets.append('%s = an_texto(%s)' % (q(col), q(col)))
    # MRBTS_id/LNBTS_id al final: las funciones de nombre usan los ids reales.
    sets.sort(key=lambda s: s.startswith('"MRBTS_id"') or s.startswith('"LNBTS_id"'))
    if sets:
        con.execute("UPDATE main.%s SET %s" % (q(tabla), ", ".join(sets)))


def aplicar_tacs(con, an, tablas_columnas):
    an.numerar_tacs()
    con.create_function("an_tac2", 1, lambda v: None if v is None else an.tacs.get(int(v), v), deterministic=True)
    for tabla, columnas in tablas_columnas.items():
        for col, _ in columnas:
            if RE_TAC.match(col):
                con.execute("UPDATE main.%s SET %s = an_tac2(%s)" % (q(tabla), q(col), q(col)))


def verificar(con, an, tablas_columnas, mcc_real, mnc_real):
    problemas = []
    reales = [n for n in an.nombres_reales if n and len(n) >= 4]
    # buscar por el "nucleo" del nombre real (sin prefijo de ciudad), ej. "Los Olivos"
    nucleos = set()
    for n in reales:
        nucleos.add(n)
        partes = re.split(r"[._]", n, maxsplit=1)
        if len(partes) == 2 and len(partes[1]) >= 5:
            nucleos.add(partes[1])
    for tabla, columnas in tablas_columnas.items():
        for col, tipo in columnas:
            if col.lower() in ("timestamp", "moversion"):
                continue
            if es_texto(tipo):
                for (v,) in con.execute("SELECT DISTINCT %s FROM main.%s WHERE %s IS NOT NULL" % (q(col), q(tabla), q(col))):
                    s = str(v)
                    if any(nu in s for nu in nucleos):
                        problemas.append("%s.%s contiene un nombre real: %r" % (tabla, col, s[:60]))
                        break
                    for m in RE_IPV4.finditer(s):
                        try:
                            if not ipaddress.ip_address(m.group(0)).is_private:
                                problemas.append("%s.%s contiene una IP publica" % (tabla, col))
                        except ValueError:
                            pass
            elif col not in ("MRBTS_id", "LNBTS_id") and not RE_REF_ENB.search(col):
                # columnas numericas con muchos ids reales de eNB: posible referencia sin anonimizar
                valores = {v for (v,) in con.execute(
                    "SELECT DISTINCT %s FROM main.%s WHERE typeof(%s) = 'integer' AND %s >= 1000"
                    % (q(col), q(tabla), q(col), q(col)))}
                if len(valores & an.ids_reales) >= 5:
                    problemas.append("%s.%s parece tener ids reales de eNB" % (tabla, col))
            if RE_MCC.search(col) and mcc_real is not None:
                n = con.execute("SELECT count(*) FROM main.%s WHERE %s = ?" % (q(tabla), q(col)), (mcc_real,)).fetchone()[0]
                if n:
                    problemas.append("%s.%s conserva el mcc real en %d filas" % (tabla, col, n))
    return problemas


def guardar_mapa(ruta, muestra_real, an):
    with open(ruta, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["rc", "mrbts_real", "lnbts_real", "nombre_real", "id_anonimo", "nombre_anonimo"])
        for rc, m, l, n in muestra_real:
            nuevo = an.ids[(m, l)]
            w.writerow([rc, m, l, n, nuevo, an.nombres[nuevo]])


def main():
    ap = argparse.ArgumentParser(description="Anonimiza un dump SQLite de CM de NetAct (subconjunto que usa NEIPA).")
    ap.add_argument("--origen", required=True, help="dump SQLite original (.dba)")
    ap.add_argument("--destino", required=True, help="archivo SQLite anonimizado a generar")
    ap.add_argument("--sitios-por-rc", type=int, default=50, help="sitios (LNBTS) por RC; 0 = todos (def. 50)")
    ap.add_argument("--rc", help="RC a incluir, separados por coma (def. todos)")
    ap.add_argument("--clases", help="solo estas clases, separadas por coma (def. todas las de radio que dependen de LNBTS)")
    ap.add_argument("--sitios-test", type=int, default=0, help="sitios por RC marcados con _test_ en el nombre (def. 0)")
    ap.add_argument("--semilla", type=int, default=2024, help="semilla de la muestra y de los seudonimos (def. 2024)")
    ap.add_argument("--incluir-sensibles", action="store_true",
                    help="no excluir transporte/seguridad/trazas (igual se anonimizan los textos)")
    ap.add_argument("--mapa", help="CSV PRIVADO real -> anonimo (no compartir)")
    ap.add_argument("--forzar", action="store_true", help="sobrescribir el destino si existe")
    args = ap.parse_args()

    origen = os.path.abspath(args.origen)
    destino = os.path.abspath(args.destino)
    if not os.path.isfile(origen):
        sys.exit("No existe el dump de origen: " + origen)
    if origen == destino:
        sys.exit("El destino no puede ser el mismo archivo que el origen.")
    if os.path.exists(destino):
        if not args.forzar:
            sys.exit("Ya existe " + destino + " (usar --forzar para sobrescribirlo).")
        open(destino, "wb").close()  # se vacia (un archivo vacio es una base SQLite vacia)
    os.makedirs(os.path.dirname(destino) or ".", exist_ok=True)

    t0 = time.time()
    # El origen se adjunta en solo lectura e "immutable" (no crea -wal/-shm ni lo modifica).
    con = sqlite3.connect(pathlib.Path(destino).as_uri(), uri=True)
    con.execute("ATTACH DATABASE ? AS src", (pathlib.Path(origen).as_uri() + "?mode=ro&immutable=1",))
    con.execute("PRAGMA main.journal_mode = OFF")
    con.execute("PRAGMA main.synchronous = OFF")

    an = Anonimizador(sal=str(args.semilla))
    con.create_function("an_mrbts", 1, an.mrbts, deterministic=True)
    con.create_function("an_lnbts", 1, an.lnbts, deterministic=True)
    con.create_function("an_dn", 1, an.dn, deterministic=True)
    con.create_function("an_texto", 1, an.texto, deterministic=True)
    con.create_function("an_nombre", 4, lambda v, m, l, s: an.nombre(v, m, l, bool(s)), deterministic=True)
    con.create_function("an_tac", 1, an.tac, deterministic=True)

    # 1. muestra
    muestra = elegir_muestra(con, args)
    if not muestra:
        sys.exit("No hay sitios para los RC pedidos.")
    con.execute("CREATE TEMP TABLE muestra (m INTEGER, l INTEGER, PRIMARY KEY (m, l))")
    siguiente = PRIMER_ID
    muestra_real = []
    for rc, sitios in muestra:
        for i, (m, l, n) in enumerate(sitios):
            nombre_nuevo = "SITIO-%05d" % siguiente
            if i < args.sitios_test:
                nombre_nuevo += "_test_"
            an.registrar(m, l, siguiente, nombre_nuevo, n)
            con.execute("INSERT INTO temp.muestra VALUES (?, ?)", (m, l))
            muestra_real.append((rc, m, l, n))
            siguiente += 1
        log("RC %-5s %4d sitios en la muestra" % (rc, len(sitios)))
    fila = con.execute("SELECT mcc, mnc FROM src.LNBTS WHERE mcc IS NOT NULL LIMIT 1").fetchone()
    mcc_real, mnc_real = (fila or (None, None))

    # 2. tablas a copiar
    todas = [r[0] for r in con.execute("SELECT name FROM src.sqlite_master WHERE type='table' ORDER BY name")]
    pedidas = None
    if args.clases:
        pedidas = {c.strip().upper() for c in args.clases.split(",") if c.strip()}
        pedidas |= {"LNBTS"}
    tablas, omitidas = [], []
    for t in todas:
        cols = con.execute("PRAGMA src.table_info(%s)" % q(t)).fetchall()
        if not any(c[1] == "LNBTS_id" for c in cols):
            continue
        if pedidas is not None and t.upper() not in pedidas:
            continue
        if excluida(t, args.incluir_sensibles):
            omitidas.append(t)
            continue
        tablas.append(t)
    if "LNBTS" not in tablas:
        sys.exit("El dump no tiene la tabla LNBTS con LNBTS_id.")
    log("%d tablas a copiar, %d omitidas (vecindades/transporte/seguridad)" % (len(tablas), len(omitidas)))

    crear_tablas(con, tablas)
    tablas_columnas = {}
    total = 0
    for i, t in enumerate(tablas, 1):
        columnas = [(c[1], c[2]) for c in con.execute("PRAGMA src.table_info(%s)" % q(t))]
        tablas_columnas[t] = columnas
        n = copiar(con, t, columnas, not args.incluir_sensibles)
        total += n
        log("[%3d/%d] %-34s %8d filas" % (i, len(tablas), t, n))
    con.commit()

    # 3. anonimizar
    log("Anonimizando...")
    for t, columnas in tablas_columnas.items():
        anonimizar_tabla(con, an, t, columnas)
    aplicar_tacs(con, an, tablas_columnas)
    con.commit()

    # tablas vacias: se dejan (NEIPA solo avisa si falta una clase), pero se informan
    vacias = [t for t in tablas if con.execute("SELECT 1 FROM main.%s LIMIT 1" % q(t)).fetchone() is None]

    # 4. verificar
    log("Verificando que no queden datos reales...")
    problemas = verificar(con, an, tablas_columnas, mcc_real, mnc_real)
    con.execute("DETACH DATABASE src")
    con.commit()
    con.execute("VACUUM")
    con.close()

    if args.mapa:
        guardar_mapa(args.mapa, muestra_real, an)
        log("Mapa PRIVADO real -> anonimo en %s (NO compartir)" % args.mapa)

    tam = os.path.getsize(destino) / 1024 / 1024
    print()
    print("Listo: %s (%.1f MB) en %.0f s" % (destino, tam, time.time() - t0))
    print("  Sitios: %d   Tablas: %d (%d vacias)   Filas: %d   TAC distintos: %d"
          % (len(muestra_real), len(tablas), len(vacias), total, len(an.tacs)))
    if problemas:
        print("\nATENCION: la verificacion encontro posibles datos reales:")
        for p in problemas[:30]:
            print("  - " + p)
        sys.exit(2)
    print("  Verificacion OK: sin nombres reales, sin mcc real, sin IPs publicas.")


if __name__ == "__main__":
    main()
