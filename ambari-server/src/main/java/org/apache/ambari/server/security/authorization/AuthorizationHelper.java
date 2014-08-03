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

import com.google.inject.Singleton;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

@Singleton
/**
 * Provides utility methods for authentication functionality
 */
public class AuthorizationHelper {

  /**
   * Converts collection of RoleEntities to collection of GrantedAuthorities
   */
  public Collection<GrantedAuthority> convertPrivilegesToAuthorities(Collection<PrivilegeEntity> privilegeEntities) {
    Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>(privilegeEntities.size());

    for (PrivilegeEntity privilegeEntity : privilegeEntities) {
      authorities.add(new AmbariGrantedAuthority(privilegeEntity));
    }

    return authorities;
  }

  /**
   * Gets the name of the logged in user.  Thread-safe due to use of thread-local.
   * @return the name of the logged in user, or <code>null</code> if none set.
   */
  public static String getAuthenticatedName() {
    return getAuthenticatedName(null);
  }
  
  /**
   * Gets the name of the logged-in user, if any.  Thread-safe due to use of
   * thread-local.
   * @param defaultUsername the value if there is no logged-in user
   * @return the name of the logged-in user, or the default
   */
  public static String getAuthenticatedName(String defaultUsername) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    
    Authentication auth = securityContext.getAuthentication();
    
    return (null == auth) ? defaultUsername : auth.getName();
  }
}
