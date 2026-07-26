#!/bin/sh
# Gradle wrapper for Panoplia Android
DEFAULT_JVM_OPTS=""
APP_HOME=$(cd "$(dirname "$0")" && pwd)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
exec "$JAVA_HOME/bin/java" $DEFAULT_JVM_OPTS -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
