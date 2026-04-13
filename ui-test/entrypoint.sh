#!/bin/bash

java -jar -Dmodules="$MODULES" -Denv.user="$ENV_USER" -Denv.endpoint="$ENV_ENDPOINT" -Denv.testLevel="$ENV_TESTLEVEL" uitest-injiverify-*.jar
