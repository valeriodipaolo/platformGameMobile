#!/bin/sh
# Salva in build/preview alcune schermate dei due livelli (il livello 3D è disegnato da un renderer software).
set -e
cd "$(dirname "$0")/../.."
OUT=build/preview
mkdir -p "$OUT/classes"
javac -nowarn -d "$OUT/classes" app/src/main/java/com/valeriodipaolo/platformgame/core/*.java \
    app/src/main/java/com/valeriodipaolo/platformgame/core3d/*.java tools/preview/Preview.java tools/preview/Scene.java \
    tools/preview/Soft3D.java tools/preview/Scene3.java
java -Djava.awt.headless=true -cp "$OUT/classes" Scene app/src/main/res/drawable-nodpi "$OUT"
java -Djava.awt.headless=true -cp "$OUT/classes" Scene3 app/src/main/res/drawable-nodpi "$OUT"
