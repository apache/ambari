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

package org.apache.ambari.server.utils;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;

/**
 * Provides functionality for hiding credentials in URLs.
 */
public class URLCredentialsHider {
  public static final String INVALID_URL = "invalid_url";
  public static final String HIDDEN_USER = "****";
  public static final String HIDDEN_CREDENTIALS = HIDDEN_USER + ":" + HIDDEN_USER;

  public static String hideCredentials(String urlString) {
    if (StringUtils.isEmpty(urlString)) {
      return urlString;
    }
    URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      // it is better to miss url instead of spreading it out
      return INVALID_URL;
    }
    String userInfo = url.getUserInfo();
    if (StringUtils.isNotEmpty(userInfo)) {
      if (userInfo.contains(":")) {
        return urlString.replaceFirst(userInfo, HIDDEN_CREDENTIALS);
      } else {
        return urlString.replaceFirst(userInfo, HIDDEN_USER);
      }
    }
    return urlString;
  }
}
