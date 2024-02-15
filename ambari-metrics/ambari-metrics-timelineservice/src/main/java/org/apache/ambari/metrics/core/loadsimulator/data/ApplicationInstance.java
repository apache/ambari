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
package org.apache.ambari.metrics.core.loadsimulator.data;

/**
 * AppId is a helper class that encapsulates the common part of metrics message.
 * It contains hostName, appId and instanceId. It is immutable,
 * and it can not hold null values.
 */
public final class ApplicationInstance {

  private final transient String hostName;
  private final transient AppID appId;
  private final transient String instanceId;

  /**
   * @param hostname
   * @param appId
   * @param instanceId
   */
  public ApplicationInstance(String hostname, AppID appId, String instanceId) {
    if (hostname == null || appId == null || instanceId == null)
      throw new IllegalArgumentException("ApplicationInstance can not be " +
        "instantiated with null values");

    this.hostName = hostname;
    this.appId = appId;
    this.instanceId = instanceId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public AppID getAppId() {
    return appId;
  }

  public String getHostName() {
    return hostName;
  }

}
