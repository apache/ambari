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

package org.apache.ambari.server.serveraction.kerberos;

import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.junit.Ignore;
import org.junit.Test;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;

import java.util.Properties;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class ADKerberosOperationHandlerTest extends EasyMockSupport {
  private static final String DEFAULT_ADMIN_PRINCIPAL = "cluser_admin@HDP01.LOCAL";
  private static final String DEFAULT_ADMIN_PASSWORD = "Hadoop12345";
  private static final String DEFAULT_LDAP_URL = "ldaps://10.0.100.4";
  private static final String DEFAULT_PRINCIPAL_CONTAINER_DN = "ou=HDP,DC=HDP01,DC=LOCAL";
  private static final String DEFAULT_REALM = "HDP01.LOCAL";

  @Test(expected = KerberosKDCConnectionException.class)
  public void testOpenExceptionLdapUrlNotProvided() throws Exception {
    KerberosOperationHandler handler = new ADKerberosOperationHandler();
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null);
    handler.open(kc, DEFAULT_REALM, null, DEFAULT_PRINCIPAL_CONTAINER_DN);
    handler.close();
  }

  @Test(expected = KerberosLDAPContainerException.class)
  public void testOpenExceptionPrincipalContainerDnNotProvided() throws Exception {
    KerberosOperationHandler handler = new ADKerberosOperationHandler();
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null);
    handler.open(kc, DEFAULT_REALM, DEFAULT_LDAP_URL, null);
    handler.close();
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testOpenExceptionAdminCredentialsNotProvided() throws Exception {
    KerberosOperationHandler handler = new ADKerberosOperationHandler();
    handler.open(null, DEFAULT_REALM, DEFAULT_LDAP_URL, DEFAULT_PRINCIPAL_CONTAINER_DN);
    handler.close();
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testTestAdministratorCredentialsIncorrectAdminPassword() throws Exception {
    ADKerberosOperationHandler handler = createMockBuilder(ADKerberosOperationHandler.class)
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createInitialLdapContext", Properties.class, Control[].class))
        .createNiceMock();

    expect(handler.createInitialLdapContext(anyObject(Properties.class), anyObject(Control[].class))).andAnswer(new IAnswer<LdapContext>() {
      @Override
      public LdapContext answer() throws Throwable {
        throw new AuthenticationException();
      }
    }).once();

    replayAll();

    handler.open(new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, "wrong", null),
        DEFAULT_REALM, DEFAULT_LDAP_URL, DEFAULT_PRINCIPAL_CONTAINER_DN);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testTestAdministratorCredentialsIncorrectAdminPrincipal() throws Exception {
    ADKerberosOperationHandler handler = createMockBuilder(ADKerberosOperationHandler.class)
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createInitialLdapContext", Properties.class, Control[].class))
        .createNiceMock();

    expect(handler.createInitialLdapContext(anyObject(Properties.class), anyObject(Control[].class))).andAnswer(new IAnswer<LdapContext>() {
      @Override
      public LdapContext answer() throws Throwable {
        throw new AuthenticationException();
      }
    }).once();

    replayAll();

    handler.open(new KerberosCredential("wrong", DEFAULT_ADMIN_PASSWORD, null),
        DEFAULT_REALM, DEFAULT_LDAP_URL, DEFAULT_PRINCIPAL_CONTAINER_DN);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test(expected = KerberosKDCConnectionException.class)
  public void testTestAdministratorCredentialsKDCConnectionException() throws Exception {
    ADKerberosOperationHandler handler = createMockBuilder(ADKerberosOperationHandler.class)
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createInitialLdapContext", Properties.class, Control[].class))
        .createNiceMock();

    expect(handler.createInitialLdapContext(anyObject(Properties.class), anyObject(Control[].class))).andAnswer(new IAnswer<LdapContext>() {
      @Override
      public LdapContext answer() throws Throwable {
        throw new CommunicationException();
      }
    }).once();

    replayAll();

    handler.open(new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null),
        DEFAULT_REALM, "invalid", DEFAULT_PRINCIPAL_CONTAINER_DN);
    handler.testAdministratorCredentials();
    handler.close();
  }


  @Test
  public void testTestAdministratorCredentialsSuccess() throws Exception {
    ADKerberosOperationHandler handler = createMockBuilder(ADKerberosOperationHandler.class)
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createInitialLdapContext", Properties.class, Control[].class))
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createSearchControls"))
        .createNiceMock();

    expect(handler.createInitialLdapContext(anyObject(Properties.class), anyObject(Control[].class)))
        .andAnswer(new IAnswer<LdapContext>() {
          @Override
          public LdapContext answer() throws Throwable {
            LdapContext ldapContext = createNiceMock(LdapContext.class);
            expect(ldapContext.search(anyObject(String.class), anyObject(String.class), anyObject(SearchControls.class)))
                .andAnswer(new IAnswer<NamingEnumeration<SearchResult>>() {
                  @Override
                  public NamingEnumeration<SearchResult> answer() throws Throwable {
                    NamingEnumeration<SearchResult> result = createNiceMock(NamingEnumeration.class);
                    expect(result.hasMore()).andReturn(false).once();
                    replay(result);
                    return result;
                  }
                })
                .once();
            replay(ldapContext);
            return ldapContext;
          }
        })
        .once();
    expect(handler.createSearchControls()).andAnswer(new IAnswer<SearchControls>() {
      @Override
      public SearchControls answer() throws Throwable {
        SearchControls searchControls = createNiceMock(SearchControls.class);
        replay(searchControls);
        return searchControls;
      }
    }).once();

    replayAll();

    handler.open(new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null),
        DEFAULT_REALM, DEFAULT_LDAP_URL, DEFAULT_PRINCIPAL_CONTAINER_DN);
    handler.testAdministratorCredentials();
    handler.close();
  }

  /**
   * Implementation to illustrate the use of operations on this class
   *
   * @throws Throwable
   */
  @Test
  @Ignore
  public void testLive() throws Throwable {

    /* ******************************************************************************************
     * SSL Certificate of AD should have been imported into truststore when that certificate
     * is not issued by trusted authority. This is typical with self signed certificated in
     * development environment.  To use specific trust store, set path to it in
     * javax.net.ssl.trustStore System property.  Example:
     *      System.setProperty(
     *        "javax.net.ssl.trustStore",
     *        "/tmp/workspace/ambari/apache-ambari-rd/cacerts"
     *       );
     * ****************************************************************************************** */

    ADKerberosOperationHandler handler = new ADKerberosOperationHandler();
    String principal = System.getProperty("principal");
    String password = System.getProperty("password");
    String realm = System.getProperty("realm");
    String ldapUrl = System.getProperty("ldap_url");
    String containerDN = System.getProperty("container_dn");

    if (principal == null) {
      principal = DEFAULT_ADMIN_PRINCIPAL;
    }

    if (password == null) {
      password = DEFAULT_ADMIN_PASSWORD;
    }

    if (realm == null) {
      realm = DEFAULT_REALM;
    }

    if (ldapUrl == null) {
      ldapUrl = DEFAULT_LDAP_URL;
    }

    if (containerDN == null) {
      containerDN = DEFAULT_PRINCIPAL_CONTAINER_DN;
    }

    KerberosCredential credentials = new KerberosCredential(principal, password, null);

    handler.open(credentials, realm, ldapUrl, containerDN);

    System.out.println("Test Admin Credentials: " + handler.testAdministratorCredentials());
    // does the principal already exist?
    System.out.println("Principal exists: " + handler.principalExists("nn/c1508.ambari.apache.org"));

    //create principal
    handler.createPrincipal("nn/c1508.ambari.apache.org@" + DEFAULT_REALM.toLowerCase(), handler.createSecurePassword(), true);
    handler.createPrincipal("nn/c1508.ambari.apache.org", handler.createSecurePassword(), true);

    //update the password
    handler.setPrincipalPassword("nn/c1508.ambari.apache.org", handler.createSecurePassword());

    // remove the principal
    // handler.removeServicePrincipal("nn/c1508.ambari.apache.org");

    handler.close();
  }

}
