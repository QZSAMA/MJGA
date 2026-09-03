#!/bin/sh

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAD_FILE="$SCRIPT_DIR/dist/MJGA.jad"

if [ ! -f "$JAD_FILE" ]; then
    echo "Missing $JAD_FILE; run: ant clean test dist" >&2
    exit 1
fi

EMULATOR_CP="$SCRIPT_DIR/emulator/microemulator.jar:$SCRIPT_DIR/emulator/microemu-jsr-75.jar"
exec java -cp "$EMULATOR_CP" org.microemu.app.Main "$JAD_FILE"
