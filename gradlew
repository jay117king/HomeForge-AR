#!/usr/bin/env bash
set -e
# Minimal gradlew helper: downloads Gradle and runs it if not present
GRADLE_VERSION=8.6
INSTALL_DIR="$HOME/.gradle-wrapper"
GRADLE_DIR="$INSTALL_DIR/gradle-$GRADLE_VERSION"
if [ ! -d "$GRADLE_DIR" ]; then
  echo "Gradle $GRADLE_VERSION not found, downloading..."
  mkdir -p "$INSTALL_DIR"
  wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip
  unzip -q /tmp/gradle.zip -d "$INSTALL_DIR"
fi
export PATH="$GRADLE_DIR/bin:$PATH"
exec gradle "$@"
