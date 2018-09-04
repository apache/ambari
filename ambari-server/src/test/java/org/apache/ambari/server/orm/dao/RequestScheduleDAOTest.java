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
package org.apache.ambari.server.orm.dao;

import java.sql.SQLException;
import java.util.List;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.*;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.state.scheduler.BatchRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class RequestScheduleDAOTest {
  private Injector injector;
  private HostDAO hostDAO;
  private ClusterDAO clusterDAO;
  private RequestScheduleDAO requestScheduleDAO;
  private RequestScheduleBatchRequestDAO batchRequestDAO;
  private ResourceTypeDAO resourceTypeDAO;
  private RequestDAO requestDao;
  private String testUri = "http://localhost/blah";
  private String testBody = "ValidJson";
  private String testType = BatchRequest.Type.POST.name();

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    // required to load stacks into the DB
    injector.getInstance(AmbariMetaInfo.class);

    clusterDAO = injector.getInstance(ClusterDAO.class);
    hostDAO = injector.getInstance(HostDAO.class);
    clusterDAO = injector.getInstance(ClusterDAO.class);
    requestScheduleDAO = injector.getInstance(RequestScheduleDAO.class);
    batchRequestDAO = injector.getInstance(RequestScheduleBatchRequestDAO.class);
    resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    requestDao = injector.getInstance(RequestDAO.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  private RequestScheduleEntity createScheduleEntity() {
    RequestScheduleEntity scheduleEntity = new RequestScheduleEntity();

    // create an admin resource to represent this cluster
    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceType.CLUSTER.getId());
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
      resourceTypeEntity.setName(ResourceType.CLUSTER.name());
      resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
    }

    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    StackEntity stackEntity = stackDAO.find("HDP", "2.2.0");

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName("c1");
    clusterEntity.setResource(resourceEntity);
    clusterEntity.setDesiredStack(stackEntity);

    clusterDAO.create(clusterEntity);

    scheduleEntity.setClusterEntity(clusterEntity);
    scheduleEntity.setClusterId(clusterEntity.getClusterId());
    scheduleEntity.setStatus("SCHEDULED");
    scheduleEntity.setMinutes("30");
    scheduleEntity.setHours("12");
    scheduleEntity.setDayOfWeek("*");
    scheduleEntity.setDaysOfMonth("*");
    scheduleEntity.setYear("*");

    requestScheduleDAO.create(scheduleEntity);

    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName("h1");
    hostEntity.setOsType("centOS");
    hostDAO.create(hostEntity);

    RequestScheduleBatchRequestEntity batchRequestEntity = new
      RequestScheduleBatchRequestEntity();

    batchRequestEntity.setBatchId(1L);
    batchRequestEntity.setScheduleId(scheduleEntity.getScheduleId());
    batchRequestEntity.setRequestScheduleEntity(scheduleEntity);
    batchRequestEntity.setRequestScheduleEntity(scheduleEntity);
    batchRequestEntity.setRequestType(testType);
    batchRequestEntity.setRequestUri(testUri);
    batchRequestEntity.setRequestBody(testBody);

    batchRequestDAO.create(batchRequestEntity);

    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setClusterId(clusterEntity.getClusterId());
    requestEntity.setRequestScheduleId(scheduleEntity.getScheduleId());
    requestEntity.setRequestId(11L);
    requestEntity.setCommandName("testCommand");
    requestDao.create(requestEntity);

    scheduleEntity.getRequestScheduleBatchRequestEntities().add
      (batchRequestEntity);
    scheduleEntity.getRequestEntities().add(requestEntity);
    scheduleEntity = requestScheduleDAO.merge(scheduleEntity);

    return scheduleEntity;
  }

  @Test
  public void testCreateRequestSchedule() throws Exception {
    RequestScheduleEntity scheduleEntity = createScheduleEntity();

    Assert.assertTrue(scheduleEntity.getScheduleId() > 0);
    Assert.assertEquals("SCHEDULED", scheduleEntity.getStatus());
    Assert.assertEquals("12", scheduleEntity.getHours());
    RequestScheduleBatchRequestEntity batchRequestEntity = scheduleEntity
      .getRequestScheduleBatchRequestEntities().iterator().next();
    Assert.assertNotNull(batchRequestEntity);
    Assert.assertEquals(testUri, batchRequestEntity.getRequestUri());
    Assert.assertEquals(testType, batchRequestEntity.getRequestType());
    Assert.assertEquals(testBody, batchRequestEntity.getRequestBodyAsString());
  }

  @Test
  public void testRemoveRequestSchedule()throws Exception {
    RequestScheduleEntity scheduleEntity = createScheduleEntity();

    requestScheduleDAO.remove(scheduleEntity);
    List<RequestScheduleEntity> scheduleEntities =  requestScheduleDAO.findAll();
    Assert.assertEquals(0, scheduleEntities.size());
  }

  @Test
  public void testFindById() throws Exception {
    RequestScheduleEntity scheduleEntity = createScheduleEntity();

    RequestScheduleEntity testScheduleEntity = requestScheduleDAO.findById
      (scheduleEntity.getScheduleId());

    Assert.assertEquals(scheduleEntity, testScheduleEntity);
  }

  @Test
  public void testFindByStatus() throws Exception {
    RequestScheduleEntity scheduleEntity = createScheduleEntity();

    List<RequestScheduleEntity> scheduleEntities = requestScheduleDAO
      .findByStatus("SCHEDULED");

    Assert.assertNotNull(scheduleEntities);
    Assert.assertEquals(scheduleEntity, scheduleEntities.get(0));
  }
}
