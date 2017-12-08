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

package org.apache.ambari.metrics.adservice.configuration

import com.fasterxml.jackson.annotation.JsonProperty

class SparkConfiguration {

  @JsonProperty("mode")
  private val mode: String = "standalone"

  @JsonProperty("master")
  private val master: String = "spark://localhost:7077"

  @JsonProperty("sparkHome")
  private val sparkHome: String = "/usr/lib/ambari-metrics-anomaly-detection/spark"

  @JsonProperty("jarfile")
  private val jarfile: String = "/usr/lib/ambari-metrics-anomaly-detection/ambari-metrics-anomaly-detection-service.jar"


  def getMode: String = mode

  def getMaster: String = master

  def getSparkHome: String = sparkHome

  def getJarFile: String = jarfile

}
