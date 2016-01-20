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
package org.apache.ambari.server.state.stack.upgrade;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aggregates all upgrade tasks for a HostComponent into one wrapper.
 */
public class TaskWrapper {

  private String service;
  private String component;
  private Set<String> hosts; // all the hosts that all the tasks must run
  private Map<String, String> params;
  private List<Task> tasks; // all the tasks defined for the hostcomponent

  /**
   * @param s the service name for the tasks
   * @param c the component name for the tasks
   * @param hosts the set of hosts that the tasks are for
   * @param tasks an array of tasks as a convenience
   */
  public TaskWrapper(String s, String c, Set<String> hosts, Task... tasks) {
    this(s, c, hosts, null, Arrays.asList(tasks));
  }
  
  /**
   * @param s the service name for the tasks
   * @param c the component name for the tasks
   * @param hosts the set of hosts that the tasks are for
   * @param params additional command parameters
   * @param tasks an array of tasks as a convenience
   */
  public TaskWrapper(String s, String c, Set<String> hosts, Map<String, String> params, Task... tasks) {
    this(s, c, hosts, params, Arrays.asList(tasks));
  }


  /**
   * @param s the service name for the tasks
   * @param c the component name for the tasks
   * @param hosts the set of hosts for the
   * @param tasks the list of tasks
   */
  public TaskWrapper(String s, String c, Set<String> hosts, Map<String, String> params, List<Task> tasks) {
    service = s;
    component = c;

    this.hosts = hosts;
    this.params = (params == null) ? new HashMap<String, String>() : params;
    this.tasks = tasks;
  }

  /**
   * @return the additional command parameters.
   */
  public Map<String, String> getParams() {
    return params;
  }

  /**
   * @return the tasks associated with this wrapper
   */
  public List<Task> getTasks() {
    return tasks;
  }

  /**
   * @return the hosts associated with this wrapper
   */
  public Set<String> getHosts() {
    return hosts;
  }


  @Override
  public String toString() {
    return service + ":" + component + ":" + tasks + ":" + hosts;
  }

  /**
   * @return the service name
   */
  public String getService() {
    return service;
  }

  /**
   * @return the component name
   */
  public String getComponent() {
    return component;
  }

  /**
   * @return true if any task is sequential, otherwise, return false.
   */
  public boolean isAnyTaskSequential() {
    for (Task t : getTasks()) {
      if (t.isSequential) {
        return true;
      }
    }

    return false;
  }

}
