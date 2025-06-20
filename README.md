# Projet Vélib Spark Streaming

## Structure du projet

```
velib_project/
├── build.sbt
├── README.md
└── src/
    └── main/
        └── scala/
            ├── VelibProducer.scala
            └── VelibConsumer.scala
```

- `build.sbt` : configuration SBT et dépendances.
- `src/main/scala/VelibProducer.scala` : Producer Kafka (collecte API Vélib).
- `src/main/scala/VelibConsumer.scala` : Consumer Spark Streaming (lecture Kafka).

## Compilation et exécution

1. Compiler le projet :
   ```bash
   sbt compile
   ```
2. Lancer le Producer :
   ```bash
   sbt "runMain VelibProducer"
   ```
3. Lancer le Consumer :
   ```bash
   sbt "runMain VelibConsumer"
   ```

Assurez-vous que Kafka et Spark sont installés et que le serveur Kafka tourne sur `localhost:9092`.
