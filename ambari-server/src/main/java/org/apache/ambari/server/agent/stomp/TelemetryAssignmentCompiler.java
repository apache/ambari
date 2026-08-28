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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.TelemetryUpdateEvent;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Resolves Stack telemetry descriptors into a host-specific Agent assignment.
 */
@Singleton
public class TelemetryAssignmentCompiler {
  static final int DEFAULT_TIMEOUT_SECONDS = 5;
  static final int DEFAULT_MAX_RESPONSE_BYTES = 32 * 1024 * 1024;
  static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 2;

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Pattern METRIC_NAME =
      Pattern.compile("^[a-zA-Z_:][a-zA-Z0-9_:]*$");
  private static final Pattern LABEL_NAME =
      Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

  private final Provider<Clusters> clusters;
  private final AmbariMetaInfo ambariMetaInfo;
  private final ConfigHelper configHelper;

  @Inject
  public TelemetryAssignmentCompiler(Provider<Clusters> clusters,
      AmbariMetaInfo ambariMetaInfo, ConfigHelper configHelper) {
    this.clusters = clusters;
    this.ambariMetaInfo = ambariMetaInfo;
    this.configHelper = configHelper;
  }

  public TelemetryUpdateEvent compile(Long hostId) throws AmbariException {
    Host host = clusters.get().getHostById(hostId);
    String hostName = host.getHostName();
    List<ObjectNode> targets = new ArrayList<>();
    Map<String, JsonNode> profiles = new TreeMap<>();

    List<Cluster> hostClusters = new ArrayList<>(clusters.get().getClustersForHost(hostName));
    hostClusters.sort(Comparator.comparingLong(Cluster::getClusterId));
    for (Cluster cluster : hostClusters) {
      compileCluster(cluster, hostName, targets, profiles);
    }

    targets.sort(Comparator.comparing(target -> target.path("id").asText()));
    ObjectNode assignment = JsonNodeFactory.instance.objectNode();
    assignment.put("schemaVersion", 1);
    ArrayNode targetArray = assignment.putArray("targets");
    targets.forEach(targetArray::add);
    return new TelemetryUpdateEvent(hostId, assignment, profiles);
  }

  private void compileCluster(Cluster cluster, String hostName, List<ObjectNode> targets,
      Map<String, JsonNode> profiles) throws AmbariException {
    StackId stackId = cluster.getDesiredStackVersion();
    Map<String, Map<String, String>> configurations =
        configHelper.getEffectiveConfigProperties(cluster.getClusterName(), hostName);

    for (ServiceComponentHost componentHost : cluster.getServiceComponentHosts(hostName)) {
      if (!isInstalled(componentHost.getState())) {
        continue;
      }

      ServiceInfo serviceInfo = ambariMetaInfo.getService(stackId.getStackName(),
          stackId.getStackVersion(), componentHost.getServiceName());
      File descriptorFile = serviceInfo.getTelemetryFile();
      if (descriptorFile == null) {
        continue;
      }

      JsonNode descriptor = readJson(descriptorFile, "telemetry descriptor");
      validateDescriptor(descriptor, descriptorFile);
      JsonNode component = descriptor.path("components").get(componentHost.getServiceComponentName());
      if (component == null) {
        continue;
      }

      targets.add(compileTarget(cluster, componentHost, component, configurations,
          descriptorFile, profiles));
    }
  }

