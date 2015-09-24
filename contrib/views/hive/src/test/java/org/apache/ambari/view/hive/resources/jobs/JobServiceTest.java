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

import org.apache.ambari.view.hive.ServiceTestUtils;
import org.apache.ambari.view.hive.BaseHiveTest;
import org.apache.ambari.view.hive.client.UserLocalConnection;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.hive.utils.HdfsApiMock;
import org.apache.ambari.view.hive.client.Connection;
import org.apache.ambari.view.hive.client.HiveClientException;
import org.apache.ambari.view.hive.resources.savedQueries.SavedQuery;
import org.apache.ambari.view.hive.resources.savedQueries.SavedQueryService;
import org.apache.ambari.view.hive.utils.BadRequestFormattedException;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.apache.hive.service.cli.thrift.*;
import org.json.simple.JSONObject;
import org.junit.*;
import org.junit.rules.ExpectedException;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.easymock.EasyMock.*;

public class JobServiceTest extends BaseHiveTest {
  private SavedQueryService savedQueryService;
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
  @Before
  public void setUp() throws Exception {
    super.setUp();
    savedQueryService = getService(SavedQueryService.class, handler, context);
    jobService = getService(JobService.class, handler, context);

    Connection hiveConnection = configureHiveConnectionMock();

    new UserLocalConnection().set(hiveConnection, context);
    jobService.setAggregator(
        new Aggregator(
            jobService.getResourceManager(),
            jobService.getOperationHandleResourceManager(),
            new AggregatorTest.MockATSParser())
    );
  }

  @Test
  public void createJobFromQuery() throws IOException, InterruptedException, HdfsApiException {
    setupHdfsApiMock();

    SavedQuery savedQueryForJob = createSavedQuery("Test", null);
    JobService.JobRequest jobCreationRequest = new JobService.JobRequest();
    jobCreationRequest.job = new JobImpl();
    jobCreationRequest.job.setQueryId(savedQueryForJob.getId());

    Response response = jobService.create(jobCreationRequest,
        ServiceTestUtils.getResponseWithLocation(), ServiceTestUtils.getDefaultUriInfo());
    ServiceTestUtils.assertHTTPResponseCreated(response);
    JSONObject jobObj = (JSONObject)response.getEntity();


    assertResponseJobSanity(jobObj);
    Assert.assertEquals(getFieldFromJobJSON(jobObj, "queryId"), savedQueryForJob.getId());
  }

  @Test
  public void createJobForcedContent() throws IOException, InterruptedException, HdfsApiException {
    HdfsApiMock hdfsApiMock = setupHdfsApiMock();

    JobService.JobRequest request = new JobService.JobRequest();
    request.job = new JobImpl();
    request.job.setForcedContent("Hello world");


    Response response = jobService.create(request,
        ServiceTestUtils.getResponseWithLocation(), ServiceTestUtils.getDefaultUriInfo());
    ServiceTestUtils.assertHTTPResponseCreated(response);
    JSONObject jobObj = (JSONObject)response.getEntity();


    assertResponseJobSanity(jobObj);
    Assert.assertNull(getFieldFromJobJSON(jobObj, "queryId"));
    Assert.assertEquals("", getFieldFromJobJSON(jobObj, "forcedContent"));
    Assert.assertEquals("Hello world", hdfsApiMock.getQueryOutputStream().toString());
  }

  @Test
  public void createJobNoSource() throws IOException, InterruptedException {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    expect(hdfsApi.mkdir(anyString())).andReturn(true).anyTimes();
    jobService.getSharedObjectsFactory().setInstance(HdfsApi.class, hdfsApi);
    replay(hdfsApi);

    JobService.JobRequest request = new JobService.JobRequest();
    request.job = new JobImpl();
    request.job.setForcedContent(null);
    request.job.setQueryId(null);

    thrown.expect(BadRequestFormattedException.class);
    jobService.create(request,
        ServiceTestUtils.getResponseWithLocation(), ServiceTestUtils.getDefaultUriInfo());
  }

