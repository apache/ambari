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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.persist.PersistService;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UpgradeCatalogTest {
  private Injector injector;
  private AmbariMetaInfo metaInfo;
  private final String DESIRED_STACK_VERSION = "{\"stackName\":\"HDP\"," +
    "\"stackVersion\":\"1.2.0\"}";

  private static class UpgradeCatalog149 extends AbstractUpgradeCatalog {

    @Inject
    public UpgradeCatalog149(Injector injector) {
      super(injector);
    }

    @Override
    public void executeDDLUpdates() throws AmbariException, SQLException {
      // do nothing: only for path testing
    }

    @Override
    public void executePreDMLUpdates() throws AmbariException, SQLException {

    }

    @Override
    public void executeDMLUpdates() throws AmbariException, SQLException {

    }

    @Override
    public String getTargetVersion() {
      return "1.4.9";
    }
  }

  private static class UpgradeHelperModuleTest extends InMemoryDefaultTestModule {
    @Override
    protected void configure() {
      super.configure();

      // Add binding to each newly created catalog
      Multibinder<UpgradeCatalog> catalogBinder =
        Multibinder.newSetBinder(binder(), UpgradeCatalog.class);
      catalogBinder.addBinding().to(UpgradeCatalog149.class);
      catalogBinder.addBinding().to(UpgradeCatalog150.class);
      catalogBinder.addBinding().to(UpgradeCatalog170.class);
      catalogBinder.addBinding().to(UpgradeCatalog200.class);
      catalogBinder.addBinding().to(UpgradeCatalog210.class);
    }
  }

  @Before
  public void setup() throws Exception {
    injector  = Guice.createInjector(new UpgradeHelperModuleTest());
    injector.getInstance(GuiceJpaInitializer.class);
    metaInfo = injector.getInstance(AmbariMetaInfo.class);
  }

  @After
  public void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testUpgradePath() throws Exception {
    SchemaUpgradeHelper schemaUpgradeHelper = injector.getInstance(SchemaUpgradeHelper.class);

    Set<UpgradeCatalog> upgradeCatalogSet = schemaUpgradeHelper.getAllUpgradeCatalogs();

    Assert.assertNotNull(upgradeCatalogSet);
    Assert.assertEquals(5, upgradeCatalogSet.size());

    List<UpgradeCatalog> upgradeCatalogs = schemaUpgradeHelper.getUpgradePath(null, "1.5.1");

    Assert.assertNotNull(upgradeCatalogs);
    Assert.assertEquals(2, upgradeCatalogs.size());
    Assert.assertEquals("1.4.9", upgradeCatalogs.get(0).getTargetVersion());
    Assert.assertEquals("1.5.0", upgradeCatalogs.get(1).getTargetVersion());
  }

  @Test
  public void testUpdateConfigurationProperties() throws Exception {
    AmbariManagementController controller = injector.getInstance(AmbariManagementController.class);
    ClusterRequest r = new ClusterRequest(null, "c1", "HDP-0.1", null);
    controller.createCluster(r);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = clusters.getCluster("c1");

    Assert.assertNotNull(cluster);

    Map<String, String> properties = new HashMap<String, String>() {{
      put("a", "b");
    }};

    final ClusterRequest cl = new ClusterRequest(
      cluster.getClusterId(),
      cluster.getClusterName(),
      cluster.getDesiredStackVersion().getStackVersion(),
      null
    );

    ConfigurationRequest cr = new ConfigurationRequest();
    cr.setClusterName(cluster.getClusterName());
    cr.setType("global");
    cr.setVersionTag("version1");
    cr.setProperties(properties);

    cl.setDesiredConfig(Collections.singletonList(cr));

    controller.updateClusters(new HashSet<ClusterRequest>() {{ add(cl); }}, null);

    Config config = cluster.getConfig("global", "version1");

    Assert.assertNotNull(config);
    Assert.assertEquals(properties, config.getProperties());

    // Add new
    UpgradeCatalog149 testCatalog = injector.getInstance(UpgradeCatalog149.class);
    testCatalog.updateConfigurationProperties("global",
      Collections.singletonMap("x", "y"), false, false);
    config = cluster.getDesiredConfigByType("global");
    String version = config.getTag();
    Assert.assertNotNull(config);
    Assert.assertNotSame("version1", version);
    Assert.assertTrue(config.getProperties().containsKey("x"));
    Assert.assertEquals("y", config.getProperties().get("x"));

    // Override value
    testCatalog.updateConfigurationProperties("global",
      Collections.singletonMap("x", "z"), true, false);
    config = cluster.getDesiredConfigByType("global");
    Assert.assertNotNull(config);
    Assert.assertNotSame(version, config.getTag());
    Assert.assertTrue(config.getProperties().containsKey("x"));
    Assert.assertEquals("z", config.getProperties().get("x"));
    version = config.getTag();

    // Retain original
    testCatalog.updateConfigurationProperties("global",
      Collections.singletonMap("x", "y"), false, false);
    config = cluster.getDesiredConfigByType("global");
    Assert.assertNotNull(config);
    Assert.assertSame(version, config.getTag());
    Assert.assertTrue(config.getProperties().containsKey("x"));
    Assert.assertEquals("z", config.getProperties().get("x"));
  }
}