  private ObjectNode compileTarget(Cluster cluster, ServiceComponentHost componentHost,
      JsonNode component, Map<String, Map<String, String>> configurations,
      File descriptorFile, Map<String, JsonNode> profiles) throws AmbariException {
    String componentName = componentHost.getServiceComponentName();
    String format = requiredText(component, "format", componentName);
    if (!Set.of("prometheus_text", "jmx_json").contains(format)) {
      throw new AmbariException("Unsupported telemetry format " + format + " for " + componentName);
    }

    String path = requiredText(component, "path", componentName);
    if (!path.startsWith("/") || path.startsWith("//") || path.contains("?") || path.contains("#")) {
      throw new AmbariException("Invalid telemetry path " + path + " for " + componentName);
    }

    Endpoint endpoint = resolveEndpoint(component.path("endpoint"), configurations,
        componentHost.getHostName(), componentName);
    String routeId = String.format(Locale.ROOT, "c%d-%s-%s", cluster.getClusterId(),
        componentHost.getServiceName().toLowerCase(Locale.ROOT),
        componentName.toLowerCase(Locale.ROOT));

    ObjectNode target = JsonNodeFactory.instance.objectNode();
    target.put("id", routeId);
    target.put("clusterId", cluster.getClusterId());
    target.put("clusterName", cluster.getClusterName());
    target.put("service", componentHost.getServiceName());
    target.put("component", componentName);
    target.put("hostName", componentHost.getHostName());
    target.put("format", format);
    target.put("url", endpoint.toUrl(path));
    target.put("timeoutSeconds", boundedInteger(component, "timeoutSeconds",
        DEFAULT_TIMEOUT_SECONDS, 1, 60, componentName));
    target.put("maxResponseBytes", boundedInteger(component, "maxResponseBytes",
        DEFAULT_MAX_RESPONSE_BYTES, 1024, 64 * 1024 * 1024, componentName));
    target.put("maxConcurrentRequests", boundedInteger(component, "maxConcurrentRequests",
        DEFAULT_MAX_CONCURRENT_REQUESTS, 1, 16, componentName));

    if ("jmx_json".equals(format)) {
      String profilePath = requiredText(component, "profile", componentName);
      JsonNode profile = readProfile(descriptorFile, profilePath);
      String profileHash = profileDigest(profile);
      target.put("profileHash", profileHash);
      profiles.put(profileHash, profile);
    }

    addAuthentication(target, component.path("auth"), configurations,
        componentHost.getHostName(), cluster.getSecurityType());
    return target;
  }

  private void addAuthentication(ObjectNode target, JsonNode auth,
      Map<String, Map<String, String>> configurations, String hostName,
      SecurityType securityType) throws AmbariException {
    ObjectNode resolved = target.putObject("auth");
    if (securityType != SecurityType.KERBEROS || auth.isMissingNode()) {
      resolved.put("type", "none");
      return;
    }

    String principal = resolveReference(auth.path("principal"), configurations);
    String keytab = resolveReference(auth.path("keytab"), configurations);
    if (principal == null || keytab == null) {
      throw new AmbariException("Kerberos telemetry authentication is missing principal or keytab");
    }
    resolved.put("type", "kerberos");
    resolved.put("principal", principal.replace("_HOST", hostName.toLowerCase(Locale.ROOT)));
    resolved.put("keytab", keytab);
  }

  private Endpoint resolveEndpoint(JsonNode endpoint,
      Map<String, Map<String, String>> configurations, String hostName,
      String componentName) throws AmbariException {
    if (!endpoint.isObject()) {
      throw new AmbariException("Telemetry endpoint is missing for " + componentName);
    }

    boolean useHttps = false;
    JsonNode policy = endpoint.path("policy");
    if (policy.isObject()) {
      String value = resolveReference(policy, configurations);
      for (JsonNode httpsValue : policy.path("httpsValues")) {
        useHttps |= httpsValue.asText().equalsIgnoreCase(value);
      }
    }

    String scheme = useHttps ? "https" : "http";
    JsonNode addressDefinition = endpoint.path(scheme);
    if (!addressDefinition.isObject()) {
      throw new AmbariException("Telemetry " + scheme + " endpoint is missing for " + componentName);
    }
    int defaultPort = addressDefinition.path("defaultPort").asInt(-1);
    String address = resolveAddress(addressDefinition, configurations, hostName);
    int port = parsePort(address, defaultPort);
    if (port < 1 || port > 65535) {
      throw new AmbariException("Unable to resolve telemetry port for " + componentName);
    }
    return new Endpoint(scheme, hostName, port);
  }

  private String resolveAddress(JsonNode definition,
      Map<String, Map<String, String>> configurations, String hostName) {
    String configType = definition.path("configType").asText(null);
    Map<String, String> properties = configurations.get(configType);
    if (properties == null) {
      return null;
    }

    String prefix = definition.path("propertyPrefix").asText(null);
    if (prefix != null) {
      for (Map.Entry<String, String> entry : new TreeMap<>(properties).entrySet()) {
        if (entry.getKey().startsWith(prefix)
            && hostMatches(hostName, parseHost(entry.getValue()))) {
          return entry.getValue();
        }
      }
    }
    return properties.get(definition.path("property").asText());
  }

  private String resolveReference(JsonNode reference,
      Map<String, Map<String, String>> configurations) {
    if (!reference.isObject()) {
      return null;
    }
    Map<String, String> properties = configurations.get(reference.path("configType").asText());
    return properties == null ? null : properties.get(reference.path("property").asText());
  }

