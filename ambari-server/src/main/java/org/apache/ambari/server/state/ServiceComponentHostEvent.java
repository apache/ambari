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

package org.apache.ambari.server.state;

import org.apache.ambari.server.state.fsm.event.AbstractEvent;
import org.apache.ambari.server.state.svccomphost.*;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Base class for all events that affect the ServiceComponentHost FSM
 */
public abstract class ServiceComponentHostEvent
    extends AbstractEvent<ServiceComponentHostEventType> {

  /**
   * ServiceComponent that this event relates to
   */
  private final String serviceComponentName;

  /**
   * Hostname of the Host that this event relates to
   */
  private final String hostName;

  /**
   * Time when the event was triggered
   */
  private final long opTimestamp;

  public ServiceComponentHostEvent(ServiceComponentHostEventType type,
      String serviceComponentName, String hostName, long opTimestamp) {
    super(type);
    this.serviceComponentName = serviceComponentName;
    this.hostName = hostName;
    this.opTimestamp = opTimestamp;
  }

  /**
   * @return the serviceComponentName
   */
  public String getServiceComponentName() {
    return serviceComponentName;
  }

  /**
   * @return the hostName
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * @return the opTimestamp
   */
  public long getOpTimestamp() {
    return opTimestamp;
  }

  @JsonCreator
  public static ServiceComponentHostEvent create(@JsonProperty("type") ServiceComponentHostEventType type,
                                                 @JsonProperty("serviceComponentName") String serviceComponentName,
                                                 @JsonProperty("hostName") String hostName, @JsonProperty("opTimestamp") long opTimestamp) {
    switch (type) {
      case HOST_SVCCOMP_INSTALL:
        return new ServiceComponentHostInstallEvent(serviceComponentName, hostName, opTimestamp);
      case HOST_SVCCOMP_OP_FAILED:
        return new ServiceComponentHostOpFailedEvent(serviceComponentName, hostName, opTimestamp);
      case HOST_SVCCOMP_OP_IN_PROGRESS:
        return new ServiceComponentHostOpInProgressEvent(serviceComponentName, hostName, opTimestamp);
      case HOST_SVCCOMP_OP_RESTART:
        return new ServiceComponentHostOpRestartedEvent(serviceComponentName, hostName, opTimestamp);
      case HOST_SVCCOMP_OP_SUCCEEDED:
        return new ServiceComponentHostOpSucceededEvent(serviceComponentName, hostName, opTimestamp);
      case HOST_SVCCOMP_START:
        return new ServiceComponentHostStartEvent(serviceComponentName, hostName, opTimestamp);
      case HOST_SVCCOMP_STOP:
        return new ServiceComponentHostStopEvent(serviceComponentName, hostName, opTimestamp);
      case HOST_SVCCOMP_UNINSTALL:
        return new ServiceComponentHostUninstallEvent(serviceComponentName, hostName, opTimestamp);
      case HOST_SVCCOMP_WIPEOUT:
        return new ServiceComponentHostWipeoutEvent(serviceComponentName, hostName, opTimestamp);
    }
    return null;
  }

}
