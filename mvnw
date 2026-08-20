#!/bin/sh
# Maven Wrapper script
set -e

MAVEN_PROJECTBASEDIR="$(cd "$(dirname "$0")" && pwd)"
MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    echo "Downloading Maven Wrapper..."
    if command -v curl > /dev/null 2>&1; then
        curl -sL -o "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
    elif command -v wget > /dev/null 2>&1; then
        wget -q -O "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
    else
        echo "Error: Neither curl nor wget found." >&2
        exit 1
    fi
fi

exec java "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
    -classpath "$MAVEN_WRAPPER_JAR" \
    org.apache.maven.wrapper.MavenWrapperMain "$@"
