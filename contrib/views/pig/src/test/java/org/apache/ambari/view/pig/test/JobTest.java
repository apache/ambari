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

package org.apache.ambari.view.pig.test;

import org.apache.ambari.view.pig.BasePigTest;
import org.apache.ambari.view.pig.resources.jobs.JobService;
import org.apache.ambari.view.pig.resources.jobs.models.PigJob;
import org.apache.ambari.view.pig.templeton.client.TempletonApi;
import org.apache.ambari.view.pig.utils.BadRequestFormattedException;
import org.apache.ambari.view.pig.utils.HdfsApi;
import org.apache.ambari.view.pig.utils.NotFoundFormattedException;
import org.apache.ambari.view.pig.utils.ServiceFormattedException;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.json.simple.JSONObject;
import org.junit.*;
import org.junit.rules.ExpectedException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.net.URI;
import java.util.HashMap;

import static org.easymock.EasyMock.*;

public class JobTest extends BasePigTest {
  private JobService jobService;
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    jobService = getService(JobService.class, handler, context);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
    jobService.getResourceManager().setTempletonApi(null);
    JobService.setHdfsApi(null);
  }

  private Response doCreateJob(String title, String pigScript, String templetonArguments) {
    return doCreateJob(title, pigScript, templetonArguments, null);
  }

  private Response doCreateJob(String title, String pigScript, String templetonArguments, String forcedContent) {
    JobService.PigJobRequest request = new JobService.PigJobRequest();
    request.job = new PigJob();
    request.job.setTitle(title);
    request.job.setPigScript(pigScript);
    request.job.setTempletonArguments(templetonArguments);
    request.job.setForcedContent(forcedContent);

    UriInfo uriInfo = createNiceMock(UriInfo.class);
    URI uri = UriBuilder.fromUri("http://host/a/b").build();
    expect(uriInfo.getAbsolutePath()).andReturn(uri);

    HttpServletResponse resp_obj = createStrictMock(HttpServletResponse.class);

    resp_obj.setHeader(eq("Location"), anyString());

    replay(uriInfo, resp_obj);
    return jobService.runJob(request, resp_obj, uriInfo);
  }

  @Test
  public void testSubmitJob() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    expect(hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"))).andReturn(true);

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    JobService.setHdfsApi(hdfsApi);

    TempletonApi api = createNiceMock(TempletonApi.class);
    jobService.getResourceManager().setTempletonApi(api);
    TempletonApi.JobData data = api.new JobData();
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    Response response = doCreateJob("Test", "/tmp/script.pig", "-useHCatalog");

    Assert.assertEquals("-useHCatalog", do_stream.toString());
    Assert.assertEquals(201, response.getStatus());

    JSONObject obj = (JSONObject)response.getEntity();
    Assert.assertTrue(obj.containsKey("job"));
    Assert.assertNotNull(((PigJob) obj.get("job")).getId());
    Assert.assertFalse(((PigJob) obj.get("job")).getId().isEmpty());
    Assert.assertTrue(((PigJob) obj.get("job")).getStatusDir().startsWith("/tmp/.pigjobs/ambari-qa/test"));

    PigJob job = ((PigJob) obj.get("job"));
    Assert.assertEquals(PigJob.Status.SUBMITTED, job.getStatus());
    Assert.assertTrue(job.isInProgress());
  }

  @Test
  public void testSubmitJobUsernameProvided() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    expect(hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"))).andReturn(true);

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    JobService.setHdfsApi(hdfsApi);

    TempletonApi api = createNiceMock(TempletonApi.class);
    jobService.getResourceManager().setTempletonApi(api);
    TempletonApi.JobData data = api.new JobData();
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    properties.put("dataworker.username", "luke");
    Response response = doCreateJob("Test", "/tmp/script.pig", "-useHCatalog");
    JSONObject obj = (JSONObject)response.getEntity();
    Assert.assertTrue(obj.containsKey("job"));
    Assert.assertTrue(((PigJob) obj.get("job")).getStatusDir().startsWith("/tmp/.pigjobs/luke/test"));
  }

  @Test
  public void testSubmitJobNoArguments() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    expect(hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"))).andReturn(true);

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    JobService.setHdfsApi(hdfsApi);

    TempletonApi api = createNiceMock(TempletonApi.class);
    jobService.getResourceManager().setTempletonApi(api);
    TempletonApi.JobData data = api.new JobData();
    expect(api.runPigQuery((File) anyObject(), anyString(), (String) isNull())).andReturn(data);
    replay(api);

    Response response = doCreateJob("Test", "/tmp/script.pig", null);

    Assert.assertEquals("", do_stream.toString());
    Assert.assertEquals(201, response.getStatus());

    JSONObject obj = (JSONObject)response.getEntity();
    Assert.assertTrue(obj.containsKey("job"));
    Assert.assertNotNull(((PigJob) obj.get("job")).getId());
    Assert.assertFalse(((PigJob) obj.get("job")).getId().isEmpty());
    Assert.assertTrue(((PigJob) obj.get("job")).getStatusDir().startsWith("/tmp/.pigjobs/ambari-qa/test"));

    PigJob job = ((PigJob) obj.get("job"));
    Assert.assertEquals(PigJob.Status.SUBMITTED, job.getStatus());
    Assert.assertTrue(job.isInProgress());
  }

  @Test
  public void testSubmitJobNoFile() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    expect(hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"))).andReturn(true);

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    JobService.setHdfsApi(hdfsApi);

    TempletonApi api = createNiceMock(TempletonApi.class);
    jobService.getResourceManager().setTempletonApi(api);
    TempletonApi.JobData data = api.new JobData();
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    thrown.expect(ServiceFormattedException.class);
    doCreateJob("Test", null, "-useHCatalog");
  }

  @Test
  public void testSubmitJobForcedContent() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);

    ByteArrayOutputStream baScriptStream = new ByteArrayOutputStream();
    ByteArrayOutputStream baTempletonArgsStream = new ByteArrayOutputStream();

    FSDataOutputStream scriptStream = new FSDataOutputStream(baScriptStream);
    FSDataOutputStream templetonArgsStream = new FSDataOutputStream(baTempletonArgsStream);
    expect(hdfsApi.create(endsWith("script.pig"), eq(true))).andReturn(scriptStream);
    expect(hdfsApi.create(endsWith("params"), eq(true))).andReturn(templetonArgsStream);
    replay(hdfsApi);
    JobService.setHdfsApi(hdfsApi);

    TempletonApi api = createNiceMock(TempletonApi.class);
    jobService.getResourceManager().setTempletonApi(api);
    TempletonApi.JobData data = api.new JobData();
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    Response response = doCreateJob("Test", null, "-useHCatalog", "pwd");  // with forcedContent
    Assert.assertEquals(201, response.getStatus());
    Assert.assertEquals("-useHCatalog", baTempletonArgsStream.toString());
    Assert.assertEquals("pwd", baScriptStream.toString());
  }

  @Test
  public void testSubmitJobNoTitle() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    expect(hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"))).andReturn(true);

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    JobService.setHdfsApi(hdfsApi);

    TempletonApi api = createNiceMock(TempletonApi.class);
    jobService.getResourceManager().setTempletonApi(api);
    TempletonApi.JobData data = api.new JobData();
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    thrown.expect(BadRequestFormattedException.class);
    doCreateJob(null, "/tmp/1.pig", "-useHCatalog");
  }

  @Test
  public void testSubmitJobFailed() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    expect(hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"))).andReturn(false);

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    JobService.setHdfsApi(hdfsApi);

    TempletonApi api = createNiceMock(TempletonApi.class);
    jobService.getResourceManager().setTempletonApi(api);
    TempletonApi.JobData data = api.new JobData();
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    thrown.expect(ServiceFormattedException.class);
    doCreateJob("Test", "/tmp/script.pig", "-useHCatalog");
  }

  @Test
  public void testSubmitJobTempletonError() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    expect(hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"))).andReturn(true);

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    JobService.setHdfsApi(hdfsApi);

    TempletonApi api = createNiceMock(TempletonApi.class);
    jobService.getResourceManager().setTempletonApi(api);
    TempletonApi.JobData data = api.new JobData();
    // Templeton returns 500 e.g.
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andThrow(new IOException());
    replay(api);

    thrown.expect(ServiceFormattedException.class);
    doCreateJob("Test", "/tmp/script.pig", "-useHCatalog");
  }

  @Test
  public void testKillJob() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    expect(hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"))).andReturn(true);

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    JobService.setHdfsApi(hdfsApi);

    TempletonApi api = createStrictMock(TempletonApi.class);
    jobService.getResourceManager().setTempletonApi(api);
    TempletonApi.JobData data = api.new JobData();
    data.id = "job_id_##";
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    Response response = doCreateJob("Test", "/tmp/script.pig", "-useHCatalog");
    Assert.assertEquals(201, response.getStatus());

    reset(api);
    api.killJob(eq("job_id_##"));
    replay(api);
    JSONObject obj = (JSONObject)response.getEntity();
    PigJob job = ((PigJob)obj.get("job"));
    response = jobService.killJob(job.getId());
    Assert.assertEquals(204, response.getStatus());
  }

  @Test
  public void testJobStatusFlow() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    expect(hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"))).andReturn(true);

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    JobService.setHdfsApi(hdfsApi);

    TempletonApi api = createNiceMock(TempletonApi.class);
    jobService.getResourceManager().setTempletonApi(api);
    TempletonApi.JobData data = api.new JobData();
    data.id = "job_id_#";
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    Response response = doCreateJob("Test", "/tmp/script.pig", "-useHCatalog");

    Assert.assertEquals("-useHCatalog", do_stream.toString());
    Assert.assertEquals(201, response.getStatus());

    PigJob job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.Status.SUBMITTED, job.getStatus());
    Assert.assertTrue(job.isInProgress());

    // Retrieve status:
    // SUBMITTED
    reset(api);
    TempletonApi.JobInfo info = api.new JobInfo();
    expect(api.checkJob(eq("job_id_#"))).andReturn(info);
    replay(api);
    response = jobService.getJob(job.getId());
    Assert.assertEquals(200, response.getStatus());
    job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.Status.SUBMITTED, job.getStatus());

    // RUNNING
    reset(api);
    info = api.new JobInfo();
    info.status = new HashMap<String, Object>();
    info.status.put("runState", (double)PigJob.RUN_STATE_RUNNING);
    info.percentComplete = "30% complete";
    expect(api.checkJob(eq("job_id_#"))).andReturn(info);
    replay(api);
    response = jobService.getJob(job.getId());
    Assert.assertEquals(200, response.getStatus());
    job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.Status.RUNNING, job.getStatus());
    Assert.assertTrue(job.isInProgress());
    Assert.assertEquals(30, (Object) job.getPercentComplete());

    // SUCCEED
    reset(api);
    info = api.new JobInfo();
    info.status = new HashMap<String, Object>();
    info.status.put("runState", (double)PigJob.RUN_STATE_SUCCEEDED);
    expect(api.checkJob(eq("job_id_#"))).andReturn(info);
    replay(api);
    response = jobService.getJob(job.getId());
    Assert.assertEquals(200, response.getStatus());
    job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.Status.COMPLETED, job.getStatus());
    Assert.assertFalse(job.isInProgress());
    Assert.assertNull(job.getPercentComplete());

    // PREP
    reset(api);
    info = api.new JobInfo();
    info.status = new HashMap<String, Object>();
    info.status.put("runState", (double)PigJob.RUN_STATE_PREP);
    expect(api.checkJob(eq("job_id_#"))).andReturn(info);
    replay(api);
    response = jobService.getJob(job.getId());
    Assert.assertEquals(200, response.getStatus());
    job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.Status.RUNNING, job.getStatus());

    // FAILED
    reset(api);
    info = api.new JobInfo();
    info.status = new HashMap<String, Object>();
    info.status.put("runState", (double)PigJob.RUN_STATE_FAILED);
    expect(api.checkJob(eq("job_id_#"))).andReturn(info);
    replay(api);
    response = jobService.getJob(job.getId());
    Assert.assertEquals(200, response.getStatus());
    job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.Status.FAILED, job.getStatus());
    Assert.assertFalse(job.isInProgress());

    // KILLED
    reset(api);
    info = api.new JobInfo();
    info.status = new HashMap<String, Object>();
    info.status.put("runState", (double)PigJob.RUN_STATE_KILLED);
    expect(api.checkJob(eq("job_id_#"))).andReturn(info);
    replay(api);
    response = jobService.getJob(job.getId());
    Assert.assertEquals(200, response.getStatus());
    job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.Status.KILLED, job.getStatus());
    Assert.assertFalse(job.isInProgress());
  }
}
