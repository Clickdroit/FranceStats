# 📊 FranceStats — Fuel Open Data Analytics

> Java 17 application that ingests French government Open Data fuel-price feeds, caches the data locally and exposes reusable services for analysis.

## Why this project?

FranceStats is a practical data-engineering project built around a real public dataset. The interesting part is not only the statistics: the application has to ingest a large XML feed, normalise it, persist useful history and keep the domain logic independent from the data source.

## Architecture

```text
Government Open Data
        │
        ▼
XmlStationRepository
        │
        ├──────────► Local XML cache
        │
        └──────────► Price history (JSON)
                         │
                         ▼
                StationService
                         │
                         ▼
                   Analytics
                         │
                         ▼
                 Console UI
```

The code separates repositories, services and application wiring through `ApplicationContext`.

## Features

- XML streaming/parsing of station data
- Fuel price extraction and normalisation
- Local disk caching
- Price history persistence
- National and department-level statistics
- Search for competitive stations around a location
- JUnit 5 test suite
- Repository/service separation

## Technical stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| Build | Maven |
| Data | XML + JSON |
| Tests | JUnit 5 |
| Source | French government Open Data |

## Run locally

### Prerequisites

- JDK 17+
- Maven

### Test

```bash
mvn test
```

### Package

```bash
mvn clean package
```

### Run

```bash
java -jar target/FranceStats-1.0-SNAPSHOT.jar
```

Local behaviour and location preferences can be configured through `station_config.properties`.

## Engineering notes

The project deliberately keeps data access behind interfaces such as `StationRepository` and `PriceHistoryRepository`. This makes the application easier to test and leaves room for alternative data sources without rewriting the service layer.

## Limitations / next steps

- Improve automated coverage around edge cases in external data.
- Add richer visual reporting.
- Make ingestion scheduling configurable.
- Add a reproducible CI build.

## License

See the repository for the current project licensing information.
