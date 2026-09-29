# 📊 FranceStats — Fuel Open Data Analytics & Telemetry (Java 17)

Application Java 17 d'ingestion, d'analyse statistique et de suivi en temps réel des prix des carburants sur l'ensemble du territoire français, exploitant les flux officiels **Open Data** du gouvernement français (`data.gouv.fr`).

---

## 📋 Présentation du projet

**FranceStats** agrège les données publiques de plus de 10 000 stations-service en France afin d'offrir une vision analytique des tendances de prix, des variations régionales et des opportunités d'approvisionnement économique.

```
+------------------------------------+
|  Flux Open Data Gouvernemental     |  (Flux XML instantané des stations)
+-----------------+------------------+
                  |
                  v
+-----------------+------------------+
|      XmlStationRepository          |  (Parsing & normalisation XML)
+-----------------+------------------+
                  |
         +--------+--------+
         |                 |
         v                 v
+--------+-------+  +------+---------+
| Cache XML Local|  | Historique JSON|  (prix_carburants_cache.xml & historique_prix.json)
+----------------+  +----------------+
                           |
                           v
+--------------------------+---------+
|       StationService & Analytics   |  (Moyennes nationales, top stations, écarts)
+--------------------------+---------+
                           |
                           v
+--------------------------+---------+
|  UserInterface / Console Analytics |
+------------------------------------+
```

---

## ✨ Fonctionnalités clés

- **⚡ Ingestion & Parsing XML haute performance :**
  - Parsing streaming du catalogue complet des stations de France (`prix_carburants_cache.xml`).
  - Extraction des coordonnées géographiques, des types de carburants (Gazole, SP95, SP98, E10, E85, GPLc) et des dates de mise à jour.

- **💾 Caching & Historisation :**
  - Système de cache intelligent sur disque pour limiter les appels réseau redondants.
  - Sauvegarde structurée de l'historique des prix au format JSON (`historique_prix.json`).

- **📈 Moteur Statistique National :**
  - Calcul des métriques nationales et départementales (`StatistiquesNationales`).
  - Détection instantanée des stations les plus compétitives autour d'une localisation géographique.

- **🧩 Architecture Modulaire (Clean Code) :**
  - **Conteneur d'injection / IoC :** `ApplicationContext` gérant le cycle de vie des singletons.
  - **Pattern Repository :** Interfaces découplées (`StationRepository`, `PriceHistoryRepository`, `LocationConfigRepository`).
  - **Services contextuels :** Intégration de `LocationService`, `WeatherService` et `TransportService`.
  - **Qualité & Tests :** Suite de tests unitaires JUnit (`ApplicationContextTest`, `StationServiceTest`, etc.).

---

## 🛠️ Stack Technique

- **Langage :** Java 17 (LTS)
- **Gestionnaire de dépendances :** Apache Maven (`pom.xml`)
- **Formats de données :** XML (flux gouvernemental), JSON (stockage historique), Properties (configurations locales)
- **Tests :** JUnit 5

---

## 🚀 Installation & Exécution

### Prérequis
- Java JDK 17 ou supérieur (`java -version`).
- Apache Maven (`mvn -version`).

### Compilation & Tests
```bash
# Lancer les tests unitaires
mvn test

# Compiler le projet et packager le JAR
mvn clean package
```

### Configuration & Lancement
Les paramètres locaux et les préférences de localisation sont configurables dans `station_config.properties`.
```bash
java -jar target/FranceStats-1.0-SNAPSHOT.jar
```