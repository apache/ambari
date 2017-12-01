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

package org.apache.ambari.metrics.adservice.model

import java.time.DayOfWeek
import java.util.Calendar

import javax.xml.bind.annotation.XmlRootElement

import org.apache.ambari.metrics.adservice.model.SeasonType.SeasonType

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

/**
  * Class to capture a 'Season' for a metric anomaly.
  * A Season is a combination of DAY Range and HOUR Range.
  * @param DAY Day Range
  * @param HOUR Hour Range
  */
@XmlRootElement
case class Season(var DAY: Range, var HOUR: Range) {

  def belongsTo(timestamp : Long) : Boolean = {
    val c = Calendar.getInstance
    c.setTimeInMillis(timestamp)
    val dayOfWeek = c.get(Calendar.DAY_OF_WEEK)
    val hourOfDay = c.get(Calendar.HOUR_OF_DAY)

    if (DAY.lower != -1 && !DAY.withinRange(dayOfWeek))
      return false
    if (HOUR.lower != -1 && !HOUR.withinRange(hourOfDay))
      return false
    true
  }

  @Override
  override def equals(obj: scala.Any): Boolean = {

    if (obj == null) {
      return false
    }

    val that : Season = obj.asInstanceOf[Season]
    DAY.equals(that.DAY) && HOUR.equals(that.HOUR)
  }

  @Override
  override def toString: String = {

    var prettyPrintString = ""

    var dLower: Int = DAY.lower - 1
    if (dLower == 0) {
      dLower = 7
    }

    var dHigher: Int = DAY.higher - 1
    if (dHigher == 0) {
      dHigher = 7
    }

    if (DAY != null) {
      prettyPrintString = prettyPrintString.concat("DAY : [" + DayOfWeek.of(dLower) + "," + DayOfWeek.of(dHigher)) + "]"
    }

    if (HOUR != null) {
      prettyPrintString = prettyPrintString.concat(" HOUR : [" + HOUR.lower + "," + HOUR.higher) + "]"
    }
    prettyPrintString
  }
}

object Season {

  def apply(DAY: Range, HOUR: Range): Season = new Season(DAY, HOUR)

  def apply(range: Range, seasonType: SeasonType): Season = {
    if (seasonType.equals(SeasonType.DAY)) {
      new Season(range, Range(-1,-1))
    } else {
      new Season(Range(-1,-1), range)
    }
  }

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  def getSeasons(timestamp: Long, seasons : List[Season]) : List[Season] = {
    val validSeasons : scala.collection.mutable.MutableList[Season] = scala.collection.mutable.MutableList.empty[Season]
    for ( season <- seasons ) {
      if (season.belongsTo(timestamp)) {
        validSeasons += season
      }
    }
    validSeasons.toList
  }

  def toJson(season: Season) : String = {
    mapper.writeValueAsString(season)
  }

  def fromJson(seasonString: String) : Season = {
    mapper.readValue[Season](seasonString)
  }
}
