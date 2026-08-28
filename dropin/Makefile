# The whole demo, credential-free, from a clean clone.
#
#   make setup   build everything (Java modules + .NET client)
#   make test    unit tests - these pass
#   make parity  the differential gate - this fails today, on purpose
#   make proof   run the untouched incumbent facade against both backends
#   make demo    the switch, serving one contract from either implementation

DROPIN := $(CURDIR)
MVN := mvn -q -f $(DROPIN)/pom.xml
DOTNET := $(shell command -v dotnet 2>/dev/null || echo $(HOME)/.dotnet/dotnet)

.PHONY: setup test parity proof demo dotnet-test clean

setup:
	$(MVN) install -DskipTests
	$(DOTNET) build $(DROPIN)/clients/dotnet/PasClient.slnx

test:
	$(MVN) test
	$(MAKE) dotnet-test

dotnet-test:
	$(DOTNET) test $(DROPIN)/clients/dotnet/PasClient.slnx

# Runs the same JUnit gate the CI "parity" job runs, with the skip lifted.
parity:
	$(MVN) -pl parity test -DskipParityTests=false -Dsurefire.failIfNoSpecifiedTests=false || true
	@$(MAKE) --no-print-directory parity-report

parity-report:
	@java -cp "$(DROPIN)/parity/target/pas-parity-1.0.0.jar:$(DROPIN)/legacy/target/pas-legacy-1.0.0.jar:$(DROPIN)/mirror/target/pas-mirror-1.0.0.jar:$(DROPIN)/contract/target/pas-contract-1.0.0.jar:$$(cat $(DROPIN)/target/mirror-classpath.txt)" \
		com.acme.dropin.parity.ParityMain

proof:
	bash $(DROPIN)/scripts/facade-against-both-backends.sh

demo:
	@echo "mainframe path on :8081, mirror on :8082 - ctrl-c to stop"
	java -jar $(DROPIN)/switch/target/pas-switch-1.0.0.jar --pas.backend=mainframe --server.port=8081 & \
	java -jar $(DROPIN)/switch/target/pas-switch-1.0.0.jar --pas.backend=mirror --server.port=8082 \
		--pas.mirror.url='jdbc:h2:mem:pasdemo;DB_CLOSE_DELAY=-1;MODE=DB2'; \
	wait

clean:
	$(MVN) clean
	$(DOTNET) clean $(DROPIN)/clients/dotnet/PasClient.slnx
