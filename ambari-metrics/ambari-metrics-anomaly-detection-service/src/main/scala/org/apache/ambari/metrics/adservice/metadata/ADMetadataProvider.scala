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

package org.apache.ambari.metrics.adservice.metadata

import java.net.{HttpURLConnection, URL}

import org.apache.ambari.metrics.adservice.configuration.MetricCollectorConfiguration
import org.apache.commons.lang.StringUtils
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricKey

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

/**
  * Class to invoke Metrics Collector metadata API.
  * TODO : Instantiate a sync thread that regularly updates the internal maps by reading off AMS metadata.
  */
class ADMetadataProvider extends MetricMetadataProvider {

  var metricCollectorHostPorts: Array[String] = Array.empty[String]
  var metricMetadataPath: String = "/v1/timeline/metrics/metadata/keys"

  val connectTimeout: Int = 10000
  val readTimeout: Int = 10000
  //TODO: Add retries for metrics collector GET call.
  //val retries: Long = 5

  def this(configuration: MetricCollectorConfiguration) {
    this
    if (StringUtils.isNotEmpty(configuration.getHostPortList)) {
      metricCollectorHostPorts = configuration.getHostPortList.split(",")
    }
    metricMetadataPath = configuration.getMetadataEndpoint
  }

  override def getMetricKeysForDefinitions(metricSourceDefinition: MetricSourceDefinition): (Map[MetricDefinition,
    Set[MetricKey]], Set[MetricKey]) = {

    val keysMap = scala.collection.mutable.Map[MetricDefinition, Set[MetricKey]]()
    val numDefinitions: Int = metricSourceDefinition.metricDefinitions.size
    val metricKeySet: scala.collection.mutable.Set[MetricKey] = scala.collection.mutable.Set.empty[MetricKey]

    for (metricDef <- metricSourceDefinition.metricDefinitions) {
      for (hostPort <- metricCollectorHostPorts) {
        val metricKeys: Set[MetricKey] = getKeysFromMetricsCollector(hostPort + metricMetadataPath, metricDef)
        if (metricKeys != null) {
          keysMap += (metricDef -> metricKeys)
          metricKeySet.++(metricKeys)
        }
      }
    }
    (keysMap.toMap, metricKeySet.toSet)
  }

  /**
    * Make Metrics Collector REST API call to fetch keys.
    *
    * @param url
    * @param metricDefinition
    * @return
    */
  def getKeysFromMetricsCollector(url: String, metricDefinition: MetricDefinition): Set[MetricKey] = {

    val mapper = new ObjectMapper() with ScalaObjectMapper
    try {
      val connection = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
      connection.setConnectTimeout(connectTimeout)
      connection.setReadTimeout(readTimeout)
      connection.setRequestMethod("GET")
      val inputStream = connection.getInputStream
      val content = scala.io.Source.fromInputStream(inputStream).mkString
      if (inputStream != null) inputStream.close()
      val metricKeySet: Set[MetricKey] = fromTimelineMetricKey(mapper.readValue[java.util.Set[TimelineMetricKey]](content))
      return metricKeySet
    } catch {
      case _: java.io.IOException | _: java.net.SocketTimeoutException => // handle this
    }
    null
  }

  def fromTimelineMetricKey(timelineMetricKeys: java.util.Set[TimelineMetricKey]): Set[MetricKey] = {
    val metricKeySet: scala.collection.mutable.Set[MetricKey] = scala.collection.mutable.Set.empty[MetricKey]
    val iter = timelineMetricKeys.iterator()
    while (iter.hasNext) {
      val timelineMetricKey: TimelineMetricKey = iter.next()
      val metricKey: MetricKey = MetricKey(timelineMetricKey.metricName,
        timelineMetricKey.appId,
        timelineMetricKey.instanceId,
        timelineMetricKey.hostName,
        timelineMetricKey.uuid)

      metricKeySet.add(metricKey)
    }
    metricKeySet.toSet
  }

}