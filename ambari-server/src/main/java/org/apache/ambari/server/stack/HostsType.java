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

package org.apache.ambari.server.stack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.ambari.server.state.ServiceComponentHost;

/**
 * Wrapper around a collection of hosts for components.  Some components
 * also have master/secondary designators.
 */
public class HostsType {

  /**
   * The master host, if any.
   */
  public String master = null;

  /**
   * The secondary host, if any.
   */
  public String secondary = null;

  /**
   * Ordered collection of hosts.  This represents all hosts where an upgrade
   * or downgrade must occur.  For an upgrade, all hosts are set.  For a downgrade,
   * this list may be a subset of all hosts where the downgrade MUST be executed.
   * That is to say, a downgrade only occurs where the current version is not
   * the target version.
   */
  public LinkedHashSet<String> hosts = new LinkedHashSet<>();

  /**
   * Unhealthy hosts are those which are explicitely put into maintenance mode.
   * If there is a host which is not heartbeating (or is generally unhealthy)
   * but not in maintenance mode, then the prerequisite upgrade checks will let
   * the administrator know that it must be put into maintenance mode before an
   * upgrade can begin.
   */
  public List<ServiceComponentHost> unhealthy = new ArrayList<>();

}
