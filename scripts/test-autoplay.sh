#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
javac --release 8 -d "$classes" core/src/autoplay/AutoConfig.java core/src/autoplay/AutoPlayController.java tests/AutoPlayControllerTest.java
java -cp "$classes" AutoPlayControllerTest
