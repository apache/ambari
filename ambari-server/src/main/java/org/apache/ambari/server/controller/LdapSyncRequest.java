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

package org.apache.ambari.server.controller;

import org.apache.ambari.server.orm.entities.LdapSyncSpecEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Request for LDAP synchronization.
 */
public class LdapSyncRequest {

  /**
   * The principal names for the request.
   */
  private final Set<String> principalNames;

  /**
   * The type of request.
   */
  private final LdapSyncSpecEntity.SyncType type;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an LdapSyncRequest.
   *
   * @param type            the request type
   * @param principalNames  the principal names
   */
  public LdapSyncRequest(LdapSyncSpecEntity.SyncType type, Set<String> principalNames) {
    this.type  = type;
    this.principalNames = principalNames == null ? new HashSet<String>() : principalNames;
  }

  /**
   * Construct an LdapSyncRequest.
   *
   * @param type  the request type
   */
  public LdapSyncRequest(LdapSyncSpecEntity.SyncType type) {
    this.principalNames = new HashSet<String>();
    this.type  = type;
  }


  // ----- LdapSyncRequest ---------------------------------------------------

  /**
   * Add principal names to the request.
   *
   * @param principalNames  the principal names to be added
   */
  public void addPrincipalNames(Set<String> principalNames) {
    this.principalNames.addAll(principalNames);
  }

  /**
   * Get the principal names.
   *
   * @return the principal names
   */
  public Set<String> getPrincipalNames() {
    return principalNames;
  }

  /**
   * Get the request type.
   *
   * @return the request type
   */
  public LdapSyncSpecEntity.SyncType getType() {
    return type;
  }
}
