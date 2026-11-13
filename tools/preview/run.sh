#!/bin/sh
# Compila la logica di gioco sul PC, fa giocare il bot e salva le schermate in build/preview.
set -e
cd "$(dirname "$0")/../.."
OUT=build/preview
mkdir -p "$OUT/classes"
javac -nowarn -d "$OUT/classes" app/src/main/java/com/valeriodipaolo/platformgame/core/*.java tools/preview/Preview.java
java -Djava.awt.headless=true -cp "$OUT/classes" Preview app/src/main/res/drawable-nodpi "$OUT" "$@"
