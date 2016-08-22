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

import java.util.Collection;

import org.apache.ambari.logsearch.web.model.Role;
import org.apache.ambari.logsearch.web.model.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/applicationContext.xml" })
public class UserDaoTest {

  @Autowired
  private UserDao dao;
  
  @Test
  public void testUserDaoInitAndFindUser() throws Exception {
    User user = dao.loadUserByUsername("testUserName");
    assertEquals(user.getUsername(), "testUserName");
    assertEquals(user.getFirstName(), "Test User Name");
    assertEquals(user.getLastName(), "Test User Name");
    
    Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
    assertTrue(authorities.size() == 1);
    
    Role authority = (Role)authorities.iterator().next();
    assertEquals(authority.getName(), "ROLE_USER");
    assertTrue(authority.getPrivileges().size() == 1);
    assertEquals(authority.getPrivileges().get(0).getName(), "READ_PRIVILEGE");
  }
}
