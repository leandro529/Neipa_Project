#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Genera un catalogo de parametros REDUCIDO para compartir junto con un dump de
CM anonimizado (ver anonimizar_cm.py).

Parte del catalogo completo (catalogo_parametros_nokia.csv, generado por
convertir_diccionarios_nokia.py) y se queda solo con:
  - las clases que estan en el dump (las tablas del .dba), y
  - de esas clases, los parametros que son columnas del dump
    (los hijos de estructuras ya vienen como padre+hijo, igual que en el CM).
Ademas:
  - vacia la columna "descripcion" (texto largo de la documentacion de Nokia),
    salvo que se pida --con-descripcion;
  - con --parametros deja solo una lista de parametros (por ejemplo, los que
    se usan en el manual de pruebas).

El CSV de salida tiene las mismas columnas que el original, asi que NEIPA lo
importa igual desde la pantalla "Catalogo".

Uso
---
  python reducir_catalogo.py \
      --catalogo ../../external_data/catalogo_parametros_nokia.csv \
      --dump ../datos/cm_anonimizado.dba \
      --salida ../datos/catalogo_reducido.csv

  python reducir_catalogo.py --catalogo ... --dump ... --salida ... \
      --parametros n310Qci1,t310Qci1,pMax,n310,qrxlevmin --con-descripcion
"""

import argparse
import csv
import os
import pathlib
import sqlite3
import sys

csv.field_size_limit(min(sys.maxsize, 2 ** 31 - 1))


def columnas_del_dump(ruta):
    uri = pathlib.Path(os.path.abspath(ruta)).as_uri() + "?mode=ro"
    con = sqlite3.connect(uri, uri=True)
    try:
        resultado = {}
        for (tabla,) in con.execute("SELECT name FROM sqlite_master WHERE type='table'"):
            cols = [r[1] for r in con.execute('PRAGMA table_info("%s")' % tabla.replace('"', '""'))]
            resultado[tabla.upper()] = {c.lower() for c in cols}
        return resultado
    finally:
        con.close()


def main():
    ap = argparse.ArgumentParser(description="Reduce el catalogo de parametros a las clases/columnas de un dump de CM.")
    ap.add_argument("--catalogo", required=True, help="catalogo completo (CSV)")
    ap.add_argument("--dump", required=True, help="dump SQLite (normalmente el anonimizado)")
    ap.add_argument("--salida", required=True, help="CSV reducido a generar")
    ap.add_argument("--parametros", help="solo estos parametros, separados por coma")
    ap.add_argument("--con-descripcion", action="store_true", help="conservar la descripcion larga")
    args = ap.parse_args()

    clases = columnas_del_dump(args.dump)
    pedidos = None
    if args.parametros:
        pedidos = {p.strip().lower() for p in args.parametros.split(",") if p.strip()}

    leidas = escritas = 0
    por_clase = {}
    os.makedirs(os.path.dirname(os.path.abspath(args.salida)) or ".", exist_ok=True)
    with open(args.catalogo, newline="", encoding="utf-8-sig") as fe, \
            open(args.salida, "w", newline="", encoding="utf-8") as fs:
        lector = csv.DictReader(fe)
        if "clase" not in lector.fieldnames or "parametro" not in lector.fieldnames:
            sys.exit("El CSV no es un catalogo de parametros (faltan las columnas clase y parametro).")
        escritor = csv.DictWriter(fs, fieldnames=lector.fieldnames)
        escritor.writeheader()
        for fila in lector:
            leidas += 1
            clase = (fila.get("clase") or "").strip().upper()
            parametro = (fila.get("parametro") or "").strip()
            columnas = clases.get(clase)
            if columnas is None or parametro.lower() not in columnas:
                continue
            if pedidos is not None and parametro.lower() not in pedidos:
                continue
            if not args.con_descripcion and "descripcion" in fila:
                fila["descripcion"] = ""
            escritor.writerow(fila)
            escritas += 1
            por_clase[clase] = por_clase.get(clase, 0) + 1

    tam = os.path.getsize(args.salida) / 1024
    print("Catalogo reducido: %s" % args.salida)
    print("  %d de %d filas, %d clases, %.0f KB" % (escritas, leidas, len(por_clase), tam))
    for clase, n in sorted(por_clase.items(), key=lambda x: -x[1])[:10]:
        print("    %-14s %5d" % (clase, n))
    if escritas == 0:
        sys.exit("No quedo ninguna fila: revisar que el dump y el catalogo sean de la misma tecnologia.")


if __name__ == "__main__":
    main()
