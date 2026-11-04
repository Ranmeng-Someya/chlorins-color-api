#!/usr/bin/env bash
# Compiles and runs the dependency-free self test (parser + overlay + animation maths).
# Needs only a JDK 17+; no Minecraft, no Gradle, no network.
set -euo pipefail
cd "$(dirname "$0")/.."
JAVAC="${JAVAC:-javac}"
JAVA="${JAVA:-java}"
rm -rf build/classes
mkdir -p build/classes
FILES=$(find src/main/java/com/chlorins/colorapi/parser \
    src/main/java/com/chlorins/colorapi/cache \
    src/main/java/net src/test/java -name '*.java')
FILES="$FILES src/main/java/com/chlorins/colorapi/ChlorinsColors.java"
FILES="$FILES src/main/java/com/chlorins/colorapi/render/Overlay.java"
FILES="$FILES src/main/java/com/chlorins/colorapi/render/OverlayScanner.java"
FILES="$FILES src/main/java/com/chlorins/colorapi/render/AnimationEngine.java"
"$JAVAC" -encoding UTF-8 -d build/classes $FILES
"$JAVA" -cp build/classes com.chlorins.colorapi.ParserSelfTest
