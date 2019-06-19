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

import org.apache.commons.configuration2.SubsetConfiguration;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.metrics2.AbstractMetric;
import org.apache.hadoop.metrics2.MetricType;
import org.apache.hadoop.metrics2.MetricsRecord;
import org.apache.hadoop.metrics2.MetricsSink;
import org.apache.hadoop.metrics2.MetricsTag;
import org.apache.hadoop.metrics2.impl.MsInfo;
import org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache;
import org.apache.hadoop.net.DNS;
import java.io.Closeable;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@InterfaceAudience.Public
@InterfaceStability.Evolving
public class HadoopTimelineMetricsSink extends AbstractTimelineMetricsSink implements MetricsSink, Closeable {
  private Map<String, Set<String>> useTagsMap = new HashMap<String, Set<String>>();
  private TimelineMetricsCache metricsCache;
  private String hostName = "UNKNOWN.example.com";
  private String instanceId = null;
  private boolean setInstanceId;
  private String serviceName = "";
  private Collection<String> collectorHosts;
  private String collectorUri;
  private String containerMetricsUri;
  private String protocol;
  private String port;
  public static final String WS_V1_CONTAINER_METRICS = "/ws/v1/timeline/containermetrics";

  private static final String SERVICE_NAME_PREFIX = "serviceName-prefix";
  private static final String SERVICE_NAME = "serviceName";
  private int timeoutSeconds = 10;
  private SubsetConfiguration conf;
  // Cache the rpc port used and the suffix to use if the port tag is found
  private Map<String, String> rpcPortSuffixes = new HashMap<>(10);

