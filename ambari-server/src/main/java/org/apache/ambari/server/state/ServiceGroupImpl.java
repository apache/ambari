/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.ServiceGroupKey;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ServiceGroupDependencyResponse;
import org.apache.ambari.server.controller.ServiceGroupResponse;
import org.apache.ambari.server.events.ServiceGroupInstalledEvent;
import org.apache.ambari.server.events.ServiceGroupRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ServiceGroupDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupEntityPK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;


public class ServiceGroupImpl implements ServiceGroup {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceGroupImpl.class);

  private ServiceGroupEntityPK serviceGroupEntityPK;

  private final Cluster cluster;

  private final ClusterDAO clusterDAO;
  private final ServiceGroupDAO serviceGroupDAO;
  private final AmbariEventPublisher eventPublisher;

  private Long serviceGroupId;
  private String serviceGroupName;
  private Set<ServiceGroupKey> serviceGroupDependencies = new HashSet<>();

  @AssistedInject
  public ServiceGroupImpl(@Assisted Cluster cluster,
                          @Assisted("serviceGroupName") String serviceGroupName,
                          @Assisted("serviceGroupDependencies") Set<ServiceGroupKey> serviceGroupDependencies,
                          ClusterDAO clusterDAO,
                          ServiceGroupDAO serviceGroupDAO,
                          AmbariEventPublisher eventPublisher,
                          AmbariManagementController controller) throws AmbariException {

    this.cluster = cluster;
    this.clusterDAO = clusterDAO;
    this.serviceGroupDAO = serviceGroupDAO;
    this.eventPublisher = eventPublisher;

    this.serviceGroupName = serviceGroupName;

    ServiceGroupEntity serviceGroupEntity = new ServiceGroupEntity();
    serviceGroupEntity.setClusterId(cluster.getClusterId());
    serviceGroupEntity.setServiceGroupId(serviceGroupId);
    serviceGroupEntity.setServiceGroupName(serviceGroupName);

    List<ServiceGroupEntity> serviceGroupEntities = new ArrayList<>();
    if (serviceGroupDependencies != null) {
      this.serviceGroupDependencies = serviceGroupDependencies;
      for (ServiceGroupKey serviceGroupKey : serviceGroupDependencies) {
        Clusters clusters = controller.getClusters();
        Cluster dependencyCluster = serviceGroupKey.getClusterName() == null ? cluster :
                clusters.getCluster(serviceGroupKey.getClusterName());
        serviceGroupKey.setClusterName(dependencyCluster.getClusterName());
        ServiceGroup dependencyServiceGroup = dependencyCluster.getServiceGroup(serviceGroupKey.getServiceGroupName());

        ServiceGroupEntityPK serviceGroupEntityPK = new ServiceGroupEntityPK();
        serviceGroupEntityPK.setServiceGroupId(dependencyServiceGroup.getServiceGroupId());
        serviceGroupEntityPK.setClusterId(dependencyServiceGroup.getClusterId());
        ServiceGroupEntity dependentServiceGroupEntity = serviceGroupDAO.findByPK(serviceGroupEntityPK);
        serviceGroupEntities.add(dependentServiceGroupEntity);
      }
    } else {
      this.serviceGroupDependencies = new HashSet<>();
    }
    serviceGroupEntity.setServiceGroupDependencies(serviceGroupEntities);

    this.serviceGroupEntityPK = getServiceGroupEntityPK(serviceGroupEntity);
    persist(serviceGroupEntity);
  }

  @AssistedInject
  public ServiceGroupImpl(@Assisted Cluster cluster,
                          @Assisted ServiceGroupEntity serviceGroupEntity,
                          ClusterDAO clusterDAO,
                          ServiceGroupDAO serviceGroupDAO,
                          AmbariEventPublisher eventPublisher) throws AmbariException {
    this.cluster = cluster;
    this.clusterDAO = clusterDAO;
    this.serviceGroupDAO = serviceGroupDAO;
    this.eventPublisher = eventPublisher;

    this.serviceGroupId = serviceGroupEntity.getServiceGroupId();
    this.serviceGroupName = serviceGroupEntity.getServiceGroupName();
    this.serviceGroupDependencies = getServiceGroupDependencies(serviceGroupEntity.getServiceGroupDependencies());

    this.serviceGroupEntityPK = getServiceGroupEntityPK(serviceGroupEntity);
  }

  @Override
  public Long getServiceGroupId() {
    return serviceGroupId;
  }

  @Override
  public String getServiceGroupName() {
    return serviceGroupName;
  }

  @Override
  public void setServiceGroupName(String serviceGroupName) {
    ServiceGroupEntity entity = getServiceGroupEntity();
    entity.setServiceGroupName(serviceGroupName);
    serviceGroupDAO.merge(entity);
    this.serviceGroupName = serviceGroupName;
  }

  @Override
  public long getClusterId() {
    return cluster.getClusterId();
  }

  @Override
  public Set<ServiceGroupKey> getServiceGroupDependencies() {
    return serviceGroupDependencies;
  }

  @Override
  public void setServiceGroupDependencies(Set<ServiceGroupKey> serviceGroupDependencies) {
    this.serviceGroupDependencies = serviceGroupDependencies;
  }

  @Override
  public ServiceGroupResponse convertToResponse() {
    ServiceGroupResponse r = new ServiceGroupResponse(cluster.getClusterId(),
      cluster.getClusterName(), getServiceGroupId(), getServiceGroupName());
    return r;
  }

  @Override
  public Set<ServiceGroupDependencyResponse> getServiceGroupDependencyResponses() {
    Set<ServiceGroupDependencyResponse> responses = new HashSet<>();
    if (getServiceGroupDependencies() != null) {
      for (ServiceGroupKey sgk : getServiceGroupDependencies()) {
        responses.add(new ServiceGroupDependencyResponse(cluster.getClusterId(), cluster.getClusterName(),
                serviceGroupId, serviceGroupName, sgk.getClusterId(), sgk.getClusterName(), sgk.getServiceGroupId(), sgk.getServiceGroupName()));
      }
    }
    return responses;
  }

  public Set<ServiceGroupKey> getServiceGroupDependencies(List<ServiceGroupEntity> serviceGroupDependencies) {
    Set<ServiceGroupKey> serviceGroupDependenciesList = new HashSet<>();
    if (serviceGroupDependencies != null) {
      for (ServiceGroupEntity sge : serviceGroupDependencies) {
        ServiceGroupKey serviceGroupKey = new ServiceGroupKey();
        String clusterName = "";
        Long clusterId = null;
        if (sge.getClusterId() == cluster.getClusterId()) {
          clusterName = cluster.getClusterName();
          clusterId = cluster.getClusterId();
        } else {
          ClusterEntity clusterEntity = clusterDAO.findById(sge.getClusterId());
          if (clusterEntity != null) {
            clusterName = clusterEntity.getClusterName();
            clusterId = clusterEntity.getClusterId();
          } else {
            LOG.error("Unable to get cluster id for service group " + sge.getServiceGroupName());
          }
        }

        ServiceGroupEntityPK serviceGroupEntityPK = new ServiceGroupEntityPK();
        serviceGroupEntityPK.setClusterId(sge.getClusterId());
        serviceGroupEntityPK.setServiceGroupId(sge.getServiceGroupId());
        ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(serviceGroupEntityPK);
        String serviceGroupDependencyName = "";
        Long serviceGroupDependencId = null;
        if (serviceGroupEntity != null) {
          serviceGroupDependencyName = serviceGroupEntity.getServiceGroupName();
          serviceGroupDependencId = serviceGroupEntity.getServiceGroupId();
        } else {
          LOG.error("Unable to get service group entity for service group " + sge.getServiceGroupName());
        }

        serviceGroupKey.setServiceGroupName(serviceGroupDependencyName);
        serviceGroupKey.setServiceGroupId(serviceGroupDependencId);
        serviceGroupKey.setClusterName(clusterName);
        serviceGroupKey.setClusterId(clusterId);
        serviceGroupDependenciesList.add(serviceGroupKey);
      }
    }
    return serviceGroupDependenciesList;
  }

  @Override
  public Cluster getCluster() {
    return cluster;
  }

  @Override
  public void debugDump(StringBuilder sb) {
    sb.append("ServiceGroup={ serviceGroupName=" + getServiceGroupName() + ", clusterName="
      + cluster.getClusterName() + ", clusterId=" + cluster.getClusterId() + "}");
  }

  /**
   * This method uses Java locks and then delegates to internal methods which
   * perform the JPA merges inside of a transaction. Because of this, a
   * transaction is not necessary before this calling this method.
   */
  private void persist(ServiceGroupEntity serviceGroupEntity) {
    persistEntities(serviceGroupEntity);
    refresh();

    cluster.addServiceGroup(this);

    // publish the service group installed event
    ServiceGroupInstalledEvent event = new ServiceGroupInstalledEvent(
      getClusterId(), getServiceGroupName());
    eventPublisher.publish(event);
  }

  @Transactional
  protected void persistEntities(ServiceGroupEntity serviceGroupEntity) {
    long clusterId = cluster.getClusterId();

    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);
    serviceGroupEntity.setClusterEntity(clusterEntity);
    serviceGroupDAO.create(serviceGroupEntity);
    serviceGroupId = serviceGroupEntity.getServiceGroupId();
    clusterEntity.getServiceGroupEntities().add(serviceGroupEntity);
    clusterDAO.merge(clusterEntity);
    serviceGroupDAO.merge(serviceGroupEntity);
  }

  @Override
  @Transactional
  public void refresh() {
    ServiceGroupEntityPK pk = new ServiceGroupEntityPK();
    pk.setClusterId(getClusterId());
    pk.setServiceGroupId(getServiceGroupId());
    ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(pk);
    serviceGroupDAO.refresh(serviceGroupEntity);
  }

  @Override
  public boolean canBeRemoved() {
    // TODO: Add check for services in the service group
    return true;
  }


  @Override
  @Transactional
  public void delete() throws AmbariException {
    removeEntities();
    // publish the service removed event
    ServiceGroupRemovedEvent event = new ServiceGroupRemovedEvent(getClusterId(), getServiceGroupName());
    eventPublisher.publish(event);
  }

  @Override
  @Transactional
  public ServiceGroupEntity deleteDependency(String dependencyServiceGroupName) throws AmbariException {
    ServiceGroupEntityPK pk = new ServiceGroupEntityPK();
    pk.setClusterId(getClusterId());
    pk.setServiceGroupId(getServiceGroupId());
    ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(pk);
    ServiceGroupEntity dependencyToRemove = null;
    for (ServiceGroupEntity dependency : serviceGroupEntity.getServiceGroupDependencies()) {
      if (dependency.getServiceGroupName().equals(dependencyServiceGroupName)) {
        dependencyToRemove = dependency;
        break;
      }
    }
    serviceGroupEntity.getServiceGroupDependencies().remove(dependencyToRemove);
    serviceGroupEntity = serviceGroupDAO.merge(serviceGroupEntity);
    return serviceGroupEntity;
  }

  @Transactional
  protected void removeEntities() throws AmbariException {

    ServiceGroupEntityPK pk = new ServiceGroupEntityPK();
    pk.setClusterId(getClusterId());
    pk.setServiceGroupId(getServiceGroupId());
    serviceGroupDAO.removeByPK(pk);
  }

  // Refresh the cached reference on setters
  private ServiceGroupEntity getServiceGroupEntity() {
    return serviceGroupDAO.findByPK(serviceGroupEntityPK);
  }

  private ServiceGroupEntityPK getServiceGroupEntityPK(ServiceGroupEntity serviceGroupEntity) {
    ServiceGroupEntityPK pk = new ServiceGroupEntityPK();
    pk.setClusterId(serviceGroupEntity.getClusterId());
    pk.setServiceGroupId(serviceGroupEntity.getServiceGroupId());
    return pk;
  }
}
