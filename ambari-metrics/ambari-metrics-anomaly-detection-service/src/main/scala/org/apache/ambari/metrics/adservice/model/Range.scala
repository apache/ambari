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

/**
  * Class to capture a Range in a Season.
  * For example Monday - Wednesday is a 'Range' in a DAY Season.
  * @param lower lower end
  * @param higher higher end
  */
case class Range (lower: Int, higher: Int) {

  def withinHourRange(value: Int) : Boolean = {
    if (lower <= higher) {
      (value >= lower) && (value < higher)
    } else {
      !(value >= higher) && (value < lower)
    }
  }

  def withinRange(value: Int) : Boolean = {
    if (lower <= higher) {
      (value >= lower) && (value <= higher)
    } else {
      !(value > higher) && (value < lower)
    }
  }

  @Override
  override def equals(obj: scala.Any): Boolean = {
    if (obj == null) {
      return false
    }
    val that : Range = obj.asInstanceOf[Range]
    (lower == that.lower) && (higher == that.higher)
  }
}
