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
package org.apache.hadoop.metrics2.sink.timeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.host.aggregator.TimelineMetricsHolder;

import java.util.Collection;
import java.util.Map;

/**
 * Abstract class that runs a thread that publishes metrics data to AMS collector in specified intervals.
 */
public abstract class AbstractMetricPublisher extends AbstractTimelineMetricsSink implements Runnable {

    private static final String AMS_SITE_SSL_KEYSTORE_PATH_PROPERTY = "ssl.server.truststore.location";
    private static final String AMS_SITE_SSL_KEYSTORE_TYPE_PROPERTY = "ssl.server.truststore.password";
    private static final String AMS_SITE_SSL_KEYSTORE_PASSWORD_PROPERTY = "ssl.server.truststore.type";
    private static final String AMS_SITE_HTTP_POLICY_PROPERTY = "timeline.metrics.service.http.policy";
    private static final String AMS_SITE_COLLECTOR_WEBAPP_ADDRESS_PROPERTY = "timeline.metrics.service.webapp.address";
    private static final String PUBLISHER_COLLECTOR_HOSTS_PROPERTY = "timeline.metrics.collector.hosts";
    private static final String PUBLISHER_ZOOKEEPER_QUORUM_PROPERTY = "timeline.metrics.zk.quorum";
    private static final String PUBLISHER_HOSTNAME_PROPERTY = "timeline.metrics.hostname";
    protected static String BASE_POST_URL = "%s://%s:%s/ws/v1/timeline/metrics";
    protected int publishIntervalInSeconds;
    private Log LOG;
    protected TimelineMetricsHolder timelineMetricsHolder;
    protected Configuration configuration;
    private String collectorProtocol;
    private String collectorPort;
    private Collection<String> collectorHosts;
    private String hostname;
    private String zkQuorum;

    public AbstractMetricPublisher(TimelineMetricsHolder timelineMetricsHolder, Configuration configuration, int publishIntervalInSeconds) {
        LOG = LogFactory.getLog(this.getClass());
        this.configuration = configuration;
        this.publishIntervalInSeconds = publishIntervalInSeconds;
        this.timelineMetricsHolder = timelineMetricsHolder;
        configure();
    }

    protected void configure() {
        collectorProtocol = configuration.get(AMS_SITE_HTTP_POLICY_PROPERTY, "HTTP_ONLY").equalsIgnoreCase("HTTP_ONLY") ? "http" : "https";
        collectorPort = configuration.getTrimmed(AMS_SITE_COLLECTOR_WEBAPP_ADDRESS_PROPERTY, "0.0.0.0:6188").split(":")[1];
        collectorHosts = parseHostsStringIntoCollection(configuration.getTrimmed(PUBLISHER_COLLECTOR_HOSTS_PROPERTY, ""));
        zkQuorum = configuration.get(PUBLISHER_ZOOKEEPER_QUORUM_PROPERTY, "");
        hostname = configuration.get(PUBLISHER_HOSTNAME_PROPERTY, "localhost");
        collectorHosts = parseHostsStringIntoCollection(configuration.get(PUBLISHER_COLLECTOR_HOSTS_PROPERTY, ""));
        if (collectorHosts.isEmpty()) {
            LOG.error("No Metric collector configured.");
        } else {
            if (collectorProtocol.contains("https")) {
                String trustStorePath = configuration.get(AMS_SITE_SSL_KEYSTORE_PATH_PROPERTY).trim();
                String trustStoreType = configuration.get(AMS_SITE_SSL_KEYSTORE_TYPE_PROPERTY).trim();
                String trustStorePwd = configuration.get(AMS_SITE_SSL_KEYSTORE_PASSWORD_PROPERTY).trim();
                loadTruststore(trustStorePath, trustStoreType, trustStorePwd);
            }
        }
    }

    /**
     * Publishes metrics to collector in specified intervals while not interrupted.
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(this.publishIntervalInSeconds * 1000);
            } catch (InterruptedException e) {
                //Ignore
            }
            try {
                processAndPublishMetrics(getMetricsFromCache());
            } catch (Exception e) {
                //ignore
            }
        }
    }

    /**
     * Processes and sends metrics to collector.
     * @param metricsFromCache
     * @throws Exception
     */
    protected void processAndPublishMetrics(Map<String, TimelineMetrics> metricsFromCache) throws Exception {
        if (metricsFromCache.size()==0) return;

        LOG.info(String.format("Preparing %s timeline metrics for publishing", metricsFromCache.size()));
        emitMetricsJson(getCollectorUri(getCurrentCollectorHost()), processMetrics(metricsFromCache));
    }

    /**
     * Returns metrics map. Source is based on implementation.
     * @return
     */
    protected abstract Map<String,TimelineMetrics> getMetricsFromCache();

    /**
     * Processes given metrics (aggregates or merges them) and converts them into json string that will be send to collector
     * @param metricValues
     * @return
     */
    protected abstract String processMetrics(Map<String, TimelineMetrics> metricValues);

    protected abstract String getPostUrl();

    @Override
    protected String getCollectorUri(String host) {
        return String.format(getPostUrl(), getCollectorProtocol(), host, getCollectorPort());
    }

    @Override
    protected String getCollectorProtocol() {
        return collectorProtocol;
    }

    @Override
    protected String getCollectorPort() {
        return collectorPort;
    }

    @Override
    protected int getTimeoutSeconds() {
        return DEFAULT_POST_TIMEOUT_SECONDS;
    }

    @Override
    protected String getZookeeperQuorum() {
        return zkQuorum;
    }

    @Override
    protected Collection<String> getConfiguredCollectorHosts() {
        return collectorHosts;
    }

    @Override
    protected String getHostname() {
        return hostname;
    }

    @Override
    protected boolean isHostInMemoryAggregationEnabled() {
        return false;
    }

    @Override
    protected int getHostInMemoryAggregationPort() {
        return 0;
    }
}
