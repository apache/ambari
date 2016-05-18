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

package org.apache.ambari.server.view;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.RemoteAmbariClusterDAO;
import org.apache.ambari.server.orm.entities.RemoteAmbariClusterEntity;
import org.apache.ambari.server.orm.entities.RemoteAmbariClusterServiceEntity;
import org.apache.ambari.view.AmbariHttpException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for Remote Ambari Cluster
 */
@Singleton
public class RemoteAmbariClusterRegistry {

  private ConcurrentHashMap<String,RemoteAmbariCluster> clusterMap = new ConcurrentHashMap<String,RemoteAmbariCluster>();

  @Inject
  private RemoteAmbariClusterDAO remoteAmbariClusterDAO;

  @Inject
  private Configuration configuration;

  public RemoteAmbariCluster get(String clusterName){
    RemoteAmbariCluster remoteAmbariCluster = clusterMap.get(clusterName);
    if(remoteAmbariCluster == null){
      RemoteAmbariCluster cluster = getCluster(clusterName);
      RemoteAmbariCluster oldCluster = clusterMap.putIfAbsent(clusterName, cluster);
      if(oldCluster == null) remoteAmbariCluster = cluster;
      else remoteAmbariCluster = oldCluster;
    }
    return remoteAmbariCluster;
  }


  private RemoteAmbariCluster getCluster(String clusterName) {
    RemoteAmbariClusterEntity remoteAmbariClusterEntity = remoteAmbariClusterDAO.findByName(clusterName);
    RemoteAmbariCluster remoteAmbariCluster = new RemoteAmbariCluster(remoteAmbariClusterEntity, configuration);
    return remoteAmbariCluster;
  }

  /**
   * Update the remote cluster properties
   *
   * @param entity
   */
  public void update(RemoteAmbariClusterEntity entity){
    remoteAmbariClusterDAO.update(entity);
    clusterMap.remove(entity.getName());
  }

  /**
   * Remove the cluster entity from registry and database
   *
   * @param entity
   */
  public void delete(RemoteAmbariClusterEntity entity) {
    remoteAmbariClusterDAO.delete(entity);
    clusterMap.remove(entity.getName());
  }

  /**
   * Save Remote Cluster Entity after setting services.
   *
   * @param entity
   * @param update
   * @throws IOException
   * @throws AmbariHttpException
   */
  public void saveOrUpdate(RemoteAmbariClusterEntity entity, boolean update) throws IOException, AmbariHttpException {
    RemoteAmbariCluster cluster = new RemoteAmbariCluster(entity,configuration);
    Set<String> services = cluster.getServices();
    Collection<RemoteAmbariClusterServiceEntity> serviceEntities = new ArrayList<RemoteAmbariClusterServiceEntity>();

    for (String service : services) {
      RemoteAmbariClusterServiceEntity serviceEntity = new RemoteAmbariClusterServiceEntity();
      serviceEntity.setServiceName(service);
      serviceEntity.setCluster(entity);
      serviceEntities.add(serviceEntity);
    }

    entity.setServices(serviceEntities);

    if(update){
      update(entity);
    }else{
      remoteAmbariClusterDAO.save(entity);
    }
  }

}
