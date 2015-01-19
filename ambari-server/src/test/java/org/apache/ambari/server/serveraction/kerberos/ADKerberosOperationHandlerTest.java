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

import junit.framework.Assert;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };
    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.close();
  }

  @Test(expected = KerberosLDAPContainerException.class)
  public void testOpenExceptionPrincipalContainerDnNotProvided() throws Exception {
    KerberosOperationHandler handler = new ADKerberosOperationHandler();
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
      }
    };
    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.close();
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testOpenExceptionAdminCredentialsNotProvided() throws Exception {
    KerberosOperationHandler handler = new ADKerberosOperationHandler();
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };
    handler.open(null, DEFAULT_REALM, kerberosEnvMap);
    handler.close();
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testTestAdministratorCredentialsIncorrectAdminPassword() throws Exception {
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, "wrong", null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };

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

    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testTestAdministratorCredentialsIncorrectAdminPrincipal() throws Exception {
    KerberosCredential kc = new KerberosCredential("wrong", DEFAULT_ADMIN_PASSWORD, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };

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

    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test(expected = KerberosKDCConnectionException.class)
  public void testTestAdministratorCredentialsKDCConnectionException() throws Exception {
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, "invalid");
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };

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

    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.testAdministratorCredentials();
    handler.close();
  }


  @Test
  public void testTestAdministratorCredentialsSuccess() throws Exception {
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };

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

    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test
  public void testProcessCreateTemplateDefault() throws Exception {
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };

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

    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);

    Map<String, Object> context = new HashMap<String, Object>();
    context.put("principal", "nn/c6501.ambari.apache.org");
    context.put("principal_primary", "nn");
    context.put("principal_instance", "c6501.ambari.apache.org");
    context.put("realm", "EXAMPLE.COM");
    context.put("realm_lowercase", "example.com");
    context.put("password", "secret");
    context.put("is_service", true);
    context.put("container_dn", "ou=cluster,DC=EXAMPLE,DC=COM");

    Map<String, Object> data;

    data = handler.processCreateTemplate(context);

    Assert.assertNotNull(data);
    Assert.assertEquals(7, data.size());
    Assert.assertEquals(new ArrayList<String>(Arrays.asList("top", "person", "organizationalPerson", "user")), data.get("objectClass"));
    Assert.assertEquals("nn/c6501.ambari.apache.org", data.get("cn"));
    Assert.assertEquals("nn/c6501.ambari.apache.org", data.get("servicePrincipalName"));
    Assert.assertEquals("nn/c6501.ambari.apache.org@example.com", data.get("userPrincipalName"));
    Assert.assertEquals("\"secret\"", data.get("unicodePwd"));
    Assert.assertEquals("0", data.get("accountExpires"));
    Assert.assertEquals("512", data.get("userAccountControl"));


    context.put("is_service", false);
    data = handler.processCreateTemplate(context);

    Assert.assertNotNull(data);
    Assert.assertEquals(6, data.size());
    Assert.assertEquals(new ArrayList<String>(Arrays.asList("top", "person", "organizationalPerson", "user")), data.get("objectClass"));
    Assert.assertEquals("nn/c6501.ambari.apache.org", data.get("cn"));
    Assert.assertEquals("nn/c6501.ambari.apache.org@example.com", data.get("userPrincipalName"));
    Assert.assertEquals("\"secret\"", data.get("unicodePwd"));
    Assert.assertEquals("0", data.get("accountExpires"));
    Assert.assertEquals("512", data.get("userAccountControl"));

    handler.close();
  }

  @Test
  public void testProcessCreateTemplateCustom() throws Exception {
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
        put(ADKerberosOperationHandler.KERBEROS_ENV_CREATE_ATTRIBUTES_TEMPLATE, "{" +
            "  \"objectClass\": [" +
            "    \"top\"," +
            "    \"person\"," +
            "    \"organizationalPerson\"," +
            "    \"user\"" +
            "  ]," +
            "  \"cn\": \"$principal@$realm\"," +
            "  \"dn\": \"$principal@$realm,$container_dn\"," +
            "  \"distinguishedName\": \"$principal@$realm,$container_dn\"," +
            "  \"sAMAccountName\": \"$principal\"," +
            "  #if( $is_service )" +
            "  \"servicePrincipalName\": \"$principal\"," +
            "  #end" +
            "  \"userPrincipalName\": \"$principal@$realm.toLowerCase()\"," +
            "  \"unicodePwd\": \"`$password`\"," +
            "  \"accountExpires\": \"0\"," +
            "  \"userAccountControl\": \"66048\"" +
            "}");
      }
    };

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

    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);


    Map<String, Object> context = new HashMap<String, Object>();
    context.put("principal", "nn/c6501.ambari.apache.org");
    context.put("principal_primary", "nn");
    context.put("principal_instance", "c6501.ambari.apache.org");
    context.put("realm", "EXAMPLE.COM");
    context.put("realm_lowercase", "example.com");
    context.put("password", "secret");
    context.put("is_service", true);
    context.put("container_dn", "ou=cluster,DC=EXAMPLE,DC=COM");

    Map<String, Object> data = handler.processCreateTemplate(context);

    Assert.assertNotNull(data);
    Assert.assertEquals(10, data.size());
    Assert.assertEquals(new ArrayList<String>(Arrays.asList("top", "person", "organizationalPerson", "user")), data.get("objectClass"));
    Assert.assertEquals("nn/c6501.ambari.apache.org@EXAMPLE.COM", data.get("cn"));
    Assert.assertEquals("nn/c6501.ambari.apache.org", data.get("servicePrincipalName"));
    Assert.assertEquals("nn/c6501.ambari.apache.org@example.com", data.get("userPrincipalName"));
    Assert.assertEquals("nn/c6501.ambari.apache.org", data.get("sAMAccountName"));
    Assert.assertEquals("nn/c6501.ambari.apache.org@EXAMPLE.COM,ou=cluster,DC=EXAMPLE,DC=COM", data.get("distinguishedName"));
    Assert.assertEquals("nn/c6501.ambari.apache.org@EXAMPLE.COM,ou=cluster,DC=EXAMPLE,DC=COM", data.get("dn"));
    Assert.assertEquals("`secret`", data.get("unicodePwd"));
    Assert.assertEquals("0", data.get("accountExpires"));
    Assert.assertEquals("66048", data.get("userAccountControl"));

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
    Map<String, String> kerberosEnvMap = new HashMap<String, String>();

    kerberosEnvMap.put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, ldapUrl);
    kerberosEnvMap.put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, containerDN);

    handler.open(credentials, realm, kerberosEnvMap);

    System.out.println("Test Admin Credentials: " + handler.testAdministratorCredentials());
    // does the principal already exist?
    System.out.println("Principal exists: " + handler.principalExists("nn/c1508.ambari.apache.org"));

    //create principal
