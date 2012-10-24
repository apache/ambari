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
import org.apache.ambari.server.orm.entities.RoleEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
/**
 * Provides utility methods for authentication functionality
 */
public class AuthorizationHelper {

  /**
   * Converts collection of RoleEntities to collection of GrantedAuthorities
   */
  public Collection<GrantedAuthority> convertRolesToAuthorities(Collection<RoleEntity> roleEntities) {
    List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>(roleEntities.size());

    for (RoleEntity roleEntity : roleEntities) {
      authorities.add(new SimpleGrantedAuthority(roleEntity.getRoleName().toUpperCase()));
    }

    return authorities;
  }
}
