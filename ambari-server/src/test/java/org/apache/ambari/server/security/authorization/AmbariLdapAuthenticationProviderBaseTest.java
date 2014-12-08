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

package org.apache.ambari.server.security.authorization;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.ldap.LdapServer;
import org.easymock.EasyMockSupport;

public class AmbariLdapAuthenticationProviderBaseTest extends EasyMockSupport {

  public static DirectoryService getService() {
    return AbstractLdapTestUnit.service;
  }


  public static void setService(DirectoryService service) {
    AbstractLdapTestUnit.service = service;
  }


  public static LdapServer getLdapServer() {
    return AbstractLdapTestUnit.ldapServer;
  }


  public static void setLdapServer(LdapServer ldapServer) {
    AbstractLdapTestUnit.ldapServer = ldapServer;
  }


  public static KdcServer getKdcServer() {
    return AbstractLdapTestUnit.kdcServer;
  }


  public static void setKdcServer(KdcServer kdcServer) {
    AbstractLdapTestUnit.kdcServer = kdcServer;
  }

}
