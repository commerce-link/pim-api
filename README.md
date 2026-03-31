# PIM API

This library defines the public API contracts for the PIM (Product Information Management) microservice. It provides shared data models and interfaces used by both the PIM service and the main CommerceLink application.

The core `PimCatalog` interface provides methods for looking up PIM entries by identifiers (GTIN, MPN, PIM ID). The `PimEntry` record represents the canonical product information record with identifiers, brand, name, category, and review status.

## Data Models

- `PimEntry` — product information record (identifiers, brand, name, category, subcategory, approval status)
- `PimIdentifier` — product identifier with type (GTIN or MPN)
- `PimEntryRequest` — request to add a product to the PIM queue
- `PIMEntryAddedEvent` / `PIMEntryDeletedEvent` — events for PIM index changes

## Usage

Add as a Maven dependency:

```xml
<dependency>
    <groupId>pl.commercelink</groupId>
    <artifactId>pim-api</artifactId>
    <version>0.1.0</version>
</dependency>
```
