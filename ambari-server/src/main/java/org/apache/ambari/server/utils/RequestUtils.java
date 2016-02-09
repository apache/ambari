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

import com.google.common.collect.Queues;
import java.util.Arrays;
import java.util.Queue;
import javax.servlet.http.HttpServletRequest;

public class RequestUtils {

  public static String getRemoteAddress(HttpServletRequest request) {
    Queue<String> headersToCheck = Queues.newLinkedBlockingQueue(Arrays.asList(
      "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"));

    return getRemoteAddressFromHeader(request, headersToCheck);
  }

  private static String getRemoteAddressFromHeader(HttpServletRequest request, Queue<String> headersToCheck) {
    String ip;
    if (!headersToCheck.isEmpty()) {
      ip = request.getHeader(headersToCheck.poll());
      if (isRemoteAddressUnknown(ip)) {
        ip = getRemoteAddressFromHeader(request, headersToCheck);
      }
    } else {
      ip = request.getRemoteAddr();
    }
    return ip;
  }

  private static boolean isRemoteAddressUnknown(String ip) {
    return ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip);
  }

}
