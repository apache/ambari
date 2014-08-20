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
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.resetToStrict;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.apache.ambari.server.state.alert.Source;
import org.apache.ambari.server.state.alert.SourceType;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * AlertDefinition tests
 */
public class AlertDefinitionResourceProviderTest {

  private AlertDefinitionDAO dao = null;
  private AlertDefinitionHash definitionHash = null;
  private AlertDefinitionFactory m_factory = new AlertDefinitionFactory();
  private Injector m_injector;

  private static String DEFINITION_UUID = UUID.randomUUID().toString();

  @Before
  public void before() {
    dao = createStrictMock(AlertDefinitionDAO.class);
    definitionHash = createNiceMock(AlertDefinitionHash.class);

    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    AlertDefinitionResourceProvider.init(m_injector);
    m_injector.injectMembers(m_factory);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetResourcesNoPredicate() throws Exception {
    AlertDefinitionResourceProvider provider = createProvider(null);

    Request request = PropertyHelper.getReadRequest("AlertDefinition/cluster_name",
        "AlertDefinition/id");

    Set<Resource> results = provider.getResources(request, null);

    assertEquals(0, results.size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetResourcesClusterPredicate() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_ID,
        AlertDefinitionResourceProvider.ALERT_DEF_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_LABEL);

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();

    Predicate predicate = new PredicateBuilder().property(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME).equals("c1").toPredicate();

    expect(dao.findAll(1L)).andReturn(getMockEntities());

    replay(amc, clusters, cluster, dao);

    AlertDefinitionResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();

    Assert.assertEquals("my_def", r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_NAME));

    Assert.assertEquals("Mock Label",
        r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_LABEL));

    verify(amc, clusters, cluster, dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetSingleResource() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_ID,
        AlertDefinitionResourceProvider.ALERT_DEF_NAME,
        AlertDefinitionResourceProvider.ALERT_DEF_LABEL,
        AlertDefinitionResourceProvider.ALERT_DEF_SOURCE,
        AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE);

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();

    Predicate predicate = new PredicateBuilder().property(
        AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME).equals("c1")
          .and().property(AlertDefinitionResourceProvider.ALERT_DEF_ID).equals("1").toPredicate();

    expect(dao.findById(1L)).andReturn(getMockEntities().get(0));

    replay(amc, clusters, cluster, dao);

    AlertDefinitionResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();