  private JsonNode readProfile(File descriptorFile, String profilePath) throws AmbariException {
    try {
      File serviceDirectory = descriptorFile.getCanonicalFile().getParentFile();
      File profileFile = new File(serviceDirectory, profilePath).getCanonicalFile();
      if (!profileFile.toPath().startsWith(serviceDirectory.toPath())) {
        throw new AmbariException("Telemetry profile path escapes its service directory");
      }
      JsonNode profile = readJson(profileFile, "telemetry profile");
      validateProfile(profile, profileFile);
      return profile;
    } catch (IOException e) {
      throw new AmbariException("Unable to resolve telemetry profile " + profilePath, e);
    }
  }

  private JsonNode readJson(File file, String description) throws AmbariException {
    try {
      return MAPPER.readTree(file);
    } catch (IOException e) {
      throw new AmbariException("Unable to read " + description + " " + file, e);
    }
  }

  private void validateDescriptor(JsonNode descriptor, File file) throws AmbariException {
    if (!descriptor.path("schemaVersion").isIntegralNumber()
        || descriptor.path("schemaVersion").asInt() != 1
        || !descriptor.path("components").isObject()) {
      throw new AmbariException("Invalid telemetry descriptor " + file);
    }
  }

  private void validateProfile(JsonNode profile, File file) throws AmbariException {
    JsonNode rules = profile.path("rules");
    JsonNode maxSeriesNode = profile.path("maxSeries");
    int maxSeries = maxSeriesNode.isMissingNode() ? 10000 : maxSeriesNode.asInt();
    if (!profile.path("schemaVersion").isIntegralNumber()
        || profile.path("schemaVersion").asInt() != 1
        || !profile.path("id").isTextual() || profile.path("id").asText().isEmpty()
        || !rules.isArray() || rules.size() == 0
        || (!maxSeriesNode.isMissingNode()
            && (!maxSeriesNode.isIntegralNumber() || !maxSeriesNode.canConvertToInt()))
        || maxSeries < 1 || maxSeries > 100000) {
      throw new AmbariException("Invalid telemetry profile " + file);
    }

    Map<String, String> families = new TreeMap<>();
    for (JsonNode rule : rules) {
      JsonNode bean = rule.path("bean");
      JsonNode properties = bean.path("properties");
      JsonNode labels = rule.path("labels");
      JsonNode attributes = rule.path("attributes");
      if (!bean.isObject() || !bean.path("domain").isTextual()
          || bean.path("domain").asText().isEmpty()
          || !properties.isObject() || properties.size() == 0
          || (!labels.isMissingNode() && !labels.isObject())
          || !attributes.isObject() || attributes.size() == 0) {
        throw new AmbariException("Invalid telemetry profile rule in " + file);
      }

      Iterator<Map.Entry<String, JsonNode>> propertyIterator = properties.fields();
      while (propertyIterator.hasNext()) {
        Map.Entry<String, JsonNode> property = propertyIterator.next();
        if (property.getKey().isEmpty() || !property.getValue().isTextual()) {
          throw new AmbariException("Invalid telemetry bean property in " + file);
        }
      }

      if (labels.isObject()) {
        Iterator<Map.Entry<String, JsonNode>> labelIterator = labels.fields();
        while (labelIterator.hasNext()) {
          Map.Entry<String, JsonNode> label = labelIterator.next();
          JsonNode source = label.getValue();
          boolean validSource = source.isObject() && source.size() == 1
              && ((source.path("property").isTextual() && !source.path("property").asText().isEmpty())
                  || source.path("value").isTextual());
          if (!LABEL_NAME.matcher(label.getKey()).matches()
              || label.getKey().startsWith("__") || !validSource) {
            throw new AmbariException("Invalid telemetry label in " + file);
          }
        }
      }

      Iterator<Map.Entry<String, JsonNode>> attributeIterator = attributes.fields();
      while (attributeIterator.hasNext()) {
        Map.Entry<String, JsonNode> attribute = attributeIterator.next();
        validateMetricDefinition(attribute.getKey(), attribute.getValue(), families, file);
      }
    }
  }

