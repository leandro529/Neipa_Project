#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Calcula, directamente sobre un dump de CM (normalmente el anonimizado), los
resultados que NEIPA deberia dar en los escenarios del manual de pruebas
(MANUAL-PRUEBAS-CM.md). Sirve para:
  - completar las tablas de "resultado esperado" del manual, y
  - volver a calcularlas si se genera otro dump (otra semilla, otros RC).

Reproduce la logica de CmAuditoriaService:
  - se compara cada objeto que TIENE valor para el parametro (NULL = no se audita);
  - entre varias reglas del mismo parametro gana la mas especifica que aplica
    (la que tiene condicion, si la condicion se cumple);
  - una excepcion solo cambia DESVIO -> EXCEPTUADO.

Escenarios (iguales a los del manual):
  A  R1 LNCEL n310Qci1 = 8 | R2 LNCEL t310Qci1 = 1000 | R3 SIB n310 = 6 | R4 LNCEL pMax en 400..460
  B  A + excepcion por PATRON "*_test_*" para todos los parametros ("*")
  C  B + R5 LNCEL n310Qci1 = 6 si LNBTS.actDLCAggr = 0
  D  C sobre el dump corregido (aplicar_plan_cm.py): 0 desvios

Uso:
  python esperados_cm.py --dump ../datos/cm_anonimizado.dba [--rc rc7] [--escenario C]
"""

import argparse
import fnmatch
import os
import pathlib
import sqlite3
from collections import OrderedDict, defaultdict


def abrir(ruta):
    return sqlite3.connect(pathlib.Path(os.path.abspath(ruta)).as_uri() + "?mode=ro", uri=True)


def texto(v):
    if v is None:
        return None
    if isinstance(v, float) and v.is_integer():
        return str(int(v))
    s = str(v).strip()
    return s or None


def num(v):
    try:
        return float(v)
    except (TypeError, ValueError):
        return None


def lnbts_dn(dn):
    i = dn.find("/LNBTS-")
    if i < 0:
        return None
    j = dn.find("/", i + 1)
    return dn if j < 0 else dn[:j]


def calcular(con, escenario, rc_filtro=None):
    lnbts = {}
    for dn, rc, act in con.execute("SELECT distName, PLMN_id, actDLCAggr FROM LNBTS"):
        lnbts[dn] = (rc, texto(act))
    nombres_cel = {dn: n for dn, n in con.execute("SELECT distName, name FROM LNCEL")}

    reglas = [
        ("R1", "LNCEL", "n310Qci1", lambda v: v == "8"),
        ("R2", "LNCEL", "t310Qci1", lambda v: v == "1000"),
        ("R3", "SIB", "n310", lambda v: v == "6"),
        ("R4", "LNCEL", "pMax", lambda v: num(v) is not None and 400 <= num(v) <= 460),
    ]
    con_excepcion = escenario in ("B", "C", "D")
    con_r5 = escenario in ("C", "D")

    tot = defaultdict(lambda: OrderedDict(comparaciones=0, conformes=0, desvios=0, exceptuados=0))
    por_regla = defaultdict(lambda: [0, 0, 0])  # (rc, regla) -> C, D, E
    for id_regla, clase, param, cumple in reglas:
        for dn, valor, nombre in con.execute('SELECT distName, "%s", %s FROM "%s"'
                                             % (param, "name" if clase == "LNCEL" else "NULL", clase)):
            valor = texto(valor)
            if valor is None:
                continue
            padre = lnbts.get(lnbts_dn(dn))
            if padre is None:
                continue
            rc, act = padre
            if rc_filtro and rc.lower() != rc_filtro.lower():
                continue
            regla_usada = id_regla
            ok = cumple(valor)
            if con_r5 and param == "n310Qci1" and act == "0":
                regla_usada = "R5"
                ok = valor == "6"
            if nombre is None:
                # SIB: el nombre es el de su celda
                cel = dn[:dn.rfind("/")]
                nombre = nombres_cel.get(cel)
            t = tot[rc]
            t["comparaciones"] += 1
            if ok:
                t["conformes"] += 1
                por_regla[(rc, regla_usada)][0] += 1
            elif con_excepcion and nombre and fnmatch.fnmatchcase(nombre, "*_test_*"):
                t["exceptuados"] += 1
                por_regla[(rc, regla_usada)][2] += 1
            else:
                t["desvios"] += 1
                por_regla[(rc, regla_usada)][1] += 1
    return tot, por_regla


def main():
    ap = argparse.ArgumentParser(description="Resultados esperados de los escenarios del manual de pruebas de CM.")
    ap.add_argument("--dump", required=True)
    ap.add_argument("--rc", help="solo este RC (ej. rc7)")
    ap.add_argument("--escenario", default="ABC", help="A, B, C o combinacion (def. ABC)")
    args = ap.parse_args()
    con = abrir(args.dump)
    for esc in args.escenario.upper():
        tot, por_regla = calcular(con, esc, args.rc)
        print("\nEscenario %s" % esc)
        print("| RC | Comparaciones | Conformes | Desvios | Exceptuados | Filas guardadas (D+E) |")
        print("|---|---:|---:|---:|---:|---:|")
        for rc in sorted(tot, key=lambda r: (len(r), r)):
            t = tot[rc]
            print("| %s | %d | %d | %d | %d | %d |" % (rc, t["comparaciones"], t["conformes"], t["desvios"],
                                                  t["exceptuados"], t["desvios"] + t["exceptuados"]))
        print("\n  Por regla (C/D/E):")
        for rc in sorted(tot, key=lambda r: (len(r), r)):
            partes = ["%s %d/%d/%d" % (rg, *por_regla[(rc, rg)]) for rg in ("R1", "R2", "R3", "R4", "R5")
                      if (rc, rg) in por_regla]
            print("    %-5s %s" % (rc, "  ".join(partes)))
    con.close()


if __name__ == "__main__":
    main()
