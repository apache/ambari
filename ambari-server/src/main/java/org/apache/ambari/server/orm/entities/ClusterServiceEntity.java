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

package org.apache.ambari.server.orm.entities;

import javax.persistence.*;
import java.util.Collection;

@javax.persistence.IdClass(ClusterServiceEntityPK.class)
@javax.persistence.Table(name = "clusterservices", schema = "ambari", catalog = "")
@NamedQueries({
        @NamedQuery(name = "clusterServiceByClusterAndServiceNames", query =
                "SELECT clusterService " +
                        "FROM ClusterServiceEntity clusterService " +
                        "JOIN clusterService.clusterEntity cluster " +
                        "WHERE clusterService.serviceName=:serviceName AND cluster.clusterName=:clusterName")
})
@Entity
public class ClusterServiceEntity {
  private Long clusterId;

  @javax.persistence.Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  @Id
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  private String serviceName;

  @javax.persistence.Column(name = "service_name", nullable = false, insertable = true, updatable = true)
  @Id
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  private Integer serviceEnabled = 0;

  @javax.persistence.Column(name = "service_enabled", nullable = false, insertable = true, updatable = true, length = 10)
  @Basic
  public int getServiceEnabled() {
    return serviceEnabled;
  }

  public void setServiceEnabled(int serviceEnabled) {
    this.serviceEnabled = serviceEnabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClusterServiceEntity that = (ClusterServiceEntity) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (serviceEnabled != null ? !serviceEnabled.equals(that.serviceEnabled) : that.serviceEnabled != null)
      return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId !=null ? clusterId.intValue() : 0;
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + serviceEnabled;
    return result;
  }

  private ClusterEntity clusterEntity;

  @ManyToOne
  @javax.persistence.JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false)
  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  private ServiceDesiredStateEntity serviceDesiredStateEntity;

  @OneToOne(mappedBy = "clusterServiceEntity")
  public ServiceDesiredStateEntity getServiceDesiredStateEntity() {
    return serviceDesiredStateEntity;
  }

  public void setServiceDesiredStateEntity(ServiceDesiredStateEntity serviceDesiredStateEntity) {
    this.serviceDesiredStateEntity = serviceDesiredStateEntity;
  }

  private Collection<ServiceComponentDesiredStateEntity> serviceComponentDesiredStateEntities;

  @OneToMany(mappedBy = "clusterServiceEntity")
  public Collection<ServiceComponentDesiredStateEntity> getServiceComponentDesiredStateEntities() {
    return serviceComponentDesiredStateEntities;
  }

  public void setServiceComponentDesiredStateEntities(Collection<ServiceComponentDesiredStateEntity> serviceComponentDesiredStateEntities) {
    this.serviceComponentDesiredStateEntities = serviceComponentDesiredStateEntities;
  }

  private Collection<ServiceConfigMappingEntity> serviceConfigMappings;
  @OneToMany(mappedBy = "serviceEntity", cascade = CascadeType.ALL)
  public Collection<ServiceConfigMappingEntity> getServiceConfigMappings() {
    return serviceConfigMappings;
  }

  public void setServiceConfigMappings(Collection<ServiceConfigMappingEntity> entities) {
    serviceConfigMappings = entities;
  }



}
