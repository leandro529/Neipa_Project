#!/usr/bin/env python3
"""
Convierte los diccionarios de parametros de Nokia (Parameters_LTE_*.xls,
SBTS_Parameters_*.xls, 5g*BTS parameter*.xlsx) a un CSV que NEIPA importa
como catalogo de parametros (pantalla "Catalogo").

Uso:
    python convertir_diccionarios_nokia.py <carpeta> [salida.csv]

Lee todos los .xls/.xlsx de la carpeta que tengan una hoja "... Parameter List"
y escribe <carpeta>/catalogo_parametros_nokia.csv (UTF-8, separador coma).
Requiere: pip install xlrd openpyxl

El CM de NetAct guarda los valores INTERNOS (por ejemplo pMax 430 = 43 dBm).
Por eso, ademas del rango que muestra el documento, se calcula el rango en
valores internos aplicando la formula "Formula for Getting Internal Value"
cuando es lineal (internal = a * gui + b). Las reglas de NEIPA se validan
contra el rango interno.
"""
import csv
import os
import re
import sys

COLUMNAS = ["fuente", "release", "tecnologia", "clase", "ruta_clase", "parametro", "parametro_doc",
            "estructura", "nombre", "tipo", "unidad", "rango", "minimo", "maximo", "paso",
            "minimo_interno", "maximo_interno", "paso_interno", "enumeracion", "formula",
            "default_valor", "default_interno", "modificacion", "funciones", "relacionados",
            "relaciones", "descripcion"]

CAMPOS = {
    "parametro": "Abbreviated Name", "ruta": "MO Class", "estructura": "Parent Structure",
    "nombre": "Full Name", "tipo": "Data Type", "rango": "Range and step",
    "formula": "Formula for Getting Internal Value", "default": "Default Value",
    "modificacion": "Modification", "funciones": "Related Functions",
    "relacionados": "Related Parameters", "relaciones": "Parameter Relationships",
    "descripcion": "Description", "tecnologia": "Technology",
}
LIMITE = {"descripcion": 2000, "relaciones": 4000, "relacionados": 2000, "funciones": 500}
NUM = r"-?\d+(?:\.\d+)?"


def texto(v):
    if v is None:
        return ""
    if isinstance(v, float) and v.is_integer():
        return str(int(v))
    s = str(v).strip()
    return "" if s == "None" else s


def numero(s):
    f = float(s)
    return str(int(f)) if f.is_integer() else repr(round(f, 6))


def leer_hojas(ruta):
    """Devuelve (nombre_hoja, filas) por cada hoja "Parameter List"."""
    if ruta.lower().endswith(".xlsx"):
        import openpyxl
        wb = openpyxl.load_workbook(ruta, read_only=True, data_only=True)
        for ws in wb.worksheets:
            if ws.title.strip().lower().endswith("parameter list"):
                yield ws.title, ([texto(c) for c in fila] for fila in ws.iter_rows(values_only=True))
    else:
        import xlrd
        wb = xlrd.open_workbook(ruta, on_demand=True)
        for nombre in wb.sheet_names():
            if nombre.strip().lower().endswith("parameter list"):
                sh = wb.sheet_by_name(nombre)
                yield nombre, ([texto(c) for c in sh.row_values(r)] for r in range(sh.nrows))


def rango_numerico(rango):
    """'-500...18000 m, step 1 m' -> (min, max, paso, unidad)."""
    m = re.match(r"^\s*(" + NUM + r")\s*\.\.\.\s*(" + NUM + r")\s*([^,\n]*)(?:,\s*step\s*(" + NUM + r"))?", rango)
    if not m:
        return None
    return float(m.group(1)), float(m.group(2)), (float(m.group(4)) if m.group(4) else None), m.group(3).strip()


def enumeracion(rango, tipo):
    """'1: n1 \\n2: n2' -> '1=n1|2=n2'; '0 (false), 1 (true)' -> '0=false|1=true'."""
    valores = []
    for linea in re.split(r"[\n\r]+", rango):
        m = re.match(r"^\s*(" + NUM + r")\s*:\s*(.*?)\s*$", linea)
        if m:
            valores.append((numero(m.group(1)), m.group(2)))
    if not valores:
        for m in re.finditer(r"(" + NUM + r")\s*\(([^)]*)\)", rango):
            valores.append((numero(m.group(1)), m.group(2).strip()))
    if not valores or ("enum" not in tipo.lower() and "bool" not in tipo.lower() and len(valores) < 2):
        return ""
    return "|".join(k + "=" + v.replace("|", "/") for k, v in valores)


