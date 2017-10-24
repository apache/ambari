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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;


@Entity
@Table(name = "topology_configurations")
@TableGenerator(name = "topology_configurations_id_generator", table = "ambari_sequences",
        pkColumnName = "sequence_name", valueColumnName = "sequence_value",
        pkColumnValue = "topology_configurations_id_seq", initialValue = 0)
public class TopologyConfigurationsEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "topology_configurations_id_generator")
  private Long id;

  @OneToOne
  @JoinColumn(name = "request_id", referencedColumnName = "id", nullable = false)
  private TopologyRequestEntity topologyRequestEntity;

  @Column(name = "service_group_name", length = 100, nullable = false)
  private String serviceGroupName;

  @Column(name = "service_name", length = 100, nullable = false)
  private String serviceName;

  @Column(name = "component_name", length = 100, nullable = true)
  private String componentName;

  @Column(name = "host_group_name", length = 100, nullable = true)
  private String hostGroupName;

  @Column(name = "cluster_properties")
  @Basic(fetch = FetchType.LAZY)
  @Lob
  private String configProperties;

  @Column(name = "cluster_attributes")
  @Basic(fetch = FetchType.LAZY)
  @Lob
  private String configAttributes;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public TopologyRequestEntity getTopologyRequestEntity() {
    return topologyRequestEntity;
  }

  public void setTopologyRequestEntity(TopologyRequestEntity topologyRequestEntity) {
    this.topologyRequestEntity = topologyRequestEntity;
  }

  public String getServiceGroupName() {
    return serviceGroupName;
  }

  public void setServiceGroupName(String serviceGroupName) {
    this.serviceGroupName = serviceGroupName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public String getHostGroupName() {
    return hostGroupName;
  }

  public void setHostGroupName(String hostGroupName) {
    this.hostGroupName = hostGroupName;
  }

  public String getConfigProperties() {
    return configProperties;
  }

  public void setConfigProperties(String configProperties) {
    this.configProperties = configProperties;
  }

  public String getConfigAttributes() {
    return configAttributes;
  }

  public void setConfigAttributes(String configAttributes) {
    this.configAttributes = configAttributes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TopologyConfigurationsEntity that = (TopologyConfigurationsEntity) o;
    if (!id.equals(that.id)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
