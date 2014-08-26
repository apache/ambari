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
package org.apache.ambari.server.state.alert;

import org.apache.ambari.server.orm.dao.AlertsDAO;

/**
 * The {@link TargetType} enumeration is used to represent the built-in target
 * dispatch mechanisms that are supported internally. {@link AlertTarget}
 * instances may have other custom target types that are not listed here.
 */
public enum TargetType {
  /**
   * Alerts will be distributed via email.
   */
  EMAIL,

  /**
   * {@link AlertsDAO} will be distributed via SNMP.
   */
  SNMP;
}
