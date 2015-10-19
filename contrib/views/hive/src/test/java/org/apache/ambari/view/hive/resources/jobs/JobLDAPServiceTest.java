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

package org.apache.ambari.view.hive.resources.jobs;

import org.apache.ambari.view.hive.BaseHiveTest;
import org.apache.ambari.view.hive.ServiceTestUtils;
import org.apache.ambari.view.hive.client.Connection;
import org.apache.ambari.view.hive.client.HiveAuthRequiredException;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.hive.utils.HdfsApiMock;
import org.apache.ambari.view.utils.UserLocal;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.json.simple.JSONObject;
import org.junit.*;
import org.junit.rules.ExpectedException;

import javax.ws.rs.WebApplicationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;

public class JobLDAPServiceTest extends BaseHiveTest {
  private JobService jobService;
  @Rule public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void startUp() throws Exception {
    BaseHiveTest.startUp(); // super
  }

  @AfterClass
  public static void shutDown() throws Exception {
    BaseHiveTest.shutDown(); // super
  }

  @Override
  @After
  public void tearDown() throws Exception {
    jobService.getSharedObjectsFactory().clear(HdfsApi.class);
  }

  @Override
  protected void setupProperties(Map<String, String> properties, File baseDir) throws Exception {
    super.setupProperties(properties, baseDir);
    properties.put("hive.transport.mode", "binary");
    properties.put("hive.host", "127.0.0.1");
    properties.put("hive.port", "42420");

    properties.put("scripts.dir", "/tmp/.hiveQueries");
    properties.put("jobs.dir", "/tmp/.hiveJobs");
  }

  private HdfsApiMock setupHdfsApiMock() throws IOException, InterruptedException, HdfsApiException {
    HdfsApiMock hdfsApiMock = new HdfsApiMock("select * from Z");
    HdfsApi hdfsApi = hdfsApiMock.getHdfsApi();
    jobService.getSharedObjectsFactory().setInstance(HdfsApi.class, hdfsApi);
    replay(hdfsApi);
    return hdfsApiMock;
  }

  @Test
  public void createJobNoPasswordProvided() throws Exception {
    UserLocal.dropAllConnections(Connection.class);
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("hive.auth", "auth=NONE;password=${ask_password}");
    context = makeContext(properties, "ambari-qa-1", "MyHive");
    replay(context);
    jobService = getService(JobService.class, handler, context);
    setupHdfsApiMock();

    JobService.JobRequest request = new JobService.JobRequest();
    request.job = new JobImpl();
    request.job.setForcedContent("Hello world");

    thrown.expect(HiveAuthRequiredException.class);
    jobService.create(request,
        ServiceTestUtils.getResponseWithLocation(), ServiceTestUtils.getDefaultUriInfo());
  }

  @Test
  public void createJobNoPasswordRequired() throws Exception {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("hive.auth", "auth=NONE");
    context = makeContext(properties, "ambari-qa-2", "MyHive");
    replay(context);
    jobService = getService(JobService.class, handler, context);
    setupHdfsApiMock();

    JobService.JobRequest request = new JobService.JobRequest();
    request.job = new JobImpl();
    request.job.setForcedContent("Hello world");

    thrown.expect(new ExpectedJSONErrorMessage("Connection refused"));
    jobService.create(request,
        ServiceTestUtils.getResponseWithLocation(), ServiceTestUtils.getDefaultUriInfo());
  }

  @Test
  public void createJobPasswordProvided() throws Exception {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("hive.auth", "auth=NONE;password=${ask_password}");
    context = makeContext(properties, "ambari-qa-3", "MyHive");
    replay(context);
    jobService = getService(JobService.class, handler, context);
    setupHdfsApiMock();

    JobService.JobRequest request = new JobService.JobRequest();
    request.job = new JobImpl();
    request.job.setForcedContent("Hello world");

    JobService.AuthRequest authRequest = new JobService.AuthRequest();
    authRequest.password = "ok";

    thrown.expect(new ExpectedJSONErrorMessage("Connection refused"));
    jobService.setupPassword(authRequest);
  }

  private static class ExpectedJSONErrorMessage extends BaseMatcher<WebApplicationException> {
    private String expectedMessage;

    public ExpectedJSONErrorMessage(String message) {
      this.expectedMessage = message;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(this.expectedMessage);
    }

    @Override
    public boolean matches(Object o) {
      JSONObject response = (JSONObject) ((WebApplicationException) o).getResponse().getEntity();
      String message = (String) response.get("message");
      return message.contains(expectedMessage);
    }
  }
}
