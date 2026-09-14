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

public class ServiceDependencySnapshotEntityPK implements Serializable {
  private String bindingId;
  private Long snapshotVersion;

  public ServiceDependencySnapshotEntityPK() { }

  public ServiceDependencySnapshotEntityPK(String bindingId, Long snapshotVersion) {
    this.bindingId = bindingId;
    this.snapshotVersion = snapshotVersion;
  }

  public String getBindingId() {
    return bindingId;
  }

  public void setBindingId(String bindingId) {
    this.bindingId = bindingId;
  }

  public Long getSnapshotVersion() {
    return snapshotVersion;
  }

  public void setSnapshotVersion(Long snapshotVersion) {
    this.snapshotVersion = snapshotVersion;
  }

  @Override
  public boolean equals(Object value) {
    if (!(value instanceof ServiceDependencySnapshotEntityPK other)) {
      return false;
    }
    return Objects.equals(bindingId, other.bindingId)
        && Objects.equals(snapshotVersion, other.snapshotVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bindingId, snapshotVersion);
  }
}
