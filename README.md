# VéliTracker — Dashboard en temps réel des stations Vélib'

VéliTracker est une solution complète de traitement et visualisation de données Vélib en temps réel. Le projet combine Kafka, Spark Streaming et Streamlit pour offrir une interface claire, interactive et utile aux usagers du service Vélib à Paris.

---

## Structure du projet

```
velib/
├── src/
│   └── main/
│       └── scala/
│           ├── VelibProducer.scala
│           └── VelibConsumer.scala
├── dashboard.py
├── station_name_mapping.py
├── station_name_mapping.json
├── requirements.txt
├── build.sbt
├── .gitignore
└── README.md
```

- `VelibProducer.scala` : collecte des données temps réel depuis l'API Vélib et envoie dans Kafka (Spark Core, batch toutes les 10s).
- `VelibConsumer.scala` : traitement des données avec Spark Streaming et stockage en fichiers JSON.
- `dashboard.py` : application Streamlit pour la visualisation.
- `station_name_mapping.py` : mapping Python des noms de stations (à convertir en JSON).
- `station_name_mapping.json` : mapping des noms de stations au format JSON (utilisé par le parser Scala).
- `requirements.txt` : dépendances Python.
- `build.sbt` : configuration du projet Scala/Spark.

---

## Installation et exécution

### Étape 0 — Se rendre dans le dossier du projet

```powershell
cd C:\Users\Zeineb Rekik\velib
```

### Étape 1 — Générer le mapping JSON des stations

```powershell
python station_name_mapping.py
```
Cela crée le fichier `station_name_mapping.json` nécessaire au parser Scala.

### Étape 2 — Lancer Zookeeper (Terminal 1)

```powershell
cd kafka_2.13-3.9.1
.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties
```

### Étape 3 — Lancer le serveur Kafka (Terminal 2)

```powershell
cd kafka_2.13-3.9.1
.\bin\windows\kafka-server-start.bat .\config\server.properties
.\bin\windows\kafka-server-start.bat .\config\server.properties

```

### Étape 4 — Créer un topic Kafka (Terminal 3)

```powershell
cd kafka_2.13-3.9.1
.\bin\windows\kafka-topics.bat --create --topic velib-data --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

### Étape 5 — Lancer le Producer Scala (Terminal 4)

```powershell
cd C:\Users\Zeineb Rekik\velib
sbt
> runMain VelibProducer
```
Le producer interroge l'API toutes les 10 secondes et envoie les données dans Kafka.

### Étape 6 — Lancer le Consumer Spark Streaming (Terminal 5)

```powershell
cd C:\Users\Zeineb Rekik\velib
sbt
> runMain VelibConsumer
```
Les données sont enregistrées automatiquement dans `velib_output/`.

### Étape 7 — Activer l’environnement Python (Terminal 6)

```powershell
cd C:\Users\Zeineb Rekik\velib
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

### Étape 8 — Installer les dépendances Python

```powershell
pip install -r requirements.txt
```

### Étape 9 — Lancer le dashboard Streamlit

```powershell
streamlit run dashboard.py
```

---

## Fonctionnalités du dashboard

- Carte interactive avec recherche d’adresse
- Station la plus proche depuis l’adresse saisie
- Lien Google Maps vers l’itinéraire
- KPIs dynamiques (vélos électriques, mécaniques, bornes, etc.)
- Graphique donut (répartition électrique/mécanique)

---

## Données utilisées

API officielle en temps réel :

https://data.opendatasoft.com/api/explore/v2.1/catalog/datasets/velib-disponibilite-en-temps-reel@parisdata/records?limit=100

---

## Remarques

- Vérifiez que le fichier `station_name_mapping.json` est bien généré avant de lancer le pipeline Scala.
- Les chemins sont donnés pour Windows, adaptez-les si besoin pour votre environnement.
- Le producer Spark Core interroge l’API toutes les 10 secondes (modifiable dans le code).

