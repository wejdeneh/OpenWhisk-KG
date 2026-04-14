# Migration and Performance Analysis of QSE Serverless Architecture

 This project details the migration of the **Quality Shapes Extraction (QSE)** algorithm from a vendor-locked Azure Functions environment to a portable, open-source architecture using **Apache OpenWhisk** and **MinIO**.  The QSE algorithm extracts meaningful SHACL shapes from large RDF knowledge graphs using support and confidence metrics to filter out spurious constraints.

---

## System Overview

 The system processes RDF datasets through a four-phase workflow to generate quality validation mechanisms for knowledge graphs:

1.   **Entity Extraction:** Identifies entities and builds an Entity Type Dictionary (ETD).
2.   **Entity Constraints Extraction:** Analyzes property usage patterns.
3.   **Support and Confidence Computation:** Calculates metrics to filter constraints.
4.   **Shape Generation:** Produces SHACL-compliant shapes based on configurable thresholds.

### Architectural Migration
 The following table outlines the transition from Azure-specific services to an open-source stack:

| Component | Azure Functions (Original) | Apache OpenWhisk (Migrated) |
| :--- | :--- | :--- |
| **Orchestration** |  Durable Functions Orchestrator |  Action Sequences and Compositions |
| **Compute** |  Activity Functions  |  Individual Actions |
| **Storage** |  Azure Blob Storage |  MinIO Object Storage (S3-compatible)|
| **Caching** |  Azure Redis Cache |  Self-hosted Redis |
| **API Entry** |  HTTP Triggers |  Web Actions |

---

## Key Performance Improvements

 Before migration, the original implementation underwent systematic debugging, resulting in a **5x overall performance improvement**:

*  **StringEncoder Optimization:** Removed over 10 million excessive debug logging calls, reducing processing time for 1,000 records from over 10 minutes to under 2 minutes.
*  **Dependency Management:** Downgraded to stable library versions (RDF4J 4.3.8 and Apache Jena 4.10.0) to eliminate parsing bugs and memory leaks.
*  **Redis Connectivity:** Implemented proper connection pooling, health monitoring, and retry logic to prevent memory leaks and connection exhaustion.
*  **Null Safety:** Added robust error handling and batch processing validation to ensure system reliability with malformed RDF data.

---

## Performance Analysis Summary

 A comparative analysis using LUBM datasets revealed platform-specific strengths[cite: 333, 347]:

*  **OpenWhisk Strengths:** Superior at **Entity Extraction**, performing **5.7x to 10.8x faster** than Azure[cite: 358].  It provides better CPU utilization for compute-intensive tasks.
*  **Azure Strengths:** Significantly more efficient at **Data Merging Operations**, outperforming OpenWhisk by **10-15x**[cite: 366].  It also maintains an advantage in shape generation speed (2.2x to 3.2x faster).

---


