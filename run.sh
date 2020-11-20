#!/bin/bash

# Save terminal state
#tput smcup

# Build and run
# shellcheck disable=SC2048
# shellcheck disable=SC2086
./gradlew fatJar -q && clear && java -jar shell-cli/build/libs/shell-cli-1.0-SNAPSHOT.jar $*

# Pause till user input
echo
read -n 1 -s -r -p "Press any key to continue..."
rm ballerina-internal.log*

clear

# Restore terminal state
#tput rmcup
