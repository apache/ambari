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

package org.apache.ambari.server.orm.entities;

import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.TableGenerator;

@javax.persistence.IdClass(ClusterServiceEntityPK.class)
@javax.persistence.Table(name = "clusterservices")
@NamedQueries({
  @NamedQuery(name = "clusterServiceById", query =
    "SELECT clusterService " +
      "FROM ClusterServiceEntity clusterService " +
      "JOIN clusterService.serviceGroupEntity serviceGroup " +
      "WHERE clusterService.serviceId=:serviceId " +
      "AND  serviceGroup.serviceGroupId=:serviceGroupId " +
      "AND serviceGroup.clusterId=:clusterId")
})
@Entity
@TableGenerator(name = "service_id_generator",
  table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
  , pkColumnValue = "service_id_seq"
  , initialValue = 1
)
public class ClusterServiceEntity {

  @Id
  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long clusterId;

  @Id
  @Column(name = "service_group_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long serviceGroupId;

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = true)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "service_id_generator")
  private Long serviceId;

  @Column(name = "service_name", nullable = false, insertable = true, updatable = true)
  private String serviceName;

  @Column(name = "service_type", nullable = false, insertable = true, updatable = true)
  private String serviceType;

  @Basic
  @Column(name = "service_enabled", nullable = false, insertable = true, updatable = true, length = 10)
  private Integer serviceEnabled = 0;


  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false, insertable = false, updatable = false)
  private ClusterEntity clusterEntity;

  @ManyToOne
  @JoinColumns({@JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false),
    @JoinColumn(name = "service_group_id", referencedColumnName = "id", nullable = false)})
  private ServiceGroupEntity serviceGroupEntity;

  @OneToOne(mappedBy = "clusterServiceEntity", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
  private ServiceDesiredStateEntity serviceDesiredStateEntity;

  @OneToMany(mappedBy = "clusterServiceEntity")
  private Collection<ServiceComponentDesiredStateEntity> serviceComponentDesiredStateEntities;

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public Long getServiceGroupId() {
    return serviceGroupId;
  }

  public void setServiceGroupId(Long serviceGroupId) {
    this.serviceGroupId = serviceGroupId;
  }

  public Long getServiceId() {
    return serviceId;
  }

  public void setServiceId(Long serviceId) {
    this.serviceId = serviceId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

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
    if (serviceGroupId != null ? !serviceGroupId.equals(that.serviceGroupId) : that.serviceGroupId != null) return false;
    if (serviceId != null ? !serviceId.equals(that.serviceId) : that.serviceId != null) return false;
    if (serviceEnabled != null ? !serviceEnabled.equals(that.serviceEnabled) : that.serviceEnabled != null)
      return false;
    if (serviceType != null ? !serviceType.equals(that.serviceType) : that.serviceType != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId !=null ? clusterId.intValue() : 0;
    result = 31 * result + (serviceGroupId != null ? serviceGroupId.hashCode() : 0);
    result = 31 * result + (serviceId != null ? serviceId.hashCode() : 0);
    result = 31 * result + (serviceType != null ? serviceType.hashCode() : 0);
    result = 31 * result + serviceEnabled;
    return result;
  }

  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  public ServiceGroupEntity getClusterServiceGroupEntity() {
    return serviceGroupEntity;
  }

  public void setServiceGroupEntity(ServiceGroupEntity serviceGroupEntity) {
    this.serviceGroupEntity = serviceGroupEntity;
  }

  public ServiceDesiredStateEntity getServiceDesiredStateEntity() {
    return serviceDesiredStateEntity;
  }

  public void setServiceDesiredStateEntity(ServiceDesiredStateEntity serviceDesiredStateEntity) {
    this.serviceDesiredStateEntity = serviceDesiredStateEntity;
  }

  public Collection<ServiceComponentDesiredStateEntity> getServiceComponentDesiredStateEntities() {
    return serviceComponentDesiredStateEntities;
  }

  public void setServiceComponentDesiredStateEntities(Collection<ServiceComponentDesiredStateEntity> serviceComponentDesiredStateEntities) {
    this.serviceComponentDesiredStateEntities = serviceComponentDesiredStateEntities;
  }

}