#!/usr/bin/env bash
# Captures the golden response fixtures from the INCUMBENT facade.
#
# The fixtures in dropin/contract/src/main/resources/golden are not written by
# hand: they are the bytes the existing Java 8 / Spring Boot 1.5 application
# returns, on its own H2 local profile, with its own seed data. That is what
# makes them evidence rather than a restatement of the code.
#
# The only edit applied is to lastUpdated: the seed data sets LAST_UPDATED to
# CURRENT_TIMESTAMP, so it differs on every run. It is normalised to 0 here and
# the parity harness compares it structurally rather than by value.
#
# Usage:  dropin/scripts/capture-golden-fixtures.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="${REPO_ROOT}/dropin/contract/src/main/resources/golden"
JAR="${REPO_ROOT}/java-facade/target/pas-facade-1.0.0.jar"
PORT="${PORT:-8080}"
POLICIES=(POL-00000001 POL-00000002 POL-00000003)

if [[ ! -f "${JAR}" ]]; then
  echo "Building the incumbent facade (unmodified) ..."
  (cd "${REPO_ROOT}/java-facade" && mvn -q -DskipTests package)
fi

mkdir -p "${OUT}"

# Spring 4 / CGLIB needs java.lang opened on a modern JDK; this is a runtime
# flag only, the incumbent sources are untouched.
java --add-opens java.base/java.lang=ALL-UNNAMED \
     -jar "${JAR}" --spring.profiles.active=local --server.port="${PORT}" \
     > /tmp/pas-facade-capture.log 2>&1 &
FACADE_PID=$!
trap 'kill ${FACADE_PID} 2>/dev/null || true' EXIT

for _ in $(seq 1 60); do
  if curl -sf "http://localhost:${PORT}/manage/health" > /dev/null; then break; fi
  sleep 1
done

normalise() { sed -E 's/"lastUpdated":[0-9]+/"lastUpdated":0/g'; }

for policy in "${POLICIES[@]}"; do
  curl -sf "http://localhost:${PORT}/api/v1/policies/${policy}" \
    | normalise > "${OUT}/${policy}.policy.json"
  curl -sf "http://localhost:${PORT}/api/v1/policies/${policy}/coverages" \
    | normalise > "${OUT}/${policy}.coverages.json"
  echo "captured ${policy}"
done

status=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${PORT}/api/v1/policies/POL-99999999")
echo "${status}" > "${OUT}/unknown-policy.status"
echo "captured unknown policy status ${status}"
