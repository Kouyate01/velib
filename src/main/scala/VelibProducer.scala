import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import scala.io.Source

object VelibProducer {
  def main(args: Array[String]): Unit = {
    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](props)
    val apiUrl = "http://data.opendatasoft.com/api/explore/v2.1/catalog/datasets/velib-disponibilite-en-temps-reel@parisdata/records?limit=100"

    while (true) {
      val data = Source.fromURL(apiUrl).mkString
      println("JSON envoyé : " + data) // Affiche le JSON produit
      val record = new ProducerRecord[String, String]("velib-data", null, data)
      producer.send(record)
      println("Data sent to Kafka")
      Thread.sleep(10000) // 10 secondes entre chaque collecte
    }
    producer.close()
  }
}
