#!/usr/bin/env sh

TEST_FOLDER=jmeter-load-test
OUTPUT_FOLDER="$TEST_FOLDER"/load-test-results

echo "👉 JMeter load test 10 threads (users) x 100 loops => 1000 samples"

rm -r "$OUTPUT_FOLDER"

jmeter -n -t "$TEST_FOLDER"/load-test.jmx -l "$OUTPUT_FOLDER"/load-test-results.csv -j "$OUTPUT_FOLDER"/load-test-run.log -e -o "$OUTPUT_FOLDER"/load-test-web-report
