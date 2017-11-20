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
package org.apache.ambari.metrics.adservice.resource

import javax.ws.rs.core.MediaType.APPLICATION_JSON
import javax.ws.rs.core.Response
import javax.ws.rs.{GET, Path, Produces, QueryParam}

import org.apache.ambari.metrics.adservice.model.{AnomalyType, MetricAnomalyInstance}
import org.apache.ambari.metrics.adservice.model.AnomalyType.AnomalyType
import org.apache.ambari.metrics.adservice.service.ADQueryService
import org.apache.commons.lang.StringUtils

import com.google.inject.Inject

@Path("/anomaly")
class AnomalyResource {

  @Inject
  var aDQueryService: ADQueryService = _

  @GET
  @Produces(Array(APPLICATION_JSON))
  def getTopNAnomalies(@QueryParam("type") anType: String,
                       @QueryParam("startTime") startTime: Long,
                       @QueryParam("endTime") endTime: Long,
                       @QueryParam("top") limit: Int): Response = {

    val anomalies: List[MetricAnomalyInstance] = aDQueryService.getTopNAnomaliesByType(
      parseAnomalyType(anType),
      parseStartTime(startTime),
      parseEndTime(endTime),
      parseTop(limit))

    Response.ok.entity(anomalies).build()
  }

  private def parseAnomalyType(anomalyType: String) : AnomalyType = {
    if (StringUtils.isEmpty(anomalyType)) {
      return AnomalyType.POINT_IN_TIME
    }
    AnomalyType.withName(anomalyType.toUpperCase)
  }

  private def parseStartTime(startTime: Long) : Long = {
    if (startTime > 0l) {
      return startTime
    }
    System.currentTimeMillis() - 60*60*1000
  }

  private def parseEndTime(endTime: Long) : Long = {
    if (endTime > 0l) {
      return endTime
    }
    System.currentTimeMillis()
  }

  private def parseTop(limit: Int) : Int = {
    if (limit > 0) {
      return limit
    }
    5
  }
}
