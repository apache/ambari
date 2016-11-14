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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.eclipse.jetty.server.SessionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import junit.framework.Assert;

/**
 * Tests that
 * {@link UpgradeResourceProvider#applyStackAndProcessConfigurations(String, Cluster, String, Direction, UpgradePack, String)}
 * works correctly.
 */
public class StackUpgradeConfigurationMergeTest extends EasyMockSupport {

  private static final StackId s_currentStackId = new StackId("HDP-2.4");
  private static final StackId s_targetStackId = new StackId("HDP-2.5");

  private Injector m_injector;
  private Clusters m_clustersMock;
  private AmbariMetaInfo m_ambariMetaInfoMock;

  /**
   * @throws Exception
   */
  @Before
  public void before() throws Exception {
    m_clustersMock = createNiceMock(Clusters.class);
    m_ambariMetaInfoMock = createNiceMock(AmbariMetaInfo.class);

    MockModule mockModule = new MockModule();

    // create an injector which will inject the mocks
    m_injector = Guice.createInjector(mockModule);
  }

  /**
   *
   */
  @After
  public void teardown() {
  }

  /**
   * Tests that properties which were explicitely removed in the current
   * configurations by stack advisor are not re-introduced as "new" properties
   * accidentally.
   * <p/>
   *
   * HDP 2.4 defaults
   * <ul>
   * <li>foo-site/foo-property-1</li>
   * <li>foo-site/foo-property-2</li>
   * <li>bar-site/bar-property-1</li>
   * </ul>
   *
   * HDP 2.5 defaults
   * <ul>
   * <li>foo-site/foo-property-1</li>
   * <li>foo-site/foo-property-2</li>
   * <li>bar-site/bar-property-1</li>
   * <li>bar-site/bar-property-2</li>
   * </ul>
   *
   * CURRENT 2.4 configs
   * <ul>
   * <li>foo-site/foo-property-1</li>
   * <li>foo-site/foo-property-99</li>
   * <li>bar-site/bar-property-1</li>
   * <li>bar-site/bar-property-20</li>
   * <li>bar-site/bar-property-99</li>
   * </ul>
   *
   * The final merged configurations should detect that {{foo-property-2}}
   * exists in both stacks but is not in the current configs and was therefore
   * purposefully removed. It shoudl also detect that {{bar-property-20}} was
   * added in the new stack and should be added in.
   *
   * @throws Exception
   */
  @Test
  public void testMergedConfigurationsDoNotAddExplicitelyRemovedProperties() throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);
    RepositoryVersionEntity repositoryVersionEntity = createNiceMock(RepositoryVersionEntity.class);
    UpgradePack upgradePack = createNiceMock(UpgradePack.class);
    StackEntity targetStack = createNiceMock(StackEntity.class);

    // mocks which were bound previously
    AmbariManagementController amc = m_injector.getInstance(AmbariManagementController.class);
    AmbariMetaInfo ambariMetaInfo = m_injector.getInstance(AmbariMetaInfo.class);
    ConfigHelper configHelper = m_injector.getInstance(ConfigHelper.class);
    RepositoryVersionDAO repositoryVersionDAO = m_injector.getInstance(RepositoryVersionDAO.class);

    EasyMock.expect(amc.getConfigHelper()).andReturn(configHelper);

    EasyMock.expect(cluster.getCurrentStackVersion()).andReturn(s_currentStackId);
    EasyMock.expect(cluster.getDesiredStackVersion()).andReturn(s_targetStackId);

    EasyMock.expect(targetStack.getStackName()).andReturn("HDP").anyTimes();
    EasyMock.expect(targetStack.getStackVersion()).andReturn("2.5").anyTimes();

    EasyMock.expect(repositoryVersionEntity.getStack()).andReturn(targetStack);
    EasyMock.expect(repositoryVersionDAO.findByStackNameAndVersion("HDP", "2.5.0.0-1234")).andReturn(repositoryVersionEntity);

    EasyMock.expect(upgradePack.getGroups(Direction.UPGRADE)).andReturn(new ArrayList<Grouping>());

    EasyMock.expect(ambariMetaInfo.getServices("HDP", "2.5")).andReturn(
        new HashMap<String, ServiceInfo>());

    // config helper mocks (the heart of this test)
    Map<String, Map<String, String>> oldStackDefaultConfigurationsByType = new HashMap<>();
    oldStackDefaultConfigurationsByType.put("foo-type", new HashMap<String, String>());
    oldStackDefaultConfigurationsByType.get("foo-type").put("foo-property-1", "foo-value-1");
    oldStackDefaultConfigurationsByType.get("foo-type").put("foo-property-2", "foo-value-2");
    oldStackDefaultConfigurationsByType.put("bar-type", new HashMap<String, String>());
    oldStackDefaultConfigurationsByType.get("bar-type").put("bar-property-1", "bar-value-1");

    Map<String, Map<String, String>> newConfigurationsByType = new HashMap<>();
    newConfigurationsByType.put("foo-type", new HashMap<String, String>());
    newConfigurationsByType.get("foo-type").put("foo-property-1", "foo-value-1");
    newConfigurationsByType.get("foo-type").put("foo-property-2", "foo-value-2");
    newConfigurationsByType.put("bar-type", new HashMap<String, String>());
    newConfigurationsByType.get("bar-type").put("bar-property-1", "bar-value-1");
    newConfigurationsByType.get("bar-type").put("bar-property-20", "bar-value-20");

    // HDP 2.4 configs
    EasyMock.expect(configHelper.getDefaultProperties(EasyMock.eq(s_currentStackId),
        EasyMock.anyObject(Cluster.class), EasyMock.anyBoolean())).andReturn(oldStackDefaultConfigurationsByType);

    // HDP 2.5 configs
    EasyMock.expect(configHelper.getDefaultProperties(EasyMock.eq(s_targetStackId),
        EasyMock.anyObject(Cluster.class), EasyMock.anyBoolean())).andReturn(newConfigurationsByType);

    // CURRENT HDP 2.4 configs
    Config currentClusterConfigFoo = createNiceMock(Config.class);
    Config currentClusterConfigBar = createNiceMock(Config.class);

    Map<String, String> existingPropertiesFoo = new HashMap<>();
    existingPropertiesFoo.put("foo-property-1", "foo-value-1");
    existingPropertiesFoo.put("foo-property-99", "foo-value-99");
    EasyMock.expect(currentClusterConfigFoo.getProperties()).andReturn(existingPropertiesFoo);

    Map<String, String> existingPropertiesBar = new HashMap<>();
    existingPropertiesBar.put("bar-property-1", "bar-value-1");
    existingPropertiesBar.put("bar-property-99", "bar-value-99");
    EasyMock.expect(currentClusterConfigBar.getProperties()).andReturn(existingPropertiesBar);

    EasyMock.expect(cluster.getDesiredConfigByType("foo-type")).andReturn(currentClusterConfigFoo);
    EasyMock.expect(cluster.getDesiredConfigByType("bar-type")).andReturn(currentClusterConfigBar);

    // desired configs
    Map<String, DesiredConfig> existingDesiredConfigurationsByType = new HashMap<>();
    existingDesiredConfigurationsByType.put("foo-type", null);
    existingDesiredConfigurationsByType.put("bar-type", null);
    EasyMock.expect(cluster.getDesiredConfigs()).andReturn(existingDesiredConfigurationsByType);

    // we need to know what configs are being created, so capture them
    Capture<Map<String, Map<String, String>>> capturedArgument = EasyMock.newCapture();
    configHelper.createConfigTypes(EasyMock.anyObject(Cluster.class),
        EasyMock.anyObject(AmbariManagementController.class),
        EasyMock.capture(capturedArgument),
        EasyMock.anyString(), EasyMock.anyString());

    EasyMock.expectLastCall();

    replayAll();

    UpgradeResourceProvider upgradeResourceProvider = new UpgradeResourceProvider(amc);
    m_injector.injectMembers(upgradeResourceProvider);

    upgradeResourceProvider.applyStackAndProcessConfigurations("HDP", cluster, "2.5.0.0-1234",
        Direction.UPGRADE, upgradePack, "admin");

    // assertion time!
    Map<String, Map<String, String>> mergedConfigurations = capturedArgument.getValue();
    Map<String, String> mergedFooSite = mergedConfigurations.get("foo-type");
    Map<String, String> mergedBarSite = mergedConfigurations.get("bar-type");

    // foo-site validation
    Assert.assertEquals("foo-value-1", mergedFooSite.get("foo-property-1"));
    Assert.assertEquals("foo-value-99", mergedFooSite.get("foo-property-99"));
    Assert.assertFalse(mergedFooSite.containsKey("foo-property-2"));

    // bar-site validation
    Assert.assertEquals("bar-value-1", mergedBarSite.get("bar-property-1"));
    Assert.assertEquals("bar-value-20", mergedBarSite.get("bar-property-20"));
    Assert.assertEquals("bar-value-99", mergedBarSite.get("bar-property-99"));
    Assert.assertEquals(3, mergedBarSite.size());
  }


  private class MockModule implements Module {

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Binder binder) {
      binder.bind(AmbariMetaInfo.class).toInstance(m_ambariMetaInfoMock);
      binder.bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
      binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      binder.bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
      binder.bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
      binder.bind(ActionManager.class).toInstance(createNiceMock(ActionManager.class));
      binder.bind(HostRoleCommandDAO.class).toInstance(createNiceMock(HostRoleCommandDAO.class));
      binder.bind(AmbariManagementController.class).toInstance(createNiceMock(AmbariManagementController.class));
      binder.bind(ClusterController.class).toInstance(createNiceMock(ClusterController.class));
      binder.bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
      binder.bind(SessionManager.class).toInstance(createNiceMock(SessionManager.class));
      binder.bind(RequestExecutionFactory.class).toInstance(createNiceMock(RequestExecutionFactory.class));
      binder.bind(ExecutionScheduler.class).toInstance(createNiceMock(ExecutionScheduler.class));
      binder.bind(RequestFactory.class).toInstance(createNiceMock(RequestFactory.class));
      binder.bind(StageFactory.class).toInstance(createNiceMock(StageFactory.class));
      binder.install(new FactoryModuleBuilder().build(RoleGraphFactory.class));
      binder.bind(AbstractRootServiceResponseFactory.class).toInstance(createNiceMock(AbstractRootServiceResponseFactory.class));
      binder.bind(ConfigFactory.class).toInstance(createNiceMock(ConfigFactory.class));
      binder.bind(ConfigGroupFactory.class).toInstance(createNiceMock(ConfigGroupFactory.class));
      binder.bind(ServiceFactory.class).toInstance(createNiceMock(ServiceFactory.class));
      binder.bind(ServiceComponentFactory.class).toInstance(createNiceMock(ServiceComponentFactory.class));
      binder.bind(ServiceComponentHostFactory.class).toInstance(createNiceMock(ServiceComponentHostFactory.class));
      binder.bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
      binder.bind(KerberosHelper.class).toInstance(createNiceMock(KerberosHelper.class));
      binder.bind(Users.class).toInstance(createNiceMock(Users.class));
      binder.bind(ConfigHelper.class).toInstance(createNiceMock(ConfigHelper.class));
      binder.bind(RepositoryVersionDAO.class).toInstance(createNiceMock(RepositoryVersionDAO.class));
      binder.bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
      binder.bind(HookService.class).toInstance(createMock(HookService.class));


      binder.requestStaticInjection(UpgradeResourceProvider.class);
    }
  }
}
