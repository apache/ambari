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

import org.apache.ambari.metrics.adservice.configuration.{AdServiceConfiguration, HBaseConfiguration, MetricCollectorConfiguration, MetricManagerServiceConfiguration}

import com.fasterxml.jackson.annotation.JsonProperty

import io.dropwizard.Configuration

/**
  * Top Level AD System Manager config items.
  */
class AnomalyDetectionAppConfig extends Configuration {

  /*
   Metric Definition Service configuration
    */
  @Valid
  private val metricManagerServiceConfiguration = new MetricManagerServiceConfiguration

  @Valid
  private val metricCollectorConfiguration = new MetricCollectorConfiguration

  /*
   Anomaly Service configuration
    */
  @Valid
  private val adServiceConfiguration = new AdServiceConfiguration

  /*
   HBase Conf
    */
  def getHBaseConf : org.apache.hadoop.conf.Configuration = {
    HBaseConfiguration.getHBaseConf
  }

  @JsonProperty("metricManagerService")
  def getMetricManagerServiceConfiguration: MetricManagerServiceConfiguration = {
    metricManagerServiceConfiguration
  }

  @JsonProperty("adQueryService")
  def getAdServiceConfiguration: AdServiceConfiguration = {
    adServiceConfiguration
  }

  @JsonProperty("metricsCollector")
  def getMetricCollectorConfiguration: MetricCollectorConfiguration = metricCollectorConfiguration

}
