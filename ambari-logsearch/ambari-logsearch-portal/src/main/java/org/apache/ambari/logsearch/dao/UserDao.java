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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Repository;

import org.apache.ambari.logsearch.util.CommonUtil;
import org.apache.ambari.logsearch.util.FileUtil;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.ambari.logsearch.web.model.Privilege;
import org.apache.ambari.logsearch.web.model.Role;
import org.apache.ambari.logsearch.web.model.User;
import org.apache.ambari.logsearch.web.security.LogsearchFileAuthenticationProvider;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

@Repository
public class UserDao {
  private static final Logger logger = Logger.getLogger(UserDao.class);

  private static final String USER_NAME = "username";
  private static final String PASSWORD = "password";
  private static final String ENC_PASSWORD = "en_password";
  private static final String NAME = "name";

  @Autowired
  private JSONUtil jsonUtil;
  @Autowired
  private FileUtil fileUtil;
  @Autowired
  private LogsearchFileAuthenticationProvider fileAuthenticationProvider;

  private ArrayList<HashMap<String, String>> userList = null;

  @SuppressWarnings("unchecked")
  @PostConstruct
  public void initialization() {
    if (fileAuthenticationProvider.isEnable()) {
      try {
        String userPassJsonFileName = PropertiesUtil.getProperty("logsearch.login.credentials.file");
        logger.info("USER PASS JSON  file NAME:" + userPassJsonFileName);
        File jsonFile = fileUtil.getFileFromClasspath(userPassJsonFileName);
        if (jsonFile == null || !jsonFile.exists()) {
          logger.fatal("user_pass json file not found in classpath :" + userPassJsonFileName);
          System.exit(1);
        }
        HashMap<String, Object> userInfos = jsonUtil.readJsonFromFile(jsonFile);
        userList = (ArrayList<HashMap<String, String>>) userInfos.get("users");
        if (userList != null) {
          boolean isUpdated = this.encryptAllPassword();
          userInfos.put("users", userList);
          if (isUpdated) {
            String jsonStr = jsonUtil.mapToJSON(userInfos);
            jsonUtil.writeJSONInFile(jsonStr, jsonFile, true);
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

  public User loadUserByUsername(final String username) {
    logger.debug(" loadUserByUsername username" + username);
    HashMap<String, String> userInfo = findByusername(username);
    User user = new User();

    if (userInfo != null) {
      user.setFirstName(userInfo.get(NAME) != null ? userInfo.get(NAME) : "Unknown");
      user.setLastName(userInfo.get(NAME) != null ? userInfo.get(NAME) : "Unknown");
      user.setUsername(userInfo.get(USER_NAME) != null ? userInfo.get(USER_NAME) : "");
      user.setPassword(userInfo.get(ENC_PASSWORD) != null ? userInfo.get(ENC_PASSWORD) : "");
    }

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
      if (!StringUtils.isBlank(password)) {
        encPassword = CommonUtil.encryptPassword(username, password);
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
  
  public String encryptPassword(String username, String password) {
    if (!StringUtils.isEmpty(username)) {
      username = username.toLowerCase();
    }
    String saltEncodedpasswd = CommonUtil.encryptPassword(password, username);
    return saltEncodedpasswd;
  }
}
