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

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;

public class ServiceCheckValidityCheckTest {
  private static final String CLUSTER_NAME = "cluster1";
  private static final long CLUSTER_ID = 1L;
  private static final String SERVICE_NAME = "HDFS";
  private static final long CONFIG_CREATE_TIMESTAMP = 1461518722202L;
  private static final String COMMAND_DETAIL = "HDFS service check";
  private static final long SERVICE_CHECK_START_TIME = CONFIG_CREATE_TIMESTAMP - 2000L;
  private static final String SERVICE_COMPONENT_NAME = "service component";
  private ServiceCheckValidityCheck serviceCheckValidityCheck;

  private ServiceConfigDAO serviceConfigDAO;
  private HostRoleCommandDAO hostRoleCommandDAO;
  private Service service;


  @Before
  public void setUp() throws Exception {
    final Clusters clusters = mock(Clusters.class);
    service = mock(Service.class);
    serviceConfigDAO = mock(ServiceConfigDAO.class);
    hostRoleCommandDAO = mock(HostRoleCommandDAO.class);

    serviceCheckValidityCheck = new ServiceCheckValidityCheck();
    serviceCheckValidityCheck.hostRoleCommandDAOProvider = new Provider<HostRoleCommandDAO>() {
      @Override
      public HostRoleCommandDAO get() {
        return hostRoleCommandDAO;
      }
    };
    serviceCheckValidityCheck.serviceConfigDAOProvider = new Provider<ServiceConfigDAO>() {
      @Override
      public ServiceConfigDAO get() {
        return serviceConfigDAO;
      }
    };
    serviceCheckValidityCheck.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return clusters;
      }
    };

    Cluster cluster = mock(Cluster.class);
    when(clusters.getCluster(CLUSTER_NAME)).thenReturn(cluster);
    when(cluster.getClusterId()).thenReturn(CLUSTER_ID);
    when(cluster.getServices()).thenReturn(ImmutableMap.of(SERVICE_NAME, service));

    when(service.getName()).thenReturn(SERVICE_NAME);

  }

  @Test
  public void testFailWhenServiceWithOutdatedServiceCheckExists() throws AmbariException {
    ServiceComponent serviceComponent = mock(ServiceComponent.class);
    when(serviceComponent.isVersionAdvertised()).thenReturn(true);

    when(service.getMaintenanceState()).thenReturn(MaintenanceState.OFF);
    when(service.getServiceComponents()).thenReturn(ImmutableMap.of(SERVICE_COMPONENT_NAME, serviceComponent));

    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();
    serviceConfigEntity.setServiceName(SERVICE_NAME);
    serviceConfigEntity.setCreateTimestamp(CONFIG_CREATE_TIMESTAMP);

    HostRoleCommandEntity hostRoleCommandEntity = new HostRoleCommandEntity();
    hostRoleCommandEntity.setRoleCommand(RoleCommand.SERVICE_CHECK);
    hostRoleCommandEntity.setCommandDetail(COMMAND_DETAIL);
    hostRoleCommandEntity.setStartTime(SERVICE_CHECK_START_TIME);
    hostRoleCommandEntity.setRole(Role.HDFS_SERVICE_CHECK);

    when(serviceConfigDAO.getLastServiceConfig(eq(CLUSTER_ID), eq(SERVICE_NAME))).thenReturn(serviceConfigEntity);
    when(hostRoleCommandDAO.findAll(any(Request.class), any(Predicate.class))).thenReturn(singletonList(hostRoleCommandEntity));

    PrerequisiteCheck check = new PrerequisiteCheck(null, CLUSTER_NAME);
    serviceCheckValidityCheck.perform(check, new PrereqCheckRequest(CLUSTER_NAME));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }


  @Test
  public void testFailWhenServiceWithNoServiceCheckExists() throws AmbariException {
    ServiceComponent serviceComponent = mock(ServiceComponent.class);
    when(serviceComponent.isVersionAdvertised()).thenReturn(true);

    when(service.getMaintenanceState()).thenReturn(MaintenanceState.OFF);
    when(service.getServiceComponents()).thenReturn(ImmutableMap.of(SERVICE_COMPONENT_NAME, serviceComponent));

    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();
    serviceConfigEntity.setServiceName(SERVICE_NAME);
    serviceConfigEntity.setCreateTimestamp(CONFIG_CREATE_TIMESTAMP);

    when(serviceConfigDAO.getLastServiceConfig(eq(CLUSTER_ID), eq(SERVICE_NAME))).thenReturn(serviceConfigEntity);
    when(hostRoleCommandDAO.findAll(any(Request.class), any(Predicate.class))).thenReturn(Collections.<HostRoleCommandEntity>emptyList());

    PrerequisiteCheck check = new PrerequisiteCheck(null, CLUSTER_NAME);
    serviceCheckValidityCheck.perform(check, new PrereqCheckRequest(CLUSTER_NAME));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }

  @Test
  public void testFailWhenServiceWithOutdatedServiceCheckExistsRepeated() throws AmbariException {
    ServiceComponent serviceComponent = mock(ServiceComponent.class);
    when(serviceComponent.isVersionAdvertised()).thenReturn(true);

    when(service.getMaintenanceState()).thenReturn(MaintenanceState.OFF);
    when(service.getServiceComponents()).thenReturn(ImmutableMap.of(SERVICE_COMPONENT_NAME, serviceComponent));

    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();
    serviceConfigEntity.setServiceName(SERVICE_NAME);
    serviceConfigEntity.setCreateTimestamp(CONFIG_CREATE_TIMESTAMP);

    HostRoleCommandEntity hostRoleCommandEntity1 = new HostRoleCommandEntity();
    hostRoleCommandEntity1.setRoleCommand(RoleCommand.SERVICE_CHECK);
    hostRoleCommandEntity1.setCommandDetail(COMMAND_DETAIL);
    hostRoleCommandEntity1.setStartTime(SERVICE_CHECK_START_TIME);
    hostRoleCommandEntity1.setRole(Role.HDFS_SERVICE_CHECK);

    HostRoleCommandEntity hostRoleCommandEntity2 = new HostRoleCommandEntity();
    hostRoleCommandEntity2.setRoleCommand(RoleCommand.SERVICE_CHECK);
    hostRoleCommandEntity2.setCommandDetail(COMMAND_DETAIL);
    hostRoleCommandEntity2.setStartTime(CONFIG_CREATE_TIMESTAMP - 1L);
    hostRoleCommandEntity2.setRole(Role.HDFS_SERVICE_CHECK);

    when(serviceConfigDAO.getLastServiceConfig(eq(CLUSTER_ID), eq(SERVICE_NAME))).thenReturn(serviceConfigEntity);
    when(hostRoleCommandDAO.findAll(any(Request.class), any(Predicate.class))).thenReturn(Arrays.asList(hostRoleCommandEntity1, hostRoleCommandEntity2));

    PrerequisiteCheck check = new PrerequisiteCheck(null, CLUSTER_NAME);
    serviceCheckValidityCheck.perform(check, new PrereqCheckRequest(CLUSTER_NAME));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }
}