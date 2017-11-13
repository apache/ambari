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
package org.apache.ambari.server.orm.entities;

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
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "servicegroupdependencies")
@NamedQueries({
        @NamedQuery(name = "serviceGroupDependencyByServiceGroupsAndClustersIds", query =
                "SELECT serviceGroupDependency " +
                   "FROM ServiceGroupDependencyEntity serviceGroupDependency " +
                   "WHERE serviceGroupDependency.serviceGroupId=:serviceGroupId AND serviceGroupDependency.serviceGroupClusterId=:serviceGroupClusterId " +
                   "AND serviceGroupDependency.dependentServiceGroupId=:dependentServiceGroupId " +
                   "AND serviceGroupDependency.dependentServiceGroupClusterId=:dependentServiceGroupClusterId")
})
@TableGenerator(name = "service_group_dependency_id_generator",
        table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
        , pkColumnValue = "service_group_dependency_id_seq"
        , initialValue = 1
)
public class ServiceGroupDependencyEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = true)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "service_group_dependency_id_generator")
  private Long serviceGroupDependencyId;

  @Column(name = "service_group_id", nullable = false, insertable = false, updatable = false)
  private long serviceGroupId;

  @Column(name = "service_group_cluster_id", nullable = false, insertable = false, updatable = false)
  private long serviceGroupClusterId;

  @Column(name = "dependent_service_group_id", nullable = false, insertable = false, updatable = false)
  private long dependentServiceGroupId;

  @Column(name = "dependent_service_group_cluster_id", nullable = false, insertable = false, updatable = false)
  private long dependentServiceGroupClusterId;

  @ManyToOne
  @JoinColumns({
          @JoinColumn(name = "service_group_id", referencedColumnName = "id", nullable = false),
          @JoinColumn(name = "service_group_cluster_id", referencedColumnName = "cluster_id", nullable = false) })
  private ServiceGroupEntity serviceGroup;

  @ManyToOne
  @JoinColumns({
          @JoinColumn(name = "dependent_service_group_id", referencedColumnName = "id", nullable = false),
          @JoinColumn(name = "dependent_service_group_cluster_id", referencedColumnName = "cluster_id", nullable = false) })
  private ServiceGroupEntity serviceGroupDependency;

  public ServiceGroupEntity getServiceGroup() {
    return serviceGroup;
  }

  public void setServiceGroup(ServiceGroupEntity serviceGroup) {
    this.serviceGroup = serviceGroup;
  }

  public ServiceGroupEntity getServiceGroupDependency() {
    return serviceGroupDependency;
  }

  public void setServiceGroupDependency(ServiceGroupEntity serviceGroupDependency) {
    this.serviceGroupDependency = serviceGroupDependency;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ServiceGroupDependencyEntity)) return false;

    ServiceGroupDependencyEntity that = (ServiceGroupDependencyEntity) o;

    if (!serviceGroupDependencyId.equals(that.serviceGroupDependencyId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return serviceGroupDependencyId.hashCode();
  }
}
