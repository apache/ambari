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
package org.apache.ambari.server.controller.internal;


import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.ComponentSSLConfigurationTest;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.inject.Injector;

@RunWith(Parameterized.class)
public class HttpPropertyProviderTest {
  private static final String PROPERTY_ID_CLUSTER_NAME = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String PROPERTY_ID_HOST_NAME = PropertyHelper.getPropertyId("HostRoles", "host_name");
  private static final String PROPERTY_ID_COMPONENT_NAME = PropertyHelper.getPropertyId("HostRoles", "component_name");

  private static final String PROPERTY_ID_STALE_CONFIGS = PropertyHelper.getPropertyId(
      "HostRoles", "stale_configs");

  private ComponentSSLConfiguration configuration;

  @Parameterized.Parameters
  public static Collection<Object[]> configs() {
    ComponentSSLConfiguration configuration1 = ComponentSSLConfigurationTest.getConfiguration(
        "tspath", "tspass", "tstype", false);

    ComponentSSLConfiguration configuration2 = ComponentSSLConfigurationTest.getConfiguration(
        "tspath", "tspass", "tstype", true);

    ComponentSSLConfiguration configuration3 = ComponentSSLConfigurationTest.getConfiguration(
        "tspath", "tspass", "tstype", false);

    return Arrays.asList(new Object[][] { { configuration1 },
        { configuration2 }, { configuration3 } });
  }


  public HttpPropertyProviderTest(ComponentSSLConfiguration configuration) {
    this.configuration = configuration;
  }

  @Test
  public void testReadResourceManager() throws Exception {

    TestStreamProvider streamProvider = new TestStreamProvider(false);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Injector injector = createNiceMock(Injector.class);
    Config config1 = createNiceMock(Config.class);
    Config config2 = createNiceMock(Config.class);

    Map<String, String> map = new HashMap<String, String>();
    map.put("yarn.http.policy", "HTTPS_ONLY");
    map.put("yarn.resourcemanager.webapp.https.address", "ec2-54-234-33-50.compute-1.amazonaws.com:8999");
    map.put("yarn.resourcemanager.webapp.address", "ec2-54-234-33-50.compute-1.amazonaws.com:8088");

    expect(injector.getInstance(Clusters.class)).andReturn(clusters);
    expect(clusters.getCluster("testCluster")).andReturn(cluster);
    expect(cluster.getDesiredConfigByType("yarn-site")).andReturn(config1).anyTimes();
    expect(cluster.getDesiredConfigByType("core-site")).andReturn(config2).anyTimes();
    expect(config1.getProperties()).andReturn(map).anyTimes();
    expect(config2.getProperties()).andReturn(new HashMap<String, String>()).anyTimes();

    replay(injector, clusters, cluster, config1, config2);

    HttpProxyPropertyProvider propProvider = new HttpProxyPropertyProvider(
            streamProvider, configuration, injector,
            PROPERTY_ID_CLUSTER_NAME,
            PROPERTY_ID_HOST_NAME,
            PROPERTY_ID_COMPONENT_NAME);

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(PROPERTY_ID_HOST_NAME, "ec2-54-234-33-50.compute-1.amazonaws.com");
    resource.setProperty(PROPERTY_ID_CLUSTER_NAME, "testCluster");
    resource.setProperty(PROPERTY_ID_COMPONENT_NAME, "RESOURCEMANAGER");

    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    propProvider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertTrue(resource.getPropertiesMap().get("HostRoles").get("ha_state").equals("ACTIVE"));
    Assert.assertTrue(streamProvider.getLastSpec().equals("https://ec2-54-234-33-50.compute-1.amazonaws.com:8999" +
            "/ws/v1/cluster/info"));
  }