  private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
    public Thread newThread(Runnable r) {
      Thread t = Executors.defaultThreadFactory().newThread(r);
      t.setDaemon(true);
      return t;
    }
  });
  private int hostInMemoryAggregationPort;
  private boolean hostInMemoryAggregationEnabled;
  private String hostInMemoryAggregationProtocol;

  @Override
  public void init(SubsetConfiguration conf) {
    this.conf = conf;
    LOG.info("Initializing Timeline metrics sink.");

    // Take the hostname from the DNS class.
    if (conf.getString("slave.host.name") != null) {
      hostName = conf.getString("slave.host.name");
    } else {
      try {
        hostName = DNS.getDefaultHost(
            conf.getString("dfs.datanode.dns.interface", "default"),
            conf.getString("dfs.datanode.dns.nameserver", "default"));
      } catch (UnknownHostException uhe) {
        LOG.error(uhe);
        hostName = "UNKNOWN.example.com";
      }
    }

    serviceName = getServiceName(conf);
    instanceId = conf.getString(INSTANCE_ID_PROPERTY, null);
    setInstanceId = conf.getBoolean(SET_INSTANCE_ID_PROPERTY, false);

    LOG.info("Identified hostname = " + hostName + ", serviceName = " + serviceName);
    // Initialize the collector write strategy
    super.init();

    // Load collector configs
    protocol = conf.getString(COLLECTOR_PROTOCOL, "http");
    String collectorHostStr = conf.getString(COLLECTOR_HOSTS_PROPERTY);
    String[] collectorHostArr = null;
    if(collectorHostStr !=null) {
      collectorHostArr = collectorHostStr.split(",");
    }
    collectorHosts = parseHostsStringArrayIntoCollection(collectorHostArr);
    port = conf.getString(COLLECTOR_PORT, "6188");
    hostInMemoryAggregationEnabled = conf.getBoolean(HOST_IN_MEMORY_AGGREGATION_ENABLED_PROPERTY, false);
    hostInMemoryAggregationPort = conf.getInt(HOST_IN_MEMORY_AGGREGATION_PORT_PROPERTY, 61888);
    hostInMemoryAggregationProtocol = conf.getString(HOST_IN_MEMORY_AGGREGATION_PROTOCOL_PROPERTY, "http");
    if (collectorHosts.isEmpty()) {
      LOG.error("No Metric collector configured.");
    } else {
      if (protocol.contains("https") || hostInMemoryAggregationProtocol.contains("https")) {
        String trustStorePath = conf.getString(SSL_KEYSTORE_PATH_PROPERTY).trim();
        String trustStoreType = conf.getString(SSL_KEYSTORE_TYPE_PROPERTY).trim();
        String trustStorePwd = conf.getString(SSL_KEYSTORE_PASSWORD_PROPERTY).trim();
        loadTruststore(trustStorePath, trustStoreType, trustStorePwd);
      }
      String preferredCollectorHost = findPreferredCollectHost();
      collectorUri = constructTimelineMetricUri(protocol, preferredCollectorHost, port);
      containerMetricsUri = constructContainerMetricUri(protocol, preferredCollectorHost, port);

      if (StringUtils.isNotEmpty(preferredCollectorHost)) {
        LOG.info("Collector Uri: " + collectorUri);
        LOG.info("Container Metrics Uri: " + containerMetricsUri);
      } else {
        LOG.info("No suitable collector found.");
      }
    }


    timeoutSeconds = conf.getInt(METRICS_POST_TIMEOUT_SECONDS, DEFAULT_POST_TIMEOUT_SECONDS);

    int maxRowCacheSize = conf.getInt(MAX_METRIC_ROW_CACHE_SIZE,
      TimelineMetricsCache.MAX_RECS_PER_NAME_DEFAULT);
    int metricsSendInterval = conf.getInt(METRICS_SEND_INTERVAL,
      TimelineMetricsCache.MAX_EVICTION_TIME_MILLIS); // ~ 1 min
    // Skip aggregation of counter values by calculating derivative
    metricsCache = new TimelineMetricsCache(maxRowCacheSize,
      metricsSendInterval, conf.getBoolean(SKIP_COUNTER_TRANSFROMATION, true));

    conf.setListDelimiterHandler(new DefaultListDelimiterHandler(','));
    Iterator<String> it = (Iterator<String>) conf.getKeys();
    while (it.hasNext()) {
      String propertyName = it.next();
      if (propertyName != null) {
        if (propertyName.startsWith(TAGS_FOR_PREFIX_PROPERTY_PREFIX)) {
          String contextName = propertyName.substring(TAGS_FOR_PREFIX_PROPERTY_PREFIX.length());
          String[] tags = conf.getStringArray(propertyName);
          boolean useAllTags = false;
          Set<String> set = null;
          if (tags.length > 0) {
            set = new HashSet<String>();
            for (String tag : tags) {
              tag = tag.trim();
              useAllTags |= tag.equals("*");
              if (tag.length() > 0) {
                set.add(tag);
              }
            }
            if (useAllTags) {
              set = null;
            }
          }
          useTagsMap.put(contextName, set);
        }
        // Customized RPC ports
        if (propertyName.startsWith(RPC_METRIC_PREFIX)) {
          // metric.rpc.client.port
          int beginIdx = RPC_METRIC_PREFIX.length() + 1;
          String suffixStr = propertyName.substring(beginIdx); // client.port
          String configPrefix = suffixStr.substring(0, suffixStr.indexOf(".")); // client
          rpcPortSuffixes.put(conf.getString(propertyName).trim(), configPrefix.trim());
        }
      }
    }

    if (!rpcPortSuffixes.isEmpty()) {
      LOG.info("RPC port properties configured: " + rpcPortSuffixes);
    }
  }

  /**
   * Return configured serviceName with or without prefix.
   * Default without serviceName or configured prefix : first config prefix
   * With prefix : configured prefix + first config prefix
   * Configured serviceName : Return serviceName as is.
   */
  private String getServiceName(SubsetConfiguration conf) {
    String serviceNamePrefix = conf.getString(SERVICE_NAME_PREFIX, "");
    String serviceName = conf.getString(SERVICE_NAME, "");
    return StringUtils.isEmpty(serviceName) ?
      StringUtils.isEmpty(serviceNamePrefix) ? getFirstConfigPrefix(conf)
        : serviceNamePrefix + "-" + getFirstConfigPrefix(conf) : serviceName;
  }

  private String getFirstConfigPrefix(SubsetConfiguration conf) {
    while (conf.getParent() instanceof SubsetConfiguration) {
      conf = (SubsetConfiguration) conf.getParent();
    }
    return conf.getPrefix();
  }

  /**
   * Parses input Stings array of format "host1,host2" into Collection of hostnames
   */
  protected Collection<String>  parseHostsStringArrayIntoCollection(String[] hostStrings) {
    Collection<String> result = new HashSet<>();
    if (hostStrings == null) return result;
    for (String s : hostStrings) {
      result.add(s.trim());
    }
    return result;
  }

  @Override
  protected String getCollectorUri(String host) {
    return constructTimelineMetricUri(protocol, host, port);
  }

  @Override
  protected String getCollectorProtocol() {
    return protocol;
  }

  @Override
  protected int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  @Override
  protected String getZookeeperQuorum() {
    return conf.getString(ZOOKEEPER_QUORUM);
  }

  @Override
  protected Collection<String> getConfiguredCollectorHosts() {
    return collectorHosts;
  }

  @Override
  protected String getCollectorPort() {
    return port;
  }
  @Override
  protected String getHostname() {
    return hostName;
  }

  @Override
  protected boolean isHostInMemoryAggregationEnabled() {
    return hostInMemoryAggregationEnabled;
  }

  @Override
  protected int getHostInMemoryAggregationPort() {
    return hostInMemoryAggregationPort;
  }

  @Override
  protected String getHostInMemoryAggregationProtocol() {
    return hostInMemoryAggregationProtocol;
  }

  @Override
  public void putMetrics(MetricsRecord record) {
    try {
      String recordName = record.name();
      String contextName = record.context();

      StringBuilder sb = new StringBuilder();
      boolean skipAggregation = false;

      // Transform ipc.8020 -> ipc.client,  ipc.8040 -> ipc.datanode, etc.
      if (contextName.startsWith("ipc.")) {
        String portNumber = contextName.replaceFirst("ipc.", "");
        if (rpcPortSuffixes.containsKey(portNumber)) {
          contextName = "ipc." + rpcPortSuffixes.get(portNumber);
        }
      }

      sb.append(contextName);
      sb.append('.');

      if (record.tags() != null) {
        for (MetricsTag tag : record.tags()) {
          if (StringUtils.isNotEmpty(tag.name()) && tag.name().equals("skipAggregation")) {
            skipAggregation = String.valueOf(true).equals(tag.value());
          }
                // Similar to GangliaContext adding processName to distinguish jvm
                // metrics for co-hosted daemons. We only do this for HBase since the
                // appId is shared for Master and RS.
          if (contextName.equals("jvm") && tag.info().name().equalsIgnoreCase("processName") &&
            (tag.value().equals("RegionServer") || tag.value().equals("Master"))) {
            sb.append(tag.value());
            sb.append('.');
          }
        }
      }

      sb.append(recordName);
      appendPrefix(record, sb);
      sb.append('.');

      // Add port tag for rpc metrics to distinguish rpc calls based on port
      if (!rpcPortSuffixes.isEmpty() && contextName.contains("rpc")) {
        if (record.tags() != null) {
          for (MetricsTag tag : record.tags()) {
            if (tag.info().name().equalsIgnoreCase("port") &&
                rpcPortSuffixes.keySet().contains(tag.value())) {
              sb.append(rpcPortSuffixes.get(tag.value()));
              sb.append('.');
            }
          }
        }
      }

      if (record.context().equals("container")) {
        emitContainerMetrics(record);
        return;
      }

      int sbBaseLen = sb.length();
      List<TimelineMetric> metricList = new ArrayList<TimelineMetric>();
      HashMap<String, String> metadata = null;
      if (skipAggregation) {
        metadata = new HashMap<>();
        metadata.put("skipAggregation", "true");
      }
      long startTime = record.timestamp();

      for (AbstractMetric metric : record.metrics()) {
        sb.append(metric.name());
        String name = sb.toString();
        Number value = metric.value();
        TimelineMetric timelineMetric = new TimelineMetric();
        timelineMetric.setMetricName(name);
        timelineMetric.setHostName(hostName);
        timelineMetric.setAppId(serviceName);
        if (setInstanceId) {
          timelineMetric.setInstanceId(instanceId);
        }
        timelineMetric.setStartTime(startTime);
        timelineMetric.setType(metric.type() != null ? metric.type().name() : null);
        timelineMetric.getMetricValues().put(startTime, value.doubleValue());
        if (metadata != null) {
          timelineMetric.setMetadata(metadata);
        }
        // Put intermediate values into the cache until it is time to send
        boolean isCounter = MetricType.COUNTER == metric.type();
        metricsCache.putTimelineMetric(timelineMetric, isCounter);

        // Retrieve all values from cache if it is time to send
        TimelineMetric cachedMetric = metricsCache.getTimelineMetric(name);

        if (cachedMetric != null) {
          metricList.add(cachedMetric);
        }

        sb.setLength(sbBaseLen);
      }

      TimelineMetrics timelineMetrics = new TimelineMetrics();
      timelineMetrics.setMetrics(metricList);

      if (!metricList.isEmpty()) {
        emitMetrics(timelineMetrics);
      }
    } catch (UnableToConnectException uce) {
      LOG.warn("Unable to send metrics to collector by address:" + uce.getConnectUrl());
    }
  }

  private void parseContainerMetrics(MetricsRecord record,
      ContainerMetric containerMetric) {
    for (AbstractMetric metric : record.metrics() ) {
      switch (metric.name()) {
      case "PMemUsageMBsAvgMBs":
        containerMetric.setPmemUsedAvg(metric.value().intValue());
        break;
      case "PMemUsageMBsMinMBs":
        containerMetric.setPmemUsedMin(metric.value().intValue());
        break;
      case "PMemUsageMBsMaxMBs":
        containerMetric.setPmemUsedMax(metric.value().intValue());
        break;
      case "PMemUsageMBHistogram50thPercentileMBs":
        containerMetric.setPmem50Pct(metric.value().intValue());
        break;
      case "PMemUsageMBHistogram75thPercentileMBs":
        containerMetric.setPmem75Pct(metric.value().intValue());
        break;
      case "PMemUsageMBHistogram90thPercentileMBs":
        containerMetric.setPmem90Pct(metric.value().intValue());
        break;
      case "PMemUsageMBHistogram95thPercentileMBs":
        containerMetric.setPmem95Pct(metric.value().intValue());
        break;
      case "PMemUsageMBHistogram99thPercentileMBs":
        containerMetric.setPmem99Pct(metric.value().intValue());
        break;
      case "pMemLimitMBs":
        containerMetric.setPmemLimit(metric.value().intValue());
        break;
      case "vMemLimitMBs":
        containerMetric.setVmemLimit(metric.value().intValue());
        break;
      case "launchDurationMs":
        containerMetric.setLaunchDuration(metric.value().longValue());
        break;
      case "localizationDurationMs":
        containerMetric.setLocalizationDuration(metric.value().longValue());
        break;
      case "StartTime":
        containerMetric.setStartTime(metric.value().longValue());
        break;
      case "FinishTime":
        containerMetric.setFinishTime(metric.value().longValue());
        break;
      case "ExitCode":
        containerMetric.setExitCode((metric.value().intValue()));
        break;
      default:
        break;
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(containerMetric);
    }
  }

  private void emitContainerMetrics(MetricsRecord record) {

    ContainerMetric containerMetric = new ContainerMetric();
    containerMetric.setHostName(hostName);

    for (MetricsTag tag : record.tags()) {
      if (tag.name().equals("ContainerResource")) {
        containerMetric.setContainerId(tag.value());
      }
    }

    parseContainerMetrics(record, containerMetric);
    List<ContainerMetric> list = new ArrayList<>();
    list.add(containerMetric);
    String jsonData = null;
    try {
      jsonData = mapper.writeValueAsString(list);
    } catch (IOException e) {
      LOG.error("Unable to parse container metrics ", e);
    }
    if (jsonData != null) {
      String collectorHost = getCurrentCollectorHost();
      containerMetricsUri = constructContainerMetricUri(protocol, collectorHost, port);
      emitMetricsJson(containerMetricsUri, jsonData);
    }
  }

  protected String constructContainerMetricUri(String protocol, String host, String port) {
    StringBuilder sb = new StringBuilder(protocol);
    sb.append("://");
    sb.append(host);
    sb.append(":");
    sb.append(port);
    sb.append(WS_V1_CONTAINER_METRICS);
    return sb.toString();
  }

  // Taken as is from Ganglia30 implementation
  @InterfaceAudience.Private
  public void appendPrefix(MetricsRecord record, StringBuilder sb) {
    String contextName = record.context();
    Collection<MetricsTag> tags = record.tags();
    if (useTagsMap.containsKey(contextName)) {
      Set<String> useTags = useTagsMap.get(contextName);
      for (MetricsTag t : tags) {
        if (useTags == null || useTags.contains(t.name())) {

          // the context is always skipped here because it is always added

          // the hostname is always skipped to avoid case-mismatches
          // from different DNSes.

          if (t.info() != MsInfo.Context && t.info() != MsInfo.Hostname && t.value() != null) {
            sb.append('.').append(t.name()).append('=').append(t.value());
          }
        }
      }
    }
  }

  @Override
  public void flush() {
    // TODO: Buffering implementation
  }

  @Override
  public void close() throws IOException {

    executorService.submit(new Runnable() {
      @Override
      public void run() {
        LOG.debug("Closing HadoopTimelineMetricSink. Flushing metrics to collector...");
        TimelineMetrics metrics = metricsCache.getAllMetrics();
        if (metrics != null) {
          emitMetrics(metrics, true);
        }
      }
    });
    executorService.shutdown();
  }
}
