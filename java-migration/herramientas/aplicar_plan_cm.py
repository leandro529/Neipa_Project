#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Simula "aplicar el plan en la red": copia un dump de CM y le escribe los
valores de uno o mas planes RAML generados por NEIPA
(GET /api/auditorias/reconfiguracion?idSegmento=..&tipo=plan).

El resultado es un "dump corregido" que se vuelve a importar en NEIPA para
verificar que la corrida siguiente da 0 desvios (paso 7 del manual).

Formato que entiende (el de GeneradorRaml):
  <raml><cmData type="plan" ...>
    <managedObject class="LNCEL" operation="update" distName="PLMN-PLMN/MRBTS-1001/LNBTS-1001/LNCEL-11">
      <p name="n310Qci1">8</p>
    </managedObject>
  </cmData></raml>

Uso:
  python aplicar_plan_cm.py --dump ../datos/cm_anonimizado.dba \
      --plan plan_rc7.xml [--plan plan_rc1.xml ...] \
      --salida ../datos/cm_anonimizado_corregido.dba
El dump de entrada no se modifica.
"""

import argparse
import os
import shutil
import sqlite3
import sys
import xml.etree.ElementTree as ET


def local(tag):
    return tag.rsplit("}", 1)[-1]


def clase_de(dn):
    ultimo = dn.rsplit("/", 1)[-1]
    return ultimo.rsplit("-", 1)[0] if "-" in ultimo else ultimo


def leer_plan(ruta):
    cambios = []
    for mo in ET.parse(ruta).getroot().iter():
        if local(mo.tag) != "managedObject":
            continue
        dn = mo.get("distName")
        clase = mo.get("class") or clase_de(dn)
        for p in mo:
            if local(p.tag) == "p" and p.get("name"):
                cambios.append((clase, dn, p.get("name"), (p.text or "").strip()))
    return cambios


def main():
    ap = argparse.ArgumentParser(description="Aplica planes RAML de NEIPA a una copia de un dump de CM.")
    ap.add_argument("--dump", required=True)
    ap.add_argument("--plan", required=True, action="append", help="plan RAML (se puede repetir)")
    ap.add_argument("--salida", required=True)
    args = ap.parse_args()
    if os.path.abspath(args.dump) == os.path.abspath(args.salida):
        sys.exit("La salida tiene que ser otro archivo (el dump de entrada no se modifica).")

    cambios = []
    for plan in args.plan:
        c = leer_plan(plan)
        print("%s: %d valores" % (plan, len(c)))
        cambios += c
    if not cambios:
        sys.exit("Los planes no traen cambios.")

    shutil.copyfile(args.dump, args.salida)
    con = sqlite3.connect(args.salida)
    aplicados, faltan = 0, []
    for clase, dn, param, valor in cambios:
        cols = {r[1].lower(): r[1] for r in con.execute('PRAGMA table_info("%s")' % clase.replace('"', '""'))}
        if param.lower() not in cols:
            faltan.append("%s.%s (no existe la columna)" % (clase, param))
            continue
        v = int(valor) if valor.lstrip("-").isdigit() else valor
        n = con.execute('UPDATE "%s" SET "%s" = ? WHERE distName = ?' % (clase, cols[param.lower()]), (v, dn)).rowcount
        if n:
            aplicados += n
        else:
            faltan.append(dn + " (no esta en el dump)")
    con.commit()
    con.close()
    print("Dump corregido: %s  (%d valores aplicados)" % (args.salida, aplicados))
    for f in faltan[:20]:
        print("  sin aplicar: " + f)


if __name__ == "__main__":
    main()
