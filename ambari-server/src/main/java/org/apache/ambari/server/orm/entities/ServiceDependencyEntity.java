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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "servicedependencies")
@TableGenerator(name = "service_dependency_id_generator",
        table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
        , pkColumnValue = "service_dependency_id_seq"
        , initialValue = 1
)
public class ServiceDependencyEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = true)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "service_dependency_id_generator")
  private Long serviceDependencyId;

  @Column(name = "service_id", nullable = false, insertable = false, updatable = false)
  private long serviceId;

  @Column(name = "service_cluster_id", nullable = false, insertable = false, updatable = false)
  private long serviceClusterId;

  @Column(name = "service_group_id", nullable = false, insertable = false, updatable = false)
  private long serviceGroupId;

  @Column(name = "dependent_service_id", nullable = false, insertable = false, updatable = false)
  private long dependentServiceId;

  @Column(name = "dependent_service_group_id", nullable = false, insertable = false, updatable = false)
  private long dependentServiceGroupId;

  @Column(name = "dependent_service_cluster_id", nullable = false, insertable = false, updatable = false)
  private long dependentServiceClusterId;

  @ManyToOne
  @JoinColumns({
          @JoinColumn(name = "service_id", referencedColumnName = "id", nullable = false),
          @JoinColumn(name = "service_cluster_id", referencedColumnName = "cluster_id", nullable = false),
          @JoinColumn(name = "service_group_id", referencedColumnName = "service_group_id", nullable = false) })
  private ClusterServiceEntity service;

  @ManyToOne
  @JoinColumns({
          @JoinColumn(name = "dependent_service_id", referencedColumnName = "id", nullable = false),
          @JoinColumn(name = "dependent_service_cluster_id", referencedColumnName = "cluster_id", nullable = false),
          @JoinColumn(name = "dependent_service_group_id", referencedColumnName = "service_group_id", nullable = false) })
  private ClusterServiceEntity serviceDependency;

  public Long getServiceDependencyId() {
    return serviceDependencyId;
  }

  public long getDependentServiceGroupId() {
    return dependentServiceGroupId;
  }

  public void setDependentServiceGroupId(long dependentServiceGroupId) {
    this.dependentServiceGroupId = dependentServiceGroupId;
  }

  public long getDependentServiceId() {
    return dependentServiceId;
  }

  public void setDependentServiceId(long dependentServiceId) {
    this.dependentServiceId = dependentServiceId;
  }

  public long getDependentServiceClusterId() {
    return dependentServiceClusterId;
  }

  public void setDependentServiceClusterId(long dependentServiceClusterId) {
    this.dependentServiceClusterId = dependentServiceClusterId;
  }

  public ClusterServiceEntity getService() {
    return service;
  }

  public void setService(ClusterServiceEntity service) {
    this.service = service;
  }

  public ClusterServiceEntity getServiceDependency() {
    return serviceDependency;
  }

  public void setServiceDependency(ClusterServiceEntity serviceDependency) {
    this.serviceDependency = serviceDependency;
  }

  public long getServiceGroupId() {
    return serviceGroupId;
  }

  public void setServiceGroupId(long serviceGroupId) {
    this.serviceGroupId = serviceGroupId;
  }

  public long getServiceId() {
    return serviceId;
  }

  public void setServiceId(long serviceId) {
    this.serviceId = serviceId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ServiceDependencyEntity)) return false;

    ServiceDependencyEntity that = (ServiceDependencyEntity) o;

    if (serviceDependencyId != null ? !serviceDependencyId.equals(that.serviceDependencyId) : that.serviceDependencyId != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    return serviceDependencyId != null ? serviceDependencyId.hashCode() : 0;
  }
}
