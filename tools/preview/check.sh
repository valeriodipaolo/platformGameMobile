#!/bin/sh
# Controlli automatici sulla logica di gioco (salto, blocchi, monete, punti vita).
set -e
cd "$(dirname "$0")/../.."
OUT=build/preview
mkdir -p "$OUT/classes"
javac -nowarn -d "$OUT/classes" app/src/main/java/com/valeriodipaolo/platformgame/core/*.java tools/preview/Check.java
java -cp "$OUT/classes" Check
