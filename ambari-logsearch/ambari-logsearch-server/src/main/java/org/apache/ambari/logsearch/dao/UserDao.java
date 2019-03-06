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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.apache.ambari.logsearch.util.FileUtil;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.web.model.Privilege;
import org.apache.ambari.logsearch.web.model.Role;
import org.apache.ambari.logsearch.web.model.User;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

@Repository
public class UserDao {
  private static final Logger logger = Logger.getLogger(UserDao.class);

  private static final String USER_NAME = "username";
  private static final String PASSWORD = "password";
  private static final String ENC_PASSWORD = "en_password";
  private static final String NAME = "name";

  @Inject
  private AuthPropsConfig authPropsConfig;

  @Inject
  private PasswordEncoder passwordEncoder;

  private ArrayList<HashMap<String, String>> userList = null;

  @SuppressWarnings("unchecked")
  @PostConstruct
  public void initialization() {
    if (authPropsConfig.isAuthFileEnabled()) {
      try {
        String userPassJsonFileName = authPropsConfig.getCredentialsFile();
        logger.info("USER PASS JSON  file NAME:" + userPassJsonFileName);
        File jsonFile = FileUtil.getFileFromClasspath(userPassJsonFileName);
        if (jsonFile == null || !jsonFile.exists()) {
          logger.fatal("user_pass json file not found in classpath :" + userPassJsonFileName);
          System.exit(1);
        }
        HashMap<String, Object> userInfos = JSONUtil.readJsonFromFile(jsonFile);
        userList = (ArrayList<HashMap<String, String>>) userInfos.get("users");
        if (userList != null) {
          boolean isUpdated = this.encryptAllPassword();
          userInfos.put("users", userList);
          if (isUpdated) {
            String jsonStr = JSONUtil.mapToJSON(userInfos);
            JSONUtil.writeJSONInFile(jsonStr, jsonFile, true);
          }
        } else {
          userList = new ArrayList<HashMap<String, String>>();
        }

      } catch (Exception exception) {
        logger.error("Error while reading user prop file :" + exception.getMessage());
        userList = new ArrayList<HashMap<String, String>>();
      }
    } else {
      logger.info("File auth is disabled.");
    }
  }

  public User loadUserByUsername(String username) {
    logger.debug(" loadUserByUsername username" + username);
    HashMap<String, String> userInfo = findByusername(username);
    if (userInfo == null) {
      return null;
    }
    
    User user = new User();
    user.setFirstName(StringUtils.defaultString(userInfo.get(NAME), "Unknown"));
    user.setLastName(StringUtils.defaultString(userInfo.get(NAME), "Unknown"));
    user.setUsername(StringUtils.defaultString(userInfo.get(USER_NAME), ""));
    user.setPassword(StringUtils.defaultString(userInfo.get(ENC_PASSWORD), ""));

    Role r = new Role();
    r.setName("ROLE_USER");
    Privilege priv = new Privilege();
    priv.setName("READ_PRIVILEGE");
    r.setPrivileges(Arrays.asList(priv));
    user.setAuthorities(Arrays.asList((GrantedAuthority)r));
    
    return user;
  }

  private HashMap<String, String> findByusername(final String username) {
    if (userList == null) {
      return null;
    }
    @SuppressWarnings("unchecked")
    HashMap<String, String> userInfo = (HashMap<String, String>) CollectionUtils.find(userList,
        new Predicate() {
          @Override
          public boolean evaluate(Object args) {
            HashMap<String, String> tmpUserInfo = (HashMap<String, String>) args;
            String objUsername = tmpUserInfo.get(USER_NAME);
            return (objUsername != null && username != null && username.equalsIgnoreCase(objUsername));
          }
        });
    
    return userInfo;
  }

  private boolean encryptAllPassword() {
    boolean isUpdated = false;
    for (HashMap<String, String> user : userList) {
      String encPassword = user.get(ENC_PASSWORD);
      String username = user.get(USER_NAME);
      String password = user.get(PASSWORD);
      if (StringUtils.isNotBlank(password)) {
        encPassword = passwordEncoder.encode(password);
        user.put(PASSWORD, "");
        user.put(ENC_PASSWORD, encPassword);
        isUpdated = true;
      }
      if (StringUtils.isBlank(password) && StringUtils.isBlank(encPassword)) {
        logger.error("Password is empty or null for username : " + username);
      }
    }
    return isUpdated;
  }
}
