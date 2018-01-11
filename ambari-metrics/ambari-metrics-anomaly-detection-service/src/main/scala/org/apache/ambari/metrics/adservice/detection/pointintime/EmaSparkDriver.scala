/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.metrics.adservice.detection.pointintime

import java.util

import org.apache.ambari.metrics.adservice.app.ADServiceScalaModule
import org.apache.ambari.metrics.adservice.metadata.MetricKey
import org.apache.ambari.metrics.adservice.model.PointInTimeAnomalyInstance
import org.apache.commons.logging.{Log, LogFactory}
import org.apache.hadoop.metrics2.sink.timeline.{TimelineMetric, TimelineMetrics}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Duration, StreamingContext}
import org.apache.spark.util.LongAccumulator

import scala.collection.JavaConverters._
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

//TODO Work in Progress. Will be updated in the next patch.
/**
  * EMA Spark streaming driver application.
  * Input :
  * EMA algorithm input - w & n.
  * Kafka brokers
  * Kafka Topic and group name
  *
  */
object EmaSparkDriver {

  val LOG: Log = LogFactory.getLog("EmaSparkDriver")

  def main(args: Array[String]): Unit = {

    val emaW = args(0)
    val emaN = args(1)

    val kafkaServers = args(2)
    val kafkaTopicName = args(3)
    val kafkaConsumerGroupName = args(4)

    var metricKeyFileName = ""
    if (args.length > 5) {
      metricKeyFileName = args(5)
    }

    var metricToBeTracked = "load_one"
    if (args.length > 6) {
      metricToBeTracked = args(6)
    }

    var hostToBeTracked = ""
    if (args.length > 7) {
      hostToBeTracked = args(7)
    }

    val sparkConf = new SparkConf().setAppName("EmaSparkDriver")

    //Instantiate Kafka stream reader
    val streamingContext = new StreamingContext(sparkConf, Duration(10000))

    val kafkaParams: java.util.HashMap[String, Object] = new java.util.HashMap[String, Object]
    kafkaParams.put("bootstrap.servers", kafkaServers)
    kafkaParams.put("key.deserializer", classOf[StringDeserializer])
    kafkaParams.put("value.deserializer", classOf[StringDeserializer])
    kafkaParams.put("group.id", kafkaConsumerGroupName)
    kafkaParams.put("auto.offset.reset", "latest")
    kafkaParams.put("enable.auto.commit", false: java.lang.Boolean)

    val metricKeys: Broadcast[Set[MetricKey]] =
      streamingContext.sparkContext.broadcast(readMetricKeysFromFile(metricKeyFileName))
    val emaRunner: Broadcast[EmaRunner] = streamingContext.sparkContext.broadcast(new EmaRunner(true))

    val emaAccumulator = new EmaAccumulator
    streamingContext.sparkContext.register(emaAccumulator, "EMA Accumulator")
//    val longAccumulator: LongAccumulator = new LongAccumulator()
//    streamingContext.sparkContext.register(longAccumulator, "LongAccumulator1")

    val kafkaStream =
      KafkaUtils.createDirectStream(
        streamingContext,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[String, String](
          util.Arrays.asList(kafkaTopicName),
          kafkaParams.asInstanceOf[java.util.Map[String, Object]]
        )
      )

    val timelineMetricsStream = kafkaStream.map(message => {
      val mapper = new ObjectMapper
      val metrics = mapper.readValue(message.value, classOf[TimelineMetrics])
      metrics
    })

    val filteredAppMetricStream = timelineMetricsStream.map(timelineMetrics => {
      val filteredMetrics: TimelineMetrics = new TimelineMetrics
      for (metric: TimelineMetric <- timelineMetrics.getMetrics.asScala) {
        val metricKey: MetricKey = MetricKey(
          metric.getMetricName,
          metric.getAppId,
          metric.getInstanceId,
          metric.getHostName,
          null)

        if ((metricKeys.value.isEmpty || metricKeys.value.contains(metricKey))
          && metric.getMetricName.equals(metricToBeTracked)
          && (hostToBeTracked.isEmpty || metric.getHostName.equals(hostToBeTracked))) {
          filteredMetrics.addOrMergeTimelineMetric(metric)
        }
      }
      filteredMetrics
    })

    filteredAppMetricStream.foreachRDD(rdd => {
      rdd.foreach(item => {
        val timelineMetrics: TimelineMetrics = item
//        if (!timelineMetrics.getMetrics.isEmpty) {
//          val msg = "Received Metric : " + timelineMetrics.getMetrics.size() + " , appId = " + timelineMetrics.getMetrics.get(0).getAppId
//          System.out.println(msg)
//        }
//        longAccumulator.add(1)
//        System.out.println("longAccumulator value : " + longAccumulator.value)
        for (timelineMetric <- timelineMetrics.getMetrics.asScala) {
          System.out.println("EmaAccumulator Object : " + emaAccumulator.hashCode() + ", Size : " + emaAccumulator.value.size)
          var anomalies: List[PointInTimeAnomalyInstance] = emaRunner.value.runEma(timelineMetric, emaAccumulator)
          for (anomaly <- anomalies) {
            System.out.println("Anomaly : " + anomaly.toString)
          }
        }
      })
    })

    streamingContext.start()
    streamingContext.awaitTermination()
  }

  def readMetricKeysFromFile(fileName: String): Set[MetricKey] = {

    Set.empty[MetricKey]
//    if (fileName.isEmpty) {
//      LOG.info("Metric Keys file name is empty. All metrics will be tracked.")
//      return Set.empty[MetricKey]
//    }
//
//    val mapper = new ObjectMapper() //with ScalaObjectMapper
//    mapper.registerModule(new ADServiceScalaModule)
//    val source = scala.io.Source.fromFile(fileName)
//    val lines = try source.mkString finally source.close()
//    val metricKeys: Set[MetricKey] = mapper.readValue[Set[MetricKey]](lines, new TypeReference[Set[MetricKey]]() {})
//    if (metricKeys != null) {
//      metricKeys
//    } else {
//      Set.empty[MetricKey]
//    }
  }
}