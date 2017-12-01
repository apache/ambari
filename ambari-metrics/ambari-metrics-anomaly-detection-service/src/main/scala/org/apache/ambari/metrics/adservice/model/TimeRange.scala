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

import java.util.Date

/**
  * A special form of a 'Range' class to denote Time range.
  */
case class TimeRange (startTime: Long, endTime: Long) {
  @Override
  override def toString: String = {
    "StartTime=" + new Date(startTime) + ", EndTime=" + new Date(endTime)
  }

  @Override
  override def equals(obj: scala.Any): Boolean = {
    if (obj == null) {
      return false
    }
    val that : TimeRange = obj.asInstanceOf[TimeRange]
    (startTime == that.startTime) && (endTime == that.endTime)
  }
}
