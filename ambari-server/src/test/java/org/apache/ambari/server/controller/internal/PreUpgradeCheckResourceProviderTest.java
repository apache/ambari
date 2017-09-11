/*
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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.checks.AbstractCheckDescriptor;
import org.apache.ambari.server.checks.UpgradeCheckRegistry;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.CheckHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeHelper;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack.PrerequisiteCheckConfig;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * PreUpgradeCheckResourceProvider tests.
 */
public class PreUpgradeCheckResourceProviderTest {

  @Test
  public void testGetResources() throws Exception{
    Injector injector = createInjector();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    Clusters clusters = injector.getInstance(Clusters.class);
    UpgradeHelper upgradeHelper = injector.getInstance(UpgradeHelper.class);
    Configuration configuration = injector.getInstance(Configuration.class);

    RepositoryVersionDAO repoDao = injector.getInstance(RepositoryVersionDAO.class);
    RepositoryVersionEntity repo = createNiceMock(RepositoryVersionEntity.class);
    UpgradePack upgradePack = createNiceMock(UpgradePack.class);
    PrerequisiteCheckConfig config = createNiceMock(PrerequisiteCheckConfig.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);

    StackId currentStackId = createNiceMock(StackId.class);
    StackId targetStackId = createNiceMock(StackId.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);

    Map<String, Service> allServiceMap = new HashMap<>();
    allServiceMap.put("Service100", service);
    Map<String, ServiceInfo> allServiceInfoMap = new HashMap<>();
    allServiceInfoMap.put("Service100", serviceInfo);

    expect(configuration.isUpgradePrecheckBypass()).andReturn(false).anyTimes();
    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getServices()).andReturn(allServiceMap).anyTimes();
    expect(cluster.getService("Service100")).andReturn(service).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(currentStackId).anyTimes();

    expect(currentStackId.getStackName()).andReturn("Stack100").anyTimes();
    expect(currentStackId.getStackVersion()).andReturn("1.0").anyTimes();
    expect(targetStackId.getStackName()).andReturn("Stack100").anyTimes();
    expect(targetStackId.getStackVersion()).andReturn("1.1").anyTimes();

    expect(repoDao.findByPK(1L)).andReturn(repo).anyTimes();
    expect(repo.getStackId()).andReturn(targetStackId).atLeastOnce();
    expect(upgradeHelper.suggestUpgradePack("Cluster100", currentStackId, targetStackId, Direction.UPGRADE, UpgradeType.NON_ROLLING, "upgrade_pack11")).andReturn(upgradePack);

    List<AbstractCheckDescriptor> upgradeChecksToRun = new LinkedList<>();
    List<String> prerequisiteChecks = new LinkedList<>();
    prerequisiteChecks.add("org.apache.ambari.server.sample.checks.SampleServiceCheck");
    expect(upgradePack.getPrerequisiteCheckConfig()).andReturn(config);
    expect(upgradePack.getPrerequisiteChecks()).andReturn(prerequisiteChecks).anyTimes();
    expect(upgradePack.getTarget()).andReturn("1.1.*.*").anyTimes();

    expect(ambariMetaInfo.getServices("Stack100", "1.0")).andReturn(allServiceInfoMap).anyTimes();
    String checks = ClassLoader.getSystemClassLoader().getResource("checks").getPath();
    expect(serviceInfo.getChecksFolder()).andReturn(new File(checks));

    // replay
    replay(managementController, clusters, cluster, service, serviceInfo, repoDao, repo, upgradeHelper,
        ambariMetaInfo, upgradePack, config, currentStackId, targetStackId, serviceFactory, configuration);

