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

import java.util.Arrays;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.common.collect.ImmutableSet;

/**
 * The purpose of this helper is to get remote address from an HTTP request
 */
public class RequestUtils {

  private static Set<String> headersToCheck= ImmutableSet.copyOf(Arrays.asList(
    "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"));

  /**
   * Returns remote address
   * @param request contains the details of http request
   * @return
   */
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
    if (containsMultipleRemoteAddresses(ip)) {
       ip = ip.substring(0, ip.indexOf(","));
    }
    return ip;
  }

  /**
   * Returns remote address by using {@link HttpServletRequest} from {@link RequestContextHolder}
   * @return
   */
  public static String getRemoteAddress() {

    if(hasValidRequest()) {
      return getRemoteAddress(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
    }

    return null;
  }

  /**
   * Checks whether ip address is null, empty or unknown
   * @param ip
   * @return
   */
  private static boolean isRemoteAddressUnknown(String ip) {
    return ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip);
  }

  /**
   * Checks if ip contains multiple IP addresses
   */
  private static boolean containsMultipleRemoteAddresses(String ip) {
    return ip != null && ip.indexOf(",") > 0;
  }

  /**
   * Checks if RequestContextHolder contains a valid HTTP request
   * @return
   */
  private static boolean hasValidRequest() {
    return RequestContextHolder.getRequestAttributes() != null &&
      RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes &&
      ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest() != null;
  }

}
