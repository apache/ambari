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

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

/**
 * WEB application with 2 publisher threads that processes received metrics and submits results to the collector
 */
public class AggregatorApplication
{
    private static final int STOP_SECONDS_DELAY = 0;
    private static final int JOIN_SECONDS_TIMEOUT = 2;
    private static String BASE_POST_URL = "%s://%s:%s/ws/v1/timeline/metrics";
    private static String AGGREGATED_POST_PREFIX = "/aggregated";
    private static final String METRICS_SITE_CONFIGURATION_FILE = "ams-site.xml";
    private static Log LOG = LogFactory.getLog("AggregatorApplication.class");
    private final int webApplicationPort;
    private final int rawPublishingInterval;
    private final int aggregationInterval;
    private Configuration configuration;
    private String [] collectorHosts;
    private AggregatedMetricsPublisher aggregatePublisher;
    private RawMetricsPublisher rawPublisher;
    private TimelineMetricsHolder timelineMetricsHolder;
    private HttpServer httpServer;

    public AggregatorApplication(String collectorHosts) {
        initConfiguration();
        this.collectorHosts = collectorHosts.split(",");
        this.aggregationInterval = configuration.getInt("timeline.metrics.host.aggregator.minute.interval", 300);
        this.rawPublishingInterval = configuration.getInt("timeline.metrics.sink.report.interval", 60);
        this.webApplicationPort = configuration.getInt("timeline.metrics.host.inmemory.aggregation.port", 61888);
        this.timelineMetricsHolder = TimelineMetricsHolder.getInstance(rawPublishingInterval, aggregationInterval);
        try {
            this.httpServer = createHttpServer();
        } catch (IOException e) {
            LOG.error("Exception while starting HTTP server. Exiting", e);
            System.exit(1);
        }
    }

    private void initConfiguration() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }

        URL amsResUrl = classLoader.getResource(METRICS_SITE_CONFIGURATION_FILE);
        LOG.info("Found metric service configuration: " + amsResUrl);
        if (amsResUrl == null) {
            throw new IllegalStateException("Unable to initialize the metrics " +
                    "subsystem. No ams-site present in the classpath.");
        }
        configuration = new Configuration(true);
        try {
            configuration.addResource(amsResUrl.toURI().toURL());
        } catch (Exception e) {
            LOG.error("Couldn't init configuration. ", e);
            System.exit(1);
        }
    }

    private String getHostName() {
        String hostName = "localhost";
        try {
            hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            LOG.error(e);
        }
        return hostName;
    }

    private URI getURI() {
        URI uri = UriBuilder.fromUri("http://" + getHostName() + "/").port(this.webApplicationPort).build();
        LOG.info(String.format("Web server at %s", uri));
        return uri;
    }

    private HttpServer createHttpServer() throws IOException {
        ResourceConfig resourceConfig = new PackagesResourceConfig("org.apache.hadoop.metrics2.host.aggregator");
        HashMap<String, Object> params = new HashMap();
        params.put("com.sun.jersey.api.json.POJOMappingFeature", "true");
        resourceConfig.setPropertiesAndFeatures(params);
        return HttpServerFactory.create(getURI(), resourceConfig);
    }

    private void startWebServer() {
        LOG.info("Starting web server.");
        this.httpServer.start();
    }

    private void startAggregatePublisherThread() {
        LOG.info("Starting aggregated metrics publisher.");
        String collectorURL = buildBasicCollectorURL(collectorHosts[0]) + AGGREGATED_POST_PREFIX;
        aggregatePublisher = new AggregatedMetricsPublisher(timelineMetricsHolder, collectorURL, aggregationInterval);
        aggregatePublisher.start();
    }

    private void startRawPublisherThread() {
        LOG.info("Starting raw metrics publisher.");
        String collectorURL = buildBasicCollectorURL(collectorHosts[0]);
        rawPublisher = new RawMetricsPublisher(timelineMetricsHolder, collectorURL, rawPublishingInterval);
        rawPublisher.start();
    }



    private void stop() {
        aggregatePublisher.stopPublisher();
        rawPublisher.stopPublisher();
        httpServer.stop(STOP_SECONDS_DELAY);
        LOG.info("Stopped web server.");
        try {
            LOG.info("Waiting for threads to join.");
            aggregatePublisher.join(JOIN_SECONDS_TIMEOUT * 1000);
            rawPublisher.join(JOIN_SECONDS_TIMEOUT * 1000);
            LOG.info("Gracefully stopped Aggregator Application.");
        } catch (InterruptedException e) {
            LOG.error("Received exception during stop : ", e);

        }

    }

    private String buildBasicCollectorURL(String host) {
        String port = configuration.get("timeline.metrics.service.webapp.address", "0.0.0.0:6188").split(":")[1];
        String protocol = configuration.get("timeline.metrics.service.http.policy", "HTTP_ONLY").equalsIgnoreCase("HTTP_ONLY") ? "http" : "https";
        return String.format(BASE_POST_URL, protocol, host, port);
    }

    public static void main( String[] args ) throws Exception {
        LOG.info("Starting aggregator application");
        if (args.length != 1) {
            throw new Exception("This jar should be run with 1 argument - collector hosts separated with coma");
        }

        final AggregatorApplication app = new AggregatorApplication(args[0]);
        app.startAggregatePublisherThread();
        app.startRawPublisherThread();
        app.startWebServer();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                LOG.info("Stopping aggregator application");
                app.stop();
            }
        });
    }
}
