#!/usr/bin/env bash
# Cross-checks the injection target declared in the mixin against the real TFMG class.
# Run it after bumping the TFMG jar in libs/: if TFMG renamed levelTick, this fails and the
# @Inject method string in TFMGCommonEventsMixin.java must be updated.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk}"
JAVA="$JAVA_HOME/bin/java"
JAVAC="$JAVA_HOME/bin/javac"

TFMG_JAR="$(ls "$ROOT"/libs/tfmg-*.jar | head -1)"
NEO_JAR="$(ls "$ROOT"/build/moddev/artifacts/neoforge-*-merged.jar 2>/dev/null | head -1)"
BUS_JAR="$(find "$HOME/.gradle/caches/modules-2/files-2.1/net.neoforged/bus" -name 'bus-*.jar' 2>/dev/null | head -1)"

if [[ -z "$NEO_JAR" || -z "$BUS_JAR" ]]; then
    echo "ERROR: run './gradlew build' once first (neoforge artifacts / event bus jar not found)" >&2
    exit 1
fi

OUT="$(mktemp -d)"
CP="$TFMG_JAR:$NEO_JAR:$BUS_JAR"
"$JAVAC" -cp "$CP" -d "$OUT" "$ROOT/tools/VerifyInjectionTarget.java"
PROBE="$("$JAVA" -cp "$CP:$OUT" VerifyInjectionTarget)"
echo "$PROBE"

DECLARED="$(grep -o 'levelTick(L[^"]*)V' "$ROOT/src/main/java/net/tfmgscanthrottle/mixin/TFMGCommonEventsMixin.java" | head -1)"
FOUND="$(printf '%s\n' "$PROBE" | sed -n 's/^DESCRIPTOR=//p')"

if [[ -z "$DECLARED" ]]; then
    echo "ERROR: could not read the @Inject method string from the mixin source" >&2
    exit 1
fi
if [[ "$DECLARED" != "$FOUND" ]]; then
    echo "MISMATCH: mixin declares '$DECLARED' but TFMG has '$FOUND'" >&2
    exit 1
fi
if ! printf '%s\n' "$PROBE" | grep -q '^STATIC=true$'; then
    echo "ERROR: target method is not static; the static @Inject handler is invalid" >&2
    exit 1
fi
echo "OK: injection target matches ($DECLARED)"
