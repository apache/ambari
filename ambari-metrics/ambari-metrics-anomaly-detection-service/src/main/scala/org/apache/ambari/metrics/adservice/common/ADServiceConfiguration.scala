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
package org.apache.ambari.metrics.adservice.common

import java.net.{MalformedURLException, URISyntaxException}

import org.apache.hadoop.conf.Configuration

object ADServiceConfiguration {

  private val AMS_AD_SITE_CONFIGURATION_FILE = "ams-ad-site.xml"
  private val HBASE_SITE_CONFIGURATION_FILE = "hbase-site.xml"

  val ANOMALY_METRICS_TTL = "timeline.metrics.anomaly.data.ttl"

  private var hbaseConf: org.apache.hadoop.conf.Configuration = _
  private var adConf: org.apache.hadoop.conf.Configuration = _

  def initConfigs(): Unit = {

    var classLoader: ClassLoader = Thread.currentThread.getContextClassLoader
    if (classLoader == null) classLoader = getClass.getClassLoader

    try {
      val hbaseResUrl = classLoader.getResource(HBASE_SITE_CONFIGURATION_FILE)
      if (hbaseResUrl == null) throw new IllegalStateException("Unable to initialize the AD subsystem. No hbase-site present in the classpath.")

      hbaseConf = new Configuration(true)
      hbaseConf.addResource(hbaseResUrl.toURI.toURL)

      val adSystemConfigUrl = classLoader.getResource(AMS_AD_SITE_CONFIGURATION_FILE)
      if (adSystemConfigUrl == null) throw new IllegalStateException("Unable to initialize the AD subsystem. No ams-ad-site present in the classpath")

      adConf = new Configuration(true)
      adConf.addResource(adSystemConfigUrl.toURI.toURL)

    } catch {
      case me : MalformedURLException => println("MalformedURLException")
      case ue : URISyntaxException => println("URISyntaxException")
    }
  }

  def getHBaseConf: org.apache.hadoop.conf.Configuration = {
    hbaseConf
  }

  def getAdConf: org.apache.hadoop.conf.Configuration = {
    adConf
  }

  def getAnomalyDataTtl: Int = {
    if (adConf != null) return adConf.get(ANOMALY_METRICS_TTL, "604800").toInt
    604800
  }

  /**
    * ttl
    *
    */
}
