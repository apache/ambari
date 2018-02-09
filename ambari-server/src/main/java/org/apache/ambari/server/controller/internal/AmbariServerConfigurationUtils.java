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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.configuration.ConfigurationPropertyType;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationKeys;

/**
 * Provides useful utility methods for AMBARI-level configuration related tasks.
 */
public class AmbariServerConfigurationUtils {

  /**
   * @param category
   *          the name of the category
   * @param propertyName
   *          the name of the property
   * @return the type of the given category/property if such category/property
   *         exists; {@code null} otherwise
   * @throws IllegalStateException
   *           if there is no property found with the given name
   */
  public static ConfigurationPropertyType getConfigurationPropertyType(String category, String propertyName) {
    if (AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName().equals(category)) {
      return AmbariLdapConfigurationKeys.fromKeyStr(propertyName).getConfigurationPropertyType();
    }
    return null;
  }

  /**
   * @param category
   *          the name of the category
   * @param propertyName
   *          the name of the property
   * @return the String representation of the type if such category/property
   *         exists; {@code null} otherwise * @throws IllegalStateException if
   *         there is no property found with the given name
   */
  public static String getConfigurationPropertyTypeName(String category, String propertyName) {
    final ConfigurationPropertyType configurationPropertyType = getConfigurationPropertyType(category, propertyName);
    return configurationPropertyType == null ? null : configurationPropertyType.name();
  }

  /**
   * Indicates whether the given property's type is
   * 
   * {@link ConfigurationPropertyType.PASSWORD}
   *
   * @param category
   *          the name of the category
   * @param propertyName
   *          the name of the property
   * @return {@code true} in case the given property's type is
   *         {@link ConfigurationPropertyType.PASSWORD}; {@code false} otherwise
   * @throws IllegalStateException
   *           if there is no property found with the given name
   */
  public static boolean isPassword(String category, String propertyName) {
    return ConfigurationPropertyType.PASSWORD.equals(getConfigurationPropertyType(category, propertyName));
  }

}
