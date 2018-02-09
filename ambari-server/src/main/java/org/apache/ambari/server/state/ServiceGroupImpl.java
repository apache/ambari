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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.ServiceGroupKey;
import org.apache.ambari.server.controller.ServiceGroupDependencyResponse;
import org.apache.ambari.server.controller.ServiceGroupResponse;
import org.apache.ambari.server.events.ServiceGroupInstalledEvent;
import org.apache.ambari.server.events.ServiceGroupRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ServiceGroupDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupDependencyEntity;
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
  private final Clusters clusters;

  private Long serviceGroupId;
  private String serviceGroupName;
  private String stackId;
  private Set<ServiceGroupKey> serviceGroupDependencies;

  @AssistedInject
  public ServiceGroupImpl(@Assisted Cluster cluster,
                          @Assisted("serviceGroupName") String serviceGroupName,
                          @Assisted("stackId") String stackId,
                          @Assisted("serviceGroupDependencies") Set<ServiceGroupKey> serviceGroupDependencies,
                          ClusterDAO clusterDAO,
                          ServiceGroupDAO serviceGroupDAO,
                          AmbariEventPublisher eventPublisher,
                          Clusters clusters) throws AmbariException {

    this.cluster = cluster;
    this.clusters = clusters;
    this.clusterDAO = clusterDAO;
    this.serviceGroupDAO = serviceGroupDAO;
    this.eventPublisher = eventPublisher;

    this.serviceGroupName = serviceGroupName;
    this.stackId = stackId;

    ServiceGroupEntity serviceGroupEntity = new ServiceGroupEntity();
    serviceGroupEntity.setClusterId(cluster.getClusterId());
    serviceGroupEntity.setServiceGroupId(serviceGroupId);
    serviceGroupEntity.setServiceGroupName(serviceGroupName);
    serviceGroupEntity.setStackId(stackId);

    if (serviceGroupDependencies == null) {
      this.serviceGroupDependencies = new HashSet<>();
    } else {
      this.serviceGroupDependencies = serviceGroupDependencies;
    }

    this.serviceGroupEntityPK = getServiceGroupEntityPK(serviceGroupEntity);

    persist(serviceGroupEntity);
  }

  @AssistedInject
  public ServiceGroupImpl(@Assisted Cluster cluster,
                          @Assisted ServiceGroupEntity serviceGroupEntity,
                          ClusterDAO clusterDAO,
                          ServiceGroupDAO serviceGroupDAO,
                          AmbariEventPublisher eventPublisher,
                          Clusters clusters) throws AmbariException {
    this.cluster = cluster;
    this.clusters = clusters;
    this.clusterDAO = clusterDAO;
    this.serviceGroupDAO = serviceGroupDAO;
    this.eventPublisher = eventPublisher;

    this.serviceGroupId = serviceGroupEntity.getServiceGroupId();
    this.serviceGroupName = serviceGroupEntity.getServiceGroupName();
    this.stackId = serviceGroupEntity.getStackId();
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
  public String getStackId() {
    ServiceGroupEntity entity = getServiceGroupEntity();
    return entity.getStackId();
  }

  @Override
  public void setStackId(String stackId) {
    ServiceGroupEntity entity = getServiceGroupEntity();
    entity.setStackId(stackId);
    serviceGroupDAO.merge(entity);
    this.stackId = stackId;
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
      cluster.getClusterName(), getServiceGroupId(), getServiceGroupName(), getStackId());
    return r;
  }

  @Override
  public Set<ServiceGroupDependencyResponse> getServiceGroupDependencyResponses() {
    Set<ServiceGroupDependencyResponse> responses = new HashSet<>();
    if (getServiceGroupDependencies() != null) {
      for (ServiceGroupKey sgk : getServiceGroupDependencies()) {
        responses.add(new ServiceGroupDependencyResponse(cluster.getClusterId(), cluster.getClusterName(),
                serviceGroupId, serviceGroupName, sgk.getClusterId(), sgk.getClusterName(), sgk.getServiceGroupId(), sgk.getServiceGroupName(), sgk.getDependencyId()));
      }
    }
    return responses;
  }

  public Set<ServiceGroupKey> getServiceGroupDependencies(List<ServiceGroupDependencyEntity> serviceGroupDependencies) {
    Set<ServiceGroupKey> serviceGroupDependenciesList = new HashSet<>();
    if (serviceGroupDependencies != null) {
      for (ServiceGroupDependencyEntity sgde : serviceGroupDependencies) {
        ServiceGroupKey serviceGroupKey = new ServiceGroupKey();
        ServiceGroupEntity dependencyServiceGroupEntity = sgde.getServiceGroupDependency();
        String clusterName = "";
        Long clusterId = null;
        if (dependencyServiceGroupEntity.getClusterId() == cluster.getClusterId()) {
          clusterName = cluster.getClusterName();
          clusterId = cluster.getClusterId();
        } else {
          ClusterEntity clusterEntity = clusterDAO.findById(dependencyServiceGroupEntity.getClusterId());
          if (clusterEntity != null) {
            clusterName = clusterEntity.getClusterName();
            clusterId = clusterEntity.getClusterId();
          } else {
            LOG.error("Unable to get cluster id for service group " + dependencyServiceGroupEntity.getServiceGroupName());
          }
        }

        ServiceGroupEntityPK serviceGroupEntityPK = new ServiceGroupEntityPK();
        serviceGroupEntityPK.setClusterId(dependencyServiceGroupEntity.getClusterId());
        serviceGroupEntityPK.setServiceGroupId(dependencyServiceGroupEntity.getServiceGroupId());
        ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(serviceGroupEntityPK);
        String serviceGroupDependencyName = "";
        Long serviceGroupDependencId = null;
        if (serviceGroupEntity != null) {
          serviceGroupDependencyName = serviceGroupEntity.getServiceGroupName();
          serviceGroupDependencId = serviceGroupEntity.getServiceGroupId();
        } else {
          LOG.error("Unable to get service group entity for service group " + dependencyServiceGroupEntity.getServiceGroupName());
        }

        serviceGroupKey.setServiceGroupName(serviceGroupDependencyName);
        serviceGroupKey.setServiceGroupId(serviceGroupDependencId);
        serviceGroupKey.setClusterName(clusterName);
        serviceGroupKey.setClusterId(clusterId);
        serviceGroupKey.setDependencyId(sgde.getServiceGroupDependencyId());
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
    sb.append("ServiceGroup={ serviceGroupName=").append(getServiceGroupName()).append(", clusterName=").append(cluster.getClusterName()).append(", clusterId=").append(cluster.getClusterId()).append(", stackVersion=").append(getStackId());
    sb.append("}");
  }

  /**
   * This method uses Java locks and then delegates to internal methods which
   * perform the JPA merges inside of a transaction. Because of this, a
   * transaction is not necessary before this calling this method.
   */
  private ServiceGroupEntity persist(ServiceGroupEntity serviceGroupEntity) {
    ServiceGroupEntity createdServiceGroupEntity = persistEntities(serviceGroupEntity);
    refresh();

    cluster.addServiceGroup(this);

    // publish the service group installed event
    ServiceGroupInstalledEvent event = new ServiceGroupInstalledEvent(
      getClusterId(), getServiceGroupName());
    eventPublisher.publish(event);
    return createdServiceGroupEntity;
  }

  @Transactional
  protected ServiceGroupEntity persistEntities(ServiceGroupEntity serviceGroupEntity) {
    long clusterId = cluster.getClusterId();

    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);
    serviceGroupEntity.setClusterEntity(clusterEntity);
    serviceGroupDAO.create(serviceGroupEntity);
    serviceGroupId = serviceGroupEntity.getServiceGroupId();
    clusterEntity.getServiceGroupEntities().add(serviceGroupEntity);
    clusterDAO.merge(clusterEntity);
    return serviceGroupDAO.merge(serviceGroupEntity);
  }

  @Override
  @Transactional
  public void refresh() {
    serviceGroupEntityPK.setClusterId(getClusterId());
    serviceGroupEntityPK.setServiceGroupId(getServiceGroupId());
    ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(serviceGroupEntityPK);
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
  public ServiceGroupEntity addServiceGroupDependency(Long dependencyServiceGroupId) throws AmbariException {
    ServiceGroupEntityPK serviceGroupEntityPK = new ServiceGroupEntityPK();
    serviceGroupEntityPK.setClusterId(getClusterId());
    serviceGroupEntityPK.setServiceGroupId(getServiceGroupId());
    ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(serviceGroupEntityPK);

    ServiceGroupEntityPK dependencyServiceGroupEntityPK = new ServiceGroupEntityPK();
    dependencyServiceGroupEntityPK.setClusterId(getServiceGroupClusterId(dependencyServiceGroupId));
    dependencyServiceGroupEntityPK.setServiceGroupId(dependencyServiceGroupId);
    ServiceGroupEntity dependencyServiceGroupEntity = serviceGroupDAO.findByPK(dependencyServiceGroupEntityPK);


    ServiceGroupDependencyEntity newDependency = new ServiceGroupDependencyEntity();
    newDependency.setServiceGroup(serviceGroupEntity);
    newDependency.setServiceGroupDependency(dependencyServiceGroupEntity);
    createServiceGroupDependency(newDependency);


    serviceGroupEntity.getServiceGroupDependencies().add(newDependency);
    serviceGroupEntity = serviceGroupDAO.merge(serviceGroupEntity);

    return serviceGroupEntity;
  }

  @Transactional
  public void createServiceGroupDependency(ServiceGroupDependencyEntity serviceGroupDependencyEntity) {
    serviceGroupDAO.createServiceGroupDependency(serviceGroupDependencyEntity);
  }


  private Long getServiceGroupClusterId(Long serviceGroupId) throws AmbariException {
    for (Cluster cl : clusters.getClusters().values()) {
      if (cl.getServiceGroupsById().containsKey(serviceGroupId)) {
        return cl.getClusterId();
      }
    }
    throw new AmbariException("Service group with id=" + serviceGroupId + " is not available.");
  }

  @Override
  public ServiceGroupEntity deleteServiceGroupDependency(Long dependencyServiceGroupId) throws AmbariException {
    ServiceGroupEntityPK pk = new ServiceGroupEntityPK();
    pk.setClusterId(getClusterId());
    pk.setServiceGroupId(getServiceGroupId());
    ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(pk);
    ServiceGroupDependencyEntity dependencyToRemove = null;
    for (ServiceGroupDependencyEntity dependency : serviceGroupEntity.getServiceGroupDependencies()) {
      if (dependency.getServiceGroupDependency().getServiceGroupId() == dependencyServiceGroupId) {
        dependencyToRemove = dependency;
        break;
      }
    }

    return removeServcieGroupDependencyEntity(serviceGroupEntity, dependencyToRemove);
  }

  @Transactional
  protected ServiceGroupEntity removeServcieGroupDependencyEntity(ServiceGroupEntity serviceGroupEntity,
                                                                  ServiceGroupDependencyEntity dependencyToRemove) {
    serviceGroupEntity.getServiceGroupDependencies().remove(dependencyToRemove);
    serviceGroupDAO.removeServiceGroupDependency(dependencyToRemove);
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
