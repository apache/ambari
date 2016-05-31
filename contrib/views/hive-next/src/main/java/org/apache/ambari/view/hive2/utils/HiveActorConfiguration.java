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

package org.apache.ambari.view.hive2.utils;

import org.apache.ambari.view.ViewContext;

/**
 * This fetches the configuration for the actor system from ambari.properties
 */
public class HiveActorConfiguration {
  private static String DEFAULT_CONFIG = "default";
  private static String CONNECTION_PREFIX = "views.ambari.hive.";
  private static String CONNECTION_INACTIVITY_TIMEOUT_PATTERN = CONNECTION_PREFIX + "%s.connection.inactivity.timeout";
  private static String CONNECTION_TERMINATION_TIMEOUT_PATTERN = CONNECTION_PREFIX + "%s.connection.termination.timeout";
  private static String SYNC_QUERY_TIMEOUT_PATTERN = CONNECTION_PREFIX + "%s.sync.query.timeout";
  private static String RESULT_FETCH_TIMEOUT_PATTERN = CONNECTION_PREFIX + "%s.result.fetch.timeout";

  private final ViewContext context;

  public HiveActorConfiguration(ViewContext context) {
    this.context = context;
  }

  public long getInactivityTimeout(long defaultValue) {
    return Long.parseLong(getPropertiesFromContext(CONNECTION_INACTIVITY_TIMEOUT_PATTERN, String.valueOf(defaultValue)));
  }

  public long getTerminationTimeout(long defaultValue) {
    return Long.parseLong(getPropertiesFromContext(CONNECTION_TERMINATION_TIMEOUT_PATTERN, String.valueOf(defaultValue)));
  }

  public long getSyncQueryTimeout(long defaultValue) {
    return Long.parseLong(getPropertiesFromContext(SYNC_QUERY_TIMEOUT_PATTERN, String.valueOf(defaultValue)));
  }

  public long getResultFetchTimeout(long defaultValue) {
    return Long.parseLong(getPropertiesFromContext(RESULT_FETCH_TIMEOUT_PATTERN, String.valueOf(defaultValue)));
  }

  /**
   * Tries to get the specific configuration with the instance name. If not found then tries to
   * find the default set in ambari.properties. If not found then returns the default value passed
   * @param keyPattern Pattern used to generate ambari.properties key
   * @param defaultValue Returned when the value is not found in ambari.properties
   * @return value of the property
   */
  private String getPropertiesFromContext(String keyPattern, String defaultValue) {
    String value;
    value = context.getAmbariProperty(String.format(keyPattern, context.getInstanceName()));
    if(value == null) {
      value = context.getAmbariProperty(String.format(keyPattern, DEFAULT_CONFIG));
    }
    return value == null ? defaultValue: value;
  }
}
