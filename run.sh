#!/bin/bash

# Save terminal state
#tput smcup

# Build and run
./gradlew fatJar -q && clear && java -jar build/libs/ballerina-shell-0.0.1-SNAPSHOT.jar "$*"

# Pause till user input
echo
read -n 1 -s -r -p "Press any key to continue..."
clear

# Restore terminal state
#tput rmcup
