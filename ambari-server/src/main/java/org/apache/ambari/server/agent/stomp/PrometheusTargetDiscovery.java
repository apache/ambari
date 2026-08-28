/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.agent.stomp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.events.TelemetryUpdateEvent;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Renders Prometheus HTTP service-discovery groups from cached Agent assignments.
 */
@Singleton
public class PrometheusTargetDiscovery {
  static final String AGENT_PORT_PROPERTY = "prometheus.agent.metrics.port";
  static final int DEFAULT_AGENT_PORT = 9101;

  private static final Logger LOG = LoggerFactory.getLogger(PrometheusTargetDiscovery.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Clusters clusters;
  private final TelemetryHolder telemetryHolder;
  private final int agentPort;
  private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

  @Inject
  public PrometheusTargetDiscovery(Clusters clusters, TelemetryHolder telemetryHolder,
      Configuration configuration) {
    this(clusters, telemetryHolder, resolveAgentPort(configuration));
  }

  PrometheusTargetDiscovery(Clusters clusters, TelemetryHolder telemetryHolder, int agentPort) {
    this.clusters = clusters;
    this.telemetryHolder = telemetryHolder;
    this.agentPort = agentPort;
  }

  public Result discover(String clusterName) throws AmbariException {
    Cluster cluster = clusters.getCluster(clusterName);
    List<HostAssignment> assignments = new ArrayList<>();
    List<Host> hosts = new ArrayList<>(cluster.getHosts());
    hosts.sort(Comparator.comparing(Host::getHostName));

    StringBuilder revision = new StringBuilder();
    revision.append(cluster.getClusterId()).append(':').append(agentPort);
    for (Host host : hosts) {
      TelemetryUpdateEvent event = telemetryHolder.initializeDataIfNeeded(host.getHostId(), true);
      assignments.add(new HostAssignment(host, event));
      revision.append('|').append(host.getHostName()).append(':').append(event.getHash());
    }

    String sourceRevision = sha256(revision.toString().getBytes(StandardCharsets.UTF_8));
    CacheEntry cached = cache.get(clusterName);
    if (cached != null && cached.sourceRevision.equals(sourceRevision)) {
      return cached.result;
    }

    Result result = render(cluster, assignments);
    cache.put(clusterName, new CacheEntry(sourceRevision, result));
    return result;
  }

  private Result render(Cluster cluster, List<HostAssignment> assignments)
      throws AmbariException {
    ArrayNode groups = JsonNodeFactory.instance.arrayNode();
    for (HostAssignment hostAssignment : assignments) {
      String address = renderAddress(hostAssignment.host.getHostName());
      groups.add(targetGroup(address, "/metrics", cluster.getClusterName(),
          hostAssignment.host.getHostName(), null, null, "host"));

      JsonNode targets = hostAssignment.event.getAssignment().path("targets");
      if (!targets.isArray()) {
        throw new AmbariException("Invalid cached telemetry assignment for host "
            + hostAssignment.host.getHostName());
      }
      for (JsonNode target : targets) {
        if (target.path("clusterId").asLong(-1) != cluster.getClusterId()) {
          continue;
        }
        String routeId = target.path("id").asText();
        groups.add(targetGroup(address, "/metrics/components/" + routeId,
            cluster.getClusterName(), hostAssignment.host.getHostName(),
            target.path("service").asText(), target.path("component").asText(),
            "component"));
      }
    }

    try {
      String body = MAPPER.writeValueAsString(groups);
      return new Result(body, sha256(body.getBytes(StandardCharsets.UTF_8)));
    } catch (JsonProcessingException e) {
      throw new AmbariException("Unable to render Prometheus target discovery", e);
    }
  }

  private ObjectNode targetGroup(String address, String metricsPath, String clusterName,
      String hostName, String serviceName, String componentName, String targetType) {
    ObjectNode group = JsonNodeFactory.instance.objectNode();
    group.putArray("targets").add(address);
    ObjectNode labels = group.putObject("labels");
    labels.put("__metrics_path__", metricsPath);
    labels.put("cluster", clusterName);
    labels.put("host", hostName);
    labels.put("ambari_target", targetType);
    if (serviceName != null) {
      labels.put("service", serviceName);
    }
    if (componentName != null) {
      labels.put("component", componentName);
    }
    return group;
  }

  private String renderAddress(String hostName) {
    String renderedHost = hostName.contains(":") ? "[" + hostName + "]" : hostName;
    return renderedHost + ":" + agentPort;
  }

  private static int resolveAgentPort(Configuration configuration) {
    String configured = configuration.getProperty(AGENT_PORT_PROPERTY);
    if (configured == null || configured.trim().isEmpty()) {
      return DEFAULT_AGENT_PORT;
    }
    try {
      int port = Integer.parseInt(configured);
      if (port >= 1 && port <= 65535) {
        return port;
      }
    } catch (NumberFormatException ignored) {
      // The warning below handles malformed and out-of-range values uniformly.
    }
    LOG.warn("Invalid {} value '{}'; using {}", AGENT_PORT_PROPERTY, configured,
        DEFAULT_AGENT_PORT);
    return DEFAULT_AGENT_PORT;
  }

  private static String sha256(byte[] value) throws AmbariException {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
      StringBuilder result = new StringBuilder();
      for (byte item : digest) {
        result.append(String.format(Locale.ROOT, "%02x", item));
      }
      return result.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new AmbariException("SHA-256 is unavailable", e);
    }
  }

  public static final class Result {
    private final String body;
    private final String etag;

    private Result(String body, String etag) {
      this.body = body;
      this.etag = etag;
    }

    public String getBody() {
      return body;
    }

    public String getEtag() {
      return etag;
    }
  }

  private static final class HostAssignment {
    private final Host host;
    private final TelemetryUpdateEvent event;

    private HostAssignment(Host host, TelemetryUpdateEvent event) {
      this.host = host;
      this.event = event;
    }
  }

  private static final class CacheEntry {
    private final String sourceRevision;
    private final Result result;

    private CacheEntry(String sourceRevision, Result result) {
      this.sourceRevision = sourceRevision;
      this.result = result;
    }
  }
}
