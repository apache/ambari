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
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Repository;
import org.apache.ambari.logsearch.util.FileUtil;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.ambari.logsearch.util.StringUtil;
import org.apache.ambari.logsearch.web.model.Privilege;
import org.apache.ambari.logsearch.web.model.Role;
import org.apache.ambari.logsearch.web.model.User;
import org.apache.ambari.logsearch.web.security.LogsearchFileAuthenticationProvider;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;

@Repository
public class UserDao {

  private static final Logger logger = Logger.getLogger(UserDao.class);
  private static final Md5PasswordEncoder md5Encoder = new Md5PasswordEncoder();

  @Autowired
  JSONUtil jsonUtil;

  @Autowired
  StringUtil stringUtil;

  @Autowired
  FileUtil fileUtil;

  @Autowired
  LogsearchFileAuthenticationProvider fileAuthenticationProvider;

  private HashMap<String, Object> userInfos = null;

  private ArrayList<HashMap<String, String>> userList = null;

  @SuppressWarnings("unchecked")
  @PostConstruct
  public void initialization() {
    if (fileAuthenticationProvider.isEnable()) {
      try {
        String USER_PASS_JSON_FILE_NAME = PropertiesUtil
          .getProperty("logsearch.login.credentials.file");
        logger.info("USER PASS JSON  file NAME:" + USER_PASS_JSON_FILE_NAME);
        File jsonFile = fileUtil
          .getFileFromClasspath(USER_PASS_JSON_FILE_NAME);
        if (jsonFile == null || !jsonFile.exists()) {
          logger.fatal("user_pass json file not found in classpath :"
            + USER_PASS_JSON_FILE_NAME);
          System.exit(1);
        }
        userInfos = jsonUtil.readJsonFromFile(jsonFile);
        userList = (ArrayList<HashMap<String, String>>) userInfos
          .get("users");
        if (userList != null) {
          // encrypting password using MD5 algo with salt username
          boolean isUpdated = this.encryptAllPassword();
          // updating json
          userInfos.put("users", userList);
          if (isUpdated) {
            String jsonStr = jsonUtil.mapToJSON(userInfos);
            jsonUtil.writeJSONInFile(jsonStr, jsonFile, true);
          }
        } else {
          userList = new ArrayList<HashMap<String, String>>();
        }

      } catch (Exception exception) {
        logger.error("Error while reading user prop file :"
          + exception.getMessage());
        userInfos = new HashMap<String, Object>();
        userList = new ArrayList<HashMap<String, String>>();
      }
    } else {
      logger.info("File auth is disabled.");
    }

  }

  /**
   * @param username
   * @return
   */
  public User loadUserByUsername(final String username) {
    logger.debug(" loadUserByUsername username" + username);
    HashMap<String, Object> userInfo = this.findByusername(username);
    User user = new User();

    if (userInfo != null) {
      user.setFirstName(userInfo.get(UserInfoAttributes.NAME) != null ? (String) userInfo
        .get(UserInfoAttributes.NAME) : "Unknown");
      user.setLastName(userInfo.get(UserInfoAttributes.NAME) != null ? (String) userInfo
        .get(UserInfoAttributes.NAME) : "Unknown");
      user.setUsername(userInfo.get(UserInfoAttributes.USER_NAME) != null ? (String) userInfo
        .get(UserInfoAttributes.USER_NAME) : "");
      user.setPassword(userInfo.get(UserInfoAttributes.ENC_PASSWORD) != null ? (String) userInfo
        .get(UserInfoAttributes.ENC_PASSWORD) : "");
    }

    Role r = new Role();
    r.setName("ROLE_USER");
    Privilege priv = new Privilege();
    priv.setName("READ_PRIVILEGE");
    ArrayList<Privilege> plist = new ArrayList<Privilege>();
    plist.add(priv);
    r.setPrivileges(plist);
    List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
    roles.add(r);
    user.setAuthorities(roles);
    return user;
  }

  /**
   * @param username
   * @return
   */
  public HashMap<String, Object> findByusername(final String username) {
    if (this.userList == null) {
      return null;
    }
    @SuppressWarnings("unchecked")
    HashMap<String, Object> userInfo = (HashMap<String, Object>) CollectionUtils
      .find(this.userList, new Predicate() {
        @Override
        public boolean evaluate(Object args) {
          HashMap<String, Object> tmpuserInfo = (HashMap<String, Object>) args;
          String objUsername = (String) tmpuserInfo
            .get(UserInfoAttributes.USER_NAME);
          if (objUsername != null && username != null) {
            return username.equalsIgnoreCase(objUsername);
          }
          return false;
        }
      });
    return userInfo;
  }

  private boolean encryptAllPassword() {
    boolean isUpdated = false;
    for (HashMap<String, String> user : userList) {
      // user
      String encPassword = user.get(UserInfoAttributes.ENC_PASSWORD);
      String username = user.get(UserInfoAttributes.USER_NAME);
      String password = user.get(UserInfoAttributes.PASSWORD);
      if (!stringUtil.isEmpty(password)) {
        encPassword = encryptPassword(username, password);
        user.put(UserInfoAttributes.PASSWORD, "");
        user.put(UserInfoAttributes.ENC_PASSWORD, encPassword);
        isUpdated = true;
      }
      if (stringUtil.isEmpty(password) && stringUtil.isEmpty(encPassword)) {
        // log error
        logger.error("Password is empty or null for username : "
          + username);
      }
    }
    return isUpdated;
  }

  /**
   * @param username
   * @param password
   * @return
   */
  public String encryptPassword(String username, String password) {
    if (!stringUtil.isEmpty(username)) {
      username = username.toLowerCase();
    }
    String saltEncodedpasswd = md5Encoder
      .encodePassword(password, username);
    return saltEncodedpasswd;
  }
}
