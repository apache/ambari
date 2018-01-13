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

import org.apache.ambari.metrics.adservice.configuration.DetectionServiceConfiguration
import org.apache.ambari.metrics.adservice.detection.{DetectionServiceUtils, SparkApplicationRunner, Subsystem}
import org.apache.ambari.metrics.adservice.metadata.MetricKey
import org.apache.spark.launcher.SparkAppHandle
import org.slf4j.{Logger, LoggerFactory}

import com.google.inject.Singleton

@Singleton
class PointInTimeSubsystem extends Subsystem{

  val LOG : Logger = LoggerFactory.getLogger(classOf[PointInTimeSubsystem])

  val appHandleMap: scala.collection.mutable.Map[String, SparkAppHandle] = scala.collection.mutable.Map()
  var applicationRunner: SparkApplicationRunner = _
  var config: DetectionServiceConfiguration = _
  var metricKeys: Set[MetricKey] = Set.empty[MetricKey]

  //EMA Stuff
  val emaDriverClass = "org.apache.ambari.metrics.adservice.detection.pointintime.EmaSparkDriver"
  val emaAppName = "Ema_Spark_Application"
  val emaConfig: scala.collection.mutable.MutableList[String] = scala.collection.mutable.MutableList()


  def this(config: DetectionServiceConfiguration, applicationRunner: SparkApplicationRunner, metricKeys: Set[MetricKey]) = {
    this
    this.applicationRunner = applicationRunner
    this.config = config
    this.metricKeys = metricKeys

    //Initialize
    initializeConfigs()
  }

  override def start(): Unit = {

    LOG.info("Starting Point in Time AD jobs...")

    if (!appHandleMap.contains(emaAppName) || !applicationRunner.isRunning(appHandleMap.apply(emaAppName))) {
      LOG.info("Starting " + emaAppName)

      if (config.getKafkaServers == null || config.getKafkaServers.isEmpty) {
        LOG.error("Cannot run" + emaAppName + " without Kafka Servers config")
      } else {
        val args : scala.collection.mutable.MutableList[String] = scala.collection.mutable.MutableList()
        val emaHandle: SparkAppHandle = applicationRunner.runSparkJob(emaAppName, emaDriverClass, emaConfig)
        appHandleMap(emaAppName) = emaHandle

        Thread.sleep(3000)

        if (applicationRunner.isRunning(emaHandle)) {
          LOG.info(emaAppName + " successfully started.")
        } else {
          LOG.error(emaAppName + " start failed.")
        }
      }
    } else {
      LOG.info(emaAppName + " already running. Moving ahead.")
    }

  }

  override def stop(): Unit = {

    LOG.info("Stopping Point in Time AD jobs...")
    for ((app, handle) <- appHandleMap) {
      handle.stop()
    }
    //Sleep for 3 seconds
    Thread.sleep(3000)

    for ((app, handle) <- appHandleMap) {
      if (!applicationRunner.isRunning(handle)) {
        LOG.info(app + " successfully stopped.")
      }
    }
  }

  override def state: Map[String, String] = {
    val stateMap: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map()
    for ((app, handle) <- appHandleMap) {
      if (handle == null) {
        stateMap(app) = null
      } else {
        stateMap(app) = handle.getState.toString
      }
    }
    stateMap.toMap
  }


  private def initializeConfigs(): Unit = {
    //EMA Configs
    emaConfig.+=(config.getEmaW)
    emaConfig.+=(config.getEmaN)
    emaConfig.+=(config.getKafkaServers)
    emaConfig.+=(config.getKafkaTopic)
    emaConfig.+=(config.getKafkaConsumerGroup)

    val metricKeyFileName: String = DetectionServiceUtils.serializeMetricKeyList(metricKeys)
    LOG.info("EMA Metric Key List file name : " + metricKeyFileName)

    emaConfig.+=(metricKeyFileName)
  }

}
