#!/bin/sh
# Controlli automatici sulla logica di gioco dei due livelli (salto, blocchi, gemme, punti vita, corridoio 3D).
set -e
cd "$(dirname "$0")/../.."
OUT=build/preview
mkdir -p "$OUT/classes"
javac -nowarn -d "$OUT/classes" app/src/main/java/com/valeriodipaolo/platformgame/core/*.java \
    app/src/main/java/com/valeriodipaolo/platformgame/core3d/*.java tools/preview/Check.java tools/preview/Check3.java tools/preview/CheckFire.java tools/preview/CheckSpring.java tools/preview/CheckB.java tools/preview/Check3B.java tools/preview/CheckGate.java
java -cp "$OUT/classes" Check
java -cp "$OUT/classes" Check3
java -cp "$OUT/classes" CheckFire
java -cp "$OUT/classes" CheckSpring
java -cp "$OUT/classes" CheckB
java -cp "$OUT/classes" Check3B
java -cp "$OUT/classes" CheckGate
