# NeuroRAG App Performance Testing Plan

## Overview
This plan outlines the steps to use the Resume.csv dataset for testing the NeuroRAG app's functionality and calculating performance metrics. The app includes a Java Spring Boot backend for resume parsing and skill extraction, and Python scripts for NER and evaluation.

## Current Codebase Analysis
- **Backend**: Java Spring Boot with resume upload endpoint `/api/resume/upload` requiring `userId`
- **Python Scripts**:
  - `app.py`: FastAPI for local NER using BERT model
  - `gemini_resume_pdf.py`: PDF parsing with Gemini API
  - `resume_evaluation_test.py`: Existing test script for resume parsing accuracy
- **Dataset**: `Resume.csv/Resume.csv` with columns: ID, Resume_str, Resume_html, Category (HR resumes)

## Identified Issues
1. API mismatch: Test script uses `sessionToken` but backend expects `userId`
2. Limited performance metrics: Only accuracy (precision/recall/f1) measured
3. No timing, throughput, or resource usage measurements
4. Hardcoded common skills for accuracy calculation

## Steps to Implement Performance Testing

### 1. Fix API Compatibility
- Modify `resume_evaluation_test.py` to use correct API parameters
- Update user creation and session handling to match backend expectations
- Ensure proper authentication flow

### 2. Enhance Performance Metrics Collection
- Add response time measurement for each API call
- Calculate throughput (requests per second)
- Monitor memory and CPU usage during testing
- Track parsing success rate and error rates

### 3. Improve Accuracy Evaluation
- Create ground truth dataset with expected skills for resumes
- Implement better skill matching algorithms
- Add category-specific accuracy metrics (e.g., HR skills)

### 4. Design Comprehensive Testing Framework
- Create configurable test parameters (sample size, concurrent users, etc.)
- Implement load testing capabilities
- Add stress testing for high-volume scenarios

### 5. Create Performance Benchmarking Scripts
- Develop `performance_benchmark.py` for automated testing
- Include visualization scripts for results (charts, graphs)
- Generate detailed reports with statistical analysis

### 6. Test with Resume.csv Dataset
- Run tests with varying sample sizes from the dataset
- Compare performance across different resume categories
- Validate results against expected benchmarks

### 7. Documentation and Usage
- Provide clear instructions for running tests
- Document performance thresholds and interpretation
- Include troubleshooting guide for common issues

## Expected Performance Metrics
- **Accuracy**: Precision, Recall, F1-Score for skill extraction
- **Speed**: Average response time per resume
- **Throughput**: Resumes processed per minute/hour
- **Reliability**: Success rate, error rates
- **Resource Usage**: Memory consumption, CPU utilization

## Implementation Priority
1. Fix API compatibility issues
2. Add basic timing measurements
3. Enhance accuracy calculations
4. Implement comprehensive metrics collection
5. Create visualization and reporting
6. Full testing and documentation

## Tools and Dependencies
- Python: requests, pandas, time, psutil (for system metrics)
- Java: Spring Boot backend running
- Dataset: Resume.csv with resume texts