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

import org.apache.ambari.view.hive.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive.resources.IResourceManager;
import org.apache.ambari.view.hive.resources.jobs.atsJobs.HiveQueryId;
import org.apache.ambari.view.hive.resources.jobs.atsJobs.IATSParser;
import org.apache.ambari.view.hive.resources.jobs.atsJobs.TezDagId;
import org.apache.ambari.view.hive.resources.jobs.atsJobs.TezVertexId;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.JobImpl;
import org.apache.hive.service.cli.thrift.TOperationHandle;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class AggregatorTest {

  public static final String SOME_QUERY = "some query";

  @Test
  public void testReadJobOutsideOfHS2() throws Exception {
    HiveQueryId hiveQueryId = getSampleHiveQueryId("ENTITY-NAME");
    ensureOperationIdUnset(hiveQueryId);

    MockATSParser atsParser = getMockATSWithQueries(hiveQueryId);


    Aggregator aggregator = new Aggregator(getEmptyJobResourceManager(),
        getEmptyOperationHandleResourceManager(),
        atsParser);

    List<Job> aggregated = aggregator.readAll("luke");

    Assert.assertEquals(1, aggregated.size());
    Job job = aggregated.get(0);
    Assert.assertEquals("ENTITY-NAME", job.getId());
    Assert.assertEquals(SOME_QUERY, job.getTitle());
  }

  @Test
  public void testReadJobWithHS2OutsideOfView() throws Exception {
    HiveQueryId hiveQueryId = getSampleHiveQueryId("ENTITY-NAME");
    ensureOperationIdSet(hiveQueryId);

    MockATSParser atsParser = getMockATSWithQueries(hiveQueryId);
    Aggregator aggregator = new Aggregator(getEmptyJobResourceManager(),
        getEmptyOperationHandleResourceManager(),
        atsParser);

    List<Job> aggregated = aggregator.readAll("luke");

    Assert.assertEquals(1, aggregated.size());
    Job job = aggregated.get(0);
    Assert.assertEquals("ENTITY-NAME", job.getId());
    Assert.assertEquals(SOME_QUERY, job.getTitle());
  }

  @Test
  public void testJobWithoutOperationIdShouldBeIgnored() throws Exception {
    MockJobResourceManager jobResourceManager = getJobResourceManagerWithJobs(getSampleViewJob("1"));

    Aggregator aggregator = new Aggregator(jobResourceManager,
        getEmptyOperationHandleResourceManager(),
        getEmptyATSParser());

    List<Job> aggregated = aggregator.readAll("luke");

    Assert.assertEquals(0, aggregated.size());
  }

  @Test
  public void testReadJobOnlyInView() throws Exception {
    MockJobResourceManager jobResourceManager = getJobResourceManagerWithJobs(getSampleViewJob("1"));

    StoredOperationHandle operationHandle = getSampleOperationHandle("5", "1");
    MockOperationHandleResourceManager operationHandleResourceManager = getOperationHandleRMWithEntities(operationHandle);

    Aggregator aggregator = new Aggregator(jobResourceManager,
        operationHandleResourceManager,
        getEmptyATSParser());

    List<Job> aggregated = aggregator.readAll("luke");

    Assert.assertEquals(1, aggregated.size());
    Job job = aggregated.get(0);
    Assert.assertEquals("1", job.getId());
  }

  private MockOperationHandleResourceManager getOperationHandleRMWithEntities(StoredOperationHandle... operationHandles) {
    MockOperationHandleResourceManager operationHandleResourceManager = getEmptyOperationHandleResourceManager();
    HashMap<String, StoredOperationHandle> storage = new HashMap<String, StoredOperationHandle>();
    for (StoredOperationHandle handle : operationHandles) {
      storage.put(handle.getJobId(), handle);
    }
    operationHandleResourceManager.setStorage(storage);
    return operationHandleResourceManager;
  }

  @Test
  public void testReadJobBothATSAndView() throws Exception {
    HiveQueryId hiveQueryId = getSampleHiveQueryId("ENTITY-NAME");
    hiveQueryId.operationId = Aggregator.hexStringToUrlSafeBase64("1b2b");
    MockATSParser atsParser = getMockATSWithQueries(hiveQueryId);

    MockJobResourceManager jobResourceManager = getJobResourceManagerWithJobs(getSampleViewJob("1"));

    StoredOperationHandle operationHandle = getSampleOperationHandle("5", "1");
    operationHandle.setGuid("1b2b");
    MockOperationHandleResourceManager operationHandleResourceManager = getOperationHandleRMWithEntities(operationHandle);

    Aggregator aggregator = new Aggregator(jobResourceManager,
        operationHandleResourceManager,
        atsParser);

    List<Job> aggregated = aggregator.readAll("luke");

    Assert.assertEquals(1, aggregated.size());
    Job job = aggregated.get(0);
    Assert.assertEquals("1", job.getId());
  }

  @Test
  public void testReadJobBothATSAndViewV2() throws Exception {
    HiveQueryId hiveQueryId = getSampleHiveQueryIdV2("ENTITY-NAME");
    hiveQueryId.operationId = Aggregator.hexStringToUrlSafeBase64("1b2b");
    MockATSParser atsParser = getMockATSWithQueries(hiveQueryId);

    MockJobResourceManager jobResourceManager = getJobResourceManagerWithJobs(getSampleViewJob("1"));

    StoredOperationHandle operationHandle = getSampleOperationHandle("5", "1");
    operationHandle.setGuid("1b2b");
    MockOperationHandleResourceManager operationHandleResourceManager = getOperationHandleRMWithEntities(operationHandle);

    Aggregator aggregator = new Aggregator(jobResourceManager,
      operationHandleResourceManager,
      atsParser);

    List<Job> aggregated = aggregator.readAll("luke");

    Assert.assertEquals(1, aggregated.size());
    Job job = aggregated.get(0);
    Assert.assertEquals("1", job.getId());
    Assert.assertEquals("app_test_1", job.getApplicationId());
    Assert.assertEquals("ENTITY-NAME", job.getDagId());
    Assert.assertEquals("SUCCEEDED", job.getStatus());
  }


  @Test
  public void testReadJobComplex() throws Exception {
    //job both on ATS and View
    HiveQueryId hiveQueryId1 = getSampleHiveQueryId("ENTITY-NAME");
    hiveQueryId1.operationId = Aggregator.hexStringToUrlSafeBase64("1a1b");
    Job job1 = getSampleViewJob("1");
    StoredOperationHandle operationHandle1 = getSampleOperationHandle("5", "1");
    operationHandle1.setGuid("1a1b");

    //job only on ATS
    HiveQueryId hiveQueryId2 = getSampleHiveQueryId("ENTITY-NAME2");
    hiveQueryId2.operationId = Aggregator.hexStringToUrlSafeBase64("2a2a");

    //job only in View
    Job job3 = getSampleViewJob("3");
    StoredOperationHandle operationHandle3 = getSampleOperationHandle("6", "3");
    operationHandle3.setGuid("3c3d");


    MockATSParser atsParser = getMockATSWithQueries(
        hiveQueryId1, hiveQueryId2);
    MockJobResourceManager jobResourceManager = getJobResourceManagerWithJobs(
        job1, job3);
    MockOperationHandleResourceManager operationHandleRM = getOperationHandleRMWithEntities(
        operationHandle1, operationHandle3);

    Aggregator aggregator = new Aggregator(jobResourceManager,
        operationHandleRM,
        atsParser);

    List<Job> aggregated = aggregator.readAll("luke");

    Assert.assertEquals(3, aggregated.size());
  }

  private MockJobResourceManager getJobResourceManagerWithJobs(Job... jobs) {
    MockJobResourceManager jobResourceManager = getEmptyJobResourceManager();
    jobResourceManager.setJobs(Arrays.asList(jobs));
    return jobResourceManager;
  }

  private MockATSParser getEmptyATSParser() {
    return new MockATSParser();
  }

  private void ensureOperationIdUnset(HiveQueryId hiveQueryId) {
    hiveQueryId.operationId = null;
  }

  public void ensureOperationIdSet(HiveQueryId hiveQueryId) {
    hiveQueryId.operationId = "operation-id";
  }

  private MockOperationHandleResourceManager getEmptyOperationHandleResourceManager() {
    return new MockOperationHandleResourceManager();
  }

  private MockJobResourceManager getEmptyJobResourceManager() {
    return new MockJobResourceManager();
  }

  private MockATSParser getMockATSWithQueries(HiveQueryId... hiveQueryIds) {
    MockATSParser atsParser = getEmptyATSParser();
    atsParser.setHiveQueryIds(Arrays.asList(hiveQueryIds));
    return atsParser;
  }

  private JobImpl getSampleViewJob(String id) {
    JobImpl job = new JobImpl();
    job.setTitle("Test");
    job.setId(id);
    job.setOwner("luke");
    return job;
  }

  private StoredOperationHandle getSampleOperationHandle(String id, String jobId) {
    StoredOperationHandle opHandle = new StoredOperationHandle();
    opHandle.setId(id);
    opHandle.setJobId(jobId);
    opHandle.setGuid("1b2b");
    return opHandle;
  }

  private HiveQueryId getSampleHiveQueryId(String id) {
    HiveQueryId hiveQueryId = new HiveQueryId();
    hiveQueryId.entity = id;
    hiveQueryId.query = SOME_QUERY;
    hiveQueryId.user = "luke";
    hiveQueryId.operationId = "fUjdt-VMRYuKRPCDTUr_rg";
    hiveQueryId.dagNames = new LinkedList<String>();
    return hiveQueryId;
  }

  private HiveQueryId getSampleHiveQueryIdV2(String id) {
    HiveQueryId hiveQueryId = getSampleHiveQueryId(id);
    hiveQueryId.version = HiveQueryId.ATS_15_RESPONSE_VERSION;
    return hiveQueryId;
  }

  @Test
  public void testGetJobByOperationId() throws Exception {

  }

  @Test
  public void testUrlSafeBase64ToHexString() throws Exception {
    String urlSafe = Aggregator.hexStringToUrlSafeBase64("1a1b");
    Assert.assertEquals("Ghs", urlSafe);
  }

  @Test
  public void testHexStringToUrlSafeBase64() throws Exception {
    String hex = Aggregator.urlSafeBase64ToHexString("Ghs");
    Assert.assertEquals("1a1b", hex);
  }

  public static class MockJobResourceManager implements IResourceManager<Job> {

    private List<Job> jobs = new LinkedList<Job>();

    @Override
    public Job create(Job object) {
      return null;
    }

    @Override
    public Job read(Object id) throws ItemNotFound {
      for(Job job : jobs) {
        if (job.getId().equals(id)) {
          return job;
        }
      }
      throw new ItemNotFound();
    }

    @Override
    public List<Job> readAll(FilteringStrategy filteringStrategy) {
      return jobs;
    }

    @Override
    public Job update(Job newObject, String id) throws ItemNotFound {
      return null;
    }

    @Override
    public void delete(Object resourceId) throws ItemNotFound {

    }

    public List<Job> getJobs() {
      return jobs;
    }

    public void setJobs(List<Job> jobs) {
      this.jobs = jobs;
    }
  }

  public static class MockOperationHandleResourceManager implements IOperationHandleResourceManager {
    private HashMap<String, StoredOperationHandle> storage = new HashMap<String, StoredOperationHandle>();

    public MockOperationHandleResourceManager() {

    }

    @Override
    public List<StoredOperationHandle> readJobRelatedHandles(Job job) {
      LinkedList<StoredOperationHandle> storedOperationHandles = new LinkedList<StoredOperationHandle>();
      StoredOperationHandle operationHandle = storage.get(job.getId());
      if (operationHandle != null)
        storedOperationHandles.add(operationHandle);
      return storedOperationHandles;
    }

    @Override
    public List<Job> getHandleRelatedJobs(StoredOperationHandle operationHandle) {
      return new LinkedList<Job>();
    }

    @Override
    public Job getJobByHandle(StoredOperationHandle handle) throws ItemNotFound {
      throw new ItemNotFound();
    }

    @Override
    public void putHandleForJob(TOperationHandle h, Job job) {

    }

    @Override
    public boolean containsHandleForJob(Job job) {
      return false;
    }

    @Override
    public StoredOperationHandle getHandleForJob(Job job) throws ItemNotFound {
      List<StoredOperationHandle> handles = readJobRelatedHandles(job);
      if (handles.size() == 0)
        throw new ItemNotFound();
      return handles.get(0);
    }

    @Override
    public StoredOperationHandle create(StoredOperationHandle object) {
      return null;
    }

    @Override
    public StoredOperationHandle read(Object id) throws ItemNotFound {
      return null;
    }

    @Override
    public List<StoredOperationHandle> readAll(FilteringStrategy filteringStrategy) {
      LinkedList<StoredOperationHandle> storedOperationHandles = new LinkedList<StoredOperationHandle>();
      for (StoredOperationHandle handle : storage.values()) {
        if (filteringStrategy.isConform(handle))
          storedOperationHandles.add(handle);
      }
      return storedOperationHandles;
    }

    @Override
    public StoredOperationHandle update(StoredOperationHandle newObject, String id) throws ItemNotFound {
      return null;
    }

    @Override
    public void delete(Object resourceId) throws ItemNotFound {

    }

    public HashMap<String, StoredOperationHandle> getStorage() {
      return storage;
    }

    public void setStorage(HashMap<String, StoredOperationHandle> storage) {
      this.storage = storage;
    }
  }

  public static class MockATSParser implements IATSParser {

    private List<HiveQueryId> hiveQueryIds = new LinkedList<HiveQueryId>();

    public MockATSParser() {
    }

    @Override
    public List<HiveQueryId> getHiveQueryIdsList(String username) {
      return hiveQueryIds;
    }

    @Override
    public List<TezVertexId> getVerticesForDAGId(String dagId) {
      List<TezVertexId> vertices = new LinkedList<TezVertexId>();
      TezVertexId tezVertexId1 = new TezVertexId();
      tezVertexId1.entity = "vertex_1234567_99_99_01";
      tezVertexId1.vertexName = "Map 1";
      vertices.add(tezVertexId1);

      TezVertexId tezVertexId2 = new TezVertexId();
      tezVertexId2.entity = "vertex_1234567_99_99_00";
      tezVertexId2.vertexName = "Reduce 1";
      vertices.add(tezVertexId2);
      return vertices;
    }

    @Override
    public HiveQueryId getHiveQueryIdByOperationId(String guid) {
      return new HiveQueryId();
    }

    @Override
    public TezDagId getTezDAGByName(String name) {
      return new TezDagId();
    }

    @Override
    public TezDagId getTezDAGByEntity(String entity) {
      TezDagId dagId = new TezDagId();
      dagId.applicationId = "app_test_1";
      dagId.entity = entity;
      dagId.status = "SUCCEEDED";
      return dagId;
    }

    public List<HiveQueryId> getHiveQueryIds() {
      return hiveQueryIds;
    }

    public void setHiveQueryIds(List<HiveQueryId> hiveQueryIds) {
      this.hiveQueryIds = hiveQueryIds;
    }
  }
}