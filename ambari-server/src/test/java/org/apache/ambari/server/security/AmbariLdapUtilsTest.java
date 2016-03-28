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

import org.apache.ambari.server.security.authorization.AmbariLdapUtils;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class AmbariLdapUtilsTest {

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
}
