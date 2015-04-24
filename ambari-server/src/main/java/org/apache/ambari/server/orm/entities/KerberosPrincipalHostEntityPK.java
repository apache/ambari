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
import javax.persistence.Id;
import java.io.Serializable;

/**
 * Composite primary key for KerberosPrincipalHostEntity.
 */
public class KerberosPrincipalHostEntityPK implements Serializable{

  @Id
  @Column(name = "principal_name", insertable = false, updatable = false, nullable = false)
  private String principalName = null;

  @Id
  @Column(name = "host_id", insertable = false, updatable = false, nullable = false)
  private Long hostId = null;

  public KerberosPrincipalHostEntityPK() {
  }

  public KerberosPrincipalHostEntityPK(String principalName, Long hostId) {
    setPrincipalName(principalName);
    setHostId(hostId);
  }

  /**
   * Get the name of the associated principal.
   *
   * @return principal name
   */
  public String getPrincipalName() {
    return principalName;
  }

  /**
   * Set the name of the associated principal.
   *
   * @param principalName principal name
   */
  public void setPrincipalName(String principalName) {
    this.principalName = principalName;
  }

  /**
   * Get the host id.
   *
   * @return host id
   */
  public Long getHostId() {
    return hostId;
  }

  /**
   * Set the configuration type.
   *
   * @param hostId host id
   */
  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    KerberosPrincipalHostEntityPK that = (KerberosPrincipalHostEntityPK) o;

    return this.principalName.equals(that.principalName) &&
        this.hostId.equals(that.hostId);
  }

  @Override
  public int hashCode() {
    return 31 * principalName.hashCode() + hostId.hashCode();
  }
}
