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
package org.apache.ambari.logsearch.dao;

import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.List;

public class RoleDaoTest {

  private RoleDao underTest;

  @Before
  public void setUp() {
    underTest = new RoleDao();
    AuthPropsConfig authPropsConfig = new AuthPropsConfig();
    authPropsConfig.setFileAuthorization(true);
    underTest.setAuthPropsConfig(authPropsConfig);
  }

  @Test
  public void testCreateDefaultAuthorities() {
    // GIVEN
    // WHEN
    List<GrantedAuthority> authorityList = RoleDao.createDefaultAuthorities();
    // THEN
    Assert.assertEquals("ROLE_USER", authorityList.get(0).getAuthority());
  }

  @Test
  public void testGetRolesForUser() {
    // GIVEN
    List<String> users = Arrays.asList("admin1", "admin2");
    underTest.getSimpleRolesMap().put("user", users);
    // WHEN
    List<GrantedAuthority> result1 = underTest.getRolesForUser("admin1");
    List<GrantedAuthority> result2 = underTest.getRolesForUser("admin2");
    List<GrantedAuthority> result3 = underTest.getRolesForUser("admin3");
    // THEN
    Assert.assertEquals("ROLE_USER", result1.get(0).getAuthority());
    Assert.assertEquals("ROLE_USER", result2.get(0).getAuthority());
    Assert.assertTrue(result3.isEmpty());
  }
}
