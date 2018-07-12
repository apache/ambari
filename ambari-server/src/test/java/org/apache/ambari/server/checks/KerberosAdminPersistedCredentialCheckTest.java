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
package org.apache.ambari.server.checks;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.util.Modules;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.UpgradePlanEntity;
import org.apache.ambari.server.security.credential.Credential;
import org.apache.ambari.server.security.encryption.CredentialStoreServiceImpl;
import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.UpgradeHelper;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradeCheckResult;
import org.apache.ambari.server.state.stack.UpgradePack;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(EasyMockRunner.class)
public class KerberosAdminPersistedCredentialCheckTest extends EasyMockSupport {

  @Mock
  private UpgradeHelper upgradeHelper;

  private Injector injector;
  private Long clusterId = 1L;
  private String clusterName = "c1";
  private Clusters clusters;
  private Cluster cluster;
  private CredentialStoreServiceImpl credentialStoreService;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new KerberosAdminPersistedCredentialCheckTest.MockModule()));
    injector.getInstance(GuiceJpaInitializer.class);
    injector.getInstance(AmbariMetaInfo.class);

    OrmTestHelper ormHelper = injector.getInstance(OrmTestHelper.class);
    ormHelper.createCluster(clusterName);
    clusters = injector.getInstance(ClustersImpl.class);
    injector.injectMembers(clusters);
    cluster = createNiceMock(ClusterImpl.class);
    Method method = ClustersImpl.class.getDeclaredMethod("getClustersByName");
    method.setAccessible(true);
    Map<String, Cluster> map = (Map)method.invoke(clusters);
    map.put(clusterName, cluster);
    method = ClustersImpl.class.getDeclaredMethod("getClustersById");
    method.setAccessible(true);
    Map<Long, Cluster> map2 = (Map)method.invoke(clusters);
    map2.put(clusterId, cluster);
    credentialStoreService = injector.getInstance(CredentialStoreServiceImpl.class);
    injector.injectMembers(credentialStoreService);
  }

  @After
  public void after() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    injector = null;
  }

  @Test
  public void testMissingCredentialStoreKerberosEnabledManagingIdentities() throws Exception {
    UpgradeCheckResult result = executeCheck(true, true, false, false);
    Assert.assertEquals(PrereqCheckStatus.FAIL, result.getStatus());
  }

  @Test
  public void testMissingCredentialStoreKerberosEnabledNotManagingIdentities() throws Exception {
    UpgradeCheckResult result = executeCheck(true, false, false, false);
    Assert.assertEquals(PrereqCheckStatus.PASS, result.getStatus());
  }

  @Test
  public void testMissingCredentialStoreKerberosNotEnabled() throws Exception {
    UpgradeCheckResult result = executeCheck(false, false, false, false);
    Assert.assertEquals(PrereqCheckStatus.PASS, result.getStatus());
  }

  @Test
  public void testMissingCredentialKerberosEnabledManagingIdentities() throws Exception {
    UpgradeCheckResult result = executeCheck(true, true, true, false);
    Assert.assertEquals(PrereqCheckStatus.FAIL, result.getStatus());
    Assert.assertTrue(result.getFailReason().startsWith("The KDC administrator credential has not been stored in the persisted credential store."));
  }

  @Test
  public void testMissingCredentialKerberosEnabledNotManagingIdentities() throws Exception {
    UpgradeCheckResult result = executeCheck(true, false, true, false);
    Assert.assertEquals(PrereqCheckStatus.PASS, result.getStatus());
  }

  @Test
  public void testMissingCredentialKerberosNotEnabled() throws Exception {
    UpgradeCheckResult result = executeCheck(false, true, true, false);
    Assert.assertEquals(PrereqCheckStatus.PASS, result.getStatus());
  }

  @Test
  public void testCredentialsSetKerberosNotEnabled() throws Exception {
    UpgradeCheckResult result = executeCheck(false, false, true, true);
    Assert.assertEquals(PrereqCheckStatus.PASS, result.getStatus());
  }

  @Test
  public void testCredentialsSetKerberosEnabledNotManagingIdentities() throws Exception {
    UpgradeCheckResult result = executeCheck(true, false, true, true);
    Assert.assertEquals(PrereqCheckStatus.PASS, result.getStatus());
  }

  @Test
  public void testCredentialsSetKerberosEnabledManagingIdentities() throws Exception {
    UpgradeCheckResult result = executeCheck(true, true, true, true);
    Assert.assertEquals(PrereqCheckStatus.PASS, result.getStatus());
  }

  // TODO: [AMP] Revisit unit tests
  private UpgradeCheckResult executeCheck(boolean kerberosEnabled, boolean manageIdentities, boolean credentialStoreInitialized, boolean credentialSet) throws Exception {

    Map<String, String> checkProperties = new HashMap<>();

    UpgradePack.PrerequisiteCheckConfig prerequisiteCheckConfig = createMock(UpgradePack.PrerequisiteCheckConfig.class);
    expect(prerequisiteCheckConfig.getCheckProperties(KerberosAdminPersistedCredentialCheck.class.getName())).andReturn(checkProperties).anyTimes();

    DesiredConfig desiredKerberosEnv = createMock(DesiredConfig.class);
    expect(desiredKerberosEnv.getTag()).andReturn("tag").anyTimes();

    Map<String, DesiredConfig> desiredConfigs = new HashMap<>();
    desiredConfigs.put("kerberos-env", desiredKerberosEnv);

    Config kerberosEnv = createMock(Config.class);
    expect(kerberosEnv.getProperties()).andReturn(Collections.singletonMap("manage_identities", manageIdentities ? "true" : "false")).anyTimes();
    expect(kerberosEnv.getType()).andReturn("kerberos-env").anyTimes();
    expect(kerberosEnv.getTag()).andReturn("tag").anyTimes();

    expect(cluster.getSecurityType()).andReturn(kerberosEnabled ? SecurityType.KERBEROS : SecurityType.NONE).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(desiredConfigs).anyTimes();
    expect(cluster.getConfig("kerberos-env", "tag")).andReturn(kerberosEnv).anyTimes();

    UpgradePlanEntity upgradePlan = createMock(UpgradePlanEntity.class);

    expect(upgradePlan.getClusterId()).andReturn(clusterId).anyTimes();

    PrereqCheckRequest request = createNiceMock(PrereqCheckRequest.class);
    expect(request.getUpgradePlan()).andReturn(upgradePlan).anyTimes();
    expect(request.getPrerequisiteCheckConfig()).andReturn(prerequisiteCheckConfig).anyTimes();
    expect(request.getClusterName()).andReturn(clusterName).anyTimes();

    Credential credential = createMock(Credential.class);

    expect(credentialStoreService.isInitialized(CredentialStoreType.PERSISTED)).andReturn(credentialStoreInitialized).anyTimes();
    expect(credentialStoreService.getCredential(clusterName, KerberosHelper.KDC_ADMINISTRATOR_CREDENTIAL_ALIAS, CredentialStoreType.PERSISTED)).andReturn(credentialSet ? credential : null).anyTimes();

    Provider<Clusters> clustersProvider = () -> clusters;

    replayAll();

    //injector.injectMembers(request);

    cluster.addConfig(kerberosEnv);

    injector.getInstance(AmbariMetaInfo.class).init();

    KerberosAdminPersistedCredentialCheck check = new KerberosAdminPersistedCredentialCheck();
    injector.injectMembers(check);

    check.clustersProvider = clustersProvider;
    UpgradeCheckResult result = check.perform(request);

    verifyAll();

    return result;
  }

  private UpgradePack upgradePackWithRegenKeytab() {
    UpgradePack upgradePack = createMock(UpgradePack.class);
    expect(upgradePack.anyGroupTaskMatch(anyObject())).andReturn(true).anyTimes();
    return upgradePack;
  }

  private class MockModule implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(CredentialStoreServiceImpl.class).toInstance(createNiceMock(CredentialStoreServiceImpl.class));
    }
  }
}
