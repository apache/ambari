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

package org.apache.ambari.view.utils.ambari;

import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.cluster.Cluster;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.easymock.IAnswer;
import org.json.simple.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class RemoteClusterTest {
  public static final String AMBARI_CLUSTER_REST_URL = "http://example.com:8080/api/v1/clusters/c1";

  @Rule
  public ExpectedException thrown= ExpectedException.none();

  @Test
  public void testGetRemoteClusterThatIsNotPresent() throws Exception {
    ViewContext viewContext = createNiceMock(ViewContext.class);
    Map<String, String> instanceProperties = new HashMap<String, String>();
    expect(viewContext.getProperties()).andReturn(instanceProperties).anyTimes();
    replay(viewContext);

    AmbariApi ambariApi = new AmbariApi(viewContext);
    Cluster cluster = ambariApi.getRemoteCluster();
    assertNull(cluster);
  }

  @Test
  public void testGetRemoteClusterNoCredentials() throws Exception {
    ViewContext viewContext = createNiceMock(ViewContext.class);
    Map<String, String> instanceProperties = new HashMap<String, String>();
    instanceProperties.put(AmbariApi.AMBARI_SERVER_URL_INSTANCE_PROPERTY,
        AMBARI_CLUSTER_REST_URL);
    expect(viewContext.getProperties()).andReturn(instanceProperties).anyTimes();
    replay(viewContext);

    thrown.expect(AmbariApiException.class);
    AmbariApi ambariApi = new AmbariApi(viewContext);
    Cluster cluster = ambariApi.getRemoteCluster();
  }

  @Test
  public void testGetRemoteClusterThatIsPresent() throws Exception {
    ViewContext viewContext = createNiceMock(ViewContext.class);
    Map<String, String> instanceProperties = new HashMap<String, String>();
    instanceProperties.put(AmbariApi.AMBARI_SERVER_URL_INSTANCE_PROPERTY,
        AMBARI_CLUSTER_REST_URL);
    instanceProperties.put(AmbariApi.AMBARI_SERVER_USERNAME_INSTANCE_PROPERTY, "admin");
    instanceProperties.put(AmbariApi.AMBARI_SERVER_PASSWORD_INSTANCE_PROPERTY, "admin");
    expect(viewContext.getProperties()).andReturn(instanceProperties).anyTimes();
    replay(viewContext);

    AmbariApi ambariApi = new AmbariApi(viewContext);
    Cluster cluster = ambariApi.getRemoteCluster();
    assertNotNull(cluster);
    assertEquals(cluster.getName(), "c1");
  }

  @Test
  public void testGetConfigurationValue() throws Exception {
    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);

    final String desiredConfigsString = "{\"Clusters\": {\"desired_configs\": {\"test-site\": {\"tag\": \"TAG\"}}}}";
    final String configurationString = "{\"items\": [{\"properties\": {\"test.property.name\": \"test property value\"}}]}";
    final int[] desiredConfigPolls = {0};
    final int[] testConfigPolls = {0};

    expect(urlStreamProvider.readFrom(eq(AMBARI_CLUSTER_REST_URL + "?fields=services/ServiceInfo,hosts,Clusters"),
        eq("GET"), (String) isNull(), (Map<String, String>) anyObject())).andAnswer(new IAnswer<InputStream>() {
      @Override
      public InputStream answer() throws Throwable {
        desiredConfigPolls[0] += 1;
        return new ByteArrayInputStream(desiredConfigsString.getBytes());
      }
    }).anyTimes();

    expect(urlStreamProvider.readFrom(eq(AMBARI_CLUSTER_REST_URL + "/configurations?(type=test-site&tag=TAG)"),
        eq("GET"), (String)isNull(), (Map<String, String>) anyObject())).andAnswer(new IAnswer<InputStream>() {
      @Override
      public InputStream answer() throws Throwable {
        testConfigPolls[0] += 1;
        return new ByteArrayInputStream(configurationString.getBytes());
      }
    }).anyTimes();

    replay(urlStreamProvider);

    RemoteCluster cluster = new RemoteCluster(AMBARI_CLUSTER_REST_URL, urlStreamProvider);
    PassiveExpiringMap<String, JSONObject> cache = new PassiveExpiringMap<String, JSONObject>(10000L);
    cluster.configurationCache = cache;

    String value = cluster.getConfigurationValue("test-site", "test.property.name");
    assertEquals(value, "test property value");
    assertEquals(desiredConfigPolls[0], 1);
    assertEquals(testConfigPolls[0], 1);

    value = cluster.getConfigurationValue("test-site", "test.property.name");
    assertEquals(value, "test property value");
    assertEquals(desiredConfigPolls[0], 1);  // cache hit
    assertEquals(testConfigPolls[0], 1);

    cache.clear();
    value = cluster.getConfigurationValue("test-site", "test.property.name");
    assertEquals(value, "test property value");
    assertEquals(desiredConfigPolls[0], 2);
    assertEquals(testConfigPolls[0], 2);
  }
}