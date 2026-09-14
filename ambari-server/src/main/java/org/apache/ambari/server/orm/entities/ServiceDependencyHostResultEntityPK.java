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

import java.io.Serializable;
import java.util.Objects;

public class ServiceDependencyHostResultEntityPK implements Serializable {
  private String bindingId;
  private Long snapshotVersion;
  private Long operationEpoch;
  private Long hostId;
  private String dependencyType;
  private String checkKind;

  public ServiceDependencyHostResultEntityPK() { }

  public ServiceDependencyHostResultEntityPK(String bindingId, Long snapshotVersion,
      Long operationEpoch, Long hostId, String dependencyType, String checkKind) {
    this.bindingId = bindingId;
    this.snapshotVersion = snapshotVersion;
    this.operationEpoch = operationEpoch;
    this.hostId = hostId;
    this.dependencyType = dependencyType;
    this.checkKind = checkKind;
  }

  public String getBindingId() { return bindingId; }
  public void setBindingId(String value) { bindingId = value; }
  public Long getSnapshotVersion() { return snapshotVersion; }
  public void setSnapshotVersion(Long value) { snapshotVersion = value; }
  public Long getOperationEpoch() { return operationEpoch; }
  public void setOperationEpoch(Long value) { operationEpoch = value; }
  public Long getHostId() { return hostId; }
  public void setHostId(Long value) { hostId = value; }
  public String getDependencyType() { return dependencyType; }
  public void setDependencyType(String value) { dependencyType = value; }
  public String getCheckKind() { return checkKind; }
  public void setCheckKind(String value) { checkKind = value; }

  @Override
  public boolean equals(Object value) {
    if (!(value instanceof ServiceDependencyHostResultEntityPK other)) {
      return false;
    }
    return Objects.equals(bindingId, other.bindingId)
        && Objects.equals(snapshotVersion, other.snapshotVersion)
        && Objects.equals(operationEpoch, other.operationEpoch)
        && Objects.equals(hostId, other.hostId)
        && Objects.equals(dependencyType, other.dependencyType)
        && Objects.equals(checkKind, other.checkKind);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bindingId, snapshotVersion, operationEpoch, hostId, dependencyType, checkKind);
  }
}
