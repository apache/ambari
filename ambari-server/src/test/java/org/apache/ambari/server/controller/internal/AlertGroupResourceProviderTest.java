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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.resetToStrict;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * {@link AlertGroupResourceProvider} tests.
 */
public class AlertGroupResourceProviderTest {

  private static final Long ALERT_GROUP_ID = Long.valueOf(28);
  private static final String ALERT_GROUP_NAME = "Important Alerts";
  private static final long ALERT_GROUP_CLUSTER_ID = 1L;
  private static final String ALERT_GROUP_CLUSTER_NAME = "c1";

  private AlertDispatchDAO m_dao;
  private Injector m_injector;

  @Before
  public void before() {
    m_dao = createStrictMock(AlertDispatchDAO.class);

    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    AlertGroupResourceProvider.init(m_injector);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetResourcesNoPredicate() throws Exception {
    AlertGroupResourceProvider provider = createProvider(null);

    Request request = PropertyHelper.getReadRequest("AlertGroup/cluster_name",
        "AlertGroup/id");

    Set<Resource> results = provider.getResources(request, null);

    assertEquals(0, results.size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetResourcesClusterPredicate() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertGroupResourceProvider.ALERT_GROUP_ID,
        AlertGroupResourceProvider.ALERT_GROUP_NAME,
        AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME,
        AlertGroupResourceProvider.ALERT_GROUP_DEFAULT);

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();

    Predicate predicate = new PredicateBuilder().property(
        AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME).equals("c1").toPredicate();

    expect(m_dao.findAllGroups(ALERT_GROUP_CLUSTER_ID)).andReturn(
        getMockEntities());

    replay(amc, clusters, cluster, m_dao);

    AlertGroupResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();

    Assert.assertEquals(ALERT_GROUP_NAME,
        r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_NAME));

    Assert.assertEquals(ALERT_GROUP_ID,
        r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_ID));

    Assert.assertEquals(ALERT_GROUP_CLUSTER_NAME,
        r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME));

    verify(amc, clusters, cluster, m_dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetSingleResource() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertGroupResourceProvider.ALERT_GROUP_ID,
        AlertGroupResourceProvider.ALERT_GROUP_NAME,
        AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME,
        AlertGroupResourceProvider.ALERT_GROUP_DEFAULT);

    AmbariManagementController amc = createMock(AmbariManagementController.class);

    Predicate predicate = new PredicateBuilder().property(
        AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME).equals(
        ALERT_GROUP_CLUSTER_NAME).and().property(
        AlertGroupResourceProvider.ALERT_GROUP_ID).equals(
        ALERT_GROUP_ID.toString()).toPredicate();

    expect(m_dao.findGroupById(ALERT_GROUP_ID.longValue())).andReturn(
        getMockEntities().get(0));

    replay(amc, m_dao);

    AlertGroupResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();

    Assert.assertEquals(ALERT_GROUP_NAME,
        r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_NAME));

    Assert.assertEquals(ALERT_GROUP_ID,
        r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_ID));

    Assert.assertEquals(ALERT_GROUP_CLUSTER_NAME,
        r.getPropertyValue(AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME));

    verify(amc, m_dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testCreateResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();

    Capture<List<AlertGroupEntity>> listCapture = new Capture<List<AlertGroupEntity>>();

    m_dao.createGroups(capture(listCapture));
    expectLastCall();

    replay(amc, clusters, cluster, m_dao);

    AlertGroupResourceProvider provider = createProvider(amc);
    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_NAME,
        ALERT_GROUP_NAME);

    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME,
        ALERT_GROUP_CLUSTER_NAME);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    provider.createResources(request);

    Assert.assertTrue(listCapture.hasCaptured());
    AlertGroupEntity entity = listCapture.getValue().get(0);
    Assert.assertNotNull(entity);

    Assert.assertEquals(ALERT_GROUP_NAME, entity.getGroupName());
    Assert.assertEquals(ALERT_GROUP_CLUSTER_ID,
        entity.getClusterId().longValue());

    verify(amc, clusters, cluster, m_dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testUpdateResources() throws Exception {
  }

  /**
   * @throws Exception
   */
  @Test
  public void testDeleteResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();

    Capture<AlertGroupEntity> entityCapture = new Capture<AlertGroupEntity>();
    Capture<List<AlertGroupEntity>> listCapture = new Capture<List<AlertGroupEntity>>();

    m_dao.createGroups(capture(listCapture));
    expectLastCall();

    replay(amc, clusters, cluster, m_dao);

    AlertGroupResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_NAME,
        ALERT_GROUP_NAME);

    requestProps.put(AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME,
        ALERT_GROUP_CLUSTER_NAME);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    provider.createResources(request);

    Assert.assertTrue(listCapture.hasCaptured());
    AlertGroupEntity entity = listCapture.getValue().get(0);
    Assert.assertNotNull(entity);

    Predicate predicate = new PredicateBuilder().property(
        AlertGroupResourceProvider.ALERT_GROUP_CLUSTER_NAME).equals(
        ALERT_GROUP_CLUSTER_NAME).and().property(
        AlertGroupResourceProvider.ALERT_GROUP_ID).equals(
        ALERT_GROUP_ID.toString()).toPredicate();

    // everything is mocked, there is no DB
    entity.setGroupId(ALERT_GROUP_ID);

    resetToStrict(m_dao);
    expect(m_dao.findGroupById(ALERT_GROUP_ID.longValue())).andReturn(entity).anyTimes();
    m_dao.remove(capture(entityCapture));
    expectLastCall();
    replay(m_dao);

    provider.deleteResources(predicate);

    AlertGroupEntity entity1 = entityCapture.getValue();
    Assert.assertEquals(ALERT_GROUP_ID, entity1.getGroupId());

    verify(amc, clusters, cluster, m_dao);
  }

  /**
   * @param amc
   * @return
   */
  private AlertGroupResourceProvider createProvider(
      AmbariManagementController amc) {
    return new AlertGroupResourceProvider(
        PropertyHelper.getPropertyIds(Resource.Type.AlertGroup),
        PropertyHelper.getKeyPropertyIds(Resource.Type.AlertGroup), amc);
  }

  /**
   * @return
   */
  private List<AlertGroupEntity> getMockEntities() throws Exception {
    AlertGroupEntity entity = new AlertGroupEntity();
    entity.setGroupId(ALERT_GROUP_ID);
    entity.setGroupName(ALERT_GROUP_NAME);
    entity.setClusterId(ALERT_GROUP_CLUSTER_ID);
    entity.setDefault(false);
    return Arrays.asList(entity);
  }

  /**
  *
  */
  private class MockModule implements Module {
    /**
    *
    */
    @Override
    public void configure(Binder binder) {
      binder.bind(AlertDispatchDAO.class).toInstance(m_dao);
      binder.bind(Clusters.class).toInstance(
          EasyMock.createNiceMock(Clusters.class));
      binder.bind(Cluster.class).toInstance(
          EasyMock.createNiceMock(Cluster.class));
      binder.bind(ActionMetadata.class);
    }
  }
}
