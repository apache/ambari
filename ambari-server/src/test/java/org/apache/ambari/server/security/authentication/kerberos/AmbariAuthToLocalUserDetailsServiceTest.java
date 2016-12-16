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

package org.apache.ambari.server.security.authentication.kerberos;

import static org.easymock.EasyMock.expect;

import java.util.Collection;
import java.util.Collections;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.UserType;
import org.apache.ambari.server.security.authorization.Users;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import junit.framework.Assert;

public class AmbariAuthToLocalUserDetailsServiceTest extends EasyMockSupport {
  @Before
  public void setup() {
    // These system properties need to be set to properly configure the KerberosName object when
    // a krb5.conf file is not available
    System.setProperty("java.security.krb5.realm", "EXAMPLE.COM");
    System.setProperty("java.security.krb5.kdc", "localhost");
  }

  @Test
  public void loadUserByUsernameSuccess() throws Exception {
    AmbariKerberosAuthenticationProperties properties = new AmbariKerberosAuthenticationProperties();

    Configuration configuration = createMock(Configuration.class);
    expect(configuration.getKerberosAuthenticationProperties()).andReturn(properties).once();

    User user = createMock(User.class);
    expect(user.getUserName()).andReturn("user1").once();
    expect(user.getUserType()).andReturn(UserType.LDAP).once();

    Collection<AmbariGrantedAuthority> userAuthorities = Collections.singletonList(createNiceMock(AmbariGrantedAuthority.class));

    Users users = createMock(Users.class);
    expect(users.getUser("user1", UserType.LDAP)).andReturn(user).once();
    expect(users.getUserAuthorities("user1", UserType.LDAP)).andReturn(userAuthorities).once();

    replayAll();

    UserDetailsService userdetailsService = new AmbariAuthToLocalUserDetailsService(configuration, users);

    UserDetails userDetails = userdetailsService.loadUserByUsername("user1@EXAMPLE.COM");

    verifyAll();

    Assert.assertNotNull(userDetails);
    Assert.assertEquals("user1", userDetails.getUsername());
    Assert.assertEquals(userAuthorities.size(), userDetails.getAuthorities().size());
    Assert.assertEquals("", userDetails.getPassword());
  }

  @Test(expected = UsernameNotFoundException.class)
  public void loadUserByUsernameUserNotFound() throws Exception {
    AmbariKerberosAuthenticationProperties properties = new AmbariKerberosAuthenticationProperties();

    Configuration configuration = createMock(Configuration.class);
    expect(configuration.getKerberosAuthenticationProperties()).andReturn(properties).once();

    Users users = createMock(Users.class);
    expect(users.getUser("user1", UserType.LDAP)).andReturn(null).once();
    expect(users.getUser("user1", UserType.LOCAL)).andReturn(null).once();

    replayAll();

    UserDetailsService userdetailsService = new AmbariAuthToLocalUserDetailsService(configuration, users);

    userdetailsService.loadUserByUsername("user1@EXAMPLE.COM");

    verifyAll();

    Assert.fail("UsernameNotFoundException was not thrown");
  }

}