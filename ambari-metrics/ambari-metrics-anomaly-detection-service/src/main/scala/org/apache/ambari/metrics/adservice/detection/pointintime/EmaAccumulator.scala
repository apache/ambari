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

import org.apache.ambari.metrics.adservice.detection.pointintime.EmaConstants.EmaDataStructure
import org.apache.ambari.metrics.adservice.model.Season
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric
import org.apache.spark.util.AccumulatorV2


class EmaAccumulator extends AccumulatorV2[EmaDataStructure, EmaDataStructure]{

  private val emaModelMap: EmaDataStructure = scala.collection.mutable.Map.empty

  override def isZero: Boolean = {
    emaModelMap.isEmpty
  }

  override def copy(): AccumulatorV2[EmaDataStructure, EmaDataStructure] = {
    val emaAccumulatorCopy: EmaAccumulator = new EmaAccumulator
    emaAccumulatorCopy.add(emaModelMap)
    emaAccumulatorCopy
  }

  override def reset(): Unit = {
    emaModelMap.clear()
  }

  override def add(v: EmaDataStructure): Unit = {

    System.out.println("EmaAccumulator add called.")
    for ((key: TimelineMetric, modelMap) <- v) {
      if (!emaModelMap.contains(key)) {
        System.out.println("New entry in accumulator map for [" + key.getMetricName + "," + key.getAppId + "," + key.getHostName)
        emaModelMap(key) = modelMap
      } else {
        val currentModelMap = emaModelMap.apply(key)
        val updatedModelMap: scala.collection.mutable.Map[Season, EmaModel] = scala.collection.mutable.Map.empty
        for ((season, model) <- modelMap) {
          if (currentModelMap.contains(season)) {
            val currentModel : EmaModel = currentModelMap.apply(season)
            if (currentModel.lastUpdatedTimestamp < model.lastUpdatedTimestamp) {
              System.out.println("Updating entry in EMA Accumulator map for : [" + key.getMetricName + "," +
                key.getAppId + "," + key.getHostName + "] Season : " + season.toString)
              updatedModelMap(season) = model
            } else {
              System.out.println("Retaining old model entry in EMA Accumulator map for : [" + key.getMetricName + "," +
                key.getAppId + "," + key.getHostName + "] Season : " + season.toString)
              updatedModelMap(season) = currentModel
            }
          } else {
            System.out.println("Adding new season entry in EMA Accumulator map for : [" + key.getMetricName + "," +
              key.getAppId + "," + key.getHostName + "] Season : " + season.toString)
            updatedModelMap(season) = model
          }
        }
        System.out.println("Updating the model entry in the master map.")
        emaModelMap(key) = updatedModelMap.toMap
      }
    }
  }

  override def merge(other: AccumulatorV2[EmaDataStructure, EmaDataStructure]): Unit = {
    add(other.value)
  }

  override def value: EmaDataStructure = {
    emaModelMap
  }
}
