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
package org.apache.ambari.shell.model;

/**
 * Holds information about the focus. Focus give you the ability to
 * provide context sensitive commands.
 *
 * @see org.apache.ambari.shell.model.FocusType
 */
public class Focus {

  private final String value;
  private final FocusType type;

  public Focus(String value, FocusType type) {
    this.value = value;
    this.type = type;
  }

  public String getPrefix() {
    return type.prefix();
  }

  public String getValue() {
    return value;
  }

  /**
   * Checks if the current focus exists with the provided one.
   *
   * @param type type to check with the current
   * @return true if they match false otherwise
   */
  public boolean isType(FocusType type) {
    return this.type == type;
  }
}
