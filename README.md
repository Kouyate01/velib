# VéliTracker — Dashboard en temps réel des stations Vélib'

VéliTracker est une solution complète de traitement et visualisation de données Vélib en temps réel. Le projet combine Kafka, Spark Streaming et Streamlit pour offrir une interface claire, interactive et utile aux usagers du service Vélib à Paris.

---

## Structure du projet

```
velib_project/
├── src/
│   └── main/
│       └── scala/
│           ├── VelibProducer.scala
│           └── VelibConsumer.scala
├── dashboard.py
├── requirements.txt
├── build.sbt
├── .gitignore
└── README.md
```

- `VelibProducer.scala` : collecte des données temps réel depuis l'API Vélib et envoie dans Kafka.
- `VelibConsumer.scala` : traitement des données avec Spark Streaming et stockage en fichiers JSON.
- `dashboard.py` : application Streamlit pour la visualisation.
- `requirements.txt` : dépendances Python.
- `build.sbt` : configuration du projet Scala/Spark.

---

## Installation et exécution

### Étape 0 — Se rendre dans le dossier du projet

```bash
cd.../velib
```

### Étape 1 — Lancer Zookeeper (Terminal 1)

```bash
cd C:\kafka\kafka_2.13-3.9.1 cd C:\kafka\kafka_2.12-3.9.1   


```

### Étape 2 — Lancer le serveur Kafka (Terminal 2)

```bash
cd C:\kafka\kafka_2.13-3.9.1
.\bin\windows\kafka-server-start.bat .\config\server.properties
```

### Étape 3 — Créer un topic Kafka (Terminal 3)

```bash
cd C:\kafka\kafka_2.13-3.9.1
.\bin\windows\kafka-topics.bat --create --topic velib-data --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

### Étape 4 — Lancer le Producer Scala (Terminal 4)

```bash
cd ...\velib
sbt
> runMain VelibProducer
```

### Étape 5 — Lancer le Consumer Spark Streaming (Terminal 5)

```bash
cd ...\velib
sbt
> runMain VelibConsumer
```

Les données sont enregistrées automatiquement dans `velib_output/`.

### Étape 6 — Activer l’environnement Python (Terminal 6)

```bash
cd ...\velib
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

### Étape 7 — Installer les dépendances Python

```bash
pip install -r requirements.txt
```

### Étape 8 — Lancer le dashboard Streamlit

```bash
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

https://velib-metropole-opendata.smovengo.cloud/opendata/Velib_Metropole/station_status.json

---

