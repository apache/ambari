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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.HeartbeatTestHelper;
import org.apache.ambari.server.agent.RecoveryConfig;
import org.apache.ambari.server.agent.RecoveryConfigHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

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

  @Before
  public void setup() throws Exception {
    module = HeartbeatTestHelper.getTestModule();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
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
      throws NoSuchFieldException, IllegalAccessException, AmbariException {
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
      throws NoSuchFieldException, IllegalAccessException, AmbariException {
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
