import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object VelibConsumer {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("VelibConsumer")
      .master("local[*]")
      .getOrCreate()

    val df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "velib-data")
      .load()

    // Schéma adapté à la structure réelle du JSON
    val stationSchema = StructType(Seq(
      StructField("stationcode", StringType),
      StructField("name", StringType),
      StructField("numbikesavailable", IntegerType),
      StructField("numdocksavailable", IntegerType)
    ))
    val rootSchema = StructType(Seq(
      StructField("total_count", IntegerType),
      StructField("results", ArrayType(stationSchema))
    ))

    val jsonDf = df.selectExpr("CAST(value AS STRING) as json")
      .select(from_json(col("json"), rootSchema).as("data"))
      .selectExpr("explode(data.results) as station")
      .select(
        col("station.stationcode"),
        col("station.name"),
        col("station.numbikesavailable"),
        col("station.numdocksavailable")
      )

    val query = jsonDf.writeStream
      .outputMode("append")
      .format("console")
      .start()

    query.awaitTermination()
  }
}
