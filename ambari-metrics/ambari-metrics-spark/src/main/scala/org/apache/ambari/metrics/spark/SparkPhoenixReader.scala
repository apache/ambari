package org.apache.ambari.metrics.spark

import org.apache.ambari.metrics.alertservice.common.TimelineMetric
import org.apache.ambari.metrics.alertservice.methods.ema.EmaModel
import org.apache.spark.mllib.stat.Statistics
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

object SparkPhoenixReader {

  def main(args: Array[String]) {

    if (args.length < 6) {
      System.err.println("Usage: SparkPhoenixReader <metric_name> <appId> <hostname> <weight> <timessdev> <phoenixConnectionString> <model_dir>")
      System.exit(1)
    }

    var metricName = args(0)
    var appId = args(1)
    var hostname = args(2)
    var weight = args(3).toDouble
    var timessdev = args(4).toInt
    var phoenixConnectionString = args(5) //avijayan-ams-3.openstacklocal:61181:/ams-hbase-unsecure
    var modelDir = args(6)

    val conf = new SparkConf()
    conf.set("spark.app.name", "AMSAnomalyModelBuilder")
    //conf.set("spark.master", "spark://avijayan-ams-2.openstacklocal:7077")

    var sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    val currentTime = System.currentTimeMillis()
    val oneDayBack = currentTime - 24*60*60*1000

    val df = sqlContext.load("org.apache.phoenix.spark", Map("table" -> "METRIC_RECORD", "zkUrl" -> phoenixConnectionString))
    df.registerTempTable("METRIC_RECORD")
    val result = sqlContext.sql("SELECT METRIC_NAME, HOSTNAME, APP_ID, SERVER_TIME, METRIC_SUM, METRIC_COUNT FROM METRIC_RECORD " +
      "WHERE METRIC_NAME = '" + metricName + "' AND HOSTNAME = '" + hostname + "' AND APP_ID = '" + appId + "' AND SERVER_TIME < " + currentTime + " AND SERVER_TIME > " + oneDayBack)

    var metricValues = new java.util.TreeMap[java.lang.Long, java.lang.Double]
    result.collect().foreach(
      t => metricValues.put(t.getLong(3), t.getDouble(4) / t.getInt(5))
    )

    //val metricName = result.head().getString(0)
    //val hostname = result.head().getString(1)
    //val appId = result.head().getString(2)

    val timelineMetric = new TimelineMetric(metricName, appId, hostname, metricValues)

    var emaModel = new EmaModel()
    emaModel.train(timelineMetric, weight, timessdev)
    emaModel.save(sc, modelDir)

//    var metricData:Seq[Double] = Seq.empty
//    result.collect().foreach(
//      t => metricData :+ t.getDouble(4) / t.getInt(5)
//    )
//    val data: RDD[Double] = sc.parallelize(metricData)
//    val myCDF = Map(0.1 -> 0.2, 0.15 -> 0.6, 0.2 -> 0.05, 0.3 -> 0.05, 0.25 -> 0.1)
//    val testResult2 = Statistics.kolmogorovSmirnovTest(data, myCDF)

  }

}
