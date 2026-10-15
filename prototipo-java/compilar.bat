@echo off
rem Compila el prototipo de NEIPA con el JDK instalado (no necesita Maven).
if not exist out mkdir out
dir /s /b src\main\java\*.java > fuentes.txt
javac -d out @fuentes.txt
if errorlevel 1 (
  echo Hubo errores de compilacion.
) else (
  echo Compilacion correcta. Ejecute ejecutar.bat
)
del fuentes.txt
