import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import scala.io.Source

object VelibProducer {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("VelibProducer")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    val apiUrl = "https://data.opendatasoft.com/api/explore/v2.1/catalog/datasets/velib-disponibilite-en-temps-reel@parisdata/records?limit=100"

    while (true) {
      try {
        // Lecture de l'API
        val rawJson = Source.fromURL(apiUrl).mkString
        val df = Seq(rawJson).toDF("value")

        // Envoi vers Kafka
        df.selectExpr("CAST(value AS STRING)")
          .write
          .format("kafka")
          .option("kafka.bootstrap.servers", "localhost:9092")
          .option("topic", "velib-data")
          .save()

        println("✓ Données envoyées vers Kafka.")
      } catch {
        case e: Exception =>
          println(s" Erreur lors de la récupération ou l’envoi : ${e.getMessage}")
      }

      // Attente de 10 secondes
      Thread.sleep(10000)
    }

    // Ce code n’est jamais atteint à cause du `while(true)` mais on le laisse pour bonne pratique
    spark.stop()
  }
}
