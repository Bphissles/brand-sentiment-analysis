#!/bin/bash

# Script to run Grails tests with correct Java version
# Ensures Java 17 is used for Gradle

export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

echo "Using Java version:"
$JAVA_HOME/bin/java -version

echo ""
echo "Running Gradle tests..."
./gradlew test "$@"
