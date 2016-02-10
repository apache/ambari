/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.utils;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.apache.ambari.server.api.services.Request;

public class RequestUtils {

  private static Set<String> headersToCheck= ImmutableSet.copyOf(Arrays.asList(
    "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"));;

  public static String getRemoteAddress(HttpServletRequest request) {
    String ip = null;
    for (String header : headersToCheck) {
      ip = request.getHeader(header);
      if (!isRemoteAddressUnknown(ip)) {
        break;
      }
    }
    if (isRemoteAddressUnknown(ip)) {
      ip = request.getRemoteAddr();
    }
    return ip;
  }

  public static String getRemoteAddress(Request request) {
    for (String header : headersToCheck) {
      if(request.getHttpHeaders().containsKey(header)) {
        for (String ip : request.getHttpHeaders().get(header))
          if (!isRemoteAddressUnknown(ip)) {
            return ip;
          }
      }
    }
    return null;
  }

  private static boolean isRemoteAddressUnknown(String ip) {
    return ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip);
  }

}
