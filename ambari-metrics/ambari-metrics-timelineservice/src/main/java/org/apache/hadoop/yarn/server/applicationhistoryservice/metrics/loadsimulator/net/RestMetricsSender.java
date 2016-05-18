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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.loadsimulator.net;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.concurrent.TimeUnit;

/**
 * Implements MetricsSender and provides a way of pushing metrics to application metrics history service using REST
 * endpoint.
 */
public class RestMetricsSender implements MetricsSender {
  private final static Logger LOG = LoggerFactory.getLogger(RestMetricsSender.class);

  private final static String COLLECTOR_URL = "http://%s/ws/v1/timeline/metrics";
  private final String collectorServiceAddress;

  /**
   * Creates unconnected RestMetricsSender with endpoint configured as
   * http://${metricsHost}:6188/ws/v1/timeline/metrics,
   * where ${metricsHost} is specified by metricHost param.
   *
   * @param metricsHost the hostname that will be used to access application metrics history service.
   */
  public RestMetricsSender(String metricsHost) {
    collectorServiceAddress = String.format(COLLECTOR_URL, metricsHost);
  }

  /**
   * Push metrics to the REST endpoint. Connection is always open and closed on every call.
   *
   * @param payload the payload with metrics to be sent to metrics service
   * @return response message either acknowledgement or error, empty on exception
   */
  @Override
  public String pushMetrics(String payload) {
    String responseString = "";
    UrlService svc = null;
    Stopwatch timer = new Stopwatch().start();

    try {
      LOG.info("server: {}", collectorServiceAddress);

      svc = getConnectedUrlService();
      responseString = svc.send(payload);

      timer.stop();
      LOG.info("http response time: " + timer.elapsed(TimeUnit.MILLISECONDS)
        + " ms");

      if (responseString.length() > 0) {
        LOG.debug("POST response from server: " + responseString);
      }
    } catch (IOException e) {
      LOG.error("", e);
    } finally {
      if (svc != null) {
        svc.disconnect();
      }
    }

    return responseString;
  }

  /**
   * Relaxed to protected for testing.
   */
  protected UrlService getConnectedUrlService() throws IOException {
    return UrlService.newConnection(collectorServiceAddress);
  }

}
