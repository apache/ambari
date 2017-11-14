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
package org.apache.ambari.metrics.adservice.configuration

import java.net.{MalformedURLException, URISyntaxException}

import org.apache.hadoop.conf.Configuration
import org.slf4j.{Logger, LoggerFactory}

object HBaseConfiguration {

  val HBASE_SITE_CONFIGURATION_FILE: String = "hbase-site.xml"
  val hbaseConf: org.apache.hadoop.conf.Configuration = new Configuration(true)
  var isInitialized: Boolean = false
  val LOG : Logger = LoggerFactory.getLogger("HBaseConfiguration")

  def initConfigs(): Unit = {
    if (!isInitialized) {
      var classLoader: ClassLoader = Thread.currentThread.getContextClassLoader
      if (classLoader == null) classLoader = getClass.getClassLoader

      try {
        val hbaseResUrl = classLoader.getResource(HBASE_SITE_CONFIGURATION_FILE)
        if (hbaseResUrl == null) throw new IllegalStateException("Unable to initialize the AD subsystem. No hbase-site present in the classpath.")

        hbaseConf.addResource(hbaseResUrl.toURI.toURL)
        isInitialized = true

      } catch {
        case me : MalformedURLException => println("MalformedURLException")
        case ue : URISyntaxException => println("URISyntaxException")
      }
    }
  }

  def getHBaseConf: org.apache.hadoop.conf.Configuration = {
    if (!isInitialized) {
      initConfigs()
    }
    hbaseConf
  }
}
