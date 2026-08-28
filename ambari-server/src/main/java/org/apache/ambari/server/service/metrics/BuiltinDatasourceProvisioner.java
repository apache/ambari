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

import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.dao.DatasourceDAO;
import org.apache.ambari.server.orm.entities.DatasourceEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/** Creates the default datasource for an installed, unauthenticated VictoriaMetrics service. */
@Singleton
public class BuiltinDatasourceProvisioner {
  static final String DATASOURCE_NAME = "Ambari VictoriaMetrics";

  private static final Logger LOG = LoggerFactory.getLogger(BuiltinDatasourceProvisioner.class);
  private static final String SERVICE_NAME = "VICTORIAMETRICS";
  private static final String VMAUTH_COMPONENT = "VMAUTH";
  private static final String SERVER_COMPONENT = "VICTORIAMETRICS_SERVER";
  private static final String VMSELECT_COMPONENT = "VMSELECT";

  private final DatasourceDAO datasourceDAO;
  private final Provider<Clusters> clusters;

  @Inject
  public BuiltinDatasourceProvisioner(DatasourceDAO datasourceDAO, Provider<Clusters> clusters) {
    this.datasourceDAO = datasourceDAO;
    this.clusters = clusters;
  }

  public void provision(String clusterName) {
    if (datasourceDAO.findByNameAndCluster(DATASOURCE_NAME, clusterName) != null) {
      return;
    }
    try {
      Cluster cluster = clusters.get().getCluster(clusterName);
      Service service = cluster.getServices().get(SERVICE_NAME);
      if (service == null) {
        return;
      }
      Config authConfig = cluster.getDesiredConfigByType("victoriametrics-auth");
      Map<String, String> auth = authConfig == null ? Map.of() : authConfig.getProperties();
      if (Boolean.parseBoolean(auth.getOrDefault("require_authentication", "false"))) {
        LOG.info("Skipping managed VictoriaMetrics datasource for cluster {} because VMAUTH authentication is enabled",
            clusterName);
        return;
      }

      String url = endpoint(service, cluster, auth);
      if (url == null) {
        return;
      }
      DatasourceEntity entity = new DatasourceEntity();
      entity.setName(DATASOURCE_NAME);
      entity.setDescription("VictoriaMetrics managed by Ambari");
      entity.setCategory("prometheus");
      entity.setPluginType("prometheus");
      entity.setPluginTypeName("Prometheus");
      entity.setClusterName(clusterName);
      entity.setSettings("{\"managed\":true,\"provider\":\"victoriametrics\"}");
      entity.setHttp("{\"url\":\"" + url + "\"}");
      entity.setAuth("{}");
      entity.setStatus(DatasourceEntity.STATUS_ENABLED);
      entity.setDefaultDatasource(datasourceDAO.findDefaultByCluster(clusterName) == null);
      entity.setCreatedBy("system");
      entity.setUpdatedBy("system");
      datasourceDAO.create(entity);
    } catch (AmbariException | RuntimeException e) {
      if (datasourceDAO.findByNameAndCluster(DATASOURCE_NAME, clusterName) == null) {
        LOG.warn("Unable to provision the managed VictoriaMetrics datasource for cluster {}", clusterName, e);
      }
    }
  }

  private String endpoint(Service service, Cluster cluster, Map<String, String> auth) {
    String vmauthHost = firstHost(service, VMAUTH_COMPONENT);
    if (vmauthHost != null) {
      return "http://" + vmauthHost + ":" + port(auth, "vmauth_http_port", 8427);
    }

    Config metricsConfig = cluster.getDesiredConfigByType("victoriametrics");
    Map<String, String> metrics = metricsConfig == null ? Map.of() : metricsConfig.getProperties();
    String deploymentMode = metrics.getOrDefault("deployment_mode", "single");
    if ("single".equalsIgnoreCase(deploymentMode)) {
      String serverHost = firstHost(service, SERVER_COMPONENT);
      return serverHost == null ? null
          : "http://" + serverHost + ":" + port(metrics, "server_http_port", 8428);
    }

    String selectHost = firstHost(service, VMSELECT_COMPONENT);
    String tenant = metrics.getOrDefault("tenant_id", "0");
    return selectHost == null ? null : "http://" + selectHost + ":"
        + port(metrics, "vmselect_http_port", 8481) + "/select/" + tenant + "/prometheus";
  }

  private String firstHost(Service service, String componentName) {
    ServiceComponent component = service.getServiceComponents().get(componentName);
    return component == null || component.getServiceComponentHosts().isEmpty()
        ? null : component.getServiceComponentHosts().keySet().stream().sorted().findFirst().orElse(null);
  }

  private int port(Map<String, String> properties, String name, int fallback) {
    try {
      int value = Integer.parseInt(properties.getOrDefault(name, Integer.toString(fallback)));
      return value > 0 && value <= 65535 ? value : fallback;
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
