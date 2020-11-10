#!/bin/bash

./gradlew fatJar -q || exit
java -jar build/libs/ballerina-shell-0.0.1-SNAPSHOT.jar "$*"
