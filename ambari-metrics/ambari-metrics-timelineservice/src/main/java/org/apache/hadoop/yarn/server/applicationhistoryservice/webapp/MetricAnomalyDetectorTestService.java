/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.yarn.server.applicationhistoryservice.webapp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.ambari.metrics.alertservice.prototype.MetricAnomalyDetectorTestInput;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.api.records.timeline.TimelinePutResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@Path("/ws/v1/metrictestservice")
public class MetricAnomalyDetectorTestService {

  private static final Log LOG = LogFactory.getLog(MetricAnomalyDetectorTestService.class);

  @Inject
  public MetricAnomalyDetectorTestService() {
  }

  private void init(HttpServletResponse response) {
    response.setContentType(null);
  }

  @Path("/anomaly")
  @POST
  @Consumes({ MediaType.APPLICATION_JSON /* , MediaType.APPLICATION_XML */})
  public TimelinePutResponse postAnomalyDetectionRequest(
    @Context HttpServletRequest req,
    @Context HttpServletResponse res,
    MetricAnomalyDetectorTestInput input) {

    init(res);
    if (input == null) {
      return new TimelinePutResponse();
    }

    try {
      return null;
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/dataseries")
  @Produces({MediaType.APPLICATION_JSON})
  public TimelineMetrics getTestDataSeries(
    @Context HttpServletRequest req,
    @Context HttpServletResponse res,
    @QueryParam("type") String seriesType,
    @QueryParam("configs") String config
  ) {
    return null;
  }
}
