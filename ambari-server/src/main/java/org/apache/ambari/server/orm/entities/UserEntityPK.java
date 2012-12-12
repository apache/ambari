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

import javax.persistence.Id;
import java.io.Serializable;

public class UserEntityPK implements Serializable {

  private String userName;

  @javax.persistence.Column(name = "user_name")
  @Id
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  private Boolean ldapUser;

  @javax.persistence.Column(name = "ldap_user")
  @Id
  public Boolean getLdapUser() {
    return ldapUser;
  }

  public void setLdapUser(Boolean ldapUser) {
    this.ldapUser = ldapUser;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserEntityPK that = (UserEntityPK) o;

    if (userName != null ? !userName.equals(that.userName) : that.userName != null) return false;
    if (ldapUser != null ? !ldapUser.equals(that.ldapUser) : that.ldapUser != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = userName != null ? userName.hashCode() : 0;
    result = 31 * result + (ldapUser != null ? ldapUser.hashCode() : 0);
    return result;
  }

}
