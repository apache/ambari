/**
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

import org.apache.commons.lang.StringUtils;

/**
 * Enumeration of internal controllers used via API.
 */
public enum ControllerType {
  LDAP("ldap");

  /**
   * Controller name.
   */
  private String name;

  /**
   * Constructor.
   *
   * @param name controller name
   */
  private ControllerType(String name) {
    this.name = name;
  }

  /**
   * Getter.
   *
   * @return controller name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns corresponding controller type to given name.
   *
   * @param name controller name
   * @return null if controller type was not found
   */
  public static ControllerType getByName(String name) {
    for (ControllerType type : ControllerType.values()) {
      if (StringUtils.equals(type.getName(), name)) {
        return type;
      }
    }
    return null;
  }
}