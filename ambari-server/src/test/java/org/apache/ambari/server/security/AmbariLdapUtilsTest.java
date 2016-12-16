/**
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
package org.apache.ambari.server.security;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.ambari.server.security.authorization.AmbariLdapUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.security.ldap.LdapUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LdapUtils.class)
public class AmbariLdapUtilsTest {

  private static final String USER_DN = "uid=myuser,ou=hdp,ou=Users,dc=apache,dc=org";

  @Test
  public void testIsUserPrincipalNameFormat_True() throws Exception {
    // Given
    String testLoginName = "testuser@domain1.d_1.com";

    // When
    boolean isUserPrincipalNameFormat = AmbariLdapUtils.isUserPrincipalNameFormat(testLoginName);

    // Then
    assertTrue(isUserPrincipalNameFormat);
  }

  @Test
  public void testIsUserPrincipalNameFormatMultipleAtSign_True() throws Exception {
    // Given
    String testLoginName = "@testuser@domain1.d_1.com";

    // When
    boolean isUserPrincipalNameFormat = AmbariLdapUtils.isUserPrincipalNameFormat(testLoginName);

    // Then
    assertTrue(isUserPrincipalNameFormat);
  }

  @Test
  public void testIsUserPrincipalNameFormat_False() throws Exception {
    // Given
    String testLoginName = "testuser";

    // When
    boolean isUserPrincipalNameFormat = AmbariLdapUtils.isUserPrincipalNameFormat(testLoginName);

    // Then
    assertFalse(isUserPrincipalNameFormat);
  }

  @Test
  public void testIsUserPrincipalNameFormatWithAtSign_False() throws Exception {
    // Given
    String testLoginName = "@testuser";

    // When
    boolean isUserPrincipalNameFormat = AmbariLdapUtils.isUserPrincipalNameFormat(testLoginName);

    // Then
    assertFalse(isUserPrincipalNameFormat);
  }

  @Test
  public void testIsUserPrincipalNameFormatWithAtSign1_False() throws Exception {
    // Given
    String testLoginName = "testuser@";

    // When
    boolean isUserPrincipalNameFormat = AmbariLdapUtils.isUserPrincipalNameFormat(testLoginName);

    // Then
    assertFalse(isUserPrincipalNameFormat);
  }

  @Test
  public void testIsLdapObjectOutOfScopeFromBaseDn() throws NamingException {
    // GIVEN
    DistinguishedName fullDn = new DistinguishedName(USER_DN);
    Context context = createNiceMock(Context.class);
    DirContextAdapter adapter = createNiceMock(DirContextAdapter.class);

    PowerMock.mockStatic(LdapUtils.class);
    expect(LdapUtils.getFullDn(anyObject(DistinguishedName.class), anyObject(Context.class)))
      .andReturn(fullDn).anyTimes();

    expect(adapter.getDn()).andReturn(fullDn);
    expect(context.getNameInNamespace()).andReturn(USER_DN);

    replay(adapter, context);
    PowerMock.replayAll();

    // WHEN
    boolean isOutOfScopeFromBaseDN = AmbariLdapUtils.isLdapObjectOutOfScopeFromBaseDn(adapter, "dc=apache,dc=org");
    // THEN
    assertFalse(isOutOfScopeFromBaseDN);
  }

  @Test
  public void testIsLdapObjectOutOfScopeFromBaseDn_dnOutOfScope() throws NamingException {
    // GIVEN
    DistinguishedName fullDn = new DistinguishedName(USER_DN);
    Context context = createNiceMock(Context.class);
    DirContextAdapter adapter = createNiceMock(DirContextAdapter.class);

    PowerMock.mockStatic(LdapUtils.class);
    expect(LdapUtils.getFullDn(anyObject(DistinguishedName.class), anyObject(Context.class)))
      .andReturn(fullDn).anyTimes();

    expect(adapter.getDn()).andReturn(fullDn);
    expect(context.getNameInNamespace()).andReturn(USER_DN);

    replay(adapter, context);
    PowerMock.replayAll();

    // WHEN
    boolean isOutOfScopeFromBaseDN = AmbariLdapUtils.isLdapObjectOutOfScopeFromBaseDn(adapter, "dc=apache,dc=org,ou=custom");
    // THEN
    assertTrue(isOutOfScopeFromBaseDN);
  }
}
