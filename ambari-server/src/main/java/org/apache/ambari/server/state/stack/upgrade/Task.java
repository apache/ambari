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

import javax.xml.bind.annotation.XmlSeeAlso;


/**
 * Base class to identify the items that could possibly occur during an upgrade
 */
@XmlSeeAlso(value={ExecuteTask.class, ConfigureTask.class, ManualTask.class})
public abstract class Task {

  /**
   * @return the type of the task
   */
  public abstract Type getType();

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
     * Task that displays a message and must be confirmed before continuing
     */
    MANUAL;

    /**
     * @return {@code true} if the task is manual or automated.
     */
    public boolean isManual() {
      return this == MANUAL;
    }
  }
}
