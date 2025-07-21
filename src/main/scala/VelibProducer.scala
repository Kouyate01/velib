import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object VelibProducer {
  def main(args: Array[String]): Unit = {

    // Crée la session Spark
    val spark = SparkSession.builder()
      .appName("VelibProducer")
      .master("local[*]")
      .getOrCreate()

    // URL de l’API JSON
    val apiUrl = "https://data.opendatasoft.com/api/explore/v2.1/catalog/datasets/velib-disponibilite-en-temps-reel@parisdata/records?limit=100"

    // Lis le JSON depuis l’URL (Spark supporte l’URL directement si c’est un fichier statique ou via DataFrame JSON → ici, on triche avec un DataFrame temporaire)
    import scala.io.Source
    import spark.implicits._

    // On lit l’URL via Scala standard
    val rawJson = Source.fromURL(apiUrl).mkString

    // On crée un DataFrame avec une seule colonne "value"
    val df = Seq(rawJson).toDF("value")

    // Écrit le DataFrame vers Kafka
    df.selectExpr("CAST(value AS STRING)")
      .write
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("topic", "velib-data")
      .save()

    println("✓ Données envoyées vers Kafka avec Spark.")
    spark.stop()
  }
}
