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

import java.util.Calendar

import org.apache.ambari.metrics.adservice.detection.pointintime.EmaConstants.EmaDataStructure
import org.apache.ambari.metrics.adservice.metadata.MetricKey
import org.apache.ambari.metrics.adservice.model._
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric
import scala.collection.JavaConverters._

class EmaRunner extends Serializable{

  var EMA_SEASONS: List[Season] = List.empty[Season]

  def this(initialize: Boolean) = {
    this
    if (initialize) {
      initializeSeasons()
    }
  }

  def initializeSeasons(): Unit = {

    val emaSeasons: scala.collection.mutable.MutableList[Season] = scala.collection.mutable.MutableList()

    //Work Week - Weekend.
    //2 Periods
    emaSeasons.+=(Season(Range(Calendar.MONDAY, Calendar.FRIDAY), SeasonType.DAY))
    emaSeasons.+=(Season(Range(Calendar.SATURDAY, Calendar.SUNDAY), SeasonType.DAY))

    //Day of the Week
    //7 Days
    emaSeasons.+=(Season(Range(Calendar.MONDAY, Calendar.MONDAY), SeasonType.DAY))
    emaSeasons.+=(Season(Range(Calendar.TUESDAY, Calendar.TUESDAY), SeasonType.DAY))
    emaSeasons.+=(Season(Range(Calendar.WEDNESDAY, Calendar.WEDNESDAY), SeasonType.DAY))
    emaSeasons.+=(Season(Range(Calendar.THURSDAY, Calendar.THURSDAY), SeasonType.DAY))
    emaSeasons.+=(Season(Range(Calendar.FRIDAY, Calendar.FRIDAY), SeasonType.DAY))
    emaSeasons.+=(Season(Range(Calendar.SATURDAY, Calendar.SATURDAY), SeasonType.DAY))
    emaSeasons.+=(Season(Range(Calendar.SUNDAY, Calendar.SUNDAY), SeasonType.DAY))

    //Hour of the day
    //24 Hours * 7 Days
    for (day <- Calendar.SUNDAY to Calendar.SATURDAY) {
      for (hour <- 1 to 24) {
        emaSeasons.+=(Season(Range(day, day), Range(hour - 1, hour)))
      }
    }

    EMA_SEASONS = emaSeasons.toList
  }

  def runEma(metric: TimelineMetric, emaAccumulator: EmaAccumulator): (List[PointInTimeAnomalyInstance]) = {

    System.out.println("runEma invoked for [ " + metric.getMetricName + ", " + metric.getHostName + ", " + metric.getAppId + "]")
    System.out.println("Before Size of Accumulator Map : " + emaAccumulator.value.size)

    var metricModelMap: Map[Season, EmaModel] = Map.empty[Season, EmaModel]
    if (emaAccumulator.value.contains(metric)) {
      metricModelMap = emaAccumulator.value.apply(metric)
    }

    val anomalies: scala.collection.mutable.MutableList[PointInTimeAnomalyInstance] = scala.collection.mutable.MutableList()
    val toBeUpdated: EmaDataStructure = scala.collection.mutable.Map.empty[TimelineMetric, Map[Season, EmaModel]]

    for ((timestamp: java.lang.Long, metricValue: java.lang.Double) <- metric.getMetricValues.asScala) {
      val seasons: List[Season] = Season.getSeasons(timestamp, EMA_SEASONS)
      System.out.println("Selected Seasons for this timestamp : ")
      for (s <- seasons) {
        System.out.println("Season :" + s.toString)
        var model: EmaModel = new EmaModel(s)

        if (metricModelMap.contains(s)) {
          System.out.println("Ema Model already found for the season.")
          model = metricModelMap.apply(s)
        }
        val anomalyScore: Double = model.updateAndTest(timestamp, metricValue)

        if (anomalyScore > 0.0) {
          anomalies.+=(
            new PointInTimeAnomalyInstance(MetricKey(metric.getMetricName, metric.getAppId, metric.getInstanceId, metric.getHostName, Array.emptyByteArray),
              timestamp,
              metricValue,
              AnomalyDetectionMethod.EMA,
              anomalyScore,
              s,
              model.getModelParameters)
          )
        }
      }
    }

    toBeUpdated(metric) = metricModelMap
    emaAccumulator.add(toBeUpdated)
    System.out.println("After Size of Accumulator Map : " + emaAccumulator.value.size)
    anomalies.toList
  }


}
