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

package org.apache.ambari.metrics.adservice.service

import org.apache.ambari.metrics.adservice.app.AnomalyDetectionAppConfig
import org.apache.ambari.metrics.adservice.detection.AdJobManager
import org.slf4j.{Logger, LoggerFactory}

import com.google.inject.{Inject, Singleton}

@Singleton
class DetectionServiceImpl extends DetectionService{

  val LOG : Logger = LoggerFactory.getLogger(classOf[DetectionServiceImpl])

  var adJobManager: AdJobManager = _
  var config : AnomalyDetectionAppConfig = _
  var metricDefinitionService: MetricDefinitionService = _

  @Inject
  def this (anomalyDetectionAppConfig: AnomalyDetectionAppConfig, metricDefinitionService: MetricDefinitionService) = {
    this ()
    this.config = anomalyDetectionAppConfig
    this.metricDefinitionService = metricDefinitionService
  }

  override def initialize(): Unit = {
    this.adJobManager = new AdJobManager(config, metricDefinitionService)
    adJobManager.startAdJobs()
  }

  override def state(): Map[String, Map[String, String]] = {
    adJobManager.state()
  }

  override def start(): Unit = {
  }

  override def stop(): Unit = {
    LOG.info("Stop Detection Service hook invoked. Stopping AD Jobs")
    adJobManager.stopAdJobs()
  }
}
