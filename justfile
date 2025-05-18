# Justfile used to build and deploy Kestra locally.
# By default Kestra will be installed under: $HOME/.kestra/current. Set $KESTRA_HOME to override default.
#
# Usage:
# just install
# just start-standalone-local
#
# NOTE: This file is intended for development purposes only.

set shell := ["bash", "-c"]

# Default variables
kestra_basedir := env_var_or_default("KESTRA_HOME", env_var("HOME") + "/.kestra/current")
kestra_worker_thread := env_var_or_default("KESTRA_WORKER_THREAD", "4")
version := `./gradlew properties -q | awk '/^version:/ {print $2}'`
java_home := "/Users/louloulin/Library/Java/JavaVirtualMachines/graalvm-ce-23.0.2/Contents/Home"

# Default recipe (runs when you just type 'just')
default: clean build-exec install

# Display the current version
version:
    echo "{{version}}"

# Clean the project
clean:
    ./gradlew clean

# Build the project
build: clean
    ./gradlew build

# Build the project skipping tests
build-skip-tests: clean
    ./gradlew build -x test -x integrationTest -x testCodeCoverageReport --refresh-dependencies

# Run tests
test: clean
    ./gradlew test

# Build executable jar
build-exec:
    ./gradlew -q executableJar --no-daemon --priority=normal

# Install Kestra
install: build-exec
    echo "Installing Kestra: {{kestra_basedir}}"
    mkdir -p {{kestra_basedir}}/bin {{kestra_basedir}}/plugins {{kestra_basedir}}/flows {{kestra_basedir}}/logs
    cp build/executable/* {{kestra_basedir}}/bin/kestra && chmod +x {{kestra_basedir}}/bin
    echo "Kestra installed successfully 🚀"

# Kill Kestra running process
kill:
    bash -c 'PID=$(ps aux | grep java | grep "kestra" | grep -v "grep" | awk "{print \$2}"); if [ ! -z "$PID" ]; then echo "Killing Kestra process (pid=$PID)."; kill $PID; else echo "No Kestra process to kill."; fi'

# Verify whether Kestra is running
health:
    bash -c 'PID=$(ps aux | grep java | grep "kestra" | grep -v "grep" | awk "{print \$2}"); if [ ! -z "$PID" ]; then echo -e "\n⏳ Waiting for Kestra server..."; KESTRA_URL=http://localhost:8080; while [ $(curl -s -L -o /dev/null -w %{http_code} $KESTRA_URL) != 200 ]; do echo -e $(date) "\tKestra server HTTP state: " $(curl -k -L -s -o /dev/null -w %{http_code} $KESTRA_URL) " (waiting for 200)"; sleep 2; done; echo "Kestra is running (pid=$PID): $KESTRA_URL 🚀"; fi'

# Start Kestra in standalone mode with In-Memory backend
start-standalone-local: kill
    rm -f "{{kestra_basedir}}/logs/*.log"
    {{java_home}}/bin/java -jar {{kestra_basedir}}/bin/kestra server local --worker-thread {{kestra_worker_thread}} --plugins "{{kestra_basedir}}/plugins" --flow-path "{{kestra_basedir}}/flows" 2>{{kestra_basedir}}/logs/err.log 1>{{kestra_basedir}}/logs/out.log &
    just health
