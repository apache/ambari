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
package org.apache.ambari.server.state.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.eclipse.jetty.server.SessionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import junit.framework.Assert;

/**
 * Tests that cluster effective version is calcualted correctly during upgrades.
 */
@RunWith(value = PowerMockRunner.class)
@PrepareForTest({ ClusterImpl.class })
public class ClusterEffectiveVersionTest extends EasyMockSupport {

  private Injector m_injector;
  private ClusterEntity m_clusterEntity;
  private Cluster m_cluster;

  /**
   * @throws Exception
   */
  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new MockModule());
    m_clusterEntity = createNiceMock(ClusterEntity.class);

    expectClusterEntityMocks();

    replayAll();

    ClusterFactory clusterFactory = m_injector.getInstance(ClusterFactory.class);
    m_cluster = clusterFactory.create(m_clusterEntity);

    verifyAll();
  }

  /**
   * Tests that {@link Cluster#getEffectiveClusterVersion()} returns the
   * "current" version when there is no upgrade in progress.
   */
  @Test
  public void testEffectiveVersionWithNoUpgrade() throws Exception {
    Cluster clusterSpy = Mockito.spy(m_cluster);

    Mockito.doReturn(null).when(clusterSpy).getUpgradeInProgress();

    ClusterVersionEntity currentClusterVersion = new ClusterVersionEntity();
    Mockito.doReturn(currentClusterVersion).when(clusterSpy).getCurrentClusterVersion();

    ClusterVersionEntity effectiveVersion = clusterSpy.getEffectiveClusterVersion();
    Assert.assertEquals(currentClusterVersion, effectiveVersion);
  }

  /**
   * Tests that {@link Cluster#getEffectiveClusterVersion()} returns the target
   * version in an active rolling upgrade.
   */
  @Test
  public void testEffectiveVersionWithActiveRollingUpgrade() throws Exception {
    resetAll();
    expectClusterEntityMocks();

    Cluster clusterSpy = Mockito.spy(m_cluster);

    UpgradeEntity upgradeEntity = createNiceMock(UpgradeEntity.class);
    EasyMock.expect(upgradeEntity.getUpgradeType()).andReturn(UpgradeType.ROLLING).atLeastOnce();
    EasyMock.expect(upgradeEntity.getFromVersion()).andReturn("2.3.0.0-1234").anyTimes();
    EasyMock.expect(upgradeEntity.getToVersion()).andReturn("2.4.0.0-1234").atLeastOnce();

    RepositoryVersionEntity repositoryVersionEntity = createNiceMock(RepositoryVersionEntity.class);
    EasyMock.expect(repositoryVersionEntity.getVersion()).andReturn("2.4.0.0-1234").atLeastOnce();

    ClusterVersionEntity clusterVersionUpgradingTo = createNiceMock(ClusterVersionEntity.class);
    EasyMock.expect(clusterVersionUpgradingTo.getRepositoryVersion()).andReturn(
        repositoryVersionEntity).atLeastOnce();

    List<ClusterVersionEntity> clusterVersionEntities = Lists.newArrayList(clusterVersionUpgradingTo);
    EasyMock.expect(m_clusterEntity.getClusterVersionEntities()).andReturn(clusterVersionEntities).atLeastOnce();

    replayAll();

    Mockito.doReturn(upgradeEntity).when(clusterSpy).getUpgradeInProgress();

    // this shouldn't be returned since there is an upgrade in progress
    ClusterVersionEntity currentClusterVersion = new ClusterVersionEntity();
    Mockito.doReturn(currentClusterVersion).when(clusterSpy).getCurrentClusterVersion();

    ClusterVersionEntity effectiveVersion = clusterSpy.getEffectiveClusterVersion();
    Assert.assertEquals(clusterVersionUpgradingTo, effectiveVersion);

    verifyAll();
  }

  /**
   * Tests that {@link Cluster#getEffectiveClusterVersion()} returns the target
   * version in an active rolling upgrade.
   */
  @Test
  public void testEffectiveVersionWithActiveExpressDowngrade() throws Exception {
    resetAll();
    expectClusterEntityMocks();

    Cluster clusterSpy = Mockito.spy(m_cluster);

    // from/to are switched on downgrade
    UpgradeEntity upgradeEntity = createNiceMock(UpgradeEntity.class);
    EasyMock.expect(upgradeEntity.getUpgradeType()).andReturn(UpgradeType.NON_ROLLING).atLeastOnce();
    EasyMock.expect(upgradeEntity.getToVersion()).andReturn("2.3.0.0-1234").atLeastOnce();
    EasyMock.expect(upgradeEntity.getFromVersion()).andReturn("2.4.0.0-1234").anyTimes();
    EasyMock.expect(upgradeEntity.getDirection()).andReturn(Direction.DOWNGRADE).atLeastOnce();

    RepositoryVersionEntity repositoryVersionEntity = createNiceMock(RepositoryVersionEntity.class);
    EasyMock.expect(repositoryVersionEntity.getVersion()).andReturn("2.3.0.0-1234").atLeastOnce();

    ClusterVersionEntity clusterVersionUpgradingTo = createNiceMock(ClusterVersionEntity.class);
    EasyMock.expect(clusterVersionUpgradingTo.getRepositoryVersion()).andReturn(
        repositoryVersionEntity).atLeastOnce();

    List<ClusterVersionEntity> clusterVersionEntities = Lists.newArrayList(clusterVersionUpgradingTo);
    EasyMock.expect(m_clusterEntity.getClusterVersionEntities()).andReturn(clusterVersionEntities).atLeastOnce();

    replayAll();

    Mockito.doReturn(upgradeEntity).when(clusterSpy).getUpgradeInProgress();

    // this shouldn't be returned since there is an upgrade in progress
    ClusterVersionEntity currentClusterVersion = new ClusterVersionEntity();
    Mockito.doReturn(currentClusterVersion).when(clusterSpy).getCurrentClusterVersion();

    ClusterVersionEntity effectiveVersion = clusterSpy.getEffectiveClusterVersion();
    Assert.assertEquals(clusterVersionUpgradingTo, effectiveVersion);

    verifyAll();
  }

  /**
   * Sets the expectations on the {@link ClusterEntity} mock.
   */
  private void expectClusterEntityMocks() {
    ClusterDAO clusterDAO = m_injector.getInstance(ClusterDAO.class);
    StackEntity stackEntity = createNiceMock(StackEntity.class);

    EasyMock.expect(clusterDAO.findById(1L)).andReturn(m_clusterEntity).anyTimes();

    EasyMock.expect(stackEntity.getStackId()).andReturn(1L).anyTimes();
    EasyMock.expect(stackEntity.getStackName()).andReturn("HDP").anyTimes();
    EasyMock.expect(stackEntity.getStackVersion()).andReturn("2.3").anyTimes();

    EasyMock.expect(m_clusterEntity.getClusterId()).andReturn(1L).anyTimes();
    EasyMock.expect(m_clusterEntity.getClusterName()).andReturn("c1").anyTimes();
    EasyMock.expect(m_clusterEntity.getDesiredStack()).andReturn(stackEntity).anyTimes();
    EasyMock.expect(m_clusterEntity.getClusterServiceEntities()).andReturn(
        new ArrayList<ClusterServiceEntity>()).anyTimes();
    EasyMock.expect(m_clusterEntity.getClusterConfigEntities()).andReturn(
        new ArrayList<ClusterConfigEntity>()).anyTimes();

    EasyMock.expect(m_clusterEntity.getConfigGroupEntities()).andReturn(
        new ArrayList<ConfigGroupEntity>()).anyTimes();

    EasyMock.expect(m_clusterEntity.getRequestScheduleEntities()).andReturn(
        new ArrayList<RequestScheduleEntity>()).anyTimes();
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
      binder.bind(Clusters.class).toInstance(EasyMock.createNiceMock(Clusters.class));
      binder.bind(OsFamily.class).toInstance(EasyMock.createNiceMock(OsFamily.class));
      binder.bind(DBAccessor.class).toInstance(EasyMock.createNiceMock(DBAccessor.class));
      binder.bind(EntityManager.class).toInstance(EasyMock.createNiceMock(EntityManager.class));
      binder.bind(ActionManager.class).toInstance(EasyMock.createNiceMock(ActionManager.class));
      binder.bind(HostRoleCommandDAO.class).toInstance(EasyMock.createNiceMock(HostRoleCommandDAO.class));
      binder.bind(AmbariManagementController.class).toInstance(EasyMock.createNiceMock(AmbariManagementController.class));
      binder.bind(ClusterController.class).toInstance(EasyMock.createNiceMock(ClusterController.class));
      binder.bind(StackManagerFactory.class).toInstance(EasyMock.createNiceMock(StackManagerFactory.class));
      binder.bind(SessionManager.class).toInstance(EasyMock.createNiceMock(SessionManager.class));
      binder.bind(RequestExecutionFactory.class).toInstance(EasyMock.createNiceMock(RequestExecutionFactory.class));
      binder.bind(ExecutionScheduler.class).toInstance(EasyMock.createNiceMock(ExecutionScheduler.class));
      binder.bind(RequestFactory.class).toInstance(EasyMock.createNiceMock(RequestFactory.class));
      binder.bind(StageFactory.class).toInstance(EasyMock.createNiceMock(StageFactory.class));
      binder.bind(RoleGraphFactory.class).toInstance(EasyMock.createNiceMock(RoleGraphFactory.class));
      binder.bind(AbstractRootServiceResponseFactory.class).toInstance(EasyMock.createNiceMock(AbstractRootServiceResponseFactory.class));
      binder.bind(ConfigFactory.class).toInstance(EasyMock.createNiceMock(ConfigFactory.class));
      binder.bind(ConfigGroupFactory.class).toInstance(EasyMock.createNiceMock(ConfigGroupFactory.class));
      binder.bind(ServiceFactory.class).toInstance(EasyMock.createNiceMock(ServiceFactory.class));
      binder.bind(ServiceComponentFactory.class).toInstance(EasyMock.createNiceMock(ServiceComponentFactory.class));
      binder.bind(ServiceComponentHostFactory.class).toInstance(EasyMock.createNiceMock(ServiceComponentHostFactory.class));
      binder.bind(PasswordEncoder.class).toInstance(EasyMock.createNiceMock(PasswordEncoder.class));
      binder.bind(KerberosHelper.class).toInstance(EasyMock.createNiceMock(KerberosHelper.class));
      binder.bind(Users.class).toInstance(EasyMock.createNiceMock(Users.class));
      binder.bind(AmbariEventPublisher.class).toInstance(createNiceMock(AmbariEventPublisher.class));
      binder.bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
      binder.bind(HookService.class).toInstance(createMock(HookService.class));
      binder.install(new FactoryModuleBuilder().implement(
          Cluster.class, ClusterImpl.class).build(ClusterFactory.class));

      try {
        AmbariMetaInfo ambariMetaInfo = EasyMock.createNiceMock(AmbariMetaInfo.class);
        EasyMock.expect(
            ambariMetaInfo.getServices(EasyMock.anyString(), EasyMock.anyString())).andReturn(
                new HashMap<String, ServiceInfo>()).anyTimes();

        EasyMock.replay(ambariMetaInfo);

        binder.bind(AmbariMetaInfo.class).toInstance(ambariMetaInfo);
      } catch (Exception exception) {
        Assert.fail(exception.toString());
      }

      binder.bind(ClusterDAO.class).toInstance(createNiceMock(ClusterDAO.class));
    }
  }
}
