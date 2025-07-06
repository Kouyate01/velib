import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object VelibConsumer {
  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "C:\\hadoop") // Mets un dossier vide si besoin
    System.setProperty("spark.sql.streaming.checkpointLocation", "checkpoint")
    System.setProperty("spark.sql.warehouse.dir", "warehouse") // chemin local valide
    val spark = SparkSession.builder
      .appName("VelibConsumer")
      .master("local[*]")
      .getOrCreate()

    // Lecture du topic Kafka
    val df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "velib-data")
      .option("failOnDataLoss", "false") // ← ici
      .load()


    //  Schéma complet de la station 
    val coordSchema = StructType(Seq(
      StructField("lon", DoubleType),
      StructField("lat", DoubleType)
    ))

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

    val rootSchema = StructType(Seq(
      StructField("total_count", IntegerType),
      StructField("results", ArrayType(stationSchema))
    ))

    // Parsing et sélection des colonnes utiles
    val jsonDf = df.selectExpr("CAST(value AS STRING) as json")
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

    // Ecriture dans dossier velib_output  
    val query = jsonDf.writeStream
      .outputMode("append")
      .format("json")
      .option("path", "velib_output")
      .option("checkpointLocation", "checkpoint")
      .start()

    query.awaitTermination()
  }
}
