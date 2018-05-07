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

import java.util.Collections;

import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.HostEntity;

import com.google.inject.Injector;

/**
 * The {@link UpgradeCatalogHelper} contains utility methods to help and of the
 * {@link UpgradeCatalog} tests.
 */
public class UpgradeCatalogHelper {

  /**
   * Create a new service in the specified cluster.
   *
   * @param injector
   * @param clusterEntity
   * @param serviceName
   * @return
   */
  protected ClusterServiceEntity createService(Injector injector,
      ClusterEntity clusterEntity, String serviceName) {
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    ClusterServiceEntity clusterServiceEntity = new ClusterServiceEntity();
    clusterServiceEntity.setClusterId(1L);
    clusterServiceEntity.setClusterEntity(clusterEntity);
    clusterServiceEntity.setServiceName(serviceName);
    clusterServiceDAO.create(clusterServiceEntity);
    return clusterServiceEntity;
  }

  /**
   * Create a host in the specified cluster.
   *
   * @param injector
   * @param clusterEntity
   * @param hostName
   * @return
   */
  protected HostEntity createHost(Injector injector,
      ClusterEntity clusterEntity, String hostName) {
    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName(hostName);
    hostEntity.setClusterEntities(Collections.singletonList(clusterEntity));
    hostDAO.create(hostEntity);
    clusterEntity.getHostEntities().add(hostEntity);
    clusterDAO.merge(clusterEntity);
    return hostEntity;
  }
}
