#!/bin/bash

# shellcheck disable=SC2048
# shellcheck disable=SC2086
./gradlew fatJar -q && clear && java -jar shell-cli/build/libs/shell-cli-1.0-SNAPSHOT.jar $*
echo
read -n 1 -s -r -p "Press any key to continue..."
clear
