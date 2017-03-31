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

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack.PrerequisiteCheckConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Provider;

/**
 * Tests for {@link ServicePresenceCheck}
 */
public class ServicePresenceCheckTest {
  private final Clusters m_clusters = Mockito.mock(Clusters.class);

  private final ServicePresenceCheck m_check = new ServicePresenceCheck();

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
  }

  @Test
  public void testPerformPass() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);

    Map<String, String> checkProperties = new HashMap<String, String>();
    checkProperties.put(ServicePresenceCheck.NO_UPGRADE_SUPPORT_SERVICES_PROPERTY_NAME,"MyServiceOne, MyServiceTwo");
    checkProperties.put(ServicePresenceCheck.REMOVED_SERVICES_PROPERTY_NAME,"OldServiceOne, OldServiceTwo");
    checkProperties.put(ServicePresenceCheck.NEW_SERVICES_PROPERTY_NAME,"NewServiceOne, NewServiceTwo");

    PrerequisiteCheckConfig prerequisiteCheckConfig = Mockito.mock(PrerequisiteCheckConfig.class);
    Mockito.when(prerequisiteCheckConfig.getCheckProperties(
        m_check.getClass().getName())).thenReturn(checkProperties);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setRepositoryVersion("2.5.0.0");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);

    m_check.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }

  @Test
  public void testPerformHasNoUpgradeSupportServices() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);

    Map<String, Service> services = new HashMap<String, Service>();
    services.put("ATLAS", Mockito.mock(Service.class));
    Mockito.when(cluster.getServices()).thenReturn(services);

    Map<String, String> checkProperties = new HashMap<String, String>();
    checkProperties.put(ServicePresenceCheck.NO_UPGRADE_SUPPORT_SERVICES_PROPERTY_NAME,"Atlas, MyService");
    PrerequisiteCheckConfig prerequisiteCheckConfig = Mockito.mock(PrerequisiteCheckConfig.class);
    Mockito.when(prerequisiteCheckConfig.getCheckProperties(
        m_check.getClass().getName())).thenReturn(checkProperties);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);

    m_check.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }

  @Test
  public void testPerformHasRemovedServices() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);

    Map<String, Service> services = new HashMap<String, Service>();
    services.put("ATLAS", Mockito.mock(Service.class));
    services.put("OLDSERVICE", Mockito.mock(Service.class));
    Mockito.when(cluster.getServices()).thenReturn(services);

    Map<String, String> checkProperties = new HashMap<String, String>();
    checkProperties.put(ServicePresenceCheck.REMOVED_SERVICES_PROPERTY_NAME,"Atlas, OldService");
    checkProperties.put(ServicePresenceCheck.NEW_SERVICES_PROPERTY_NAME,"Atlas2, NewService");

    PrerequisiteCheckConfig prerequisiteCheckConfig = Mockito.mock(PrerequisiteCheckConfig.class);
    Mockito.when(prerequisiteCheckConfig.getCheckProperties(
        m_check.getClass().getName())).thenReturn(checkProperties);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);

    m_check.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }

  @Test
  public void testPerformMixOne() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);

    Map<String, Service> services = new HashMap<String, Service>();
    services.put("ATLAS", Mockito.mock(Service.class));
    Mockito.when(cluster.getServices()).thenReturn(services);

    Map<String, String> checkProperties = new HashMap<String, String>();
    checkProperties.put(ServicePresenceCheck.NO_UPGRADE_SUPPORT_SERVICES_PROPERTY_NAME,"MyServiceOne, MyServiceTwo");
    checkProperties.put(ServicePresenceCheck.REMOVED_SERVICES_PROPERTY_NAME,"Atlas, OldService");
    checkProperties.put(ServicePresenceCheck.NEW_SERVICES_PROPERTY_NAME,"Atlas2, NewService");

    PrerequisiteCheckConfig prerequisiteCheckConfig = Mockito.mock(PrerequisiteCheckConfig.class);
    Mockito.when(prerequisiteCheckConfig.getCheckProperties(
        m_check.getClass().getName())).thenReturn(checkProperties);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);

    m_check.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }

  @Test
  public void testPerformMixTwo() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);

    Map<String, Service> services = new HashMap<String, Service>();
    services.put("OLDSERVICE", Mockito.mock(Service.class));
    Mockito.when(cluster.getServices()).thenReturn(services);

    Map<String, String> checkProperties = new HashMap<String, String>();
    checkProperties.put(ServicePresenceCheck.NO_UPGRADE_SUPPORT_SERVICES_PROPERTY_NAME,"Atlas, MyService");
    checkProperties.put(ServicePresenceCheck.REMOVED_SERVICES_PROPERTY_NAME,"OldService");
    checkProperties.put(ServicePresenceCheck.NEW_SERVICES_PROPERTY_NAME,"NewService");

    PrerequisiteCheckConfig prerequisiteCheckConfig = Mockito.mock(PrerequisiteCheckConfig.class);
    Mockito.when(prerequisiteCheckConfig.getCheckProperties(
        m_check.getClass().getName())).thenReturn(checkProperties);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);

    m_check.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }

  @Test
  public void testPerformMixThree() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);

    Map<String, Service> services = new HashMap<String, Service>();
    services.put("ATLAS", Mockito.mock(Service.class));
    services.put("HDFS", Mockito.mock(Service.class));
    services.put("STORM", Mockito.mock(Service.class));
    services.put("RANGER", Mockito.mock(Service.class));
    Mockito.when(cluster.getServices()).thenReturn(services);

    Map<String, String> checkProperties = new HashMap<String, String>();
    checkProperties.put(ServicePresenceCheck.NO_UPGRADE_SUPPORT_SERVICES_PROPERTY_NAME,"Atlas, HDFS");
    checkProperties.put(ServicePresenceCheck.REMOVED_SERVICES_PROPERTY_NAME,"Storm, Ranger");
    checkProperties.put(ServicePresenceCheck.NEW_SERVICES_PROPERTY_NAME,"Storm2, Ranger2");

    PrerequisiteCheckConfig prerequisiteCheckConfig = Mockito.mock(PrerequisiteCheckConfig.class);
    Mockito.when(prerequisiteCheckConfig.getCheckProperties(
        m_check.getClass().getName())).thenReturn(checkProperties);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);

    m_check.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }
}
