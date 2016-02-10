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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;


/**
 * Base class to identify the items that could possibly occur during an upgrade
 */
@XmlSeeAlso(value={ExecuteTask.class, ConfigureTask.class, ManualTask.class, RestartTask.class, StartTask.class, StopTask.class, ServerActionTask.class, ConfigureFunction.class})
public abstract class Task {

  /**
   * An optional brief description of what this task is doing.
   */
  @XmlElement(name = "summary")
  public String summary;

  /**
   * Whether the task needs to run sequentially, i.e., on its own stage.
   * If false, will be grouped with other tasks.
   */
  @XmlAttribute(name = "sequential")
  public boolean isSequential = false;

  /**
   * @return the type of the task
   */
  public abstract Type getType();

  /**
   * @return when a single Task is constructed, this is the type of stage it should belong to.
   */
  public abstract StageWrapper.Type getStageWrapperType();

  /**
   * @return a verb to display that describes the type of task, e.g., "executing".
   */
  public abstract String getActionVerb();

  /**
   * The scope for the task
   */
  @XmlElement(name = "scope")
  public UpgradeScope scope = UpgradeScope.ANY;


  @Override
  public String toString() {
    return getType().toString();
  }

  /**
   * Gets the summary of the task or {@code null}.
   *
   * @return the task summary or {@code null}.
   */
  public String getSummary() {
    return summary;
  }

  /**
   * Identifies the type of task.
   */
  public enum Type {
    /**
     * Task that is executed on a host.
     */
    EXECUTE,
    /**
     * Task that alters a configuration.
     */
    CONFIGURE,
    /**
     * Task that sets up the configuration for subsequent task
     */
    CONFIGURE_FUNCTION,
    /**
     * Task that displays a message and must be confirmed before continuing
     */
    MANUAL,
    /**
     * Task that is a restart command.
     */
    RESTART,
    /**
     * Task that is a start command.
     */
    START,
    /**
     * Task that is a stop command.
     */
    STOP,
    /**
     * Task that is a service check
     */
    SERVICE_CHECK,
    /**
     * Task meant to run against Ambari server.
     */
    SERVER_ACTION;

    /**
     * @return {@code true} if the task is manual or automated.
     */
    public boolean isServerAction() {
      return this == MANUAL || this == CONFIGURE || this == SERVER_ACTION;
    }

    /**
     * @return {@code true} if the task is a command type (as opposed to an action)
     */
    public boolean isCommand() {
      return this == RESTART || this == START || this == CONFIGURE_FUNCTION || this == STOP || this == SERVICE_CHECK;
    }
  }
}
