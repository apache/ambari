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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Provider;

public class TelemetryAssignmentCompilerTest {
  private static final String EXPECTED_PROFILE_HASH =
      "sha256:d3ad33dcbdceeea153b6e248cc2d6d96fbc5ef24cf8cf9462c891621eac4e70a";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testCompileResolvesHaEndpointAuthenticationAndProfileDigest() throws Exception {
    File serviceDirectory = temporaryFolder.newFolder("HDFS");
    File profileDirectory = new File(serviceDirectory, "telemetry-profiles");
    assertTrue(profileDirectory.mkdir());
    File profileFile = new File(profileDirectory, "test-profile.json");
    Files.write(profileFile.toPath(), profileJson().getBytes(StandardCharsets.UTF_8));
    File descriptorFile = new File(serviceDirectory, "telemetry.json");
    Files.write(descriptorFile.toPath(), descriptorJson().getBytes(StandardCharsets.UTF_8));

    Clusters clusters = createMock(Clusters.class);
    Host host = createMock(Host.class);
    Cluster cluster = createMock(Cluster.class);
    ServiceComponentHost componentHost = createMock(ServiceComponentHost.class);
    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
    ConfigHelper configHelper = createMock(ConfigHelper.class);
    ServiceInfo serviceInfo = new ServiceInfo();
    serviceInfo.setTelemetryFile(descriptorFile);

    expect(clusters.getHostById(7L)).andReturn(host);
    expect(host.getHostName()).andStubReturn("nn1.example.com");
    expect(clusters.getClustersForHost("nn1.example.com"))
        .andReturn(Collections.singleton(cluster));
    expect(cluster.getClusterId()).andStubReturn(11L);
    expect(cluster.getClusterName()).andStubReturn("cluster-one");
    expect(cluster.getDesiredStackVersion()).andReturn(new StackId("BIGTOP", "3.2.0"));
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS);
    expect(cluster.getServiceComponentHosts("nn1.example.com"))
        .andReturn(Collections.singletonList(componentHost));
    expect(configHelper.getEffectiveConfigProperties("cluster-one", "nn1.example.com"))
        .andReturn(configurations());
    expect(componentHost.getState()).andReturn(State.STARTED);
    expect(componentHost.getServiceName()).andStubReturn("HDFS");
    expect(componentHost.getServiceComponentName()).andStubReturn("NAMENODE");
    expect(componentHost.getHostName()).andStubReturn("nn1.example.com");
    expect(metaInfo.getService("BIGTOP", "3.2.0", "HDFS")).andReturn(serviceInfo);
    replay(clusters, host, cluster, componentHost, metaInfo, configHelper);

    Provider<Clusters> clustersProvider = () -> clusters;
    TelemetryAssignmentCompiler compiler =
        new TelemetryAssignmentCompiler(clustersProvider, metaInfo, configHelper);
    TelemetryUpdateEvent event = compiler.compile(7L);

    JsonNode target = event.getAssignment().path("targets").get(0);
    assertEquals("c11-hdfs-namenode", target.path("id").asText());
    assertEquals("https://nn1.example.com:9871/jmx", target.path("url").asText());
    assertEquals(2, target.path("maxConcurrentRequests").asInt());
    assertEquals("HTTP/nn1.example.com@EXAMPLE.COM",
        target.path("auth").path("principal").asText());
    assertEquals("/etc/security/keytabs/spnego.service.keytab",
        target.path("auth").path("keytab").asText());
    assertEquals(EXPECTED_PROFILE_HASH, target.path("profileHash").asText());
    assertTrue(event.getProfiles().containsKey(EXPECTED_PROFILE_HASH));
  }

  private Map<String, Map<String, String>> configurations() {
    Map<String, Map<String, String>> configurations = new HashMap<>();
    Map<String, String> hdfsSite = new HashMap<>();
    hdfsSite.put("dfs.http.policy", "HTTPS_ONLY");
    hdfsSite.put("dfs.namenode.http-address.ns.nn1", "nn1.example.com:9870");
    hdfsSite.put("dfs.namenode.http-address.ns.nn2", "nn2.example.com:9870");
    hdfsSite.put("dfs.namenode.https-address.ns.nn1", "nn1.example.com:9871");
    hdfsSite.put("dfs.namenode.https-address.ns.nn2", "nn2.example.com:9871");
    configurations.put("hdfs-site", hdfsSite);

    Map<String, String> coreSite = new HashMap<>();
    coreSite.put("hadoop.http.authentication.kerberos.principal", "HTTP/_HOST@EXAMPLE.COM");
    coreSite.put("hadoop.http.authentication.kerberos.keytab",
        "/etc/security/keytabs/spnego.service.keytab");
    configurations.put("core-site", coreSite);
    return configurations;
  }

  private String descriptorJson() {
    return "{\n"
        + "  \"schemaVersion\": 1,\n"
        + "  \"components\": {\n"
        + "    \"NAMENODE\": {\n"
        + "      \"format\": \"jmx_json\",\n"
        + "      \"path\": \"/jmx\",\n"
        + "      \"profile\": \"telemetry-profiles/test-profile.json\",\n"
        + "      \"endpoint\": {\n"
        + "        \"policy\": {\"configType\": \"hdfs-site\", "
        + "\"property\": \"dfs.http.policy\", \"httpsValues\": [\"HTTPS_ONLY\"]},\n"
        + "        \"http\": {\"configType\": \"hdfs-site\", "
        + "\"propertyPrefix\": \"dfs.namenode.http-address.\", "
        + "\"defaultPort\": 9870},\n"
        + "        \"https\": {\"configType\": \"hdfs-site\", "
        + "\"propertyPrefix\": \"dfs.namenode.https-address.\", "
        + "\"defaultPort\": 9871}\n"
        + "      },\n"
        + "      \"auth\": {\n"
        + "        \"principal\": {\"configType\": \"core-site\", "
        + "\"property\": \"hadoop.http.authentication.kerberos.principal\"},\n"
        + "        \"keytab\": {\"configType\": \"core-site\", "
        + "\"property\": \"hadoop.http.authentication.kerberos.keytab\"}\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "}\n";
  }

  private String profileJson() {
    return "{\n"
        + "  \"schemaVersion\": 1,\n"
        + "  \"id\": \"test-profile\",\n"
        + "  \"maxSeries\": 10,\n"
        + "  \"rules\": [{\n"
        + "    \"bean\": {\"domain\": \"Hadoop\", \"properties\": {\"name\": \"Server\"}},\n"
        + "    \"attributes\": {\"Value\": {\"name\": \"test_value\", "
        + "\"type\": \"gauge\", \"unit\": \"unitless\", "
        + "\"scale\": 0.001, \"help\": \"Test value.\"}}\n"
        + "  }]\n"
        + "}\n";
  }
}
