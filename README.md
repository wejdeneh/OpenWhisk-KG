# Migration and Performance Analysis of QSE Serverless Architecture

[cite_start]This project details the migration of the **Quality Shapes Extraction (QSE)** algorithm from a vendor-locked Azure Functions environment to a portable, open-source architecture using **Apache OpenWhisk** and **MinIO**[cite: 1, 16, 17]. [cite_start]The QSE algorithm is designed to extract meaningful SHACL shapes from large RDF knowledge graphs using support and confidence metrics to filter out spurious constraints[cite: 14, 15, 23].

---

## ## System Overview

[cite_start]The system processes RDF datasets through a four-phase workflow to generate quality validation mechanisms for knowledge graphs[cite: 16, 21, 33]:

1.  [cite_start]**Entity Extraction:** Identifies entities and builds an Entity Type Dictionary (ETD)[cite: 34].
2.  [cite_start]**Entity Constraints Extraction:** Analyzes property usage patterns[cite: 35].
3.  [cite_start]**Support and Confidence Computation:** Calculates metrics to filter constraints[cite: 36].
4.  [cite_start]**Shape Generation:** Produces SHACL-compliant shapes based on configurable thresholds[cite: 38].

### ### Architectural Migration
| Component | Azure Functions (Original) | Apache OpenWhisk (Migrated) |
| :--- | :--- | :--- |
| **Orchestration** | [cite_start]Durable Functions Orchestrator [cite: 46] | [cite_start]Action Sequences and Compositions [cite: 53] |
| **Compute** | [cite_start]Activity Functions [cite: 47] | [cite_start]Individual Actions [cite: 54] |
| **Storage** | [cite_start]Azure Blob Storage [cite: 48] | [cite_start]MinIO Object Storage (S3-compatible) [cite: 56] |
| **Caching** | [cite_start]Azure Redis Cache [cite: 49] | [cite_start]Self-hosted Redis [cite: 57] |
| **API Entry** | [cite_start]HTTP Triggers [cite: 50] | [cite_start]Web Actions [cite: 55] |

---

## ## Key Performance Improvements

[cite_start]Before migration, the original implementation underwent systematic debugging, resulting in a **5x overall performance improvement**[cite: 17, 276].

* [cite_start]**StringEncoder Optimization:** Removed excessive debug logging (10M+ console calls), reducing processing time for 1,000 records from 10+ minutes to under 2 minutes[cite: 76, 78, 129].
* [cite_start]**Dependency Management:** Downgraded to stable library versions (RDF4J 4.3.8 and Apache Jena 4.10.0) to eliminate memory leaks and parsing bugs[cite: 65, 66, 68].
* [cite_start]**Redis Connectivity:** Implemented proper connection pooling and retry logic to prevent memory leaks and connection exhaustion[cite: 131, 136, 193].
* [cite_start]**Null Safety:** Added robust error handling and batch processing validation to ensure system reliability even with malformed RDF data[cite: 195, 253].

---

## ## Performance Analysis Summary

[cite_start]A comparative analysis using LUBM datasets revealed platform-specific strengths[cite: 18, 333]:

* [cite_start]**OpenWhisk Strengths:** Superior at **Entity Extraction**, performing **5.7x to 10.8x faster** than Azure[cite: 18, 358]. [cite_start]It provides better CPU utilization for compute-intensive tasks[cite: 370, 382].
* [cite_start]**Azure Strengths:** Significantly more efficient at **Data Merging Operations**, outperforming OpenWhisk by **10-15x**[cite: 18, 366]. [cite_start]It also maintains an advantage in shape generation speed (2.2x to 3.2x faster)[cite: 362].

---