  private void validateMetricDefinition(String sourceName, JsonNode definition,
      Map<String, String> families, File file) throws AmbariException {
    String name = definition.path("name").asText();
    String type = definition.path("type").asText();
    String help = definition.path("help").asText();
    JsonNode scale = definition.path("scale");
    if (sourceName.isEmpty() || !definition.isObject()
        || !METRIC_NAME.matcher(name).matches()
        || !("counter".equals(type) || "gauge".equals(type))
        || ("counter".equals(type) && !name.endsWith("_total"))
        || !definition.path("help").isTextual() || help.trim().isEmpty()
        || (!definition.path("unit").isMissingNode() && !definition.path("unit").isTextual())
        || (!scale.isMissingNode()
            && (!scale.isNumber() || !Double.isFinite(scale.asDouble())))) {
      throw new AmbariException("Invalid telemetry metric definition in " + file);
    }

    String metadata = type + '\0' + help;
    String existing = families.putIfAbsent(name, metadata);
    if (existing != null && !existing.equals(metadata)) {
      throw new AmbariException("Conflicting telemetry metric family " + name + " in " + file);
    }
  }

  private String profileDigest(JsonNode profile) throws AmbariException {
    try {
      byte[] canonicalJson = MAPPER.writeValueAsBytes(canonicalize(profile));
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonicalJson);
      StringBuilder value = new StringBuilder("sha256:");
      for (byte item : digest) {
        value.append(String.format(Locale.ROOT, "%02x", item));
      }
      return value.toString();
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new AmbariException("Unable to hash telemetry profile", e);
    }
  }

  private JsonNode canonicalize(JsonNode node) {
    if (node.isObject()) {
      ObjectNode result = JsonNodeFactory.instance.objectNode();
      Map<String, JsonNode> fields = new TreeMap<>();
      Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
      iterator.forEachRemaining(entry -> fields.put(entry.getKey(), entry.getValue()));
      fields.forEach((name, value) -> result.set(name, canonicalize(value)));
      return result;
    }
    if (node.isArray()) {
      ArrayNode result = JsonNodeFactory.instance.arrayNode();
      node.forEach(value -> result.add(canonicalize(value)));
      return result;
    }
    return node;
  }

  private String requiredText(JsonNode parent, String field, String componentName)
      throws AmbariException {
    JsonNode value = parent.path(field);
    if (!value.isTextual() || value.asText().isEmpty()) {
      throw new AmbariException("Telemetry " + field + " is missing for " + componentName);
    }
    return value.asText();
  }

  private int boundedInteger(JsonNode parent, String field, int defaultValue, int minimum,
      int maximum, String componentName) throws AmbariException {
    JsonNode value = parent.path(field);
    if (value.isMissingNode()) {
      return defaultValue;
    }
    if (!value.isIntegralNumber() || !value.canConvertToInt()
        || value.asInt() < minimum || value.asInt() > maximum) {
      throw new AmbariException("Telemetry " + field + " is out of range for " + componentName);
    }
    return value.asInt();
  }

  private boolean isInstalled(State state) {
    return Set.of(State.INSTALLED, State.STARTING, State.STARTED, State.STOPPING,
        State.UPGRADING, State.DISABLED, State.UNKNOWN).contains(state);
  }

  private int parsePort(String address, int defaultPort) {
    if (address == null || address.isEmpty()) {
      return defaultPort;
    }
    try {
      return Integer.parseInt(address);
    } catch (NumberFormatException ignored) {
      try {
        int port = new URI("http://" + address).getPort();
        return port == -1 ? defaultPort : port;
      } catch (URISyntaxException invalidAddress) {
        return defaultPort;
      }
    }
  }

  private String parseHost(String address) {
    if (address == null) {
      return null;
    }
    try {
      return new URI("http://" + address).getHost();
    } catch (URISyntaxException e) {
      return null;
    }
  }

  private boolean hostMatches(String expected, String configured) {
    if (configured == null || configured.equals("0.0.0.0") || configured.equals("::")) {
      return false;
    }
    String expectedShort = expected.split("\\.", 2)[0];
    String configuredShort = configured.split("\\.", 2)[0];
    return expected.equalsIgnoreCase(configured)
        || expectedShort.equalsIgnoreCase(configuredShort);
  }

  private static final class Endpoint {
    private final String scheme;
    private final String host;
    private final int port;

    private Endpoint(String scheme, String host, int port) {
      this.scheme = scheme;
      this.host = host;
      this.port = port;
    }

    private String toUrl(String path) {
      String renderedHost = host.contains(":") ? "[" + host + "]" : host;
      return scheme + "://" + renderedHost + ":" + port + path;
    }
  }
}
