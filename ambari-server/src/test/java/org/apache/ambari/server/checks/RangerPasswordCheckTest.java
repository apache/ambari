/*
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
package org.apache.ambari.server.checks;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Provider;


/**
 * Unit tests for RangerPasswordCheck
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(RangerPasswordCheck.class)
public class RangerPasswordCheckTest {

  private static final String RANGER_URL = "http://foo:6080/";

  private static final String GOOD_LOGIN_RESPONSE = "{\"count\": 0 }";

  private static final String BAD_LOGIN_RESPONSE = "<html>Ranger redirects to login HTML</html>";

  private static final String GOOD_USER_RESPONSE =
      "{\"queryTimeMS\": 1446758948823," +
      "\"vXUsers\": [" +
      "  {" +
      "    \"name\": \"r_admin\"" +
      "  }" +
      "]}";

  private static final String NO_USER_RESPONSE =
      "{\"queryTimeMS\": 1446758948823," +
      "\"vXUsers\": [" +
      "]}";

  private Clusters m_clusters = EasyMock.createMock(Clusters.class);
  private Map<String, String> m_configMap = new HashMap<String, String>();
  private RangerPasswordCheck m_rpc = null;
  private URLStreamProvider m_streamProvider = EasyMock.createMock(URLStreamProvider.class);

  @Before
  public void setup() throws Exception {
    m_configMap.put("policymgr_external_url", RANGER_URL);
    m_configMap.put("admin_username", "admin");
    m_configMap.put("admin_password", "pass");
    m_configMap.put("ranger_admin_username", "r_admin");
    m_configMap.put("ranger_admin_password", "r_pass");

    Cluster cluster = EasyMock.createMock(Cluster.class);

    Config config = EasyMock.createMock(Config.class);
    final Map<String, Service> services = new HashMap<>();
    final Service service = EasyMock.createMock(Service.class);

    services.put("RANGER", service);

    Map<String, DesiredConfig> desiredMap = new HashMap<>();
    DesiredConfig dc = EasyMock.createMock(DesiredConfig.class);
    desiredMap.put("admin-properties", dc);
    desiredMap.put("ranger-env", dc);

    expect(dc.getTag()).andReturn("").anyTimes();
    expect(config.getProperties()).andReturn(m_configMap).anyTimes();
    expect(cluster.getServices()).andReturn(services).anyTimes();
    expect(cluster.getService("RANGER")).andReturn(service).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(desiredMap).anyTimes();
    expect(cluster.getDesiredConfigByType((String) anyObject())).andReturn(config).anyTimes();
    expect(cluster.getConfig((String) anyObject(), (String) anyObject())).andReturn(config).anyTimes();
    expect(m_clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();

    replay(m_clusters, cluster, dc, config);

    m_rpc = new RangerPasswordCheck();
    m_rpc.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        // TODO Auto-generated method stub
        return m_clusters;
      }
    };

    EasyMock.reset(m_streamProvider);
    PowerMockito.whenNew(URLStreamProvider.class).withAnyArguments().thenReturn(m_streamProvider);
  }

  @Test
  public void testApplicable() throws Exception {

    final Service service = EasyMock.createMock(Service.class);
    Map<String, Service> services = new HashMap<>();
    services.put("RANGER", service);

    Cluster cluster = m_clusters.getCluster("cluster");
    EasyMock.reset(cluster);
    expect(cluster.getServices()).andReturn(services).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP-2.3")).anyTimes();
    replay(cluster);

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setSourceStackId(new StackId("HDP-2.3"));
    assertTrue(m_rpc.isApplicable(request));

    request = new PrereqCheckRequest("cluster");
    request.setSourceStackId(new StackId("HDP-2.2"));
    assertFalse(m_rpc.isApplicable(request));

    EasyMock.reset(cluster);
    expect(cluster.getServices()).andReturn(services).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("WILDSTACK-2.0")).anyTimes();
    replay(cluster);

    request = new PrereqCheckRequest("cluster");
    request.setSourceStackId(new StackId("HDP-2.2"));
    assertTrue(m_rpc.isApplicable(request));

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testMissingProps() throws Exception {

    HttpURLConnection conn = EasyMock.createMock(HttpURLConnection.class);

    m_configMap.clear();

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));
    assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
    assertEquals("Could not check credentials.  Missing property admin-properties/policymgr_external_url", check.getFailReason());

    m_configMap.put("policymgr_external_url", RANGER_URL);
    check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));
    assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
    assertEquals("Could not check credentials.  Missing property ranger-env/admin_username", check.getFailReason());

    m_configMap.put("admin_username", "admin");
    check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));
    assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
    assertEquals("Could not check credentials.  Missing property ranger-env/admin_password", check.getFailReason());


    m_configMap.put("admin_password", "pass");
    check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));
    assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
    assertEquals("Could not check credentials.  Missing property ranger-env/ranger_admin_username", check.getFailReason());

    m_configMap.put("ranger_admin_username", "r_admin");
    check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));
    assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
    assertEquals("Could not check credentials.  Missing property ranger-env/ranger_admin_password", check.getFailReason());

    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_LOGIN_RESPONSE.getBytes()));
    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_USER_RESPONSE.getBytes()));
    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_LOGIN_RESPONSE.getBytes()));
    expect(m_streamProvider.processURL((String) anyObject(), (String) anyObject(),
        (InputStream) anyObject(), (Map<String, List<String>>) anyObject())).andReturn(conn).anyTimes();

    replay(conn, m_streamProvider);

    m_configMap.put("ranger_admin_password", "r_pass");
    check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));
    assertEquals(PrereqCheckStatus.PASS, check.getStatus());

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNormal() throws Exception {

    HttpURLConnection conn = EasyMock.createMock(HttpURLConnection.class);

    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_LOGIN_RESPONSE.getBytes())).once();
    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_USER_RESPONSE.getBytes())).once();
    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_LOGIN_RESPONSE.getBytes())).once();

    expect(m_streamProvider.processURL((String) anyObject(), (String) anyObject(),
        (InputStream) anyObject(), (Map<String, List<String>>) anyObject())).andReturn(conn).anyTimes();

    replay(conn, m_streamProvider);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));

    assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    verify(conn, m_streamProvider);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNoUser() throws Exception {

    HttpURLConnection conn = EasyMock.createMock(HttpURLConnection.class);

    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_LOGIN_RESPONSE.getBytes())).once();
    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(NO_USER_RESPONSE.getBytes())).once();

    expect(m_streamProvider.processURL((String) anyObject(), (String) anyObject(),
        (InputStream) anyObject(), (Map<String, List<String>>) anyObject())).andReturn(conn).anyTimes();

    replay(conn, m_streamProvider);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));

    assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    verify(conn, m_streamProvider);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBadUserParsing() throws Exception {

    HttpURLConnection conn = EasyMock.createMock(HttpURLConnection.class);

    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_LOGIN_RESPONSE.getBytes())).once();
    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(
        "some really bad non-json".getBytes()));

    expect(m_streamProvider.processURL((String) anyObject(), (String) anyObject(),
        (InputStream) anyObject(), (Map<String, List<String>>) anyObject())).andReturn(conn).anyTimes();

    replay(conn, m_streamProvider);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));

    String error = "The response from Ranger was malformed. ";
    error += "com.google.gson.stream.MalformedJsonException: Expected EOF at line 1 column 6. ";
    error += "Request: " + RANGER_URL + "service/xusers/users?name=r_admin";

    assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
    assertEquals(error, check.getFailReason());

    verify(conn, m_streamProvider);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testJsonCasting() throws Exception {

    HttpURLConnection conn = EasyMock.createMock(HttpURLConnection.class);

    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_LOGIN_RESPONSE.getBytes()));
    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(
        "{ \"data\": \"bad\", \"vXUsers\": \"xyz\" }".getBytes()));

    expect(m_streamProvider.processURL((String) anyObject(), (String) anyObject(),
        (InputStream) anyObject(), (Map<String, List<String>>) anyObject())).andReturn(conn).anyTimes();

    replay(conn, m_streamProvider);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));

    String error = "The response from Ranger was malformed. ";
    error += "java.lang.String cannot be cast to java.util.List. ";
    error += "Request: " + RANGER_URL + "service/xusers/users?name=r_admin";

    assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
    assertEquals(error, check.getFailReason());

    verify(conn, m_streamProvider);
  }


  @SuppressWarnings("unchecked")
  @Test
  public void testAdminUnauthorized() throws Exception {

    HttpURLConnection conn = EasyMock.createMock(HttpURLConnection.class);

    expect(conn.getResponseCode()).andReturn(401);

    expect(m_streamProvider.processURL((String) anyObject(), (String) anyObject(),
        (InputStream) anyObject(), (Map<String, List<String>>) anyObject())).andReturn(conn).anyTimes();

    replay(conn, m_streamProvider);

    PrerequisiteCheck check = new PrerequisiteCheck(CheckDescription.SERVICES_RANGER_PASSWORD_VERIFY, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));

    assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
    assertEquals("Credentials for user 'admin' in Ambari do not match Ranger.", check.getFailReason());

    verify(conn, m_streamProvider);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAdminUnauthorizedByRedirect() throws Exception {

    HttpURLConnection conn = EasyMock.createMock(HttpURLConnection.class);

    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(BAD_LOGIN_RESPONSE.getBytes()));

    expect(m_streamProvider.processURL((String) anyObject(), (String) anyObject(),
        (InputStream) anyObject(), (Map<String, List<String>>) anyObject())).andReturn(conn).anyTimes();

    replay(conn, m_streamProvider);

    PrerequisiteCheck check = new PrerequisiteCheck(CheckDescription.SERVICES_RANGER_PASSWORD_VERIFY, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));

    assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
    assertEquals("Credentials for user 'admin' in Ambari do not match Ranger.", check.getFailReason());

    verify(conn, m_streamProvider);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAdminIOException() throws Exception {

    HttpURLConnection conn = EasyMock.createMock(HttpURLConnection.class);

    expect(conn.getResponseCode()).andThrow(new IOException("whoops"));

    expect(m_streamProvider.processURL((String) anyObject(), (String) anyObject(),
        (InputStream) anyObject(), (Map<String, List<String>>) anyObject())).andReturn(conn).anyTimes();

    replay(conn, m_streamProvider);

    PrerequisiteCheck check = new PrerequisiteCheck(CheckDescription.SERVICES_RANGER_PASSWORD_VERIFY, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));

    assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
    assertEquals("Could not access Ranger to verify user 'admin' against " + RANGER_URL + "service/public/api/repository/count. whoops", check.getFailReason());

    verify(conn, m_streamProvider);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAdminBadResponse() throws Exception {

    HttpURLConnection conn = EasyMock.createMock(HttpURLConnection.class);

    expect(conn.getResponseCode()).andReturn(404);

    expect(m_streamProvider.processURL((String) anyObject(), (String) anyObject(),
        (InputStream) anyObject(), (Map<String, List<String>>) anyObject())).andReturn(conn).anyTimes();

    replay(conn, m_streamProvider);

    PrerequisiteCheck check = new PrerequisiteCheck(CheckDescription.SERVICES_RANGER_PASSWORD_VERIFY, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));

    assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
    assertEquals("Could not verify credentials for user 'admin'.  Response code 404 received from " + RANGER_URL + "service/public/api/repository/count", check.getFailReason());

    verify(conn, m_streamProvider);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUserUnauthorized() throws Exception {

    HttpURLConnection conn = EasyMock.createMock(HttpURLConnection.class);

    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_LOGIN_RESPONSE.getBytes())).once();
    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_USER_RESPONSE.getBytes())).once();
    expect(conn.getResponseCode()).andReturn(401);

    expect(m_streamProvider.processURL((String) anyObject(), (String) anyObject(),
        (InputStream) anyObject(), (Map<String, List<String>>) anyObject())).andReturn(conn).anyTimes();

    replay(conn, m_streamProvider);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));

    assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
    assertEquals("Credentials for user 'r_admin' in Ambari do not match Ranger.", check.getFailReason());

    verify(conn, m_streamProvider);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUserUnauthorizedByRedirect() throws Exception {

    HttpURLConnection conn = EasyMock.createMock(HttpURLConnection.class);

    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_LOGIN_RESPONSE.getBytes())).once();
    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_USER_RESPONSE.getBytes())).once();
    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(BAD_LOGIN_RESPONSE.getBytes())).once();

    expect(m_streamProvider.processURL((String) anyObject(), (String) anyObject(),
        (InputStream) anyObject(), (Map<String, List<String>>) anyObject())).andReturn(conn).anyTimes();

    replay(conn, m_streamProvider);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));

    assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
    assertEquals("Credentials for user 'r_admin' in Ambari do not match Ranger.", check.getFailReason());

    verify(conn, m_streamProvider);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUserIOException() throws Exception {

    HttpURLConnection conn = EasyMock.createMock(HttpURLConnection.class);

    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_LOGIN_RESPONSE.getBytes())).once();
    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_USER_RESPONSE.getBytes())).once();
    expect(conn.getResponseCode()).andThrow(new IOException("again!"));

    expect(m_streamProvider.processURL((String) anyObject(), (String) anyObject(),
        (InputStream) anyObject(), (Map<String, List<String>>) anyObject())).andReturn(conn).anyTimes();

    replay(conn, m_streamProvider);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));

    assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
    assertEquals("Could not access Ranger to verify user 'r_admin' against " + RANGER_URL + "service/public/api/repository/count. again!", check.getFailReason());

    verify(conn, m_streamProvider);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUserBadResponse() throws Exception {

    HttpURLConnection conn = EasyMock.createMock(HttpURLConnection.class);

    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_LOGIN_RESPONSE.getBytes())).once();
    expect(conn.getResponseCode()).andReturn(200);
    expect(conn.getInputStream()).andReturn(new ByteArrayInputStream(GOOD_USER_RESPONSE.getBytes())).once();
    expect(conn.getResponseCode()).andReturn(500);

    expect(m_streamProvider.processURL((String) anyObject(), (String) anyObject(),
        (InputStream) anyObject(), (Map<String, List<String>>) anyObject())).andReturn(
            conn).anyTimes();

    replay(conn, m_streamProvider);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_rpc.perform(check, new PrereqCheckRequest("cluster"));

    assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
    assertEquals("Could not verify credentials for user 'r_admin'.  Response code 500 received from " + RANGER_URL + "service/public/api/repository/count", check.getFailReason());

    verify(conn, m_streamProvider);
  }
}
