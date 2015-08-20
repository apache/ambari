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
package org.apache.ambari.server.checks;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Provider;

/**
 * Tests {@link HiveMultipleMetastoreCheck}
 */
public class HiveMultipleMetastoreCheckTest {
  private final Clusters m_clusters = Mockito.mock(Clusters.class);
  private final HiveMultipleMetastoreCheck m_check = new HiveMultipleMetastoreCheck();

  /**
   *
   */
  @Before
  public void setup() {
    m_check.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return m_clusters;
      }
    };
    Configuration config = Mockito.mock(Configuration.class);
    Mockito.when(config.getRollingUpgradeMinStack()).thenReturn("HDP-2.2");
    Mockito.when(config.getRollingUpgradeMaxStack()).thenReturn("");
    m_check.config = config;
  }

  /**
   * Tests that the check is applicable when hive is installed.
   *
   * @throws Exception
   */
  @Test
  public void testIsApplicable() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);

    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);
    Map<String, Service> services = new HashMap<String, Service>();
    Mockito.when(cluster.getServices()).thenReturn(services);

    services.put("HDFS", Mockito.mock(Service.class));

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setRepositoryVersion("2.3.0.0");

    // HIVE not installed
    Assert.assertFalse(m_check.isApplicable(request));

    // install HIVE
    services.put("HIVE", Mockito.mock(Service.class));

    // HIVE installed
    Assert.assertTrue(m_check.isApplicable(request));
  }

  /**
   * Tests that the warning is correctly tripped when there are not enough
   * metastores.
   *
   * @throws Exception
   */
  @Test
  public void testPerform() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Service hive = Mockito.mock(Service.class);
    ServiceComponent metastore = Mockito.mock(ServiceComponent.class);

    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);
    Mockito.when(cluster.getService("HIVE")).thenReturn(hive);

    Mockito.when(hive.getServiceComponent("HIVE_METASTORE")).thenReturn(metastore);

    Map<String, ServiceComponentHost> metastores = new HashMap<String, ServiceComponentHost>();
    Mockito.when(metastore.getServiceComponentHosts()).thenReturn(metastores);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setRepositoryVersion("2.3.0.0");
    m_check.perform(check, request);

    Assert.assertEquals(PrereqCheckStatus.WARNING, check.getStatus());

    metastores.put("c6401", Mockito.mock(ServiceComponentHost.class));
    metastores.put("c6402", Mockito.mock(ServiceComponentHost.class));

    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, request);

    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }

  @Test
  public void testPerformFail() throws Exception{
    final Cluster cluster = Mockito.mock(Cluster.class);
    final LinkedHashSet<String> failedOnExpected = new LinkedHashSet<String>();
    Service hive = Mockito.mock(Service.class);
    ServiceComponent metastore = Mockito.mock(ServiceComponent.class);
    Map<String, ServiceComponentHost> metastores = new HashMap<String, ServiceComponentHost>();

    failedOnExpected.add("HIVE");

    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);
    Mockito.when(cluster.getService("HIVE")).thenReturn(hive);
    Mockito.when(hive.getServiceComponent("HIVE_METASTORE")).thenReturn(metastore);
    Mockito.when(metastore.getServiceComponentHosts()).thenReturn(metastores);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setRepositoryVersion("2.3.0.0");
    m_check.perform(check, request);

    Assert.assertEquals(PrereqCheckStatus.WARNING, check.getStatus());

    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, request);
    Assert.assertEquals(failedOnExpected, check.getFailedOn());
    Assert.assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
  }
}
