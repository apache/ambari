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

import javax.ws.rs.core.Response

import org.apache.ambari.metrics.adservice.configuration.MetricCollectorConfiguration
import org.apache.commons.lang.StringUtils
import org.slf4j.{Logger, LoggerFactory}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import scalaj.http.{Http, HttpRequest, HttpResponse}

/**
  * Class to invoke Metrics Collector metadata API.
  * TODO : Instantiate a sync thread that regularly updates the internal maps by reading off AMS metadata.
  */
class ADMetadataProvider extends MetricMetadataProvider {

  var metricCollectorHosts: Array[String] = Array.empty[String]
  var metricCollectorPort: String = _
  var metricCollectorProtocol: String = _
  var metricMetadataPath: String = "/v1/timeline/metrics/metadata/keys"
  val LOG : Logger = LoggerFactory.getLogger(classOf[ADMetadataProvider])

  val connectTimeout: Int = 10000
  val readTimeout: Int = 10000
  //TODO: Add retries for metrics collector GET call.
  //val retries: Long = 5

  def this(configuration: MetricCollectorConfiguration) {
    this
    if (StringUtils.isNotEmpty(configuration.getHosts)) {
      metricCollectorHosts = configuration.getHosts.split(",")
    }
    metricCollectorPort = configuration.getPort
    metricCollectorProtocol = configuration.getProtocol
    metricMetadataPath = configuration.getMetadataEndpoint
  }

  override def getMetricKeysForDefinitions(metricSourceDefinition: MetricSourceDefinition): Set[MetricKey] = {

    val numDefinitions: Int = metricSourceDefinition.metricDefinitions.size
    val metricKeySet: scala.collection.mutable.Set[MetricKey] = scala.collection.mutable.Set.empty[MetricKey]

    for (metricDef <- metricSourceDefinition.metricDefinitions) {
      if (metricDef.isValid) { //Skip requesting metric keys for invalid definitions.
        for (host <- metricCollectorHosts) {
          val metricKeys: Set[MetricKey] = getKeysFromMetricsCollector(metricCollectorProtocol, host, metricCollectorPort, metricMetadataPath, metricDef)
          if (metricKeys != null) {
            metricKeySet.++=(metricKeys)
          }
        }
      }
    }
    metricKeySet.toSet
  }

  /**
    *
    * @param protocol
    * @param host
    * @param port
    * @param path
    * @param metricDefinition
    * @return
    */
  def getKeysFromMetricsCollector(protocol: String, host: String, port: String, path: String, metricDefinition: MetricDefinition): Set[MetricKey] = {

    val url: String = protocol + "://" + host + ":" + port + path
    val mapper = new ObjectMapper() //with ScalaObjectMapper

    if (metricDefinition.hosts == null || metricDefinition.hosts.isEmpty) {
      val request: HttpRequest = Http(url)
        .param("metricName", metricDefinition.metricName)
        .param("appId", metricDefinition.appId)
      makeHttpGetCall(request, mapper)
    } else {
      val metricKeySet: scala.collection.mutable.Set[MetricKey] = scala.collection.mutable.Set.empty[MetricKey]

      for (h <- metricDefinition.hosts) {
        val request: HttpRequest = Http(url)
          .param("metricName", metricDefinition.metricName)
          .param("appId", metricDefinition.appId)
          .param("hostname", h)

        val metricKeys = makeHttpGetCall(request, mapper)
        metricKeySet.++=(metricKeys)
      }
      metricKeySet.toSet
    }
  }

  private def makeHttpGetCall(request: HttpRequest, mapper: ObjectMapper): Set[MetricKey] = {

    try {
      val result: HttpResponse[String] = request.asString
      if (result.code == Response.Status.OK.getStatusCode) {
        LOG.info("Successfully fetched metric keys from metrics collector")
        val metricKeySet: java.util.Set[java.util.Map[String, String]] = mapper.readValue(result.body,
          classOf[java.util.Set[java.util.Map[String, String]]])
        getMetricKeys(metricKeySet)
      } else {
        LOG.error("Got an error when trying to fetch metric key from metrics collector. Code = " + result.code + ", Message = " + result.body)
      }
    } catch {
      case _: java.io.IOException | _: java.net.SocketTimeoutException => LOG.error("Unable to fetch metric keys from Metrics collector for : " + request.toString)
    }
    Set.empty[MetricKey]
  }


  def getMetricKeys(timelineMetricKeys: java.util.Set[java.util.Map[String, String]]): Set[MetricKey] = {
    val metricKeySet: scala.collection.mutable.Set[MetricKey] = scala.collection.mutable.Set.empty[MetricKey]
    val iter = timelineMetricKeys.iterator()
    while (iter.hasNext) {
      val timelineMetricKey: java.util.Map[String, String] = iter.next()
      val metricKey: MetricKey = MetricKey(
        timelineMetricKey.get("metricName"),
        timelineMetricKey.get("appId"),
        timelineMetricKey.get("instanceId"),
        timelineMetricKey.get("hostname"),
        timelineMetricKey.get("uuid").getBytes())

      metricKeySet.add(metricKey)
    }
    metricKeySet.toSet
  }

}