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

package org.apache.ambari.metrics.adservice.detection.pointintime

import org.apache.ambari.metrics.adservice.model.Season

/**
  * Class used to hold a EMA Model for a single metric key.
  */
class EmaModel{

  var ema: Double = 0.0
  var ems: Double = 0.0
  var season: Season = Season()
  var timessdev: Double = 3
  var weight: Double = 0.9
  var lastUpdatedTimestamp: Long = _

  def this(season: Season) = {
    this
    this.season = season
  }

  def this(timessdev: Double, weight: Double, season: Season) = {
    this
    this.timessdev = timessdev
    this.weight = weight
    this.season = season
  }

  private def test(metricValue: Double): Double = {
    val diff = Math.abs(ema - metricValue) - (timessdev * ems)
    if (diff > 0) Math.abs((metricValue - ema) / ems) //Z score
    else 0.0
  }

  def updateModel(increaseSensitivity: Boolean, percent: Double): Unit = {
    var delta = percent / 100
    if (increaseSensitivity) delta = delta * -1
    this.timessdev = timessdev + delta * timessdev
  }

  def update(timestamp: Long, metricValue: Double): Unit = {
    System.out.println("Before Update Model : " + getModelParameters)
    ema = weight * ema + (1 - weight) * metricValue
    ems = Math.sqrt(weight * Math.pow(ems, 2.0) + (1 - weight) * Math.pow(metricValue - ema, 2.0))
    System.out.println("After Update Model : " + getModelParameters)
    lastUpdatedTimestamp = timestamp
  }

  def updateAndTest(timestamp: Long, metricValue: Double): Double = {
    var anomalyScore = 0.0
    update(timestamp, metricValue)
    anomalyScore = test(metricValue)
    anomalyScore
  }

  def getModelParameters: String = {
    "[EMA=" + ema + ", EMS=" + ems + "], [w=" + weight + ", n=" + timessdev + "]"
  }
}
