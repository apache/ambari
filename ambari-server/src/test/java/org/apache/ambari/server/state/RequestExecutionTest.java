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
package org.apache.ambari.server.state;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.Transactional;
import junit.framework.Assert;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.RequestScheduleDAO;
import org.apache.ambari.server.orm.entities.RequestScheduleBatchRequestEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleEntity;
import org.apache.ambari.server.state.scheduler.Batch;
import org.apache.ambari.server.state.scheduler.BatchRequest;
import org.apache.ambari.server.state.scheduler.BatchSettings;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.scheduler.Schedule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RequestExecutionTest {
  private Injector injector;
  private Clusters clusters;
  private Cluster cluster;
  private String clusterName;
  private AmbariMetaInfo metaInfo;
  private RequestExecutionFactory requestExecutionFactory;
  private RequestScheduleDAO requestScheduleDAO;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    metaInfo = injector.getInstance(AmbariMetaInfo.class);
    requestExecutionFactory = injector.getInstance(RequestExecutionFactory.class);
    requestScheduleDAO = injector.getInstance(RequestScheduleDAO.class);

    metaInfo.init();
    clusterName = "foo";
    clusters.addCluster(clusterName);
    cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    Assert.assertNotNull(cluster);
    clusters.addHost("h1");
    clusters.addHost("h2");
    clusters.addHost("h3");
    Assert.assertNotNull(clusters.getHost("h1"));
    Assert.assertNotNull(clusters.getHost("h2"));
    Assert.assertNotNull(clusters.getHost("h3"));
    clusters.getHost("h1").persist();
    clusters.getHost("h2").persist();
    clusters.getHost("h3").persist();
  }

  @After
  public void teardown() throws Exception {
    injector.getInstance(PersistService.class).stop();
  }

  @Transactional
  private RequestExecution createRequestSchedule() throws Exception {
    Batch batches = new Batch();
    Schedule schedule = new Schedule();

    BatchSettings batchSettings = new BatchSettings();
    batchSettings.setTaskFailureToleranceLimit(10);
    batches.setBatchSettings(batchSettings);

    List<BatchRequest> batchRequests = new ArrayList<BatchRequest>();
    BatchRequest batchRequest1 = new BatchRequest();
    batchRequest1.setOrderId(10L);
    batchRequest1.setType(BatchRequest.Type.DELETE);
    batchRequest1.setUri("testUri1");

    BatchRequest batchRequest2 = new BatchRequest();
    batchRequest2.setOrderId(12L);
    batchRequest2.setType(BatchRequest.Type.POST);
    batchRequest2.setUri("testUri2");
    batchRequest2.setBody("testBody");

    batchRequests.add(batchRequest1);
    batchRequests.add(batchRequest2);

    batches.getBatchRequests().addAll(batchRequests);

    schedule.setMinutes("10");
    schedule.setEndTime("2014-01-01 00:00:00");

    RequestExecution requestExecution = requestExecutionFactory.createNew
      (cluster, batches, schedule);
    requestExecution.setDescription("Test Schedule");

    requestExecution.persist();

    return requestExecution;
  }

  @Test
  public void testCreateRequestSchedule() throws Exception {
    RequestExecution requestExecution = createRequestSchedule();
    Assert.assertNotNull(requestExecution);

    RequestScheduleEntity scheduleEntity = requestScheduleDAO.findById
      (requestExecution.getId());

    Assert.assertNotNull(scheduleEntity);
    Assert.assertEquals(requestExecution.getBatch().getBatchSettings()
      .getTaskFailureToleranceLimit(), scheduleEntity.getBatchTolerationLimit());
    Assert.assertEquals(scheduleEntity.getRequestScheduleBatchRequestEntities().size(), 2);
    Collection<RequestScheduleBatchRequestEntity> batchRequestEntities =
      scheduleEntity.getRequestScheduleBatchRequestEntities();
    Assert.assertNotNull(batchRequestEntities);
    RequestScheduleBatchRequestEntity reqEntity1 = null;
    RequestScheduleBatchRequestEntity reqEntity2 = null;
    for (RequestScheduleBatchRequestEntity reqEntity : batchRequestEntities) {
      if (reqEntity.getRequestUri().equals("testUri1")) {
        reqEntity1 = reqEntity;
      } else if (reqEntity.getRequestUri().equals("testUri2")) {
        reqEntity2 = reqEntity;
      }
    }
    Assert.assertNotNull(reqEntity1);
    Assert.assertNotNull(reqEntity2);
    Assert.assertEquals(Long.valueOf(1L), reqEntity1.getBatchId());
    Assert.assertEquals(Long.valueOf(2L), reqEntity2.getBatchId());
    Assert.assertEquals(BatchRequest.Type.DELETE.name(), reqEntity1.getRequestType());
    Assert.assertEquals(BatchRequest.Type.POST.name(), reqEntity2.getRequestType());
    Assert.assertEquals(requestExecution.getSchedule().getMinutes(),
      scheduleEntity.getMinutes());
    Assert.assertEquals(requestExecution.getSchedule().getEndTime(),
      scheduleEntity.getEndTime());
  }

  @Test
  public void testUpdateRequestSchedule() throws Exception {
    RequestExecution requestExecution = createRequestSchedule();
    Assert.assertNotNull(requestExecution);
    Long id = requestExecution.getId();
    RequestScheduleEntity scheduleEntity = requestScheduleDAO.findById(id);
    Assert.assertNotNull(scheduleEntity);

    // Read from DB
    requestExecution = requestExecutionFactory.createExisting(cluster,
      scheduleEntity);

    // Remove host and add host
    Batch batches = new Batch();

    List<BatchRequest> batchRequests = new ArrayList<BatchRequest>();
    BatchRequest batchRequest1 = new BatchRequest();
    batchRequest1.setOrderId(10L);
    batchRequest1.setType(BatchRequest.Type.PUT);
    batchRequest1.setUri("testUri3");

    BatchRequest batchRequest2 = new BatchRequest();
    batchRequest2.setOrderId(12L);
    batchRequest2.setType(BatchRequest.Type.POST);
    batchRequest2.setUri("testUri4");
    batchRequest2.setBody("testBody");

    batchRequests.add(batchRequest1);
    batchRequests.add(batchRequest2);

    batches.getBatchRequests().addAll(batchRequests);


    requestExecution.setBatch(batches);

    // Change schedule
    requestExecution.getSchedule().setHours("11");

    // Save
    requestExecution.persist();

    scheduleEntity = requestScheduleDAO.findById(id);
    Assert.assertNotNull(scheduleEntity);
    Collection<RequestScheduleBatchRequestEntity> batchRequestEntities =
      scheduleEntity.getRequestScheduleBatchRequestEntities();
    Assert.assertNotNull(batchRequestEntities);
    RequestScheduleBatchRequestEntity reqEntity1 = null;
    RequestScheduleBatchRequestEntity reqEntity2 = null;
    for (RequestScheduleBatchRequestEntity reqEntity : batchRequestEntities) {
      if (reqEntity.getRequestUri().equals("testUri3")) {
        reqEntity1 = reqEntity;
      } else if (reqEntity.getRequestUri().equals("testUri4")) {
        reqEntity2 = reqEntity;
      }
    }
    Assert.assertNotNull(reqEntity1);
    Assert.assertNotNull(reqEntity2);
    Assert.assertEquals(Long.valueOf(1L), reqEntity1.getBatchId());
    Assert.assertEquals(Long.valueOf(2L), reqEntity2.getBatchId());
    Assert.assertEquals(BatchRequest.Type.PUT.name(), reqEntity1.getRequestType());
    Assert.assertEquals(BatchRequest.Type.POST.name(), reqEntity2.getRequestType());
    Assert.assertEquals("11", scheduleEntity.getHours());
  }

  @Test
  public void testGetRequestSchedule() throws Exception {
    RequestExecution requestExecution = createRequestSchedule();
    Assert.assertNotNull(requestExecution);

    RequestScheduleEntity scheduleEntity = requestScheduleDAO.findById
      (requestExecution.getId());
    Assert.assertNotNull(scheduleEntity);

    Assert.assertNotNull(cluster.getAllRequestExecutions().get
      (requestExecution.getId()));

    Assert.assertNotNull(scheduleEntity);
    Assert.assertEquals(requestExecution.getBatch().getBatchSettings()
      .getTaskFailureToleranceLimit(), scheduleEntity.getBatchTolerationLimit());
    Assert.assertEquals(scheduleEntity.getRequestScheduleBatchRequestEntities().size(), 2);
    Collection<RequestScheduleBatchRequestEntity> batchRequestEntities =
      scheduleEntity.getRequestScheduleBatchRequestEntities();
    Assert.assertNotNull(batchRequestEntities);
    RequestScheduleBatchRequestEntity reqEntity1 = null;
    RequestScheduleBatchRequestEntity reqEntity2 = null;
    for (RequestScheduleBatchRequestEntity reqEntity : batchRequestEntities) {
      if (reqEntity.getRequestUri().equals("testUri1")) {
        reqEntity1 = reqEntity;
      } else if (reqEntity.getRequestUri().equals("testUri2")) {
        reqEntity2 = reqEntity;
      }
    }
    Assert.assertNotNull(reqEntity1);
    Assert.assertNotNull(reqEntity2);
    Assert.assertEquals(Long.valueOf(1L), reqEntity1.getBatchId());
    Assert.assertEquals(Long.valueOf(2L), reqEntity2.getBatchId());
    Assert.assertEquals(BatchRequest.Type.DELETE.name(), reqEntity1.getRequestType());
    Assert.assertEquals(BatchRequest.Type.POST.name(), reqEntity2.getRequestType());
    Assert.assertEquals(requestExecution.getSchedule().getMinutes(),
      scheduleEntity.getMinutes());
    Assert.assertEquals(requestExecution.getSchedule().getEndTime(),
      scheduleEntity.getEndTime());
  }

  @Test
  public void testDeleteRequestSchedule() throws Exception {
    RequestExecution requestExecution = createRequestSchedule();
    Assert.assertNotNull(requestExecution);

    Long id = requestExecution.getId();

    requestExecution.delete();

    Assert.assertNull(requestScheduleDAO.findById(id));
    Assert.assertNull(cluster.getAllRequestExecutions().get(id));
  }
}
