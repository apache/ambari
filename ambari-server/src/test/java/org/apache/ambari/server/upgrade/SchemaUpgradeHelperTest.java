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
package org.apache.ambari.server.upgrade;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.persist.jpa.AmbariJpaPersistService;

/**
 * Base Test Upgrade Catalog class
 */
abstract class TestUpgradeCatalog extends AbstractUpgradeCatalog{

  @Inject
  public TestUpgradeCatalog(Injector injector) {
    super(injector);
  }

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    // no op
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    // no op
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    // no op
  }
}

/**
 * Sample Upgrade Catalog version 1.0
 */
class TestUpgradeCatalog10 extends TestUpgradeCatalog {

  @Inject
  public TestUpgradeCatalog10(Injector injector) {
    super(injector);
  }

  @Override
  public String getTargetVersion() {
    return "0.1.0";
  }
}

/**
 * Sample Upgrade Catalog version 2.0
 */
class TestUpgradeCatalog20 extends TestUpgradeCatalog {

  @Inject
  public TestUpgradeCatalog20(Injector injector) {
    super(injector);
  }

  @Override
  public String getTargetVersion() {
    return "0.2.0";
  }
}

/**
 * Sample Upgrade Catalog version 3.0
 */
class TestUpgradeCatalog30 extends TestUpgradeCatalog {

  @Inject
  public TestUpgradeCatalog30(Injector injector) {
    super(injector);
  }

  @Override
  public String getTargetVersion() {
    return "0.3.0";
  }
}


class UpgradeHelperTestModule extends InMemoryDefaultTestModule {

  UpgradeHelperTestModule() {
  }

  @Override
  protected void configure() {
    super.configure();

    Multibinder<UpgradeCatalog> catalogBinder =
      Multibinder.newSetBinder(binder(), UpgradeCatalog.class);
    catalogBinder.addBinding().to(TestUpgradeCatalog10.class);
    catalogBinder.addBinding().to(TestUpgradeCatalog20.class);
    catalogBinder.addBinding().to(TestUpgradeCatalog30.class);

    EventBusSynchronizer.synchronizeAmbariEventPublisher(binder());
  }
}

/**
 * Test class for {@link SchemaUpgradeHelper}
 */
public class SchemaUpgradeHelperTest {

  private SchemaUpgradeHelper schemaUpgradeHelper;

  @Before
  public void init(){
    Injector injector = Guice.createInjector(new UpgradeHelperTestModule());
    injector.getInstance(AmbariJpaPersistService.class).start();
    schemaUpgradeHelper = injector.getInstance(SchemaUpgradeHelper.class);
  }

  @Test
  public void testGetMinimalUpgradeCatalogVersion() throws Exception{
    Method getMinimalUpgradeCatalogVersion = schemaUpgradeHelper.getClass().getDeclaredMethod("getMinimalUpgradeCatalogVersion");
    getMinimalUpgradeCatalogVersion.setAccessible(true);
    String s = (String)getMinimalUpgradeCatalogVersion.invoke(schemaUpgradeHelper);

    Assert.assertEquals("0.1.0", s);
  }

  @Test
  public void testVerifyUpgradePath() throws Exception{
    Method verifyUpgradePath = schemaUpgradeHelper.getClass().getDeclaredMethod("verifyUpgradePath", String.class, String.class);
    verifyUpgradePath.setAccessible(true);

    boolean failToVerify = (boolean)verifyUpgradePath.invoke(schemaUpgradeHelper, "0.3.0", "0.2.0");
    boolean verifyPassed = (boolean)verifyUpgradePath.invoke(schemaUpgradeHelper, "0.1.0", "0.2.0");

    Assert.assertTrue(verifyPassed);
    Assert.assertFalse(failToVerify);
  }

  @Test
  public void testSameVersionUpgradeRunsOnlyAdditiveMembershipSchema() throws Exception {
    List<UpgradeCatalog> upgradePath = schemaUpgradeHelper.getUpgradePath("0.3.0", "0.3.0");
    Assert.assertTrue(upgradePath.isEmpty());

    List<String> executionOrder = new ArrayList<>();
    RecordingHostMembershipSchemaUpgrade membershipUpgrade =
        new RecordingHostMembershipSchemaUpgrade(executionOrder);
    RecordingScopedWorkflowSchemaUpgrade workflowUpgrade =
        new RecordingScopedWorkflowSchemaUpgrade(executionOrder);
    RecordingClusterCreationSchemaUpgrade creationUpgrade =
        new RecordingClusterCreationSchemaUpgrade(executionOrder);
    RecordingTopologyProvisioningSchemaUpgrade topologyUpgrade =
        new RecordingTopologyProvisioningSchemaUpgrade(executionOrder);
    RecordingServiceDependencySchemaUpgrade dependencyUpgrade =
        new RecordingServiceDependencySchemaUpgrade(executionOrder);
    SchemaUpgradeHelper helper = new SchemaUpgradeHelper(Collections.emptySet(), null, null, null,
        membershipUpgrade, workflowUpgrade, creationUpgrade, topologyUpgrade, dependencyUpgrade);

    helper.executeUpgradeAndEnsureAdditiveSchema(upgradePath);

    Assert.assertEquals(List.of("host-membership", "scoped-workflow", "cluster-creation",
        "topology-provisioning", "service-dependency"), executionOrder);
  }

