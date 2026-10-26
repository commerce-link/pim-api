# pim-api

Client contract for the PIM (Product Information Management) system. Defines the interfaces and data models that PIM implementations must provide.

## PimCatalog

Entry point to a PIM service. It has no lookups of its own: every read first picks its scope and then works on the
returned `PimView`, the way the app's `Inventory` hands out `InventoryView`s.

| Method                     | Description                                               |
|----------------------------|-----------------------------------------------------------|
| `withGlobalData()`         | View of global entries only                               |
| `withStoreData(storeId)`   | A store's view: its own entries first, then the global ones it does not shadow; a blank `storeId` is rejected |
| `submit(request)`          | Submit a product to be fetched by the PIM service         |
| `refresh()`                | Refresh internal cache from the PIM service               |
| `onEntryAdded(listener)`   | Register a listener for new PIM entries                   |
| `onEntryDeleted(listener)` | Register a listener for deleted PIM entries               |
| `dispatch(event)`          | Route an incoming event to registered listeners (default no-op) |

## PimView

Lookups within one scope. A view is a cheap snapshot of the current index; take a fresh one per operation.

| Method                     | Description                                               |
|----------------------------|-----------------------------------------------------------|
| `findAll()`                | All entries visible in this scope                         |
| `findByPimId(id)`          | Lookup by PIM ID                                          |
| `findByGtin(gtin)`         | Lookup by GTIN/EAN                                        |
| `findByMpn(mpn)`           | Lookup by manufacturer part number                        |
| `findByGtinOrMpn(gtin, mpn)` | Lookup by GTIN first, then MPN                         |
| `findByPimIdOrGtinsOrMpns(...)` | Cascade lookup: PIM ID, then GTINs, then MPNs (default) |

`PimEntryIndex` is a ready-made immutable implementation of both scopes for adapters (`global()`, `forStore(storeId)`).

## PimCatalogDescriptor

Extends `ProviderDescriptor<PimCatalog>` from [provider-api](../provider-api). Implementations are discovered via `ServiceLoader`.

See [provider-api README](../provider-api/README.md) for `name()`, `displayName()`, `configurationFields()`, `create()`, `metadata()`, and `bindings()`.

## Data Models

| Class                | Description                                                        |
|----------------------|--------------------------------------------------------------------|
| `PimEntry`           | Product record: `pimId`, identifiers, brand, name, category, approved, `ownerStoreId` (null = global), `clonedFromPimId` (source of a store clone, else null) |
| `PimIdentifier`      | Identifier value + type                                            |
| `PimIdentifierType`  | `GTIN` or `MPN`                                                   |
| `PimEntryRequest`    | Request to fetch a product: `ean`, `mfn`, `brand`, `priority`     |
| `PIMEntryAddedEvent` | Event: entry added with `pimId`, `eans`, `mfnCodes`, `ownerStoreId`, `clonedFromPimId` (new clone only) |
| `PIMEntryDeletedEvent` | Event: entry deleted with `pimId`                                |

## Usage

```xml
<dependency>
    <groupId>pl.commercelink</groupId>
    <artifactId>pim-api</artifactId>
    <version>0.4.0</version>
</dependency>
```

## License

MIT
