import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import parser.Parser._

object VelibConsumer {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("VelibConsumer")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._
    spark.sparkContext.setLogLevel("WARN")

    println("[INFO] VelibConsumer démarré : en attente de données depuis Kafka...")

    // Lecture depuis Kafka
    val df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "velib-data")
      .option("failOnDataLoss", "false")
      .load()

    // Transformation du message Kafka
    val rawDf = df.selectExpr("CAST(value AS STRING) as value")

    // Appel à Parser
    val transformedDf = parseAndTransform(rawDf, spark)

    // Écriture dans un fichier JSON
    val queryToFile = transformedDf.writeStream
      .format("json")
      .option("path", "velib_output")
      .option("checkpointLocation", "velib_output_checkpoint")
      .start()

    queryToFile.awaitTermination()
  }
}