//    handler.createPrincipal("nn/c1508.ambari.apache.org@" + DEFAULT_REALM, handler.createSecurePassword(), true);

    handler.close();

    kerberosEnvMap.put(ADKerberosOperationHandler.KERBEROS_ENV_CREATE_ATTRIBUTES_TEMPLATE, "{" +
        "\"objectClass\": [\"top\", \"person\", \"organizationalPerson\", \"user\"]," +
        "\"distinguishedName\": \"CN=$principal@$realm,$container_dn\"," +
        "#if( $is_service )" +
        "\"servicePrincipalName\": \"$principal\"," +
        "#end" +
        "\"userPrincipalName\": \"$principal@$realm.toLowerCase()\"," +
        "\"unicodePwd\": \"\\\"$password\\\"\"," +
        "\"accountExpires\": \"0\"," +
        "\"userAccountControl\": \"66048\"" +
        "}");

    handler.open(credentials, realm, kerberosEnvMap);
    handler.createPrincipal("abcdefg/c1509.ambari.apache.org@" + DEFAULT_REALM, handler.createSecurePassword(), true);

    //update the password
    handler.setPrincipalPassword("nn/c1508.ambari.apache.org", handler.createSecurePassword());

    // remove the principal
    // handler.removeServicePrincipal("nn/c1508.ambari.apache.org");

    handler.close();
  }
}