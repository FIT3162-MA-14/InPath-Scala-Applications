package com.deline.chew.ML


//                          _ooOoo_                               //
//                         o8888888o                              //
//                         88" . "88                              //
//                         (| ^_^ |)                              //
//                         O\  =  /O                              //
//                      ____/`---'\____                           //
//                    .'  \\|     |//  `.                         //
//                   /  \\|||  :  |||//  \                        //
//                  /  _||||| -:- |||||-  \                       //
//                  |   | \\\  -  /// |   |                       //
//                  | \_|  ''\---/''  |   |                       //
//                  \  .-\__  `-`  ___/-. /                       //
//                ___`. .'  /--.--\  `. . ___                     //
//              ."" '<  `.___\_<|>_/___.'  >'"".                  //
//            | | :  `- \`.;`\ _ /`;.`/ - ` : | |                 //
//            \  \ `-.   \_ __\ /__ _/   .-` /  /                 //
//      ========`-.____`-.___\_____/___.-`____.-'========         //
//                           `=---='                              //
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^        //
//                  佛祖保佑       永不宕机     永无BUG              //

import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.evaluation.ClusteringEvaluator
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkFiles
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import java.util.HashMap
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010._
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import org.apache.spark.ml.clustering.KMeansModel
import scala.collection.mutable.ArrayBuffer
import java.sql.DriverManager
import org.apache.spark.ml.clustering.KMeans

object StreamingKMeans {
    def main(args: Array[String]): Unit = {
        StreamingExamples.setStreamingLogLevels()
        val spark = SparkSession
        .builder
        .appName("StreamingKmeans")
        .getOrCreate;
        import spark.implicits._
        val sparkConf = new SparkConf()
        val sc = new SparkContext(sparkConf)
        val ssc = new StreamingContext(sc, Seconds(1))
        val topics = Array("dataset")


        val kafkaParams = Map[String, Object](
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> "kafka:9092", 
        ConsumerConfig.GROUP_ID_CONFIG -> "my-group-id",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer],
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer])

        // Create kafka direct stream to subscribe messages
        val directStream = KafkaUtils.createDirectStream[String, String](
        ssc, LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[String, String](topics, kafkaParams))

        val model = KMeansModel.load("hdfs://namenode:9000/model")

        val lines = directStream.map(_.value)

        // val dataRDD = sc.parallelize(Seq(lines))
        // val data_cleaned = spark.createDataFrame(dataRDD)
        // val cleaned_data = data_cleaned.drop("uid")

        // val cols = Array("numOfWords", "messageRead", "timeSpent", "numOfPosts", "avgPostPerCat", "numOfReplies", "likeReceived")
        // val assembler = new VectorAssembler().setInputCols(cols).setOutputCol("features")
        // val dataset = assembler.transform(cleaned_data)

        // // Trains a k-means model
        // val kmeans = new KMeans().setK(3).setSeed(1L)
        // val model = kmeans.fit(dataset)

        // // Make predictions
        // val predictions = model.transform(dataset)
        
        lines.foreachRDD(
            rdd =>{
            if(rdd.count !=0 ){

            val vec = rdd.map{r => 
                                    val arr = r.drop(1).dropRight(1).split(',').map(r => r.toDouble)
                                    val prediction = model.predict(Vectors.dense(arr.slice(1,8)))
                                    val id = arr(0)
                                    (id,prediction)
            }
            
            vec.foreachPartition(part =>
                part.foreach{ result =>{
                    /* Send the updated data to database */
                    Class.forName("com.mysql.jdbc.Driver");
                        val connection = DriverManager.getConnection("jdbc:mysql://13.213.240.136/spark?allowPublicKeyRetrieval=true&useSSL=false", "root", "Chewk@i1")
                        val prep = connection.prepareStatement("INSERT INTO `demo_rank_prediction` (`student_id`, `rank`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `student_id` = VALUES(`student_id`), `rank` = VALUES(`rank`)")
                    prep.setInt(1, result._1.toInt)
                    prep.setInt(2, result._2.toInt)
                    prep.executeUpdate
                        connection.close()
                }
            })
            
            
            }
            }
        )
    
         // Start the computation
        ssc.start()
        ssc.awaitTermination()

    }
}
