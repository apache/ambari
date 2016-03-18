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

package org.apache.ambari.server.configuration;

import com.google.common.eventbus.EventBus;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.HeartbeatTestHelper;
import org.apache.ambari.server.agent.RecoveryConfig;
import org.apache.ambari.server.agent.RecoveryConfigHelper;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DATANODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostname1;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.NAMENODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test RecoveryConfigHelper class
 */
public class RecoveryConfigHelperTest {
  private Injector injector;

  private InMemoryDefaultTestModule module;

  @Inject
  private HeartbeatTestHelper heartbeatTestHelper;

  @Inject
  private RecoveryConfigHelper recoveryConfigHelper;

  @Inject
  private AmbariEventPublisher eventPublisher;

  @Before
  public void setup() throws Exception {
    module = HeartbeatTestHelper.getTestModule();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);

    // Synchronize the publisher (AmbariEventPublisher) and subscriber (RecoveryConfigHelper),
    // so that the events get handled as soon as they are published, allowing the tests to
    // verify the methods under test.
    EventBus synchronizedBus = EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);
    synchronizedBus.register(recoveryConfigHelper);
  }

  @After
  public void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  /**
   * Test default cluster-env properties for recovery.
   */
  @Test
  public void testRecoveryConfigDefaultValues()
      throws AmbariException {
    RecoveryConfig recoveryConfig = recoveryConfigHelper.getDefaultRecoveryConfig();
    assertEquals(recoveryConfig.getMaxLifetimeCount(), RecoveryConfigHelper.RECOVERY_LIFETIME_MAX_COUNT_DEFAULT);
    assertEquals(recoveryConfig.getMaxCount(), RecoveryConfigHelper.RECOVERY_MAX_COUNT_DEFAULT);
    assertEquals(recoveryConfig.getRetryGap(), RecoveryConfigHelper.RECOVERY_RETRY_GAP_DEFAULT);
    assertEquals(recoveryConfig.getWindowInMinutes(), RecoveryConfigHelper.RECOVERY_WINDOW_IN_MIN_DEFAULT);
    assertEquals(recoveryConfig.getType(), RecoveryConfigHelper.RECOVERY_TYPE_DEFAULT);
    assertNull(recoveryConfig.getEnabledComponents());
  }

  /**
   * Test cluster-env properties from a dummy cluster
   * @throws AmbariException
   */
  @Test
  public void testRecoveryConfigValues()
      throws AmbariException {
    String hostname = "hostname1";
    Cluster cluster = getDummyCluster(hostname);
    RecoveryConfig recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), hostname);
    assertEquals(recoveryConfig.getMaxLifetimeCount(), "10");
    assertEquals(recoveryConfig.getMaxCount(), "4");
    assertEquals(recoveryConfig.getRetryGap(), "2");
    assertEquals(recoveryConfig.getWindowInMinutes(), "23");
    assertEquals(recoveryConfig.getType(), "AUTO_START");
    assertNotNull(recoveryConfig.getEnabledComponents());
  }

  /**
   * Install a component with auto start enabled. Verify that the old config was invalidated.
   *
   * @throws AmbariException
   */
  @Test
  public void testServiceComponentInstalled()
      throws AmbariException {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();

    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();

    // Get the recovery configuration
    RecoveryConfig recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(recoveryConfig.getEnabledComponents(), "DATANODE");

    // Install HDFS::NAMENODE to trigger a component installed event
    hdfs.addServiceComponent(NAMENODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();

    // Verify that the config is stale now
    boolean isConfigStale = recoveryConfigHelper.isConfigStale(cluster.getClusterName(), DummyHostname1,
            recoveryConfig.getRecoveryTimestamp());

    assertTrue(isConfigStale);

    // Verify the new config
    recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(recoveryConfig.getEnabledComponents(), "DATANODE,NAMENODE");
  }

  /**
   * Uninstall a component and verify that the config is stale.
   * 
   * @throws AmbariException
   */
  @Test
  public void testServiceComponentUninstalled()
      throws AmbariException {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();

    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();

    hdfs.addServiceComponent(NAMENODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();

    // Get the recovery configuration
    RecoveryConfig recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(recoveryConfig.getEnabledComponents(), "DATANODE,NAMENODE");

    // Uninstall HDFS::DATANODE from host1
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).delete();

    // Verify that the config is stale
    boolean isConfigStale = recoveryConfigHelper.isConfigStale(cluster.getClusterName(), DummyHostname1,
            recoveryConfig.getRecoveryTimestamp());

    assertTrue(isConfigStale);

    // Verify the new config
    recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(recoveryConfig.getEnabledComponents(), "NAMENODE");
  }

  /**
   * Disable cluster level auto start and verify that the config is stale.
   *
   * @throws AmbariException
   */
  @Test
  public void testClusterEnvConfigChanged()
      throws AmbariException {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();

    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).setDesiredState(State.INSTALLED);

    // Get the recovery configuration
    RecoveryConfig recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(recoveryConfig.getEnabledComponents(), "DATANODE");

    // Get cluser-env config and turn off recovery for the cluster
    Config config = cluster.getDesiredConfigByType("cluster-env");

    config.updateProperties(new HashMap<String, String>() {{
      put(RecoveryConfigHelper.RECOVERY_ENABLED_KEY, "false");
    }});
    config.persist(false);

    // Recovery config should be stale because of the above change.
    boolean isConfigStale = recoveryConfigHelper.isConfigStale(cluster.getClusterName(), DummyHostname1,
            recoveryConfig.getRecoveryTimestamp());

    assertTrue(isConfigStale);

    // Get the recovery configuration again and verify that there are no components to be auto started
    recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertNull(recoveryConfig.getEnabledComponents());
  }

  /**
   * Change the maintenance mode of a service component host and verify that config is stale.
   *
   * @throws AmbariException
   */
  @Test
  public void testMaintenanceModeChanged()
      throws AmbariException {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();

    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();

    hdfs.addServiceComponent(NAMENODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();

    // Get the recovery configuration
    RecoveryConfig recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(recoveryConfig.getEnabledComponents(), "DATANODE,NAMENODE");

    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).setMaintenanceState(MaintenanceState.ON);

    // We need a new config
    boolean isConfigStale = recoveryConfigHelper.isConfigStale(cluster.getClusterName(), DummyHostname1,
            recoveryConfig.getRecoveryTimestamp());

    assertTrue(isConfigStale);

    // Only NAMENODE is left
    recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(recoveryConfig.getEnabledComponents(), "NAMENODE");
  }

  /**
   * Disable recovery on a component and verify that the config is stale.
   *
   * @throws AmbariException
   */
  @Test
  public void testServiceComponentRecoveryChanged()
      throws AmbariException {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();

    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();

    // Get the recovery configuration
    RecoveryConfig recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(recoveryConfig.getEnabledComponents(), "DATANODE");

    // Turn off auto start for HDFS::DATANODE
    hdfs.getServiceComponent(DATANODE).setRecoveryEnabled(false);

    // Config should be stale now
    boolean isConfigStale = recoveryConfigHelper.isConfigStale(cluster.getClusterName(), DummyHostname1,
            recoveryConfig.getRecoveryTimestamp());

    assertTrue(isConfigStale);

    // Get the latest config. DATANODE should not be present.
    recoveryConfig = recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), DummyHostname1);
    assertEquals(recoveryConfig.getEnabledComponents(), "");
  }

  private Cluster getDummyCluster(final String hostname)
          throws AmbariException {
    Map<String, String> configProperties = new HashMap<String, String>() {{
      put(RecoveryConfigHelper.RECOVERY_ENABLED_KEY, "true");
      put(RecoveryConfigHelper.RECOVERY_TYPE_KEY, "AUTO_START");
      put(RecoveryConfigHelper.RECOVERY_MAX_COUNT_KEY, "4");
      put(RecoveryConfigHelper.RECOVERY_LIFETIME_MAX_COUNT_KEY, "10");
      put(RecoveryConfigHelper.RECOVERY_WINDOW_IN_MIN_KEY, "23");
      put(RecoveryConfigHelper.RECOVERY_RETRY_GAP_KEY, "2");
    }};

    Set<String> hostNames = new HashSet<String>(){{
      add(hostname);
    }};

    return heartbeatTestHelper.getDummyCluster("cluster1", "HDP-0.1", configProperties, hostNames);
  }
}
