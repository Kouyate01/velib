package parser

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.expressions.Window

object Parser {

  // === Schéma des coordonnées ===
  val coordSchema = StructType(Seq(
    StructField("lon", DoubleType),
    StructField("lat", DoubleType)
  ))

  // === Schéma de chaque station Vélib ===
  val stationSchema = StructType(Seq(
    StructField("stationcode", StringType),
    StructField("name", StringType),
    StructField("is_installed", StringType),
    StructField("capacity", IntegerType),
    StructField("numdocksavailable", IntegerType),
    StructField("numbikesavailable", IntegerType),
    StructField("mechanical", IntegerType),
    StructField("ebike", IntegerType),
    StructField("is_renting", StringType),
    StructField("is_returning", StringType),
    StructField("duedate", StringType),
    StructField("coordonnees_geo", coordSchema),
    StructField("nom_arrondissement_communes", StringType),
    StructField("code_insee_commune", StringType),
    StructField("station_opening_hours", StringType)
  ))

  // === Schéma racine du JSON complet retourné par l'API ===
  val rootSchema = StructType(Seq(
    StructField("total_count", IntegerType),
    StructField("results", ArrayType(stationSchema))
  ))

  def parseAndTransform(df: DataFrame, spark: SparkSession): DataFrame = {
    import spark.implicits._

    // === 1. Parsing JSON brut depuis Kafka ===
    val parsedDf = df
      .selectExpr("CAST(value AS STRING) AS json")
      .select(from_json(col("json"), rootSchema).as("data"))
      .selectExpr("explode(data.results) as station")
      .select(
        col("station.stationcode"),
        col("station.name"),
        col("station.numbikesavailable"),
        col("station.numdocksavailable"),
        col("station.capacity"),
        col("station.mechanical"),
        col("station.ebike"),
        col("station.is_installed"),
        col("station.is_renting"),
        col("station.is_returning"),
        col("station.duedate"),
        col("station.coordonnees_geo.lat").as("lat"),
        col("station.coordonnees_geo.lon").as("lon"),
        col("station.nom_arrondissement_communes").as("arrondissement"),
        col("station.code_insee_commune")
      )
      .withColumn("timestamp", current_timestamp())
      .withColumn("is_full", col("numdocksavailable") === 0)
      .withColumn("type_dominant", when(col("ebike") > col("mechanical"), "electrique")
        .when(col("ebike") < col("mechanical"), "mecanique")
        .otherwise("egal"))

    // === 2. Enrichissement avec fichier JSON station_name_mapping.json ===
    val jsonPath = "C:\\Users\\Zeineb Rekik\\velib\\station_name_mapping.json"
    val mappingRawDf = spark.read
      .option("multiline", "true")
      .json(jsonPath)

    // Conversion du mapping en DataFrame clé/valeur
    val mappingMap = mappingRawDf.head().getValuesMap[String](mappingRawDf.columns)
    val mappingDf = mappingMap.toSeq.toDF("stationcode", "name_custom")

    val enrichedDf = parsedDf.join(mappingDf, Seq("stationcode"), "left")

    // === 3. Agrégats par arrondissement (GroupBy) ===
    val groupByArr = enrichedDf
      .groupBy("arrondissement")
      .agg(
        sum("numbikesavailable").as("sum_bikes"),
        sum("mechanical").as("sum_mechanical"),
        sum("ebike").as("sum_ebike")
      )

    // === KPIs globaux ===
    val nbStations = enrichedDf.count()
    val nbStationsPleines = enrichedDf.filter(col("is_full")).count()
    val nbStationsVides = enrichedDf.filter(col("numbikesavailable") === 0).count()
    val tauxStationsPleines = if (nbStations > 0) nbStationsPleines.toDouble / nbStations else 0.0
    val tauxStationsVides = if (nbStations > 0) nbStationsVides.toDouble / nbStations else 0.0
    val nbActives = enrichedDf.filter((col("is_installed") === "OUI") && (col("is_renting") === "OUI")).count()
    val nbInactives = nbStations - nbActives

    // === 5. Résultat final ===
    enrichedDf
  }
}
