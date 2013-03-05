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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * Entity that represents a host config mapping and override.
 */
@Table(name = "hostconfigmapping", schema = "ambari", catalog = "")
@Entity
@IdClass(HostConfigMappingEntityPK.class)
public class HostConfigMappingEntity {
  private Long clusterId;
  private String hostName;
  private String typeName;
  private String versionTag;
  private String serviceName;
  private Long createTimestamp;
  private int selectedInd = 0;
  
  @Column(name = "cluster_id", insertable = true, updatable = false, nullable = false)
  @Id
  public Long getClusterId() {
    return clusterId;
  }
  
  public void setClusterId(Long id) {
    clusterId = id;
  }
  
  @Column(name = "host_name", insertable = true, updatable = false, nullable = false)
  @Id
  public String getHostName() {
    return hostName;
  }
  
  public void setHostName(String name) {
    hostName = name;
  }
  
  @Column(name = "type_name", insertable = true, updatable = false, nullable = false)
  @Id
  public String getType() {
    return typeName;
  }
  
  public void setType(String type) {
    typeName = type;
  }
  
  @Column(name = "create_timestamp", insertable = true, updatable = false, nullable = false)
  @Id
  public Long getCreateTimestamp() {
    return createTimestamp;
  }
  
  public void setCreateTimestamp(Long timestamp) {
    createTimestamp = timestamp;
  }
  
  
  @Column(name = "version_tag", insertable = true, updatable = false, nullable = false)
  public String getVersion() {
    return versionTag;
  }
  
  public void setVersion(String version) {
    versionTag = version;
  }
 

  @Column(name = "selected", insertable = true, updatable = true, nullable = false)
  public int isSelected() {
    return selectedInd;
  }

  public void setSelected(int selected) {
    selectedInd = selected;
  }  
  
  
  @Column(name = "service_name", insertable = true, updatable = true)
  public String getServiceName() {
    return serviceName;
  }
  
  public void setServiceName(String name) {
    serviceName = name;
  }
  
}
