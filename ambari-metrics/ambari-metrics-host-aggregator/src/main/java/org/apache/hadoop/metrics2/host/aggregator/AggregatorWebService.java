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
package org.apache.hadoop.metrics2.host.aggregator;



import com.sun.jersey.spi.resource.Singleton;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Singleton
@Path("/ws/v1/timeline")
public class AggregatorWebService {
    TimelineMetricsHolder metricsHolder = TimelineMetricsHolder.getInstance();

    @GET
    @Produces("text/json")
    @Path("/metrics")
    public Response getOkResponse() throws IOException {
        return Response.ok().build();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes({ MediaType.APPLICATION_JSON /* , MediaType.APPLICATION_XML */})
    @Path("/metrics")
    public Response postMetrics(
            TimelineMetrics metrics) {
        metricsHolder.putMetricsForAggregationPublishing(metrics);
        metricsHolder.putMetricsForRawPublishing(metrics);
        return Response.ok().build();
    }
}
