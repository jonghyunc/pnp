#!/bin/bash -e

CLASSPATH="target/scala-2.11/pnp-assembly-0.1.3.jar"
echo $CLASSPATH 
java -Djava.library.path=lib -classpath $CLASSPATH $@
