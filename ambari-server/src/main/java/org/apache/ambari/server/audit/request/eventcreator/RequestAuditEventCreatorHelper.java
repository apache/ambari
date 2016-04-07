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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit.request.eventcreator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.api.services.Request;

/**
 * The purpose of this class is to retrieve information from {@link Request} objects.
 * This information can be a single value or a list of values.
 */
public class RequestAuditEventCreatorHelper {

  /**
   * Returns a named property from a request
   * @param request
   * @param propertyName
   * @return
   */
  public static String getNamedProperty(Request request, String propertyName) {
    if (isValid(request, propertyName)) {
      return String.valueOf(request.getBody().getNamedPropertySets().iterator().next().getProperties().get(propertyName));
    }
    return null;
  }

  /**
   * Returns a list of named properties from a request
   * @param request
   * @param propertyName
   * @return
   */
  public static List<String> getNamedPropertyList(Request request, String propertyName) {
    if (isValidList(request, propertyName)) {
      List<String> list = (List<String>) request.getBody().getNamedPropertySets().iterator().next().getProperties().get(propertyName);
      if (list != null) {
        return list;
      }
    }
    return Collections.emptyList();
  }

  /**
   * Checks if the property is valid: can be found and has correct type
   * @param request
   * @param propertyName
   * @return
   */
  private static boolean isValid(Request request, String propertyName) {
    return !request.getBody().getNamedPropertySets().isEmpty() &&
      request.getBody().getNamedPropertySets().iterator().next().getProperties() != null &&
      request.getBody().getNamedPropertySets().iterator().next().getProperties().get(propertyName) instanceof String;
  }

  /**
   * Checks if the property is a valid list: can be found and has correct type
   * @param request
   * @param propertyName
   * @return
   */
  private static boolean isValidList(Request request, String propertyName) {
    return !request.getBody().getNamedPropertySets().isEmpty() &&
      request.getBody().getNamedPropertySets().iterator().next().getProperties() != null &&
      request.getBody().getNamedPropertySets().iterator().next().getProperties().get(propertyName) instanceof List;
  }

  /**
   * Returns a property from a request
   * @param request
   * @param propertyName
   * @return
   */
  public static String getProperty(Request request, String propertyName) {
    List<String> list = getPropertyList(request, propertyName);
    return list.isEmpty() ? null : list.get(0);
  }

  /**
   * Returns a list of properties from a request
   * @param request
   * @param propertyName
   * @return
   */
  public static List<String> getPropertyList(Request request, String propertyName) {
    List<String> list = new LinkedList<String>();
    for (Map<String, Object> propertyMap : request.getBody().getPropertySets()) {
      String userName = String.valueOf(propertyMap.get(propertyName));
      list.add(userName);
    }
    return list;
  }
}
