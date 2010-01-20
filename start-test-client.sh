#!/bin/bash
java -classpath resources:lib/log4j-1.2.14.jar:target/classes -Dlog4j.configuration=file:///Users/elias/src/java/flash-policy-server-1.1-src/resources/log4j-client.xml test.policyserver.Client
