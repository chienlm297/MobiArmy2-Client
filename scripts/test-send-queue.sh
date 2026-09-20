#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
bash gradlew :desktop:autoplayClasspath --offline --console=plain
client_cp=$(cat desktop/build/autoplay-classpath.txt)
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
javac --release 8 -cp "$client_cp" -d "$classes" tests/stubs/model/CRes.java tests/SessionSendQueueTest.java
java -cp "$classes:$client_cp" SessionSendQueueTest
