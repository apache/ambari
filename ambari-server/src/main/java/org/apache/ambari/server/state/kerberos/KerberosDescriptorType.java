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

package org.apache.ambari.server.state.kerberos;

public enum KerberosDescriptorType {
  SERVICE("service", "services"),
  COMPONENT("component", "components"),
  IDENTITY("identity", "identities"),
  PRINCIPAL("principal", "principals"),
  KEYTAB("keytab", "keytabs"),
  CONFIGURATION("configuration", "configurations"),
  AUTH_TO_LOCAL_PROPERTY("auth_to_local_property", "auth_to_local_properties");

  private final String descriptorName;
  private final String descriptorPluralName;

  private KerberosDescriptorType(String descriptorName, String descriptorPluralName) {
    this.descriptorName = descriptorName;
    this.descriptorPluralName = descriptorPluralName;
  }

  /**
   * Gets the identifying name for this KerberosDescriptorType
   *
   * @return a String declaring the identifying name for this KerberosDescriptorType
   */
  public String getDescriptorName() {
    return descriptorName;
  }

  /**
   * Gets the identifying name for a group of this KerberosDescriptorType
   *
   * @return a String declaring the identifying name for a group of this KerberosDescriptorType
   */
  public String getDescriptorPluralName() {
    return descriptorPluralName;
  }
}
