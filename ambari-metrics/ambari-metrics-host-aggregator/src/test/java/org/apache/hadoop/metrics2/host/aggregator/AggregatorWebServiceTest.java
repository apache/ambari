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


import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly2.GrizzlyTestContainerFactory;
import junit.framework.Assert;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.junit.Test;


import javax.ws.rs.core.MediaType;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class AggregatorWebServiceTest extends JerseyTest {
    public AggregatorWebServiceTest() {
        super(new WebAppDescriptor.Builder(
                "org.apache.hadoop.metrics2.host.aggregator")
                .contextPath("jersey-guice-filter")
                .servletPath("/")
                .clientConfig(new DefaultClientConfig(JacksonJaxbJsonProvider.class))
                .build());
    }

    @Override
    public TestContainerFactory getTestContainerFactory() {
        return new GrizzlyTestContainerFactory();
    }

    @Test
    public void testOkResponse() {
        WebResource r = resource();
        ClientResponse response = r.path("ws").path("v1").path("timeline").path("metrics")
                .accept("text/json")
                .get(ClientResponse.class);
        assertEquals(200, response.getStatus());
        assertEquals("text/json", response.getType().toString());
    }

    @Test
    public void testWrongPath() {
        WebResource r = resource();
        ClientResponse response = r.path("ws").path("v1").path("timeline").path("metrics").path("aggregated")
                .accept("text/json")
                .get(ClientResponse.class);
        assertEquals(404, response.getStatus());
    }


    @Test
    public void testMetricsPost() {
        TimelineMetricsHolder timelineMetricsHolder = TimelineMetricsHolder.getInstance();

        timelineMetricsHolder.extractMetricsForAggregationPublishing();
        timelineMetricsHolder.extractMetricsForRawPublishing();

        TimelineMetrics timelineMetrics = TimelineMetricsHolderTest.getTimelineMetricsWithAppID("appid");
        WebResource r = resource();
        ClientResponse response = r.path("ws").path("v1").path("timeline").path("metrics")
                .type(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_PLAIN)
                .post(ClientResponse.class, timelineMetrics);
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN, response.getType().toString());

        Map<String, TimelineMetrics> aggregationMap =  timelineMetricsHolder.extractMetricsForAggregationPublishing();
        Map<String, TimelineMetrics> rawMap =  timelineMetricsHolder.extractMetricsForRawPublishing();

        Assert.assertEquals(1, aggregationMap.size());
        Assert.assertEquals(1, rawMap.size());

        Collection<TimelineMetrics> aggregationCollection = aggregationMap.values();
        Collection<TimelineMetrics> rawCollection = rawMap.values();

        Collection<String> aggregationCollectionKeys = aggregationMap.keySet();
        Collection<String> rawCollectionKeys = rawMap.keySet();

        for (String key : aggregationCollectionKeys) {
            Assert.assertTrue(key.contains("appid"));
        }

        for (String key : rawCollectionKeys) {
            Assert.assertTrue(key.contains("appid"));
        }

        Assert.assertEquals(1, aggregationCollection.size());
        Assert.assertEquals(1, rawCollection.size());

        TimelineMetrics aggregationTimelineMetrics = (TimelineMetrics) aggregationCollection.toArray()[0];
        TimelineMetrics rawTimelineMetrics = (TimelineMetrics) rawCollection.toArray()[0];


        Assert.assertEquals(1, aggregationTimelineMetrics.getMetrics().size());
        Assert.assertEquals(1, rawTimelineMetrics.getMetrics().size());

        Assert.assertEquals("appid", aggregationTimelineMetrics.getMetrics().get(0).getAppId());
        Assert.assertEquals("appid", rawTimelineMetrics.getMetrics().get(0).getAppId());

        aggregationMap =  timelineMetricsHolder.extractMetricsForAggregationPublishing();
        rawMap =  timelineMetricsHolder.extractMetricsForRawPublishing();

        //Cache should be empty after extraction
        Assert.assertEquals(0, aggregationMap.size());
        Assert.assertEquals(0, rawMap.size());
    }


}
