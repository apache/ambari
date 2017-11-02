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

import java.util.Calendar

import org.scalatest.FunSuite

class SeasonTest extends FunSuite {

  test("testBelongsTo") {

    //Create Season for weekdays. Mon to Friday and 9AM - 5PM
    var season : Season = Season(Range(Calendar.MONDAY,Calendar.FRIDAY), Range(9,17))

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
    season = Season(Range(Calendar.MONDAY,Calendar.MONDAY), Range(11,12))
    c.set(2017, Calendar.OCTOBER, 30, 9, 0, 0)
    assert(!season.belongsTo(c.getTimeInMillis))

    c.set(2017, Calendar.OCTOBER, 30, 11, 30, 0)
    assert(season.belongsTo(c.getTimeInMillis))


    //Create Season from Friday to Monday and 9AM - 5PM
    season = Season(Range(Calendar.FRIDAY,Calendar.MONDAY), Range(9,17))

    //Try with a timestamp on a Monday, @ 9AM.
    c.set(2017, Calendar.OCTOBER, 30, 9, 0, 0)
    assert(season.belongsTo(c.getTimeInMillis))

    //Try with a timestamp on a Sunday, @ 3PM.
    c.set(2017, Calendar.OCTOBER, 29, 15, 0, 0)
    assert(season.belongsTo(c.getTimeInMillis))

    //Try with a timestamp on a Wednesday, @ 9AM.
    c.set(2017, Calendar.NOVEMBER, 1, 9, 0, 0)
    assert(!season.belongsTo(c.getTimeInMillis))
  }

  test("testEquals") {

    var season1: Season =  Season(Range(4,5), Range(2,3))
    var season2: Season =  Season(Range(4,5), Range(2,3))
    assert(season1 == season2)

    var season3: Season =  Season(Range(4,4), Range(2,3))
    assert(!(season1 == season3))
  }

  test("testSerialize") {
    val season1 : Season = Season(Range(Calendar.MONDAY,Calendar.FRIDAY), Range(9,17))

    val seasonString = Season.toJson(season1)

    val season2 : Season = Season.fromJson(seasonString)
    assert(season1 == season2)

    val season3 : Season = Season(Range(Calendar.MONDAY,Calendar.THURSDAY), Range(9,17))
    assert(!(season2 == season3))

  }

}
