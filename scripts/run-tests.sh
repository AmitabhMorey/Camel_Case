#!/bin/bash

# Comprehensive test runner for Secure QR Voting System

echo "🧪 Running Comprehensive Test Suite"
echo "=================================="

# Function to run tests and check results
run_test_suite() {
    local test_name="$1"
    local test_command="$2"
    
    echo "📋 Running $test_name..."
    eval "$test_command"
    
    if [ $? -eq 0 ]; then
        echo "✅ $test_name passed"
        return 0
    else
        echo "❌ $test_name failed"
        return 1
    fi
}

# Initialize counters
total_suites=0
passed_suites=0

# Unit Tests
total_suites=$((total_suites + 1))
if run_test_suite "Unit Tests" "mvn test -Dtest='*Test' -Ptest"; then
    passed_suites=$((passed_suites + 1))
fi

echo ""

# Integration Tests
total_suites=$((total_suites + 1))
if run_test_suite "Integration Tests" "mvn test -Dtest='*IntegrationTest' -Ptest"; then
    passed_suites=$((passed_suites + 1))
fi

echo ""

# Security Tests
total_suites=$((total_suites + 1))
if run_test_suite "Security Tests" "mvn test -Dtest='*SecurityTest' -Ptest"; then
    passed_suites=$((passed_suites + 1))
fi

echo ""

# Performance Tests
total_suites=$((total_suites + 1))
if run_test_suite "Performance Tests" "mvn test -Dtest='*PerformanceTest' -Ptest"; then
    passed_suites=$((passed_suites + 1))
fi

echo ""

# Generate Coverage Report
echo "📊 Generating Coverage Report..."
mvn jacoco:report -Ptest

if [ $? -eq 0 ]; then
    echo "✅ Coverage report generated at: target/site/jacoco/index.html"
else
    echo "⚠️  Failed to generate coverage report"
fi

echo ""
echo "=================================="
echo "📈 Test Results Summary"
echo "=================================="
echo "Total Test Suites: $total_suites"
echo "Passed: $passed_suites"
echo "Failed: $((total_suites - passed_suites))"

if [ $passed_suites -eq $total_suites ]; then
    echo "🎉 All test suites passed!"
    echo "📊 View coverage report: open target/site/jacoco/index.html"
    exit 0
else
    echo "⚠️  Some test suites failed. Please review the output above."
    exit 1
fi