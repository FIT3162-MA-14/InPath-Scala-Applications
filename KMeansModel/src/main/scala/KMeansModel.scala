package com.deline.chew.ML
  
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.ml.evaluation.ClusteringEvaluator
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkFiles
import org.apache.spark.sql.types._
import org.apache.spark.sql.Row
import org.apache.spark.ml.feature.VectorAssembler
import java.io._

object KMeansClustering {
  def main(args: Array[String]): Unit = {
    
    val spark = SparkSession
      .builder
      .appName(s"${this.getClass.getSimpleName}")
      .getOrCreate;

    val data = spark.read.option("header", "true").option("inferSchema", "true").csv("hdfs://namenode:9000/csv/train.csv")
    val data_cleaned = data.drop("userId")

    val cols = Array("numOfWords", "messageRead", "timeSpent", "numOfPosts", "avgPostPerCat", "numOfReplies", "likeReceived")
    val assembler = new VectorAssembler().setInputCols(cols).setOutputCol("features")
    val dataset = assembler.transform(data_cleaned)

    // Trains a k-means model
    val kmeans = new KMeans().setK(3).setSeed(1L)
    val model = kmeans.fit(dataset)

    // Make predictions
    val predictions = model.transform(dataset)

    // Evaluate clustering by computing Silhouette score
    // val evaluator = new ClusteringEvaluator()

    // val silhouette = evaluator.evaluate(predictions)
    // println(s"Silhouette with squared euclidean distance = $silhouette")

    // Save model
    model.write.overwrite().save("hdfs://namenode:9000/model")

    spark.sparkContext.parallelize(model.clusterCenters).saveAsTextFile("hdfs://namenode:9000/model/output.txt")

    }
    }