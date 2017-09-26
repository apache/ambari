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
package org.apache.ambari.server.checks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.Singleton;

/**
 * Used to make sure that the password in Ambari matches that for Ranger, in case the
 * user had changed the password using the Ranger UI.
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.CONFIGURATION_WARNING,
    order = 23.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class RangerPasswordCheck extends AbstractCheckDescriptor {

  private static final Logger LOG = LoggerFactory.getLogger(RangerPasswordCheck.class);

  static final String KEY_RANGER_PASSWORD_MISMATCH = "could_not_verify_password";
  static final String KEY_RANGER_COULD_NOT_ACCESS = "could_not_access";
  static final String KEY_RANGER_UNKNOWN_RESPONSE = "unknown_response";
  static final String KEY_RANGER_USERS_ELEMENT_MISSING = "missing_vxusers";
  static final String KEY_RANGER_OTHER_ISSUE = "invalid_response";
  static final String KEY_RANGER_CONFIG_MISSING = "missing_config";

  /**
   * Constructor.
   */
  public RangerPasswordCheck() {
    super(CheckDescription.SERVICES_RANGER_PASSWORD_VERIFY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getApplicableServices() {
    return Sets.newHashSet("RANGER");
  }

  @Override
  public void perform(PrerequisiteCheck check, PrereqCheckRequest request) throws AmbariException {
    // !!! ComponentSSLConfiguration is an old-school singleton which doesn't
    // get initialized until after Guice is done - because this check is bound
    // as a singleton via Guice, we can't initialize the stream provider in the
    // constructor since the SSL configuration instance hasn't been initialized
    URLStreamProvider streamProvider = new URLStreamProvider(2000, 2000,
        ComponentSSLConfiguration.instance());

    String rangerUrl = checkEmpty("admin-properties", "policymgr_external_url", check, request);
    if (null == rangerUrl) {
      // !!! check results already filled
      return;
    }

    String adminUsername = checkEmpty("ranger-env", "admin_username", check, request);
    if (null == adminUsername) {
      return;
    }

    String adminPassword = checkEmpty("ranger-env", "admin_password", check, request);
    if (null == adminPassword) {
      return;
    }

    String rangerAdminUsername = checkEmpty("ranger-env", "ranger_admin_username", check, request);
    if (null == rangerAdminUsername) {
      return;
    }

    String rangerAdminPassword = checkEmpty("ranger-env", "ranger_admin_password", check, request);
    if (null == rangerAdminPassword) {
      return;
    }

    if (rangerUrl.endsWith("/")) {
      rangerUrl = rangerUrl.substring(0, rangerUrl.length()-1);
    }

    String rangerAuthUrl = String.format("%s/%s", rangerUrl,
        "service/public/api/repository/count");
    String rangerUserUrl = String.format("%s/%s", rangerUrl,
        "service/xusers/users");

    List<String> failReasons = new ArrayList<>();
    List<String> warnReasons = new ArrayList<>();

    // !!! first, just try the service with the admin credentials
    try {
      int response = checkLogin(streamProvider, rangerAuthUrl, adminUsername, adminPassword);

      switch (response) {
        case 401: {
          String reason = getFailReason(KEY_RANGER_PASSWORD_MISMATCH, check, request);
          failReasons.add(String.format(reason, adminUsername));
          break;
        }
        case 200: {
          break;
        }
        default: {
          String reason = getFailReason(KEY_RANGER_UNKNOWN_RESPONSE, check, request);
          warnReasons.add(String.format(reason, adminUsername, response, rangerAuthUrl));
          break;
        }
      }

    } catch (IOException e) {
      LOG.warn("Could not access the url {}.  Message: {}", rangerAuthUrl, e.getMessage(), e);
      LOG.debug("Could not access the url {}.  Message: {}", rangerAuthUrl, e.getMessage());

      String reason = getFailReason(KEY_RANGER_COULD_NOT_ACCESS, check, request);
      warnReasons.add(String.format(reason, adminUsername, rangerAuthUrl, e.getMessage()));
    }

    // !!! shortcut when something happened with the admin user
    if (!failReasons.isEmpty()) {
      check.setFailReason(StringUtils.join(failReasons, '\n'));
      check.getFailedOn().add("RANGER");
      check.setStatus(PrereqCheckStatus.FAIL);
      return;
    } else if (!warnReasons.isEmpty()) {
      check.setFailReason(StringUtils.join(warnReasons, '\n'));
      check.getFailedOn().add("RANGER");
      check.setStatus(PrereqCheckStatus.WARNING);
      return;
    }

    // !!! Check for the user, capture exceptions as a warning.
    boolean hasUser = checkRangerUser(streamProvider, rangerUserUrl, adminUsername, adminPassword,
        rangerAdminUsername, check, request, warnReasons);

    if (hasUser) {
      // !!! try credentials for specific user
      try {
        int response = checkLogin(streamProvider, rangerAuthUrl, rangerAdminUsername,
            rangerAdminPassword);

        switch (response) {
          case 401: {
            String reason = getFailReason(KEY_RANGER_PASSWORD_MISMATCH, check, request);
            failReasons.add(String.format(reason, rangerAdminUsername));
            break;
          }
          case 200: {
            break;
          }
          default: {
            String reason = getFailReason(KEY_RANGER_UNKNOWN_RESPONSE, check, request);
            warnReasons.add(String.format(reason, rangerAdminUsername, response, rangerAuthUrl));
            break;
          }
        }

      } catch (IOException e) {
        LOG.warn("Could not access the url {}.  Message: {}", rangerAuthUrl, e.getMessage());
        LOG.debug("Could not access the url {}.  Message: {}", rangerAuthUrl, e.getMessage(), e);

        String reason = getFailReason(KEY_RANGER_COULD_NOT_ACCESS, check, request);
        warnReasons.add(String.format(reason, rangerAdminUsername, rangerAuthUrl, e.getMessage()));
      }
    }

    if (!failReasons.isEmpty()) {
      check.setFailReason(StringUtils.join(failReasons, '\n'));
      check.getFailedOn().add("RANGER");
      check.setStatus(PrereqCheckStatus.FAIL);
    } else if (!warnReasons.isEmpty()) {
      check.setFailReason(StringUtils.join(warnReasons, '\n'));
      check.getFailedOn().add("RANGER");
      check.setStatus(PrereqCheckStatus.WARNING);
    } else {
      check.setStatus(PrereqCheckStatus.PASS);
    }

  }

  /**
   * Checks the credentials. From the Ranger team, bad credentials result in a
   * successful call, but the Ranger admin server will redirect to the home
   * page. They recommend parsing the result. If it parses, the credentials are
   * good, otherwise consider the user as unverified.
   *
   * @param streamProvider
   *          the stream provider to use when making requests
   * @param url
   *          the url to check
   * @param username
   *          the user to check
   * @param password
   *          the password to check
   * @return the http response code
   * @throws IOException
   *           if there was an error reading the response
   */
  private int checkLogin(URLStreamProvider streamProvider, String url, String username,
      String password) throws IOException {

    Map<String, List<String>> headers = getHeaders(username, password);

    HttpURLConnection conn = streamProvider.processURL(url, "GET", (InputStream) null, headers);

    int result = conn.getResponseCode();

    // !!! see javadoc
    if (result == 200) {
      Gson gson = new Gson();
      try {
        gson.fromJson(new InputStreamReader(conn.getInputStream()), Object.class);
      } catch (Exception e) {
        result = 401;
      }
    }

    return result;
  }

  /**
   * @param streamProvider
   *          the stream provider to use when making requests
   * @param rangerUserUrl
   *          the url to use when looking for the user
   * @param username
   *          the username to use when loading the url
   * @param password
   *          the password for the user url
   * @param userToSearch
   *          the user to look for
   * @param check
   *          the check instance for loading failure reasons
   * @param request
   *          the request instance for loading failure reasons
   * @param warnReasons
   *          the list of warn reasons to fill
   * @return {@code true} if the user was found
   */
  private boolean checkRangerUser(URLStreamProvider streamProvider, String rangerUserUrl,
      String username, String password, String userToSearch, PrerequisiteCheck check,
      PrereqCheckRequest request, List<String> warnReasons) throws AmbariException {

    String url = String.format("%s?name=%s", rangerUserUrl, userToSearch);

    Map<String, List<String>> headers = getHeaders(username, password);

    try {
      HttpURLConnection conn = streamProvider.processURL(url, "GET", (InputStream) null, headers);

      int result = conn.getResponseCode();

      if (result == 200) {

        Gson gson = new Gson();
        Object o = gson.fromJson(new InputStreamReader(conn.getInputStream()), Object.class);

        Map<?, ?> map = (Map<?,?>) o;

        if (!map.containsKey("vXUsers")) {
          String reason = getFailReason(KEY_RANGER_USERS_ELEMENT_MISSING, check, request);
          warnReasons.add(String.format(reason, url));

          return false;
        }

        @SuppressWarnings("unchecked")
        List<Map<?, ?>> list = (List<Map<?, ?>>) map.get("vXUsers");

        for (Map<?, ?> listMap : list) {
          if (listMap.containsKey("name") && listMap.get("name").equals(userToSearch)) {
            return true;
          }
        }
      }
    } catch (IOException e) {
      LOG.warn("Could not determine user {}.  Error is {}", userToSearch, e.getMessage());
      LOG.debug("Could not determine user {}.  Error is {}", userToSearch, e.getMessage(), e);

      String reason = getFailReason(KEY_RANGER_COULD_NOT_ACCESS, check, request);
      warnReasons.add(String.format(reason, username, url, e.getMessage()));

    } catch (Exception e) {
      LOG.warn("Could not determine user {}.  Error is {}", userToSearch, e.getMessage());
      LOG.debug("Could not determine user {}.  Error is {}", userToSearch, e.getMessage(), e);

      String reason = getFailReason(KEY_RANGER_OTHER_ISSUE, check, request);
      warnReasons.add(String.format(reason, e.getMessage(), url));
    }

    return false;
  }

  /**
   * Generates a list of headers, including {@code Basic} authentication
   * @param username  the username
   * @param password  the password
   * @return the map of headers
   */
  private Map<String, List<String>> getHeaders(String username, String password) {
    Map<String, List<String>> headers = new HashMap<>();

    String base64 = Base64.encodeBase64String(
        String.format("%s:%s", username, password).getBytes(Charset.forName("UTF8")));

    headers.put("Content-Type", Arrays.asList("application/json"));
    headers.put("Accept", Arrays.asList("application/json"));
    headers.put("Authorization", Arrays.asList(String.format("Basic %s", base64)));

    return headers;
  }

  /**
   * Finds the property value.  If not found, then the failure reason for the check
   * is filled in and processing should not continue.
   *
   * @param type      the type of property to find
   * @param key       the key in configs matching the type
   * @param check     the check for loading failure reasons
   * @param request   the request for loading failure reasons
   * @return the property value, or {@code null} if the property doesn't exist
   * @throws AmbariException
   */
  private String checkEmpty(String type, String key, PrerequisiteCheck check,
      PrereqCheckRequest request) throws AmbariException {

    String value = getProperty(request, type, key);
    if (null == value) {
      String reason = getFailReason(KEY_RANGER_CONFIG_MISSING, check, request);
      reason = String.format(reason, type, key);
      check.setFailReason(reason);
      check.getFailedOn().add("RANGER");
      check.setStatus(PrereqCheckStatus.WARNING);
    }
    return value;
  }


}