    ResourceProvider provider = getPreUpgradeCheckResourceProvider(managementController, injector);
    // create the request
    Request request = PropertyHelper.getReadRequest(new HashSet<>());
    PredicateBuilder builder = new PredicateBuilder();
    Predicate predicate = builder.property(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and()
        .property(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_UPGRADE_PACK_PROPERTY_ID).equals("upgrade_pack11").and()
        .property(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID).equals(UpgradeType.NON_ROLLING).and()
        .property(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_TARGET_REPOSITORY_VERSION_ID_ID).equals("1").toPredicate();


    System.out.println("PreUpgradeCheckResourceProvider - " + provider);
    Set<Resource> resources = Collections.emptySet();
    try {
      resources = provider.getResources(request, predicate);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String id = (String) resource.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_ID_PROPERTY_ID);
      Assert.assertEquals("SAMPLE_SERVICE_CHECK", id);
      String description = (String) resource.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_CHECK_PROPERTY_ID);
      Assert.assertEquals("Sample service check description.", description);
      PrereqCheckStatus status = (PrereqCheckStatus) resource.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_STATUS_PROPERTY_ID);
      Assert.assertEquals(PrereqCheckStatus.FAIL, status);
      String reason = (String) resource.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_REASON_PROPERTY_ID);
      Assert.assertEquals("Sample service check always fails.", reason);
      PrereqCheckType checkType = (PrereqCheckType) resource.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_CHECK_TYPE_PROPERTY_ID);
      Assert.assertEquals(PrereqCheckType.HOST, checkType);
      String clusterName = (String) resource.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      UpgradeType upgradeType = (UpgradeType) resource.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID);
      Assert.assertEquals(UpgradeType.NON_ROLLING, upgradeType);
    }

    // verify
    verify(managementController, clusters, cluster, service, serviceInfo, repoDao, repo, upgradeHelper,
            ambariMetaInfo, upgradePack, config, currentStackId, targetStackId, serviceFactory);
  }

  /**
   * This factory method creates PreUpgradeCheckResourceProvider using the mock managementController
   */
  public PreUpgradeCheckResourceProvider getPreUpgradeCheckResourceProvider(AmbariManagementController managementController, Injector injector) throws  AmbariException {
    //UpgradeHelper upgradeHelper = injector.getInstance(UpgradeHelper.class);
    //injector.injectMembers(upgradeHelper);
    PreUpgradeCheckResourceProvider provider = new PreUpgradeCheckResourceProvider(managementController);
    return provider;
  }

  static class TestClustersProvider implements Provider<Clusters> {
    private static Clusters clusters = createNiceMock(Clusters.class);

    @Override
    public Clusters get() {
      return clusters;
    }
  }

  static class TestConfigurationProvider implements Provider<Configuration> {
    private static Configuration configuration = createNiceMock(Configuration.class);

    @Override
    public Configuration get(){
      return configuration;
    }
  }

  static class TestUpgradeHelperProvider implements Provider<UpgradeHelper> {
    private static UpgradeHelper upgradeHelper = createNiceMock(UpgradeHelper.class);

    @Override
    public UpgradeHelper get() {
      return upgradeHelper;
    }
  }

  private Injector createInjector() throws Exception {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Provider<Clusters> clustersProvider = new TestClustersProvider();
        Provider<UpgradeHelper> upgradeHelperProvider = new TestUpgradeHelperProvider();
        CheckHelper checkHelper = new CheckHelper();
        UpgradeCheckRegistry registry = new UpgradeCheckRegistry();

        bind(Configuration.class).toProvider(TestConfigurationProvider.class);
        bind(AmbariManagementController.class).toInstance(createNiceMock(AmbariManagementController.class));
        bind(CheckHelper.class).toInstance(checkHelper);
        bind(Clusters.class).toProvider(TestClustersProvider.class);
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(RepositoryVersionDAO.class).toInstance(createNiceMock(RepositoryVersionDAO.class));
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        bind(UpgradeCheckRegistry.class).toInstance(registry);
        bind(UpgradeHelper.class).toProvider(TestUpgradeHelperProvider.class);

        requestStaticInjection(PreUpgradeCheckResourceProvider.class);
      }
    });
  }

}
