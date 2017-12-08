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

import com.fasterxml.jackson.annotation.JsonProperty

class DetectionServiceConfiguration {

  @JsonProperty("pointInTime")
  private val pointInTime: Boolean = true

  @JsonProperty("trend")
  private val trend: Boolean = true

  @JsonProperty("kafkaServers")
  private val kafkaServers: String = null

  @JsonProperty("kafkaTopic")
  private val kafkaTopic: String = "ambari-metrics-ad"

  @JsonProperty("kafkaConsumerGroup")
  private val kafkaConsumerGroup: String = "ambari-metrics-ad-group"

  @JsonProperty("ema-w")
  private val emaW: String = "0.9"

  @JsonProperty("ema-n")
  private val emaN: String = "3"

  def isPointInTimeSubsystemEnabled: Boolean = pointInTime

  def isTrendSubsystemEnabled: Boolean = trend

  def getKafkaServers : String = kafkaServers

  def getKafkaTopic : String = kafkaTopic

  def getKafkaConsumerGroup : String = kafkaConsumerGroup

  def getEmaW : String = emaW

  def getEmaN : String = emaN

}