def funcion_lineal(formula):
    """Devuelve f(gui) si la formula es lineal, o None."""
    if not formula:
        return None
    f = formula.strip().splitlines()[0]
    f = re.sub(r"(?i)^\s*internal_value\s*=\s*", "", f)
    f = re.sub(r"(?i)ui_value|gui_value", "x", f).replace(" ", "")
    if not f or not re.fullmatch(r"[x0-9.+\-*/()]+", f) or "x" not in f:
        return None
    try:
        a0 = eval(f, {"__builtins__": {}}, {"x": 0.0})
        a1 = eval(f, {"__builtins__": {}}, {"x": 1.0})
        a2 = eval(f, {"__builtins__": {}}, {"x": 7.0})
    except Exception:
        return None
    pendiente = a1 - a0
    if abs((a2 - a0) - 7 * pendiente) > 1e-6:
        return None
    return lambda v: pendiente * v + a0


def convertir_archivo(ruta):
    fuente = os.path.basename(ruta)
    release = ""
    m = re.search(r"(5g\d+\w?|\d+\w?)", fuente, re.I)
    if m:
        release = m.group(1)
    tec_archivo = "5G" if fuente.lower().startswith("5g") else ("SBTS" if "sbts" in fuente.lower() else "LTE")
    for hoja, filas in leer_hojas(ruta):
        indice = None
        for fila in filas:
            if indice is None:
                if "Abbreviated Name" in fila:
                    indice = {}
                    for clave, cabecera in CAMPOS.items():
                        if cabecera in fila:
                            indice[clave] = fila.index(cabecera)
                continue
            dato = {k: (fila[i] if i < len(fila) else "") for k, i in indice.items()}
            param = dato.get("parametro", "")
            if not param or not dato.get("ruta"):
                continue
            ruta_clase = dato["ruta"].split("\n")[0].strip()
            clase = ruta_clase.split("/")[-1].strip()
            estructura = dato.get("estructura", "")
            # En el CM de NetAct un hijo de estructura es una columna padre+hijo.
            nombre_cm = (estructura + param) if estructura else param
            tipo = dato.get("tipo", "")
            rango = dato.get("rango", "")
            fila_csv = {
                "fuente": fuente, "release": release, "clase": clase, "ruta_clase": ruta_clase,
                "tecnologia": tec_archivo + (" " + hoja.split()[0] if hoja.split()[0] in ("FDD", "TDD") else ""),
                "parametro": nombre_cm, "parametro_doc": param, "estructura": estructura,
                "nombre": dato.get("nombre", ""), "tipo": tipo, "rango": rango[:1000],
                "formula": dato.get("formula", "")[:200], "modificacion": dato.get("modificacion", ""),
                "default_valor": dato.get("default", ""),
            }
            for k in ("funciones", "relacionados", "relaciones", "descripcion"):
                fila_csv[k] = dato.get(k, "")[:LIMITE[k]]
            fila_csv["enumeracion"] = enumeracion(rango, tipo)
            f = funcion_lineal(dato.get("formula", "")) or (lambda v: v)
            if not dato.get("formula"):
                f = lambda v: v
            r = None if fila_csv["enumeracion"] else rango_numerico(rango)
            if r:
                mn, mx, paso, unidad = r
                fila_csv.update(minimo=numero(mn), maximo=numero(mx), unidad=unidad,
                                paso=numero(paso) if paso is not None else "")
                if funcion_lineal(dato.get("formula", "")) or not dato.get("formula"):
                    a, b = f(mn), f(mx)
                    fila_csv.update(minimo_interno=numero(min(a, b)), maximo_interno=numero(max(a, b)))
                    if paso is not None:
                        fila_csv["paso_interno"] = numero(abs(f(paso) - f(0)))
            # "10 (10)", "-130 dBm", "0" -> numero inicial; el interno aplica la formula.
            m = re.match(r"^\s*(" + NUM + r")(?![\d.])", fila_csv["default_valor"])
            if m:
                v = float(m.group(1))
                lineal = funcion_lineal(dato.get("formula", ""))
                if fila_csv["enumeracion"] or not dato.get("formula"):
                    fila_csv["default_interno"] = numero(v)
                elif lineal:
                    fila_csv["default_interno"] = numero(lineal(v))
            yield fila_csv


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(2)
    carpeta = sys.argv[1]
    salida = sys.argv[2] if len(sys.argv) > 2 else os.path.join(carpeta, "catalogo_parametros_nokia.csv")
    vistos, total = set(), 0
    with open(salida, "w", newline="", encoding="utf-8") as out:
        w = csv.DictWriter(out, fieldnames=COLUMNAS, extrasaction="ignore")
        w.writeheader()
        for nombre in sorted(os.listdir(carpeta)):
            if not nombre.lower().endswith((".xls", ".xlsx")) or nombre.startswith("~$"):
                continue
            n = 0
            for fila in convertir_archivo(os.path.join(carpeta, nombre)):
                clave = (fila["fuente"], fila["clase"], fila["parametro"])
                if clave in vistos:  # FDD y TDD repiten muchos parametros
                    continue
                vistos.add(clave)
                w.writerow(fila)
                n += 1
            print("%-45s %6d parametros" % (nombre, n))
            total += n
    print("Total: %d -> %s" % (total, salida))


if __name__ == "__main__":
    main()
