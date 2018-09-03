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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logsearch.web.filters;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.conf.global.LogSearchConfigState;

/**
 * Filter to decide whether the server is ready to serve requests which require Log Search configuration available.
 */
public class ConfigStateProvider implements StatusProvider {

  private static final String CONFIG_NOT_AVAILABLE = "Configuration is not available";
  private static final String CONFIG_API_DISABLED = "Configuration API is disabled";

  private final LogSearchConfigState logSearchConfigState;
  private final boolean enabled;
  
  public ConfigStateProvider(LogSearchConfigState logSearchConfigState, boolean enabled) {
    this.logSearchConfigState = logSearchConfigState;
    this.enabled = enabled;
  }

  @Override
  public StatusMessage getStatusMessage(String requestUri) {
    if (!enabled) {
      return StatusMessage.with(CONFLICT, CONFIG_API_DISABLED);
    }
    if (!logSearchConfigState.isLogSearchConfigAvailable()) {
      return StatusMessage.with(SERVICE_UNAVAILABLE, CONFIG_NOT_AVAILABLE);
    }

    return null;
  }
}
