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

import com.google.common.annotations.VisibleForTesting;
import io.jsonwebtoken.lang.Collections;
import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.apache.ambari.logsearch.util.FileUtil;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.web.model.Privilege;
import org.apache.ambari.logsearch.web.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

/**
 * Helper class to assign roles for authenticated users, can be used only by JWT and file based authentication.
 */
@Named
public class RoleDao {

  private static final Logger LOG = LoggerFactory.getLogger(RoleDao.class);

  @Inject
  private AuthPropsConfig authPropsConfig;

  private final Map<String, List<String>> simpleRolesMap = new HashMap<>();

  @SuppressWarnings("unchecked")
  @PostConstruct
  public void init() {
    if (authPropsConfig.isFileAuthorization()) {
      try {
        String userRoleFileName = authPropsConfig.getRoleFile();
        LOG.info("USER ROLE JSON file NAME:" + userRoleFileName);
        File jsonFile = FileUtil.getFileFromClasspath(userRoleFileName);
        if (jsonFile == null || !jsonFile.exists()) {
          LOG.error("Role json file not found on the classpath :" + userRoleFileName);
          System.exit(1);
        }
        Map<String, Object> userRoleInfo = JSONUtil.readJsonFromFile(jsonFile);
        Map<String, Object> roles = (Map<String, Object>) userRoleInfo.get("roles");
        for (Map.Entry<String, Object> roleEntry : roles.entrySet()) {
          simpleRolesMap.put(roleEntry.getKey(), (List<String>) roleEntry.getValue());
        }
      } catch (Exception e) {
        LOG.error("Error while reading user role file: {}", e.getMessage());
      }
    } else {
      LOG.info("File authorization is disabled");
    }
  }

  public List<GrantedAuthority> getRolesForUser(String user) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    if (authPropsConfig.isFileAuthorization()) {
        List<String > roles = simpleRolesMap.get(user);
        if (!Collections.isEmpty(roles)) {
          for (String role : roles) {
            String roleName = "ROLE_" + role;
            LOG.debug("Found role '{}' for user '{}'", roleName, user);
            authorities.add(createRoleWithReadPrivilage(roleName));
          }
        } else {
          LOG.warn("Not found roles for user '{}'", user);
        }
      return authorities;
    } else {
      return createDefaultAuthorities();
    }
  }

  public Map<String, List<String>> getSimpleRolesMap() {
    return simpleRolesMap;
  }

  @VisibleForTesting
  public void setAuthPropsConfig(AuthPropsConfig authPropsConfig) {
    this.authPropsConfig = authPropsConfig;
  }

  /**
   * Helper function to create a simple default role details
   */
  public static List<GrantedAuthority> createDefaultAuthorities() {
    Role r = createRoleWithReadPrivilage("ROLE_USER");
    return singletonList(r);
  }

  private static Role createRoleWithReadPrivilage(String roleName) {
    Role r = new Role();
    r.setName(roleName);
    Privilege priv = new Privilege();
    priv.setName("READ_PRIVILEGE");
    r.setPrivileges(singletonList(priv));
    return r;
  }
}
