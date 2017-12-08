/**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package org.apache.ambari.metrics.adservice.app

import javax.validation.Valid

import org.apache.ambari.metrics.adservice.configuration.{DetectionServiceConfiguration, HBaseConfiguration, _}

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties, JsonProperty}

import io.dropwizard.Configuration

/**
  * Top Level AD System Manager config items.
  */
@JsonIgnoreProperties(ignoreUnknown=true)
class AnomalyDetectionAppConfig extends Configuration {

  /*
   Metric Definition Service configuration
    */
  @Valid
  private val metricDefinitionServiceConfiguration = new MetricDefinitionServiceConfiguration

  @Valid
  private val metricCollectorConfiguration = new MetricCollectorConfiguration

  /*
   Anomaly Query Service configuration
    */
  @Valid
  private val adServiceConfiguration = new AdServiceConfiguration

  /**
    * LevelDB settings for metrics definitions
    */
  @Valid
  private val metricDefinitionDBConfiguration = new MetricDefinitionDBConfiguration

  /**
    * Spark configurations
    */
  @Valid
  private val sparkConfiguration = new SparkConfiguration

  /**
    * Detection Service configurations
    */
  @Valid
  private val detectionServiceConfiguration = new DetectionServiceConfiguration

  /*
   AMS HBase Conf
    */
  @JsonIgnore
  def getHBaseConf : org.apache.hadoop.conf.Configuration = {
    HBaseConfiguration.getHBaseConf
  }

  @JsonProperty("metricDefinitionService")
  def getMetricDefinitionServiceConfiguration: MetricDefinitionServiceConfiguration = {
    metricDefinitionServiceConfiguration
  }

  @JsonProperty("adQueryService")
  def getAdServiceConfiguration: AdServiceConfiguration = {
    adServiceConfiguration
  }

  @JsonProperty("metricsCollector")
  def getMetricCollectorConfiguration: MetricCollectorConfiguration = metricCollectorConfiguration

  @JsonProperty("metricDefinitionDB")
  def getMetricDefinitionDBConfiguration: MetricDefinitionDBConfiguration = metricDefinitionDBConfiguration

  @JsonProperty("spark")
  def getSparkConfiguration: SparkConfiguration = sparkConfiguration

  @JsonProperty("detection")
  def getDetectionServiceConfiguration: DetectionServiceConfiguration = detectionServiceConfiguration

}
