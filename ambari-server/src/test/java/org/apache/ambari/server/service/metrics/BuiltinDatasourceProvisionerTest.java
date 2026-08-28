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
package org.apache.ambari.server.service.metrics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.ambari.server.orm.dao.DatasourceDAO;
import org.apache.ambari.server.orm.entities.DatasourceEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.inject.Provider;

public class BuiltinDatasourceProvisionerTest {
  @Test
  @SuppressWarnings("unchecked")
  public void testCreatesManagedDatasourceThroughVmauth() throws Exception {
    DatasourceDAO datasourceDAO = mock(DatasourceDAO.class);
    Provider<Clusters> clustersProvider = mock(Provider.class);
    Clusters clusters = mock(Clusters.class);
    Cluster cluster = mock(Cluster.class);
    Service service = mock(Service.class);
    ServiceComponent vmauth = mock(ServiceComponent.class);
    Config authConfig = mock(Config.class);
    when(clustersProvider.get()).thenReturn(clusters);
    when(clusters.getCluster("west")).thenReturn(cluster);
    when(cluster.getServices()).thenReturn(Map.of("VICTORIAMETRICS", service));
    when(cluster.getDesiredConfigByType("victoriametrics-auth")).thenReturn(authConfig);
    when(authConfig.getProperties()).thenReturn(Map.of(
        "require_authentication", "false", "vmauth_http_port", "18427"));
    when(service.getServiceComponents()).thenReturn(Map.of("VMAUTH", vmauth));
    when(vmauth.getServiceComponentHosts()).thenReturn(
        Map.of("metrics-west.example.test", mock(ServiceComponentHost.class)));
    BuiltinDatasourceProvisioner provisioner =
        new BuiltinDatasourceProvisioner(datasourceDAO, clustersProvider);

    provisioner.provision("west");

    ArgumentCaptor<DatasourceEntity> captor = ArgumentCaptor.forClass(DatasourceEntity.class);
    verify(datasourceDAO).create(captor.capture());
    DatasourceEntity datasource = captor.getValue();
    Assert.assertEquals("west", datasource.getClusterName());
    Assert.assertEquals("{\"url\":\"http://metrics-west.example.test:18427\"}", datasource.getHttp());
    Assert.assertTrue(datasource.getDefaultDatasource());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testDoesNotCopyVmauthPasswordIntoDatasource() throws Exception {
    DatasourceDAO datasourceDAO = mock(DatasourceDAO.class);
    Provider<Clusters> clustersProvider = mock(Provider.class);
    Clusters clusters = mock(Clusters.class);
    Cluster cluster = mock(Cluster.class);
    Service service = mock(Service.class);
    Config authConfig = mock(Config.class);
    when(clustersProvider.get()).thenReturn(clusters);
    when(clusters.getCluster("west")).thenReturn(cluster);
    when(cluster.getServices()).thenReturn(Map.of("VICTORIAMETRICS", service));
    when(cluster.getDesiredConfigByType("victoriametrics-auth")).thenReturn(authConfig);
    when(authConfig.getProperties()).thenReturn(Map.of(
        "require_authentication", "true", "api_password", "must-not-be-copied"));
    BuiltinDatasourceProvisioner provisioner =
        new BuiltinDatasourceProvisioner(datasourceDAO, clustersProvider);

    provisioner.provision("west");

    verify(datasourceDAO, never()).create(org.mockito.ArgumentMatchers.any(DatasourceEntity.class));
  }
}
