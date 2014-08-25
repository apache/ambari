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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AmbariLocalUserDetailsServiceTest {

  private static Injector injector;

  @Inject
  AmbariLocalUserDetailsService userDetailsService;
  @Inject
  PasswordEncoder passwordEncoder;
  @Inject
  UserDAO userDAO;

  @BeforeClass
  public static void prepareData() {
    injector = Guice.createInjector(new AuthorizationTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.getInstance(OrmTestHelper.class).createTestUsers();
  }

  @Before
  public void setUp() throws Exception {
    injector.injectMembers(this);
  }

  @Test
  public void testLoadUserByUsername() throws Exception {
    UserDetails userDetails = userDetailsService.loadUserByUsername("administrator");
    assertEquals("Wrong username", "administrator", userDetails.getUsername());
    assertTrue("Password not matches", passwordEncoder.matches("admin", userDetails.getPassword()));
    assertFalse("Wrong password accepted", passwordEncoder.matches("wrong", userDetails.getPassword()));
  }

  @Test(expected = UsernameNotFoundException.class)
  public void testUsernameNotFound() throws Exception {
    userDetailsService.loadUserByUsername("notExists_123123123");
  }
}
