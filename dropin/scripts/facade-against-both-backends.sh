#!/usr/bin/env bash
#
# The incumbent caller, unmodified, against both backends.
#
# This runs java-facade/target/pas-facade-1.0.0.jar - the Spring Boot 1.5 / Java 8
# application that exists in this repo today, built from untouched sources - twice:
#
#   1. as it ships, on its own local H2 database seeded by its own data.sql;
#   2. against the mirror's database, whose schema was translated from
#      sql/ddl/create-tables.sql and seeded with that same data.sql.
#
# Then it diffs the JSON. Byte-identical output means the incumbent application
# cannot tell which side it is talking to.
#
# One field is normalised before the diff: lastUpdated, which data.sql leaves to
# the LAST_UPDATED column default, so it records when each database was seeded
# rather than anything about the policy. Every other byte is compared as served.
#
# What changes between the two runs, and nothing else:
#   --spring.datasource.url        (where the data lives)
#   --spring.datasource.initialize (the mirror database is already seeded)
#   an H2 2.x driver on the loader path, because the mirror's database is H2 2.x
#   while the 2019-vintage jar bundles H2 1.4.199. A driver version, not a code change.
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="$ROOT/dropin/target/facade-proof"
FACADE_JAR="$ROOT/java-facade/target/pas-facade-1.0.0.jar"
SEED="$ROOT/java-facade/src/main/resources/data.sql"
POLICIES=(POL-00000001 POL-00000002 POL-00000003)
H2_JAR="$HOME/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar"

JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED"   # Spring 4 CGLIB on a modern JDK

mkdir -p "$OUT"

if [[ ! -f "$FACADE_JAR" ]]; then
    echo "Building the incumbent facade from its untouched sources..."
    (cd "$ROOT/java-facade" && mvn -q -DskipTests package)
fi

wait_for_health() {
    for _ in $(seq 1 60); do
        if curl -fsS "http://localhost:8080/manage/health" >/dev/null 2>&1; then return 0; fi
        sleep 1
    done
    echo "facade did not become healthy" >&2
    return 1
}

capture() {
    local label="$1"
    for policy in "${POLICIES[@]}"; do
        curl -fsS "http://localhost:8080/api/v1/policies/$policy" \
            | sed -E 's/"lastUpdated":[0-9]+/"lastUpdated":<seeded>/g' > "$OUT/$label-$policy.json"
        curl -fsS "http://localhost:8080/api/v1/policies/$policy/coverages" \
            | sed -E 's/"lastUpdated":[0-9]+/"lastUpdated":<seeded>/g' > "$OUT/$label-$policy-coverages.json"
    done
}

echo "== run 1: the incumbent facade on its own database =="
java $JAVA_OPTS -jar "$FACADE_JAR" --spring.profiles.active=local > "$OUT/facade-incumbent.log" 2>&1 &
FACADE_PID=$!
trap 'kill $FACADE_PID 2>/dev/null || true' EXIT
wait_for_health
capture incumbent
kill $FACADE_PID; wait $FACADE_PID 2>/dev/null || true

echo "== starting the mirror's database, schema translated from sql/ddl/create-tables.sql =="
java -cp "$ROOT/dropin/mirror/target/pas-mirror-1.0.0.jar:$(cat "$ROOT/dropin/target/mirror-classpath.txt")" \
    com.acme.dropin.mirror.MirrorServer --port=9092 \
    --database="$ROOT/dropin/target/pas-mirror-facade" --seed="$SEED" > "$OUT/mirror-db.log" 2>&1 &
MIRROR_PID=$!
trap 'kill $FACADE_PID $MIRROR_PID 2>/dev/null || true' EXIT
for _ in $(seq 1 60); do grep -q "mirror database ready" "$OUT/mirror-db.log" && break; sleep 1; done
grep -q "mirror database ready" "$OUT/mirror-db.log" || { cat "$OUT/mirror-db.log"; exit 1; }

echo "== run 2: the same jar, same profile, pointed at the mirror's database =="
java $JAVA_OPTS -cp "$H2_JAR:$FACADE_JAR" \
    -Dloader.path="$H2_JAR" \
    org.springframework.boot.loader.PropertiesLauncher \
    --spring.profiles.active=local \
    --spring.datasource.url="jdbc:h2:tcp://localhost:9092/$ROOT/dropin/target/pas-mirror-facade;MODE=DB2" \
    --spring.datasource.initialize=false > "$OUT/facade-mirror.log" 2>&1 &
FACADE_PID=$!
wait_for_health
capture mirror
kill $FACADE_PID; wait $FACADE_PID 2>/dev/null || true

echo
FAILED=0
for policy in "${POLICIES[@]}"; do
    for suffix in "" "-coverages"; do
        a="$OUT/incumbent-$policy$suffix.json"
        b="$OUT/mirror-$policy$suffix.json"
        if diff -q "$a" "$b" >/dev/null; then
            echo "identical  $policy$suffix"
        else
            echo "DIFFERENT  $policy$suffix"
            diff "$a" "$b" || true
            FAILED=1
        fi
    done
done

echo
if [[ $FAILED -eq 0 ]]; then
    echo "The unmodified incumbent facade returned byte-identical JSON from both backends."
else
    echo "The incumbent facade saw a difference between the backends." >&2
fi
exit $FAILED
