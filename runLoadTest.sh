#!/usr/bin/env sh

echo "👉 JMeter load test 10 threads (users) x 100 loops => 1000 samples"

rm load-test-web-report
rm load-test-results.csv

jmeter -n -t load-test.jmx -l load-test-results.csv -j load-test-run.log -e -o ./load-test-web-report