  @Test
  public void testReadResourceManagerHA() throws Exception {

    TestStreamProvider streamProvider = new TestStreamProvider(false);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Injector injector = createNiceMock(Injector.class);
    Config config1 = createNiceMock(Config.class);
    Config config2 = createNiceMock(Config.class);

    Map<String, String> map = new HashMap<String, String>();
    map.put("yarn.http.policy", "HTTPS_ONLY");
    map.put("yarn.resourcemanager.ha.rm-ids", "rm1,rm2");
    map.put("yarn.resourcemanager.hostname.rm1", "lc6402.ambari.apache.org");
    map.put("yarn.resourcemanager.hostname.rm2", "lc6403.ambari.apache.org");
    map.put("yarn.resourcemanager.webapp.address.rm1", "lc6402.ambari.apache.org:8099");
    map.put("yarn.resourcemanager.webapp.address.rm2", "lc6403.ambari.apache.org:8099");
    map.put("yarn.resourcemanager.webapp.https.address.rm1", "lc6402.ambari.apache.org:8066");
    map.put("yarn.resourcemanager.webapp.https.address.rm2", "lc6403.ambari.apache.org:8066");

    expect(injector.getInstance(Clusters.class)).andReturn(clusters);
    expect(clusters.getCluster("testCluster")).andReturn(cluster);
    expect(cluster.getDesiredConfigByType("yarn-site")).andReturn(config1).anyTimes();
    expect(cluster.getDesiredConfigByType("core-site")).andReturn(config2).anyTimes();
    expect(config1.getProperties()).andReturn(map).anyTimes();
    expect(config2.getProperties()).andReturn(new HashMap<String, String>()).anyTimes();

    replay(injector, clusters, cluster, config1, config2);

    HttpProxyPropertyProvider propProvider = new HttpProxyPropertyProvider(
        streamProvider, configuration, injector,
        PROPERTY_ID_CLUSTER_NAME,
        PROPERTY_ID_HOST_NAME,
        PROPERTY_ID_COMPONENT_NAME);

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(PROPERTY_ID_HOST_NAME, "lc6402.ambari.apache.org");
    resource.setProperty(PROPERTY_ID_CLUSTER_NAME, "testCluster");
    resource.setProperty(PROPERTY_ID_COMPONENT_NAME, "RESOURCEMANAGER");

    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    propProvider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertTrue(resource.getPropertiesMap().get("HostRoles").get("ha_state").equals("ACTIVE"));
    Assert.assertTrue(streamProvider.getLastSpec().equals("https://lc6402.ambari.apache.org:8066" +
        "/ws/v1/cluster/info"));
  }


  @Test
  public void testReadGangliaServer() throws Exception {
    Resource resource = doPopulate("GANGLIA_SERVER",
        Collections.<String> emptySet(), new TestStreamProvider(false));

    // !!! GANGLIA_SERVER has no current http lookup
    Assert.assertNull(resource.getPropertyValue(PROPERTY_ID_STALE_CONFIGS));
  }

  private Resource doPopulate(String componentName,
      Set<String> requestProperties, StreamProvider streamProvider) throws Exception {
    Injector injector = createNiceMock(Injector.class);

    HttpProxyPropertyProvider propProvider = new HttpProxyPropertyProvider(
       streamProvider, configuration, injector,
       PROPERTY_ID_CLUSTER_NAME,
       PROPERTY_ID_HOST_NAME,
       PROPERTY_ID_COMPONENT_NAME);

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(PROPERTY_ID_HOST_NAME, "ec2-54-234-33-50.compute-1.amazonaws.com");
    resource.setProperty(PROPERTY_ID_CLUSTER_NAME, "testCluster");
    resource.setProperty(PROPERTY_ID_COMPONENT_NAME, componentName);

    Request request = PropertyHelper.getReadRequest(requestProperties);

    propProvider.populateResources(Collections.singleton(resource), request, null);

    return resource;
  }

  private static class TestStreamProvider implements StreamProvider {
    private boolean throwError = false;
    private String lastSpec = null;
    private boolean isLastSpecUpdated;

    private TestStreamProvider(boolean throwErr) {
      throwError = throwErr;
    }

    @Override
    public InputStream readFrom(String spec) throws IOException {
      if (!isLastSpecUpdated) {
        lastSpec = spec;
      }

      isLastSpecUpdated = false;

      if (throwError) {
        throw new IOException("Fake error");
      }

      String responseStr = "{\"alerts\": [{\"Alert Body\": \"Body\"}],\"clusterInfo\": {\"haState\": \"ACTIVE\"},"
          + " \"hostcounts\": {\"up_hosts\":\"1\", \"down_hosts\":\"0\"}}";
        return new ByteArrayInputStream(responseStr.getBytes("UTF-8"));
    }

    public String getLastSpec() {
      return lastSpec;
    }

    @Override
    public InputStream readFrom(String spec, String requestMethod, String params) throws IOException {
      lastSpec = spec + "?" + params;
      isLastSpecUpdated = true;
      return readFrom(spec);
    }
  }

}
