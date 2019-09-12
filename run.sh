#!/bin/bash

clear

MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=256M -XX:ReservedCodeCacheSize=64m -Xss2048k"

java -cp target/release-tools-1.0-SNAPSHOT-jar-with-dependencies.jar org.apache.openejb.tools.release.Main $*
