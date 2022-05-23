#!/bin/bash

rm -f test.csv

java -jar ../performance_tester/build/libs/performance_tester-all.jar

java -jar ../performance_tester/build/libs/performance_tester-all.jar &
java -jar ../performance_tester/build/libs/performance_tester-all.jar &
wait < <(jobs -p)

java -jar ../performance_tester/build/libs/performance_tester-all.jar &
java -jar ../performance_tester/build/libs/performance_tester-all.jar &
java -jar ../performance_tester/build/libs/performance_tester-all.jar &
java -jar ../performance_tester/build/libs/performance_tester-all.jar &
java -jar ../performance_tester/build/libs/performance_tester-all.jar &
java -jar ../performance_tester/build/libs/performance_tester-all.jar &

wait < <(jobs -p)

java -jar ../performance_tester/build/libs/performance_tester-all.jar &
java -jar ../performance_tester/build/libs/performance_tester-all.jar &
wait < <(jobs -p)

java -jar ../performance_tester/build/libs/performance_tester-all.jar