  private Connection configureHiveConnectionMock() throws HiveClientException {
    TGetOperationStatusResp statusResp = getOperationStatusResp();
    TOperationHandle operationHandle = getExecutionOperationHandle();

    Connection connection = createNiceMock(Connection.class);
    TSessionHandle sessionHandle = new TSessionHandle();
    THandleIdentifier handleIdentifier = new THandleIdentifier();
    handleIdentifier.setGuid(new byte[]{1,2,3,4,5,6,7,8});
    sessionHandle.setSessionId(handleIdentifier);
    expect(connection.openSession()).andReturn(sessionHandle).anyTimes();
    expect(connection.executeAsync((TSessionHandle) anyObject(), anyString())).andReturn(operationHandle).anyTimes();
    expect(connection.getLogs(anyObject(TOperationHandle.class))).andReturn("some logs").anyTimes();
    expect(connection.getOperationStatus(anyObject(TOperationHandle.class))).andReturn(statusResp).anyTimes();

    replay(connection);
    return connection;
  }

  private TGetOperationStatusResp getOperationStatusResp() {
    TStatus status = new TStatus();
    status.setStatusCode(TStatusCode.SUCCESS_STATUS);

    TGetOperationStatusResp statusResp = new TGetOperationStatusResp();
    statusResp.setStatus(status);

    return statusResp;
  }

  private TOperationHandle getExecutionOperationHandle() {
    THandleIdentifier handleIdentifier = new THandleIdentifier();
    handleIdentifier.setGuid("some guid".getBytes());
    handleIdentifier.setSecret("some secret".getBytes());

    TOperationHandle operationHandle = new TOperationHandle();
    operationHandle.setHasResultSet(true);
    operationHandle.setModifiedRowCount(0);
    operationHandle.setOperationType(TOperationType.EXECUTE_STATEMENT);
    operationHandle.setOperationId(handleIdentifier);
    return operationHandle;
  }

  @Override
  protected void setupProperties(Map<String, String> properties, File baseDir) throws Exception {
    super.setupProperties(properties, baseDir);
    properties.put("scripts.dir", "/tmp/.hiveQueries");
    properties.put("jobs.dir", "/tmp/.hiveJobs");
  }

  public static Response doCreateSavedQuery(String title, String path, SavedQueryService service) {
    SavedQueryService.SavedQueryRequest request = new SavedQueryService.SavedQueryRequest();
    request.savedQuery = new SavedQuery();
    request.savedQuery.setTitle(title);
    request.savedQuery.setQueryFile(path);

    return service.create(request,
        ServiceTestUtils.getResponseWithLocation(), ServiceTestUtils.getDefaultUriInfo());
  }

  private SavedQuery createSavedQuery(String title, String path) {
    Response response = doCreateSavedQuery(title, path, savedQueryService);
    JSONObject obj = (JSONObject)response.getEntity();
    SavedQuery query = ((SavedQuery) obj.get("savedQuery"));
    return query;
  }


  private Object getFieldFromJobJSON(JSONObject jobObj, String field) {
    return ((Map) jobObj.get("job")).get(field);
  }

  private HdfsApiMock setupHdfsApiMock() throws IOException, InterruptedException, HdfsApiException {
    HdfsApiMock hdfsApiMock = new HdfsApiMock("select * from Z");
    HdfsApi hdfsApi = hdfsApiMock.getHdfsApi();
    jobService.getSharedObjectsFactory().setInstance(HdfsApi.class, hdfsApi);
    replay(hdfsApi);
    return hdfsApiMock;
  }

  private void assertResponseJobSanity(JSONObject jobObj) {
    Assert.assertTrue(jobObj.containsKey("job"));
    Assert.assertNotNull(((Map) jobObj.get("job")).get("id"));
    Assert.assertNotNull(((Map) jobObj.get("job")).get("queryFile"));
  }

}
