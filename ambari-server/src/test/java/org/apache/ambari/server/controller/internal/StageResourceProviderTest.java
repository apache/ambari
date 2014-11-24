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

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

public class StageResourceProviderTest {

  private StageDAO dao = null;
  private Injector injector;

  @Before
  public void before() {
    dao = createStrictMock(StageDAO.class);

    // create an injector which will inject the mocks
    injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    Assert.assertNotNull(injector);
  }


  @Test
  public void testCreateResources() throws Exception {
    StageResourceProvider provider = new StageResourceProvider();

    Request request = createNiceMock(Request.class);
    try {
      provider.createResources(request);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  @Test
  public void testUpdateResources() throws Exception {
    StageResourceProvider provider = new StageResourceProvider();

    Request request = createNiceMock(Request.class);
    Predicate predicate = createNiceMock(Predicate.class);
    try {
      provider.updateResources(request, predicate);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  @Test
  public void testDeleteResources() throws Exception {
    StageResourceProvider provider = new StageResourceProvider();

    Predicate predicate = createNiceMock(Predicate.class);
    try {
      provider.deleteResources(predicate);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  @Test
  public void testGetResources() throws Exception {
    StageResourceProvider provider = new StageResourceProvider();

    Request request = createNiceMock(Request.class);
    Predicate predicate = createNiceMock(Predicate.class);

    List<StageEntity> entities = getStageEntities();

    expect(dao.findAll(request, predicate)).andReturn(entities);

    replay(dao, request, predicate);

    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals(100.0, resource.getPropertyValue(StageResourceProvider.STAGE_PROGRESS_PERCENT));
    Assert.assertEquals("COMPLETED", resource.getPropertyValue(StageResourceProvider.STAGE_STATUS));
    Assert.assertEquals(1000L, resource.getPropertyValue(StageResourceProvider.STAGE_START_TIME));
    Assert.assertEquals(2500L, resource.getPropertyValue(StageResourceProvider.STAGE_END_TIME));

    verify(dao);

  }

  @Test
  public void testQueryForResources() throws Exception {
    StageResourceProvider provider = new StageResourceProvider();

    Request request = createNiceMock(Request.class);
    Predicate predicate = createNiceMock(Predicate.class);

    List<StageEntity> entities = getStageEntities();

    expect(dao.findAll(request, predicate)).andReturn(entities);

    replay(dao, request, predicate);

    QueryResponse response =  provider.queryForResources(request, predicate);

    Set<Resource> resources = response.getResources();

    Assert.assertEquals(1, resources.size());

    Assert.assertFalse(response.isSortedResponse());
    Assert.assertFalse(response.isPagedResponse());
    Assert.assertEquals(1, response.getTotalResourceCount());

    verify(dao);
  }

  private List<StageEntity> getStageEntities() {
    StageEntity stage = new StageEntity();

    HostRoleCommandEntity task1 = new HostRoleCommandEntity();
    task1.setStatus(HostRoleStatus.COMPLETED);
    task1.setStartTime(1000L);
    task1.setEndTime(2000L);

    HostRoleCommandEntity task2 = new HostRoleCommandEntity();
    task2.setStatus(HostRoleStatus.COMPLETED);
    task2.setStartTime(1500L);
    task2.setEndTime(2500L);


    Collection<HostRoleCommandEntity> tasks = new HashSet<HostRoleCommandEntity>();
    tasks.add(task1);
    tasks.add(task2);

    stage.setHostRoleCommands(tasks);

    List<StageEntity> entities = new LinkedList<StageEntity>();
    entities.add(stage);
    return entities;
  }

  private class MockModule implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(StageDAO.class).toInstance(dao);
      binder.bind(Clusters.class).toInstance(
          EasyMock.createNiceMock(Clusters.class));
      binder.bind(Cluster.class).toInstance(
          EasyMock.createNiceMock(Cluster.class));
      binder.bind(ActionMetadata.class);
    }
  }
}