    Assert.assertEquals("my_def", r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_NAME));

    Assert.assertEquals(
        SourceType.METRIC.name(),
        r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE));

    Source source = getMockSource();
    String okJson = source.getReporting().getOk().getText();
    Object reporting = r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_REPORTING);

    Assert.assertTrue(reporting.toString().contains(okJson));

    Assert.assertEquals("Mock Label",
        r.getPropertyValue(AlertDefinitionResourceProvider.ALERT_DEF_LABEL));

    Assert.assertNotNull(r.getPropertyValue("AlertDefinition/source/type"));
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

    Capture<AlertDefinitionEntity> entityCapture = new Capture<AlertDefinitionEntity>();
    dao.create(capture(entityCapture));
    expectLastCall();

    // creating a single definition should invalidate hosts of the definition
    expect(
        definitionHash.invalidateHosts(EasyMock.anyObject(AlertDefinitionEntity.class))).andReturn(
        new HashSet<String>()).once();

    replay(amc, clusters, cluster, dao, definitionHash);

    Gson gson = m_factory.getGson();
    Source source = getMockSource();
    String sourceJson = gson.toJson(source);
    AlertDefinitionResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME, "c1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_INTERVAL, "1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_NAME, "my_def");

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SERVICE_NAME,
        "HDFS");

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE,
        sourceJson);

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE,
        SourceType.METRIC.name());

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_LABEL,
        "Mock Label (Create)");

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);

    provider.createResources(request);

    Assert.assertTrue(entityCapture.hasCaptured());
    AlertDefinitionEntity entity = entityCapture.getValue();
    Assert.assertNotNull(entity);

    Assert.assertEquals(Long.valueOf(1), entity.getClusterId());
    Assert.assertNull(entity.getComponentName());
    Assert.assertEquals("my_def", entity.getDefinitionName());
    Assert.assertTrue(entity.getEnabled());
    Assert.assertNotNull(entity.getHash());
    Assert.assertEquals(Integer.valueOf(1), entity.getScheduleInterval());
    Assert.assertNull(entity.getScope());
    Assert.assertEquals("HDFS", entity.getServiceName());
    Assert.assertEquals("METRIC", entity.getSourceType());
    Assert.assertEquals("Mock Label (Create)", entity.getLabel());

    // verify Source
    Assert.assertNotNull(entity.getSource());
    Source actualSource = gson.fromJson(entity.getSource(), Source.class);
    Assert.assertNotNull(actualSource);

    assertEquals(source.getReporting().getOk().getText(),
        source.getReporting().getOk().getText());

    assertEquals(source.getReporting().getWarning().getText(),
        source.getReporting().getWarning().getText());

    assertEquals(source.getReporting().getCritical().getText(),
        source.getReporting().getCritical().getText());

    verify(amc, clusters, cluster, dao);

  }

  /**
   * @throws Exception
   */
  @Test
  public void testUpdateResources() throws Exception {
    Gson gson = m_factory.getGson();

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(clusters.getClusterById(EasyMock.anyInt())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).atLeastOnce();
    expect(cluster.getClusterName()).andReturn("c1").atLeastOnce();

    Capture<AlertDefinitionEntity> entityCapture = new Capture<AlertDefinitionEntity>();
    dao.create(capture(entityCapture));
    expectLastCall();

    // updateing a single definition should invalidate hosts of the definition
    expect(
        definitionHash.invalidateHosts(EasyMock.anyObject(AlertDefinitionEntity.class))).andReturn(
        new HashSet<String>()).atLeastOnce();

    replay(amc, clusters, cluster, dao, definitionHash);

    Source source = getMockSource();
    String sourceString = gson.toJson(source);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME, "c1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_INTERVAL, "1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_NAME, "my_def");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_LABEL, "Label");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SERVICE_NAME, "HDFS");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE, "METRIC");

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE,
        sourceString);

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_ENABLED,
        Boolean.TRUE.toString());

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);

    AlertDefinitionResourceProvider provider = createProvider(amc);

    provider.createResources(request);

    Assert.assertTrue(entityCapture.hasCaptured());
    AlertDefinitionEntity entity = entityCapture.getValue();
    Assert.assertNotNull(entity);

    Predicate p = new PredicateBuilder().property(
        AlertDefinitionResourceProvider.ALERT_DEF_ID).equals("1").and().property(
            AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME).equals("c1").toPredicate();

    // everything is mocked, there is no DB
    entity.setDefinitionId(Long.valueOf(1));

    String oldName = entity.getDefinitionName();
    String oldHash = entity.getHash();
    Integer oldInterval = entity.getScheduleInterval();
    boolean oldEnabled = entity.getEnabled();
    String oldSource = entity.getSource();

    resetToStrict(dao);
    expect(dao.findById(1L)).andReturn(entity).anyTimes();
    expect(dao.merge((AlertDefinitionEntity) anyObject())).andReturn(entity).anyTimes();
    replay(dao);

    requestProps = new HashMap<String, Object>();
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_ID, "1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME, "c1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_INTERVAL, "2");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_NAME, "my_def2");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_LABEL, "Label 2");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SERVICE_NAME, "HDFS");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE, "METRIC");

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE,
        sourceString.replaceAll("CPU", "CPU2"));

    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_ENABLED,
        Boolean.FALSE.toString());

    request = PropertyHelper.getUpdateRequest(requestProps, null);

    provider.updateResources(request, p);

    Assert.assertFalse(oldHash.equals(entity.getHash()));
    Assert.assertFalse(oldName.equals(entity.getDefinitionName()));
    Assert.assertFalse(oldInterval.equals(entity.getScheduleInterval()));
    Assert.assertFalse(oldEnabled == entity.getEnabled());
    Assert.assertFalse(oldSource.equals(entity.getSource()));
    Assert.assertTrue(entity.getSource().contains("CPU2"));

    verify(amc, clusters, cluster, dao);
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

    Capture<AlertDefinitionEntity> entityCapture = new Capture<AlertDefinitionEntity>();
    dao.create(capture(entityCapture));
    expectLastCall();

    // deleting a single definition should invalidate hosts of the definition
    expect(
        definitionHash.invalidateHosts(EasyMock.anyObject(AlertDefinitionEntity.class))).andReturn(
        new HashSet<String>()).atLeastOnce();

    replay(amc, clusters, cluster, dao, definitionHash);

    AlertDefinitionResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME, "c1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_INTERVAL, "1");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_NAME, "my_def");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SERVICE_NAME, "HDFS");
    requestProps.put(AlertDefinitionResourceProvider.ALERT_DEF_SOURCE_TYPE, "METRIC");

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);

    provider.createResources(request);

    Assert.assertTrue(entityCapture.hasCaptured());
    AlertDefinitionEntity entity = entityCapture.getValue();
    Assert.assertNotNull(entity);

    Predicate p = new PredicateBuilder().property(
        AlertDefinitionResourceProvider.ALERT_DEF_ID).equals("1").and().property(
            AlertDefinitionResourceProvider.ALERT_DEF_CLUSTER_NAME).equals("c1").toPredicate();
    // everything is mocked, there is no DB
    entity.setDefinitionId(Long.valueOf(1));

    resetToStrict(dao);
    expect(dao.findById(1L)).andReturn(entity).anyTimes();
    dao.remove(capture(entityCapture));
    expectLastCall();
    replay(dao);

    provider.deleteResources(p);

    AlertDefinitionEntity entity1 = entityCapture.getValue();
    Assert.assertEquals(Long.valueOf(1), entity1.getDefinitionId());

    verify(amc, clusters, cluster, dao);
  }

  /**
   * @param amc
   * @return
   */
  private AlertDefinitionResourceProvider createProvider(AmbariManagementController amc) {
    return new AlertDefinitionResourceProvider(
        PropertyHelper.getPropertyIds(Resource.Type.AlertDefinition),
        PropertyHelper.getKeyPropertyIds(Resource.Type.AlertDefinition),
        amc);
  }

  /**
   * @return
   */
  private List<AlertDefinitionEntity> getMockEntities() throws Exception {
    Source source = getMockSource();
    String sourceJson = new Gson().toJson(source);

    AlertDefinitionEntity entity = new AlertDefinitionEntity();
    entity.setClusterId(Long.valueOf(1L));
    entity.setComponentName(null);
    entity.setDefinitionId(Long.valueOf(1L));
    entity.setDefinitionName("my_def");
    entity.setLabel("Mock Label");
    entity.setEnabled(true);
    entity.setHash(DEFINITION_UUID);
    entity.setScheduleInterval(Integer.valueOf(2));
    entity.setServiceName(null);
    entity.setSourceType(SourceType.METRIC.name());
    entity.setSource(sourceJson);
    return Arrays.asList(entity);
  }

  /**
   * @return
   */
  private Source getMockSource() throws Exception {
    File alertsFile = new File(
        "src/test/resources/stacks/HDP/2.0.5/services/HDFS/alerts.json");

    Assert.assertTrue(alertsFile.exists());

    Set<AlertDefinition> set = m_factory.getAlertDefinitions(alertsFile, "HDFS");
    AlertDefinition nameNodeCpu = null;
    Iterator<AlertDefinition> definitions = set.iterator();
    while (definitions.hasNext()) {
      AlertDefinition definition = definitions.next();

      if (definition.getName().equals("namenode_cpu")) {
        nameNodeCpu = definition;
      }
    }

    Assert.assertNotNull(nameNodeCpu.getSource());
    return nameNodeCpu.getSource();
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
      binder.bind(AlertDefinitionDAO.class).toInstance(dao);
      binder.bind(AlertDefinitionHash.class).toInstance(definitionHash);
      binder.bind(Clusters.class).toInstance(
          EasyMock.createNiceMock(Clusters.class));
      binder.bind(Cluster.class).toInstance(
          EasyMock.createNiceMock(Cluster.class));
      binder.bind(ActionMetadata.class);
    }
  }
}
