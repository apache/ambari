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
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Entity representing a KerberosPrincipal stored on a host.
 */
@Entity
@IdClass(KerberosPrincipalHostEntityPK.class)
@Table(name = "kerberos_principal_host")
@NamedQueries({
    @NamedQuery(name = "KerberosPrincipalHostEntityFindAll",
        query = "SELECT kph FROM KerberosPrincipalHostEntity kph"),
    @NamedQuery(name = "KerberosPrincipalHostEntityFindByPrincipal",
        query = "SELECT kph FROM KerberosPrincipalHostEntity kph WHERE kph.principalName=:principalName"),
    @NamedQuery(name = "KerberosPrincipalHostEntityFindByHost",
        query = "SELECT kph FROM KerberosPrincipalHostEntity kph WHERE kph.hostId=:hostId")
})
public class KerberosPrincipalHostEntity {

  @Id
  @Column(name = "principal_name", insertable = true, updatable = false, nullable = false)
  private String principalName;

  @Id
  @Column(name = "host_id", insertable = true, updatable = false, nullable = false)
  private Long hostId;

  @ManyToOne
  @JoinColumn(name = "principal_name", referencedColumnName = "principal_name", nullable = false, insertable = false, updatable = false)
  private KerberosPrincipalEntity principalEntity;

  @ManyToOne
  @JoinColumn(name = "host_id", referencedColumnName = "host_id", nullable = false, insertable = false, updatable = false)
  private HostEntity hostEntity;

  /**
   * Constucts an empty KerberosPrincipalHostEntity
   */
  public KerberosPrincipalHostEntity() {
  }

  /**
   * Constructs a new KerberosPrincipalHostEntity
   *
   * @param principalName a String indicating this KerberosPrincipalHostEntity's principal name
   * @param hostId a Long indicating the KerberosPrincipalHostEntity's host id
   */
  public KerberosPrincipalHostEntity(String principalName, Long hostId) {
    setPrincipalName(principalName);
    setHostId(hostId);
  }

  /**
   * Gets the principal name for this KerberosPrincipalHostEntity
   *
   * @return a String indicating this KerberosPrincipalHostEntity's principal name
   */
  public String getPrincipalName() {
    return principalName;
  }

  /**
   * Sets the principal name for this KerberosPrincipalHostEntity
   *
   * @param principalName a String indicating this KerberosPrincipalHostEntity's principal name
   */
  public void setPrincipalName(String principalName) {
    this.principalName = principalName;
  }

  /**
   * Gets the host name for this KerberosHostHostEntity
   *
   * @return a String indicating this KerberosHostHostEntity's host name
   */
  public String getHostName() {
    return hostEntity != null ? hostEntity.getHostName() : null;
  }

  /**
   * Gets the host id for this KerberosHostHostEntity
   *
   * @return a Long indicating this KerberosHostHostEntity's host id
   */
  public Long getHostId() {
    return hostId;
  }

  /**
   * Sets the host id for this KerberosHostHostEntity
   *
   * @param hostId a Long indicating this KerberosHostHostEntity's host id
   */
  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }

  /**
   * Gets the related HostEntity
   *
   * @return the related HostEntity
   */
  public HostEntity getHostEntity() {
    return hostEntity;
  }

  /**
   * Sets the related HostEntity
   *
   * @param hostEntity the related HostEntity
   */
  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

  /**
   * Gets the related KerberosPrincipalEntity
   *
   * @return the related KerberosPrincipalEntity
   */
  public KerberosPrincipalEntity getPrincipalEntity() {
    return principalEntity;
  }

  /**
   * Sets the related KerberosPrincipalEntity
   *
   * @param principalEntity the related KerberosPrincipalEntity
   */
  public void setPrincipalEntity(KerberosPrincipalEntity principalEntity) {
    this.principalEntity = principalEntity;
  }
}
