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
 * Types for different focuses. Its purpose to give the command availability
 * checkers a chance in decision making.
 */
public enum FocusType {

  /**
   * The focus is on a selected host.
   */
  HOST("HOST"),

  /**
   * The focus is on the cluster building phase.
   */
  CLUSTER_BUILD("CLUSTER_BUILD"),

  /**
   * No focus at all.
   */
  ROOT("CLUSTER");

  private final String prefix;

  private FocusType(String prefix) {
    this.prefix = prefix;
  }

  /**
   * Prefix provided for the prompt.
   *
   * @return focus prefix
   */
  public String prefix() {
    return prefix;
  }
}
