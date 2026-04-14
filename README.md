# Migration and Performance Analysis of QSE Serverless Architecture

 This project details the migration of the **Quality Shapes Extraction (QSE)** algorithm from a vendor-locked Azure Functions environment to a portable, open-source architecture using **Apache OpenWhisk** and **MinIO**[cite: 1, 16].  The QSE algorithm extracts meaningful SHACL shapes from large RDF knowledge graphs using support and confidence metrics to filter out spurious constraints[cite: 14, 15, 23].

---

## System Overview

 The system processes RDF datasets through a four-phase workflow to generate quality validation mechanisms for knowledge graphs[cite: 33]:

1.   **Entity Extraction:** Identifies entities and builds an Entity Type Dictionary (ETD)[cite: 34].
2.   **Entity Constraints Extraction:** Analyzes property usage patterns[cite: 35].
3.   **Support and Confidence Computation:** Calculates metrics to filter constraints[cite: 36].
4.   **Shape Generation:** Produces SHACL-compliant shapes based on configurable thresholds[cite: 38].

### Architectural Migration
 The following table outlines the transition from Azure-specific services to an open-source stack[cite: 43, 51]:

| Component | Azure Functions (Original) | Apache OpenWhisk (Migrated) |
| :--- | :--- | :--- |
| **Orchestration** |  Durable Functions Orchestrator [cite: 46] |  Action Sequences and Compositions [cite: 53] |
| **Compute** |  Activity Functions [cite: 47] |  Individual Actions [cite: 54] |
| **Storage** |  Azure Blob Storage [cite: 48] |  MinIO Object Storage (S3-compatible) [cite: 56] |
| **Caching** |  Azure Redis Cache [cite: 49] |  Self-hosted Redis [cite: 57] |
| **API Entry** |  HTTP Triggers [cite: 50] |  Web Actions [cite: 55] |

---

## Key Performance Improvements

 Before migration, the original implementation underwent systematic debugging, resulting in a **5x overall performance improvement**[cite: 17, 276]:

*  **StringEncoder Optimization:** Removed over 10 million excessive debug logging calls, reducing processing time for 1,000 records from over 10 minutes to under 2 minutes[cite: 78, 129].
*  **Dependency Management:** Downgraded to stable library versions (RDF4J 4.3.8 and Apache Jena 4.10.0) to eliminate parsing bugs and memory leaks[cite: 66, 68].
*  **Redis Connectivity:** Implemented proper connection pooling, health monitoring, and retry logic to prevent memory leaks and connection exhaustion[cite: 131, 136, 193].
*  **Null Safety:** Added robust error handling and batch processing validation to ensure system reliability with malformed RDF data[cite: 195, 253].

---

## Performance Analysis Summary

 A comparative analysis using LUBM datasets revealed platform-specific strengths[cite: 333, 347]:

*  **OpenWhisk Strengths:** Superior at **Entity Extraction**, performing **5.7x to 10.8x faster** than Azure[cite: 358].  It provides better CPU utilization for compute-intensive tasks[cite: 370, 382].
*  **Azure Strengths:** Significantly more efficient at **Data Merging Operations**, outperforming OpenWhisk by **10-15x**[cite: 366].  It also maintains an advantage in shape generation speed (2.2x to 3.2x faster)[cite: 362].

---

## Project Contributors
* **Dr.  Raja Appuswamy** - Professor, EURECOM [cite: 3, 4]
* **Baya Dhouib** - PhD Student (Supervisor), EURECOM [cite: 5, 6]
*  **Wejden Haj Mefteh** - Post Master's Student, EURECOM [cite: 9]

 **Date:** July 3, 2025 [cite: 8]
