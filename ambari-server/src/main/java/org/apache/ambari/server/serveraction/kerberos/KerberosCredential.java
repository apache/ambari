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
package org.apache.ambari.server.serveraction.kerberos;

/**
 * KerberosCredential encapsulates data needed to authenticate an identity to a KDC.
 */
public class KerberosCredential {

  private String principal = null;
  private String password = null;
  private String keytab = null;

  /**
   * Creates an empty KerberosCredential
   */
  public KerberosCredential() {
    principal = null;
    password = null;
    keytab = null;
  }

  /**
   * Creates a new KerberosCredential
   *
   * @param principal a String containing the principal name for this Kerberos credential
   * @param password  a String containing the password for this Kerberos credential
   * @param keytab    a String containing the base64 encoded keytab for this Kerberos credential
   */
  public KerberosCredential(String principal, String password, String keytab) {
    this.principal = principal;
    this.password = password;
    this.keytab = keytab;
  }

  /**
   * @return a String containing the principal name for this Kerberos credential
   */
  public String getPrincipal() {
    return principal;
  }

  /**
   * @param principal a String containing the principal name for this Kerberos credential
   */
  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  /**
   * @return a String containing the password for this Kerberos credential
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param password a String containing the password for this Kerberos credential
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * @return a String containing the base64 encoded keytab for this Kerberos credential
   */
  public String getKeytab() {
    return keytab;
  }

  /**
   * @param keytab a String containing the base64 encoded keytab for this Kerberos credential
   */
  public void setKeytab(String keytab) {
    this.keytab = keytab;
  }
}
