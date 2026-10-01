# FranceStats

FranceStats est une application Java en ligne de commande qui aide à comparer
les prix des carburants à partir d'un flux public de stations-service. Elle
charge les stations, conserve un cache local et permet de consulter des prix
par carburant, par département ou autour d'une position.

## Ce que fait l'application

Au démarrage, l'application cherche le fichier `prix_carburants_cache.xml`.
S'il est absent, trop ancien (plus de six heures) ou invalide, elle télécharge
l'archive XML depuis `https://donnees.roulez-eco.fr/opendata/instantane`, extrait
le XML et le conserve localement. Les stations sans coordonnées valides ou sans
prix sont ignorées.

Le menu console permet ensuite de :

- rechercher les stations les moins chères dans un rayon donné ;
- calculer une moyenne, un minimum et un maximum pour un carburant ;
- filtrer les stations par département ;
- sauvegarder une position et recalculer les distances ;
- enregistrer les prix du jour et afficher l'historique disponible ;
- consulter la météo et des informations de transport via des services externes ;
- supprimer le cache pour forcer une actualisation.

Les prix sont ceux présents dans le flux au moment du téléchargement. Il ne
s'agit pas d'un suivi continu en temps réel.

## Organisation du code

`XmlStationRepository` s'occupe du téléchargement, du cache et du parsing XML.
`JsonPriceHistoryRepository` lit et écrit `historique_prix.json`, tandis que
`PropertiesLocationConfigRepository` conserve la position dans
`station_config.properties`.

Les services portent les opérations métier : `StationService` filtre et trie
les stations, `LocationService` calcule les distances et
`HistoriquePrixService` gère l'historique. `ApplicationContext` assemble ces
composants avant de les transmettre à `PrixEssenceApp` et à l'interface
console `UserInterface`.

## Installation et lancement

Le projet utilise Java 17 et Maven. Vérifier les prérequis avec :

```bash
java -version
mvn -version
```

Lancer les tests :

```bash
mvn test
```

Compiler le projet et créer le JAR exécutable configuré dans `pom.xml` :

```bash
mvn clean package
```

Le nom d'artefact Maven est `Stat` et sa version actuelle est `1.0-SNAPSHOT`.
Après le package, lancer l'application avec :

```bash
java -jar target/Stat-1.0-SNAPSHOT.jar
```

Le premier lancement nécessite une connexion internet si aucun cache XML valide
n'est déjà présent. Le programme écrit ses fichiers de données dans le
répertoire courant depuis lequel il est lancé.

## Exemple avec les données présentes

Le cache actuellement suivi dans ce dépôt contient 9 946 stations XML et les
carburants E10, E85, Gazole, GPLc, SP95 et SP98. Pour le Gazole, il contient
9 646 relevés ; la moyenne calculée à partir de ces relevés est de 1,689 €/L,
avec un minimum de 1,502 €/L à Pont-Audemer (27500).

L'historique JSON contient 33 733 entrées, mais elles correspondent actuellement
à une seule date, le 29 juillet 2025. Ces chiffres décrivent les fichiers
présents dans le dépôt et peuvent changer après un téléchargement ou une
sauvegarde.

## Limites connues et suites possibles

- Le parsing XML utilise un document DOM complet, ce qui peut consommer beaucoup
        de mémoire avec un gros flux.
- Les distances sont une approximation basée sur la distance à vol d'oiseau
        multipliée par 1,3. Le réglage nommé « par la route » ne contacte pas encore
        de service de routage.
- La météo utilise toujours les coordonnées de Paris ; le nom de ville saisi
        est seulement affiché.
- Les services de transport dépendent d'API externes et peuvent afficher des
        données de secours ou simulées.
- Les tests couvrent surtout `ApplicationContext`, `StationService` et
        `LocationService`. Il n'y a pas de test d'intégration du téléchargement, du
        parsing réel ou de l'interface console.
- Une prochaine étape réaliste serait de rendre le téléchargement et le
        routage plus configurables, puis d'ajouter des tests sur les données externes.

## Fichiers locaux et Git

`prix_carburants_cache.xml` et `historique_prix.json` sont des fichiers générés
localement et volumineux. `station_config.properties` contient une position
sauvegardée par l'utilisateur. Ils sont conservés ici pour permettre un
exemple reproductible, mais un usage quotidien gagnerait à les exclure de Git
et à documenter une procédure de génération du cache.
