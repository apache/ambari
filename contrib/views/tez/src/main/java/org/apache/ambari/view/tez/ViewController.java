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

package org.apache.ambari.view.tez;

import com.google.inject.ImplementedBy;

/**
 * Interface of controller to handle requests for the tez View.
 */
@ImplementedBy(ViewControllerImpl.class)
public interface ViewController {

  public static final String PARAM_YARN_ATS_URL = "yarn.timeline-server.url";
  public static final String PARAM_YARN_RESOURCEMANAGER_URL = "yarn.resourcemanager.url";
  public static final String PARAM_YARN_PROTOCOL = "yarn.protocol";

  /**
   * @return Get the properties that any user is allowed to see, even non-admin users.
   */
  public ViewStatus getViewStatus();

  /**
   *
   * @return The Active Application timeline server URL. Though, there is currently no
   * HA in ATS, the ATS Url that is returned is considered as the Active one.
   */
  String getActiveATSUrl();

  /**
   * @return The active resource manager URL.
   */
  String getActiveRMUrl();

  /**
   * @return The protocol used by YARN daemons.
   */
  String getYARNProtocol();
}
