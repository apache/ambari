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

package org.apache.ambari.metrics.adservice.detection

import org.slf4j.{Logger, LoggerFactory}
import org.apache.ambari.metrics.adservice.app.AnomalyDetectionAppConfig
import org.apache.ambari.metrics.adservice.detection.pointintime.PointInTimeSubsystem
import org.apache.ambari.metrics.adservice.metadata.MetricKey
import org.apache.ambari.metrics.adservice.service.MetricDefinitionService

/**
  * Class to start Anomaly detection jobs on spark.
   */
class AdJobManager{

  val LOG : Logger = LoggerFactory.getLogger(classOf[AdJobManager])

  var config: AnomalyDetectionAppConfig = _
  var sparkApplicationRunner: SparkApplicationRunner = _
  var metricDefinitionService: MetricDefinitionService = _

  val configuredSubsystems: scala.collection.mutable.Map[String, Subsystem] = scala.collection.mutable.Map()
  var isInitialized : Boolean = false
  var metricKeys: Set[MetricKey] = Set.empty[MetricKey]

  def this (config: AnomalyDetectionAppConfig, metricDefinitionService: MetricDefinitionService) = {
    this ()
    this.config = config
    this.sparkApplicationRunner = new SparkApplicationRunner(config.getSparkConfiguration)
    this.metricDefinitionService = metricDefinitionService
  }

  /**
    * Initialize subsystems
    */
  def initializeSubsystems() : Unit = {

    metricKeys = metricDefinitionService.getMetricKeyList

    if (config.getDetectionServiceConfiguration.isPointInTimeSubsystemEnabled) {
      configuredSubsystems("pointintime") = new PointInTimeSubsystem(config.getDetectionServiceConfiguration, sparkApplicationRunner, metricKeys)
    }
  }


  /**
    * Start AD jobs.
    */
  def startAdJobs() : Unit = {
    if (!isInitialized) {
      initializeSubsystems()
      isInitialized = true
    }

    for (subsystem <- configuredSubsystems.values) {
      subsystem.start()
    }
  }

  /**
    * Stop AD jobs.
    */
  def stopAdJobs() : Unit = {
    for (subsystem <- configuredSubsystems.values) {
      subsystem.stop()
    }
  }

  /**
    * Get State of all the AD jobs.
    * @return
    */
  def state() : Map[String, Map[String, String]] = {
    val stateMap: scala.collection.mutable.Map[String, Map[String, String]] = scala.collection.mutable.Map()

    for ((subsystemName, subsystem) <- configuredSubsystems) {
      stateMap(subsystemName) = subsystem.state
    }
    stateMap.toMap
  }
}
