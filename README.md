# pim-api

Client contract for the PIM (Product Information Management) system. Defines the interfaces and data models that PIM implementations must provide.

## PimCatalog

Main interface for querying and interacting with a PIM service.

| Method                     | Description                                               |
|----------------------------|-----------------------------------------------------------|
| `findAll()`                | Returns all PIM entries                                   |
| `findByPimId(id)`          | Lookup by PIM ID                                          |
| `findByGtin(gtin)`         | Lookup by GTIN/EAN                                        |
| `findByMpn(mpn)`           | Lookup by manufacturer part number                        |
| `findByGtinOrMpn(gtin, mpn)` | Lookup by GTIN first, then MPN                         |
| `findByPimIdOrGtinsOrMpns(...)` | Cascade lookup: PIM ID, then GTINs, then MPNs (default) |
| `submit(request)`          | Submit a product to be fetched by the PIM service         |
| `refresh()`                | Refresh internal cache from the PIM service               |
| `onEntryAdded(listener)`   | Register a listener for new PIM entries                   |
| `onEntryDeleted(listener)` | Register a listener for deleted PIM entries               |
| `dispatch(event)`          | Route an incoming event to registered listeners (default no-op) |

## PimCatalogDescriptor

Extends `ProviderDescriptor<PimCatalog>` from [provider-api](../provider-api). Implementations are discovered via `ServiceLoader`.

See [provider-api README](../provider-api/README.md) for `name()`, `displayName()`, `configurationFields()`, `create()`, `metadata()`, and `bindings()`.

## Data Models

| Class                | Description                                                        |
|----------------------|--------------------------------------------------------------------|
| `PimEntry`           | Product record: `pimId`, identifiers, brand, name, category, approved |
| `PimIdentifier`      | Identifier value + type                                            |
| `PimIdentifierType`  | `GTIN` or `MPN`                                                   |
| `PimEntryRequest`    | Request to fetch a product: `ean`, `mfn`, `brand`, `priority`     |
| `PIMEntryAddedEvent` | Event: entry added with `pimId`, `eans`, `mfnCodes`               |
| `PIMEntryDeletedEvent` | Event: entry deleted with `pimId`                                |

## Usage

```xml
<dependency>
    <groupId>pl.commercelink</groupId>
    <artifactId>pim-api</artifactId>
    <version>0.1.0</version>
</dependency>
```

## License

MIT
