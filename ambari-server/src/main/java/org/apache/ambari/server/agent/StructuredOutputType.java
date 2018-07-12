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
package org.apache.ambari.server.agent;

/**
 * Represents the type of output from commands which report a
 * {@link CommandReport#getStructuredOut()}. This is used for deserializing
 * output.
 */
public enum StructuredOutputType {

  /**
   * The structured output from listing keytabs on a host.
   */
  CHECK_KEYTABS("check_keytabs"),

  /**
   * The structured output from writing out keytabs on a host.
   */
  SET_KEYTABS("set_keytabs"),

  /**
   * The structured output from an mpack installation.
   */
  MPACK_INSTALLATION("mpack_installation"),

  /**
   * The structured output from a start command which usually contains version
   * and mpack information.
   */
  VERSION_REPORTING("version_reporting"),

  /**
   * Information about an upgrade in progress
   */
  UPGRADE_SUMMARY("upgrade_summary");

  /**
   * The root JSON element.
   */
  private final String m_root;

  /**
   * Constructor.
   *
   * @param root  the root JSON element which the structured data is stored under.
   */
  private StructuredOutputType(String root) {
    m_root = root;
  }

  /**
   * Gets the root of the JSON for a specific structured output type.
   */
  public String getRoot() {
    return m_root;
  }
}
