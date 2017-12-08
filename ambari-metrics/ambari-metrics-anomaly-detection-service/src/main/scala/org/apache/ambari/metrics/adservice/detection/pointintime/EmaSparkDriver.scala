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

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Duration, StreamingContext}
import org.slf4j.{Logger, LoggerFactory}

import com.fasterxml.jackson.databind.ObjectMapper

//TODO Work in Progress. Will be updated in the next patch.
/**
  * EMA Spark streaming driver application.
  * Input :
  *   EMA algorithm input - w & n.
  *   Kafka brokers
  *   Kafka Topic and group name
  *
  */
object EmaSparkDriver {

//  @Inject
//  var metricDefinitionService : MetricDefinitionService = _

  val LOG : Logger = LoggerFactory.getLogger("EmaSparkDriver")

  def main(args: Array[String]): Unit = {

    val emaW = args(0)
    val emaN = args(1)
    val kafkaServers = args(2)
    val kafkaTopicName = args(3)
    val kafkaGroupName = args(4)

    val sparkConf = new SparkConf().setAppName("EmaSparkDriver")


    //Instantiate Kafka stream reader
    val streamingContext = new StreamingContext(sparkConf, Duration(10000))

    val kafkaParams: java.util.HashMap[String, Object] = new java.util.HashMap[String, Object]
    kafkaParams.put("bootstrap.servers", kafkaServers)
    kafkaParams.put("key.deserializer", classOf[StringDeserializer])
    kafkaParams.put("value.deserializer", classOf[StringDeserializer])
    kafkaParams.put("group.id", kafkaGroupName)
    kafkaParams.put("auto.offset.reset", "latest")
    kafkaParams.put("enable.auto.commit", false: java.lang.Boolean)

    val kafkaStream =
      KafkaUtils.createDirectStream(
        streamingContext,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[String, String](
          util.Arrays.asList(kafkaTopicName),
          kafkaParams.asInstanceOf[java.util.Map[String, Object]]
        )
      )

    kafkaStream.print()

    var timelineMetricsStream = kafkaStream.map(message => {
      val mapper = new ObjectMapper
      val metrics = mapper.readValue(message.value, classOf[TimelineMetrics])
      metrics
    })
    timelineMetricsStream.print()

    var filteredAppMetricStream = timelineMetricsStream.map(timelineMetrics => {
      val filteredMetrics : TimelineMetrics = timelineMetrics
//      for (metric : TimelineMetric <- timelineMetrics.getMetrics.asScala) {
//        val metricKey : MetricKey = MetricKey(
//          metric.getMetricName,
//          metric.getAppId,
//          metric.getInstanceId,
//          metric.getHostName,
//          null)
//
//        if (metricKeys.value.apply(metricKey)) {
//          filteredMetrics.addOrMergeTimelineMetric(metric)
//        }
//      }
      filteredMetrics
    })
    filteredAppMetricStream.print()

    filteredAppMetricStream.foreachRDD(rdd => {
      rdd.foreach(item => {
        val timelineMetrics : TimelineMetrics = item
        LOG.info("Received Metric : " + timelineMetrics.getMetrics.get(0).getMetricName)
//        for (timelineMetric <- timelineMetrics.getMetrics) {
//          var anomalies = emaModel.test(timelineMetric)
//          anomalyMetricPublisher.publish(anomalies)
//        }
      })
    })

    streamingContext.start()
    streamingContext.awaitTermination()

  }
}