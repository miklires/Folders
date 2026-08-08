#!/usr/bin/env bash
# Compiles and runs the version-independent core on a plain JVM.
#
# The Fabric toolchain needs maven.fabricmc.net, which is not always reachable;
# this script exists so the part of the mod that does not depend on Minecraft can
# still be verified. `gradle test` runs exactly the same sources.
set -euo pipefail

cd "$(dirname "$0")/.."
LIB=.verify/lib
OUT=.verify/out
GSON=2.11.0
SLF4J=2.0.16
JUNIT=1.11.4
CP="$LIB/gson-$GSON.jar:$LIB/slf4j-api-$SLF4J.jar:$LIB/slf4j-simple-$SLF4J.jar:$LIB/junit-platform-console-standalone-$JUNIT.jar"

if [ ! -d "$LIB" ]; then
  echo "== fetching test dependencies from Maven Central =="
  mkdir -p "$LIB"
  base=https://repo1.maven.org/maven2
  curl -sSf -o "$LIB/gson-$GSON.jar"        "$base/com/google/code/gson/gson/$GSON/gson-$GSON.jar"
  curl -sSf -o "$LIB/slf4j-api-$SLF4J.jar"  "$base/org/slf4j/slf4j-api/$SLF4J/slf4j-api-$SLF4J.jar"
  curl -sSf -o "$LIB/slf4j-simple-$SLF4J.jar" "$base/org/slf4j/slf4j-simple/$SLF4J/slf4j-simple-$SLF4J.jar"
  curl -sSf -o "$LIB/junit-platform-console-standalone-$JUNIT.jar" \
    "$base/org/junit/platform/junit-platform-console-standalone/$JUNIT/junit-platform-console-standalone-$JUNIT.jar"
fi

rm -rf "$OUT"
mkdir -p "$OUT/main" "$OUT/test"

echo "== compiling core =="
find src/main/java/dev/miklires/folders/core -name '*.java' > "$OUT/main.txt"
javac -Xlint:all -d "$OUT/main" -cp "$CP" @"$OUT/main.txt"

echo "== compiling tests =="
find src/test/java -name '*.java' > "$OUT/test.txt"
javac -d "$OUT/test" -cp "$CP:$OUT/main" @"$OUT/test.txt"

echo "== running tests =="
java -jar "$LIB/junit-platform-console-standalone-$JUNIT.jar" execute \
  --class-path "$OUT/main:$OUT/test:$CP" \
  --scan-class-path "$OUT/test" \
  --details=tree \
  --disable-ansi-colors
