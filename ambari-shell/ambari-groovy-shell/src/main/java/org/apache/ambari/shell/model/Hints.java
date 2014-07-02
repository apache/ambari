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
package org.apache.ambari.shell.model;

/**
 * Provides some guidance's to the user, what he/she can follow.
 */
public enum Hints {

  /**
   * Hint for adding blueprints.
   */
  ADD_BLUEPRINT("Add a blueprint with the 'blueprint add' or add the default blueprints with the 'blueprint defaults' command."),

  /**
   * Hint for start building a cluster.
   */
  BUILD_CLUSTER("Start building a cluster with the 'cluster build' command using a previously added blueprint."),

  /**
   * Hint for start assigning hosts to host groups in cluster build phase.
   */
  ASSIGN_HOSTS("Assign hosts to different host groups with the 'cluster assign' command."),

  /**
   * Hint for create a cluster from the assigned hosts.
   */
  CREATE_CLUSTER("Create the cluster with the 'cluster create' command or use the 'cluster reset' command and start over."),

  /**
   * Hint for check the cluster creation result.
   */
  PROGRESS("See the install progress with the 'tasks' command.");

  private final String message;

  private Hints(String message) {
    this.message = message;
  }

  public String message() {
    return message;
  }
}