  @Test
  public void testVersionedDdlRunsBeforeAdditiveMembershipSchema() throws Exception {
    List<String> executionOrder = new ArrayList<>();
    UpgradeCatalog versionedCatalog = new RecordingUpgradeCatalog(executionOrder);
    RecordingHostMembershipSchemaUpgrade membershipUpgrade =
        new RecordingHostMembershipSchemaUpgrade(executionOrder);
    RecordingScopedWorkflowSchemaUpgrade workflowUpgrade =
        new RecordingScopedWorkflowSchemaUpgrade(executionOrder);
    RecordingClusterCreationSchemaUpgrade creationUpgrade =
        new RecordingClusterCreationSchemaUpgrade(executionOrder);
    RecordingTopologyProvisioningSchemaUpgrade topologyUpgrade =
        new RecordingTopologyProvisioningSchemaUpgrade(executionOrder);
    RecordingServiceDependencySchemaUpgrade dependencyUpgrade =
        new RecordingServiceDependencySchemaUpgrade(executionOrder);
    SchemaUpgradeHelper helper = new SchemaUpgradeHelper(Collections.emptySet(), null, null, null,
        membershipUpgrade, workflowUpgrade, creationUpgrade, topologyUpgrade, dependencyUpgrade);

    helper.executeUpgradeAndEnsureAdditiveSchema(Collections.singletonList(versionedCatalog));

    Assert.assertEquals(List.of("versioned-ddl", "host-membership", "scoped-workflow", "cluster-creation",
        "topology-provisioning", "service-dependency"), executionOrder);
  }

  @Test
  public void testSameVersionUpgradeSkipsUnrelatedPostUpgradeMaintenance() throws Exception {
    RecordingSchemaUpgradeHelper helper = new RecordingSchemaUpgradeHelper();

    helper.executePostUpgradeMaintenance(Collections.emptyList());

    Assert.assertFalse(helper.uiStateReset);
    Assert.assertFalse(helper.rcaCleanup);

    helper.executePostUpgradeMaintenance(Collections.singletonList(new RecordingUpgradeCatalog(new ArrayList<>())));

    Assert.assertTrue(helper.uiStateReset);
    Assert.assertTrue(helper.rcaCleanup);
  }

  private static class RecordingHostMembershipSchemaUpgrade extends HostMembershipSchemaUpgrade {
    private final List<String> executionOrder;

    RecordingHostMembershipSchemaUpgrade(List<String> executionOrder) {
      super(null);
      this.executionOrder = executionOrder;
    }

    @Override
    public void execute() {
      executionOrder.add("host-membership");
    }
  }

  private static class RecordingScopedWorkflowSchemaUpgrade extends ScopedWorkflowSchemaUpgrade {
    private final List<String> executionOrder;

    RecordingScopedWorkflowSchemaUpgrade(List<String> executionOrder) {
      super(null);
      this.executionOrder = executionOrder;
    }

    @Override
    public void execute() {
      executionOrder.add("scoped-workflow");
    }
  }

  private static class RecordingClusterCreationSchemaUpgrade extends ClusterCreationSchemaUpgrade {
    private final List<String> executionOrder;

    RecordingClusterCreationSchemaUpgrade(List<String> executionOrder) {
      super(null);
      this.executionOrder = executionOrder;
    }

    @Override
    public void execute() {
      executionOrder.add("cluster-creation");
    }
  }

  private static class RecordingTopologyProvisioningSchemaUpgrade
      extends TopologyProvisioningSchemaUpgrade {
    private final List<String> executionOrder;

    RecordingTopologyProvisioningSchemaUpgrade(List<String> executionOrder) {
      super(null);
      this.executionOrder = executionOrder;
    }

    @Override
    public void execute() {
      executionOrder.add("topology-provisioning");
    }
  }

  private static class RecordingServiceDependencySchemaUpgrade
      extends ServiceDependencySchemaUpgrade {
    private final List<String> executionOrder;

    RecordingServiceDependencySchemaUpgrade(List<String> executionOrder) {
      super(null);
      this.executionOrder = executionOrder;
    }

    @Override
    public void execute() {
      executionOrder.add("service-dependency");
    }
  }

  private static class RecordingSchemaUpgradeHelper extends SchemaUpgradeHelper {
    private boolean uiStateReset;
    private boolean rcaCleanup;

    RecordingSchemaUpgradeHelper() {
      super(Collections.emptySet(), null, null, null,
          new RecordingHostMembershipSchemaUpgrade(new ArrayList<>()),
          new RecordingScopedWorkflowSchemaUpgrade(new ArrayList<>()),
          new RecordingClusterCreationSchemaUpgrade(new ArrayList<>()),
          new RecordingTopologyProvisioningSchemaUpgrade(new ArrayList<>()),
          new RecordingServiceDependencySchemaUpgrade(new ArrayList<>()));
    }

    @Override
    public void resetUIState() {
      uiStateReset = true;
    }

    @Override
    public void cleanUpRCATables() {
      rcaCleanup = true;
    }
  }

  private static class RecordingUpgradeCatalog implements UpgradeCatalog {
    private final List<String> executionOrder;

    RecordingUpgradeCatalog(List<String> executionOrder) {
      this.executionOrder = executionOrder;
    }

    @Override
    public void upgradeSchema() {
      executionOrder.add("versioned-ddl");
    }

    @Override
    public void preUpgradeData() {
    }

    @Override
    public void upgradeData() {
    }

    @Override
    public void setConfigUpdatesFileName(String ambariUpgradeConfigUpdatesFileName) {
    }

    @Override
    public boolean isFinal() {
      return false;
    }

    @Override
    public void onPostUpgrade() {
    }

    @Override
    public String getTargetVersion() {
      return "0.3.0";
    }

    @Override
    public String getSourceVersion() {
      return "0.2.0";
    }

    @Override
    public void updateDatabaseSchemaVersion() {
    }

    @Override
    public Map<String, String> getUpgradeJsonOutput() {
      return Collections.emptyMap();
    }
  }

}
