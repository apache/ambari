/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.authorization;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Properties;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class AmbariLdapBindAuthenticatorTest extends EasyMockSupport {

  @Test
  public void testAuthenticateWithoutLogin() throws Exception {
    testAuthenticate("username", "username", false);
  }

  @Test
  public void testAuthenticateWithNullLDAPUsername() throws Exception {
    testAuthenticate("username", null, false);
  }

  @Test
  public void testAuthenticateWithLoginAliasDefault() throws Exception {
    testAuthenticate("username", "ldapUsername", false);
  }

  @Test
  public void testAuthenticateWithLoginAliasForceToLower() throws Exception {
    testAuthenticate("username", "ldapUsername", true);
  }

  @Test
  public void testAuthenticateBadPassword() throws Exception {
    String basePathString = "dc=apache,dc=org";
    String ldapUserRelativeDNString = String.format("uid=%s,ou=people,ou=dev", "ldapUsername");
    LdapName ldapUserRelativeDN = new LdapName(ldapUserRelativeDNString);
    String ldapUserDNString = String.format("%s,%s", ldapUserRelativeDNString, basePathString);
    DistinguishedName basePath = new DistinguishedName(basePathString);

    LdapContextSource ldapCtxSource = createMock(LdapContextSource.class);
    expect(ldapCtxSource.getBaseLdapPath())
        .andReturn(basePath)
        .atLeastOnce();
    expect(ldapCtxSource.getContext(ldapUserDNString, "password"))
        .andThrow(new org.springframework.ldap.AuthenticationException(null))
        .once();

    DirContextOperations searchedUserContext = createMock(DirContextOperations.class);
    expect(searchedUserContext.getDn())
        .andReturn(ldapUserRelativeDN)
        .atLeastOnce();

    FilterBasedLdapUserSearch userSearch = createMock(FilterBasedLdapUserSearch.class);
    expect(userSearch.searchForUser(anyString())).andReturn(searchedUserContext).once();

    replayAll();

    Configuration configuration = new Configuration();

    AmbariLdapBindAuthenticator bindAuthenticator = new AmbariLdapBindAuthenticator(ldapCtxSource, configuration);
    bindAuthenticator.setUserSearch(userSearch);

    try {
      bindAuthenticator.authenticate(new UsernamePasswordAuthenticationToken("username", "password"));
      fail("Expected thrown exception: org.springframework.security.authentication.BadCredentialsException");
    } catch (org.springframework.security.authentication.BadCredentialsException e) {
      // expected
    } catch (Throwable t) {
      fail("Expected thrown exception: org.springframework.security.authentication.BadCredentialsException\nEncountered thrown exception " + t.getClass().getName());
    }

    verifyAll();
  }

  private void testAuthenticate(String ambariUsername, String ldapUsername, boolean forceUsernameToLower) throws Exception {
    String basePathString = "dc=apache,dc=org";
    String ldapUserRelativeDNString = String.format("uid=%s,ou=people,ou=dev", ldapUsername);
    LdapName ldapUserRelativeDN = new LdapName(ldapUserRelativeDNString);
    String ldapUserDNString = String.format("%s,%s", ldapUserRelativeDNString, basePathString);
    DistinguishedName basePath = new DistinguishedName(basePathString);

    @SuppressWarnings("unchecked")
    NamingEnumeration<SearchResult> adminGroups = createMock(NamingEnumeration.class);
    expect(adminGroups.hasMore())
        .andReturn(false)
        .atLeastOnce();
    adminGroups.close();
    expectLastCall().atLeastOnce();

    DirContextOperations boundUserContext = createMock(DirContextOperations.class);
    expect(boundUserContext.search(eq("ou=groups"), eq("(&(member=" + ldapUserDNString + ")(objectclass=group)(|(cn=Ambari Administrators)))"), anyObject(SearchControls.class)))
        .andReturn(adminGroups)
        .atLeastOnce();
    boundUserContext.close();
    expectLastCall().atLeastOnce();


    LdapContextSource ldapCtxSource = createMock(LdapContextSource.class);
    expect(ldapCtxSource.getBaseLdapPath())
        .andReturn(basePath)
        .atLeastOnce();
    expect(ldapCtxSource.getContext(ldapUserDNString, "password"))
        .andReturn(boundUserContext)
        .once();
    expect(ldapCtxSource.getReadOnlyContext())
        .andReturn(boundUserContext)
        .once();

    Attribute uidAttribute = createMock(Attribute.class);
    expect(uidAttribute.size())
        .andReturn(1)
        .atLeastOnce();
    expect(uidAttribute.get()).andReturn(ldapUsername).atLeastOnce();

    Attributes searchedAttributes = createMock(Attributes.class);
    expect(searchedAttributes.get("uid"))
        .andReturn(uidAttribute)
        .atLeastOnce();

    DirContextOperations searchedUserContext = createMock(DirContextOperations.class);
    expect(searchedUserContext.getDn())
        .andReturn(ldapUserRelativeDN)
        .atLeastOnce();
    expect(searchedUserContext.getAttributes())
        .andReturn(searchedAttributes)
        .atLeastOnce();

    FilterBasedLdapUserSearch userSearch = createMock(FilterBasedLdapUserSearch.class);
    expect(userSearch.searchForUser(ambariUsername)).andReturn(searchedUserContext).once();

    ServletRequestAttributes servletRequestAttributes = createMock(ServletRequestAttributes.class);

    if (!StringUtils.isEmpty(ldapUsername) && !ambariUsername.equals(ldapUsername)) {
      servletRequestAttributes.setAttribute(eq(ambariUsername), eq(forceUsernameToLower ? ldapUsername.toLowerCase() : ldapUsername), eq(RequestAttributes.SCOPE_SESSION));
      expectLastCall().once();
    }

    replayAll();

    RequestContextHolder.setRequestAttributes(servletRequestAttributes);

    Properties properties = new Properties();
    if (forceUsernameToLower) {
      properties.setProperty(Configuration.LDAP_USERNAME_FORCE_LOWERCASE.getKey(), "true");
    }
    Configuration configuration = new Configuration(properties);

    AmbariLdapBindAuthenticator bindAuthenticator = new AmbariLdapBindAuthenticator(ldapCtxSource, configuration);
    bindAuthenticator.setUserSearch(userSearch);
    DirContextOperations user = bindAuthenticator.authenticate(new UsernamePasswordAuthenticationToken(ambariUsername, "password"));

    verifyAll();

    String ldapUserNameAttribute = configuration.getLdapServerProperties().getUsernameAttribute();
    assertEquals(ldapUsername, user.getStringAttribute(ldapUserNameAttribute));
  }
}
