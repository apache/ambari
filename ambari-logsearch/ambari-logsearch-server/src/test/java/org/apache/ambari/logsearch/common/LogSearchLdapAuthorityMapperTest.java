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
package org.apache.ambari.logsearch.common;

import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class LogSearchLdapAuthorityMapperTest {

  @Test
  public void testSimpleMapping() {
    // GIVE
    Map<String, String> roleGroupMapping = new HashMap<>();
    roleGroupMapping.put("apache1", "ROLE_USER");
    LogSearchLdapAuthorityMapper underTest = new LogSearchLdapAuthorityMapper(roleGroupMapping);
    // WHEN
    List<GrantedAuthority> result = new ArrayList<>(underTest.mapAuthorities(generateAuthorities()));
    // THEN
    assertEquals("ROLE_USER", result.get(0).toString());
  }

  @Test
  public void testSimpleMappingWithoutRolePrefix() {
    // GIVE
    Map<String, String> roleGroupMapping = new HashMap<>();
    roleGroupMapping.put("apache1", "USER");
    LogSearchLdapAuthorityMapper underTest = new LogSearchLdapAuthorityMapper(roleGroupMapping);
    // WHEN
    List<GrantedAuthority> result = new ArrayList<>(underTest.mapAuthorities(generateAuthorities()));
    // THEN
    assertEquals("ROLE_USER", result.get(0).toString());
  }

  @Test
  public void testMultipleToTheSameMapping() {
    // GIVE
    Map<String, String> roleGroupMapping = new HashMap<>();
    roleGroupMapping.put("apache1", "ROLE_USER");
    roleGroupMapping.put("APACHE2", "ROLE_USER");
    roleGroupMapping.put("role_apache3", "ROLE_USER");
    roleGroupMapping.put("ROLE_APACHE4", "ROLE_USER");
    LogSearchLdapAuthorityMapper underTest = new LogSearchLdapAuthorityMapper(roleGroupMapping);
    // WHEN
    List<GrantedAuthority> result = new ArrayList<>(underTest.mapAuthorities(generateAuthorities()));
    // THEN
    assertEquals("ROLE_USER", result.get(0).toString());
    assertEquals(1, result.size());
  }

  @Test
  public void testMultipleRoles() {
    // GIVE
    Map<String, String> roleGroupMapping = new HashMap<>();
    roleGroupMapping.put("apache1", "ROLE_USER");
    roleGroupMapping.put("APACHE2", "ROLE_ADMIN");
    LogSearchLdapAuthorityMapper underTest = new LogSearchLdapAuthorityMapper(roleGroupMapping);
    // WHEN
    List<GrantedAuthority> result = new ArrayList<>(underTest.mapAuthorities(generateAuthorities()));
    // THEN
    assertEquals(2, result.size());
  }

  private List<SimpleGrantedAuthority> generateAuthorities() {
    List<SimpleGrantedAuthority> list = new ArrayList<>();
    list.add(new SimpleGrantedAuthority("apache1"));
    list.add(new SimpleGrantedAuthority("APACHE2"));
    list.add(new SimpleGrantedAuthority("role_apache3"));
    list.add(new SimpleGrantedAuthority("ROLE_APACHE4"));
    return list;
  }
}
