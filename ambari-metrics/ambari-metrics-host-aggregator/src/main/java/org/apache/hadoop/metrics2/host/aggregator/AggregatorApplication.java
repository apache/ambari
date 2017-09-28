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
import org.apache.hadoop.metrics2.sink.timeline.AbstractMetricPublisher;
import org.apache.hadoop.metrics2.sink.timeline.AggregatedMetricsPublisher;
import org.apache.hadoop.metrics2.sink.timeline.RawMetricsPublisher;

/**
 * WEB application with 2 publisher threads that processes received metrics and submits results to the collector
 */
public class AggregatorApplication
{
    private static final int STOP_SECONDS_DELAY = 0;
    private static final int JOIN_SECONDS_TIMEOUT = 5;
    private static final String METRICS_SITE_CONFIGURATION_FILE = "ams-site.xml";
    private Log LOG;
    private final int webApplicationPort;
    private final int rawPublishingInterval;
    private final int aggregationInterval;
    private Configuration configuration;
    private Thread aggregatePublisherThread;
    private Thread rawPublisherThread;
    private TimelineMetricsHolder timelineMetricsHolder;
    private HttpServer httpServer;

    public AggregatorApplication(String hostname, String collectorHosts) {
        LOG = LogFactory.getLog(this.getClass());
        configuration = new Configuration(true);
        initConfiguration();
        configuration.set("timeline.metrics.collector.hosts", collectorHosts);
        configuration.set("timeline.metrics.hostname", hostname);
        configuration.set("timeline.metrics.zk.quorum", getZkQuorumFromConfiguration());
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

    private String getZkQuorumFromConfiguration() {
        String zkClientPort = configuration.getTrimmed("cluster.zookeeper.property.clientPort", "2181");
        String zkServerHosts = configuration.getTrimmed("cluster.zookeeper.quorum", "");
        return getZkConnectionUrl(zkClientPort, zkServerHosts);
    }

    protected void initConfiguration() {
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

        try {
            configuration.addResource(amsResUrl.toURI().toURL());
        } catch (Exception e) {
            LOG.error("Couldn't init configuration. ", e);
            System.exit(1);
        }
    }

    protected String getHostName() {
        String hostName = "localhost";
        try {
            hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            LOG.error(e);
        }
        return hostName;
    }

    protected URI getURI() {
        URI uri = UriBuilder.fromUri("http://" + getHostName() + "/").port(this.webApplicationPort).build();
        LOG.info(String.format("Web server at %s", uri));
        return uri;
    }

    protected HttpServer createHttpServer() throws IOException {
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
        AbstractMetricPublisher metricPublisher = new AggregatedMetricsPublisher(timelineMetricsHolder, configuration, aggregationInterval);
        aggregatePublisherThread = new Thread(metricPublisher);
        aggregatePublisherThread.start();
    }

    private void startRawPublisherThread() {
        LOG.info("Starting raw metrics publisher.");
        AbstractMetricPublisher metricPublisher = new RawMetricsPublisher(timelineMetricsHolder, configuration, rawPublishingInterval);
        rawPublisherThread = aggregatePublisherThread = new Thread(metricPublisher);
        aggregatePublisherThread.start();
    }



    private void stop() {
        LOG.info("Stopping aggregator application");
        aggregatePublisherThread.interrupt();
        rawPublisherThread.interrupt();
        httpServer.stop(STOP_SECONDS_DELAY);
        LOG.info("Stopped web server.");
        try {
            LOG.info("Waiting for threads to join.");
            aggregatePublisherThread.join(JOIN_SECONDS_TIMEOUT * 1000);
            rawPublisherThread.join(JOIN_SECONDS_TIMEOUT * 1000);
            LOG.info("Gracefully stopped Aggregator Application.");
        } catch (InterruptedException e) {
            LOG.error("Received exception during stop : ", e);

        }

    }

    private String getZkConnectionUrl(String zkClientPort, String zkQuorum) {
        StringBuilder sb = new StringBuilder();
        String[] quorumParts = zkQuorum.split(",");
        String prefix = "";
        for (String part : quorumParts) {
            sb.append(prefix);
            sb.append(part.trim());
            if (!part.contains(":")) {
                sb.append(":");
                sb.append(zkClientPort);
            }
            prefix = ",";
        }
        return sb.toString();
    }

    public static void main( String[] args ) throws Exception {
        if (args.length != 2) {
            throw new Exception("This jar should be executed with 2 arguments : 1st - current host name, " +
                    "2nd - collector hosts separated with coma");
        }

        final AggregatorApplication app = new AggregatorApplication(args[0], args[1]);

        app.startWebServerAndPublishersThreads();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                app.stop();
            }
        });
    }

    private void startWebServerAndPublishersThreads() {
        LOG.info("Starting aggregator application");
        startAggregatePublisherThread();
        startRawPublisherThread();
        startWebServer();
    }
}
