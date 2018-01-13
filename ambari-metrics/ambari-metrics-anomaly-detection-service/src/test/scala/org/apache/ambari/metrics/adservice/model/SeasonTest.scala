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

import java.util.Calendar

import org.apache.ambari.metrics.adservice.model
import org.scalatest.FunSuite

class SeasonTest extends FunSuite {

  test("testBelongsTo") {

    //Create Season for weekdays. Mon to Friday and 9AM - 5PM
    var season : Season = Season(model.Range(Calendar.MONDAY,Calendar.FRIDAY), model.Range(9,17))

    //Try with a timestamp on a Monday, @ 9AM.
    val c = Calendar.getInstance
    c.set(2017, Calendar.OCTOBER, 30, 9, 0, 0)
    assert(season.belongsTo(c.getTimeInMillis))

    c.set(2017, Calendar.OCTOBER, 30, 18, 0, 0)
    assert(!season.belongsTo(c.getTimeInMillis))

    //Try with a timestamp on a Sunday, @ 9AM.
    c.set(2017, Calendar.OCTOBER, 29, 9, 0, 0)
    assert(!season.belongsTo(c.getTimeInMillis))

    //Create Season for Monday 11AM - 12Noon.
    season = Season(model.Range(Calendar.MONDAY,Calendar.MONDAY), model.Range(11,12))
    c.set(2017, Calendar.OCTOBER, 30, 9, 0, 0)
    assert(!season.belongsTo(c.getTimeInMillis))

    c.set(2017, Calendar.OCTOBER, 30, 11, 30, 0)
    assert(season.belongsTo(c.getTimeInMillis))


    //Create Season from Friday to Monday and 9AM - 5PM
    season = Season(model.Range(Calendar.FRIDAY,Calendar.MONDAY), model.Range(9,17))

    //Try with a timestamp on a Monday, @ 9AM.
    c.set(2017, Calendar.OCTOBER, 30, 9, 0, 0)
    assert(season.belongsTo(c.getTimeInMillis))

    //Try with a timestamp on a Sunday, @ 3PM.
    c.set(2017, Calendar.OCTOBER, 29, 15, 0, 0)
    assert(season.belongsTo(c.getTimeInMillis))

    //Try with a timestamp on a Wednesday, @ 9AM.
    c.set(2017, Calendar.NOVEMBER, 1, 9, 0, 0)
    assert(!season.belongsTo(c.getTimeInMillis))

    val ts : Long = 1513804071420l
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

    Season.getSeasons(ts, emaSeasons.toList)
  }

  test("testEquals") {

    var season1: Season =  Season(model.Range(4,5), model.Range(2,3))
    var season2: Season =  Season(model.Range(4,5), model.Range(2,3))
    assert(season1 == season2)

    var season3: Season =  Season(model.Range(4,4), model.Range(2,3))
    assert(!(season1 == season3))
  }

  test("testSerialize") {
    val season1 : Season = Season(model.Range(Calendar.MONDAY,Calendar.FRIDAY), model.Range(9,17))

    val seasonString = Season.toJson(season1)

    val season2 : Season = Season.fromJson(seasonString)
    assert(season1 == season2)

    val season3 : Season = Season(model.Range(Calendar.MONDAY,Calendar.THURSDAY), model.Range(9,17))
    assert(!(season2 == season3))

  }

}
