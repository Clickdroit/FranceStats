# FranceStats

Application Java 17 d'analyse et de suivi statistique des prix des carburants en France (Open Data).

## Développement local

Le projet Maven cible Java 17 (`pom.xml`). Depuis la racine :

```powershell
java -version
mvn test
mvn package
```

Les résultats de compilation sont dans `target/`. Les paramètres de station
sont dans `station_config.properties`. `prix_carburants_cache.xml` est un cache
des prix, tandis que `historique_prix.json` conserve l'historique : ne pas les
confondre lors d'une sauvegarde ou d'un nettoyage.
