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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;

/**
 *
 */
public class StageWrapper {

  private static Gson gson = new Gson();
  private String text;
  private Type type;

  private List<TaskWrapper> tasks;

  public StageWrapper(Type type, String text, TaskWrapper... tasks) {
    this(type, text, Arrays.asList(tasks));
  }

  public StageWrapper(Type type, String text, List<TaskWrapper> tasks) {
    this.type = type;
    this.text = text;
    this.tasks = tasks;
  }

  /**
   * Gets the hosts json.
   */
  public String getHostsJson() {
    return gson.toJson(getHosts());
  }

  /**
   * Gets the tasks json.
   */
  public String getTasksJson() {
    List<Task> realTasks = new ArrayList<Task>();
    for (TaskWrapper tw : tasks) {
      realTasks.addAll(tw.getTasks());
    }

    return gson.toJson(realTasks);
  }

  /**
   * @return the set of hosts for all tasks
   */
  public Set<String> getHosts() {
    Set<String> hosts = new HashSet<String>();
    for (TaskWrapper tw : tasks) {
      hosts.addAll(tw.getHosts());
    }

    return hosts;
  }

  /**
   * @return the wrapped tasks for this stage
   */
  public List<TaskWrapper> getTasks() {
    return tasks;
  }

  /**
   * @return the text for this stage
   */
  public String getText() {
    return text;
  }

  /**
   * @param text the new text for the stage
   */
  public void setText(String newText) {
    text = newText;
  }

  /**
   * Gets the type of stage.  All tasks defined for the stage execute this type.
   * @return the type
   */
  public Type getType() {
    return type;
  }

  /**
   * Indicates the type of wrapper.
   */
  public enum Type {
    SERVER_SIDE_ACTION,
    RESTART,
    RU_TASKS,
    SERVICE_CHECK
  }
}
