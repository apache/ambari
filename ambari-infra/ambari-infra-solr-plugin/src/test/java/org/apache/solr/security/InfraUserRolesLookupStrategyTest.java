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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.solr.security;

import com.google.common.collect.Sets;
import org.apache.hadoop.security.authentication.server.AuthenticationToken;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InfraUserRolesLookupStrategyTest {

  private InfraUserRolesLookupStrategy underTest = new InfraUserRolesLookupStrategy();

  @Test
  public void testLookupRolesForPrincipalName() {
    // GIVEN
    Map<String, Set<String>> usersVsRoles = generateUserRolesMap();
    AuthenticationToken principal = new AuthenticationToken(
      "logsearch", "logsearch/c6401.ambari.apache.org@EXAMPLE.COM", "kerberos");
    // WHEN
    Set<String> result = underTest.getUserRolesFromPrincipal(usersVsRoles, principal);
    // THEN
    assertTrue(result.contains("logsearch_user"));
    assertTrue(result.contains("ranger_user"));
    assertFalse(result.contains("admin"));
  }

  @Test
  public void testLookupRolesForNonKerberosPrincipalName() {
    // GIVEN
    Map<String, Set<String>> usersVsRoles = generateUserRolesMap();
    BasicUserPrincipal principal = new BasicUserPrincipal("infra-solr");
    // WHEN
    Set<String> result = underTest.getUserRolesFromPrincipal(usersVsRoles, principal);
    // THEN
    assertTrue(result.contains("admin"));
    assertTrue(result.contains("logsearch_user"));
  }

  @Test
  public void testLookupRolesWithNonKerberosPrincipalWithoutRoles() {
    // GIVEN
    Map<String, Set<String>> usersVsRoles = generateUserRolesMap();
    BasicUserPrincipal principal = new BasicUserPrincipal("unknownuser");
    // WHEN
    Set<String> result = underTest.getUserRolesFromPrincipal(usersVsRoles, principal);
    // THEN
    assertTrue(result.isEmpty());
  }

  private Map<String, Set<String>> generateUserRolesMap() {
    Map<String, Set<String>> usersVsRoles = new HashMap<>();
    usersVsRoles.put("logsearch@EXAMPLE.COM", Sets.newHashSet("logsearch_user", "ranger_user"));
    usersVsRoles.put("infra-solr@EXAMPLE.COM", Sets.newHashSet("admin"));
    usersVsRoles.put("infra-solr", Sets.newHashSet("admin", "logsearch_user"));
    usersVsRoles.put("unknownuser", new HashSet<String>());
    return usersVsRoles;
  }
}
