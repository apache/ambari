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

package org.apache.ambari.server.serveraction.kerberos.stageutils;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.kerberos.VariableReplacementHelper;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableSet;

/**
 * Class that represents keytab. Contains principals that mapped to host.
 * Same keytab can have different set of principals on different hosts.
 */
// TODO This class need to replace {@link org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFile}
// TODO and all related structures and become main item that {@link org.apache.ambari.server.serveraction.kerberos.KerberosServerAction}
// TODO operates with instead of identity records.
public class ResolvedKerberosKeytab {

  private String ownerName = null;
  private String ownerAccess = null;
  private String groupName = null;
  private String groupAccess = null;
  private String file = null;
  private Set<Pair<Long, String>> mappedPrincipals = null;
  private boolean isAmbariServerKeytab = false;
  private boolean mustWriteAmbariJaasFile = false;

  public ResolvedKerberosKeytab(
      String file,
      String ownerName,
      String ownerAccess,
      String groupName,
      String groupAccess,
      Set<Pair<Long, String>> mappedPrincipals,
      boolean isAmbariServerKeytab,
      boolean writeAmbariJaasFile
  ) {
    this.ownerName = ownerName;
    this.ownerAccess = ownerAccess;
    this.groupName = groupName;
    this.groupAccess = groupAccess;
    this.file = file;
    this.mappedPrincipals = mappedPrincipals;
    this.isAmbariServerKeytab = isAmbariServerKeytab;
    this.mustWriteAmbariJaasFile = writeAmbariJaasFile;
  }

  /**
   * Gets the path to the keytab file
   * <p/>
   * The value may include variable placeholders to be replaced as needed
   * <ul>
   * <li>
   * ${variable} placeholders are replaced on the server - see
   * {@link VariableReplacementHelper#replaceVariables(String, Map)}
   * </li>
   * </ul>
   *
   * @return a String declaring the keytab file's absolute path
   * @see VariableReplacementHelper#replaceVariables(String, Map)
   */
  public String getFile() {
    return file;
  }

  /**
   * Sets the path to the keytab file
   *
   * @param file a String declaring this keytab's file path
   * @see #getFile()
   */
  public void setFile(String file) {
    this.file = file;
  }

  /**
   * Gets the local username to set as the owner of the keytab file
   *
   * @return a String declaring the name of the user to own the keytab file
   */
  public String getOwnerName() {
    return ownerName;
  }

  /**
   * Sets the local username to set as the owner of the keytab file
   *
   * @param name a String declaring the name of the user to own the keytab file
   */
  public void setOwnerName(String name) {
    this.ownerName = name;
  }

  /**
   * Gets the access permissions that should be set on the keytab file related to the file's owner
   *
   * @return a String declaring the access permissions that should be set on the keytab file related
   * to the file's owner
   * @see #ownerAccess
   */
  public String getOwnerAccess() {
    return ownerAccess;
  }

  /**
   * Sets the access permissions that should be set on the keytab file related to the file's owner
   *
   * @param access a String declaring the access permissions that should be set on the keytab file
   *               related to the file's owner
   * @see #ownerAccess
   */
  public void setOwnerAccess(String access) {
    this.ownerAccess = access;
  }

  /**
   * Gets the local group name to set as the group owner of the keytab file
   *
   * @return a String declaring the name of the group to own the keytab file
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * Sets the local group name to set as the group owner of the keytab file
   *
   * @param name a String declaring the name of the group to own the keytab file
   */
  public void setGroupName(String name) {
    this.groupName = name;
  }

  /**
   * Gets the access permissions that should be set on the keytab file related to the file's group
   *
   * @return a String declaring the access permissions that should be set on the keytab file related
   * to the file's group
   * @see #groupAccess
   */
  public String getGroupAccess() {
    return groupAccess;
  }

  /**
   * Sets the access permissions that should be set on the keytab file related to the file's group
   *
   * @param access a String declaring the access permissions that should be set on the keytab file
   *               related to the file's group
   * @see #groupAccess
   */
  public void setGroupAccess(String access) {
    this.groupAccess = access;
  }

  /**
   * Gets evaluated host-to-principal set associated with given keytab.
   *
   * @return a Set with mappedPrincipals associated with given keytab
   */
  public Set<Pair<Long, String>> getMappedPrincipals() {
    return mappedPrincipals;
  }

  /**
   * Sets evaluated host-to-principal set associated with given keytab.
   *
   * @param mappedPrincipals a Map with host-to-principal mapping associated with given keytab
   */
  public void setMappedPrincipals(Set<Pair<Long, String>> mappedPrincipals) {
    this.mappedPrincipals = mappedPrincipals;
  }

  /**
   * Gets set of hosts associated with given keytab.
   *
   * @return a Set with hosts
   */
  public Set<Long> getHosts() {
    ImmutableSet.Builder<Long> builder = ImmutableSet.builder();
    for (Pair<Long, String> principal : getMappedPrincipals()) {
      if (principal.getLeft() != null) {
        builder.add(principal.getLeft());
      }
    }
    return builder.build();
  }

  /**
   * Gets a set of principals associated with given keytab.
   *
   * @return a Set of principals
   */
  public Set<String> getPrincipals() {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (Pair<Long, String> principal : getMappedPrincipals()) {
      builder.add(principal.getRight());
    }
    return builder.build();
  }

  /**
   * Indicates if given keytab is Ambari Server keytab and can be distributed to host with Ambari Server side action.
   *
   * @return true, if given keytab is Ambari Server keytab.
   */
  public boolean isAmbariServerKeytab() {
    return isAmbariServerKeytab;
  }

  /**
   * Sets flag to indicate if given keytab is Ambari Server keytab and can be distributed to host with Ambari Server
   * side action.
   *
   * @param isAmbariServerKeytab flag value
   */
  public void setAmbariServerKeytab(boolean isAmbariServerKeytab) {
    this.isAmbariServerKeytab = isAmbariServerKeytab;
  }

  /**
   * Indicates if this keytab must be written to Ambari Server jaas file.
   *
   * @return true, if this keytab must be written to Ambari Server jaas file.
   */
  public boolean isMustWriteAmbariJaasFile() {
    return mustWriteAmbariJaasFile;
  }

  /**
   * Sets flag to indicate if this keytab must be written to Ambari Server jaas file.
   *
   * @param mustWriteAmbariJaasFile flag value
   */
  public void setMustWriteAmbariJaasFile(boolean mustWriteAmbariJaasFile) {
    this.mustWriteAmbariJaasFile = mustWriteAmbariJaasFile;
  }
}
