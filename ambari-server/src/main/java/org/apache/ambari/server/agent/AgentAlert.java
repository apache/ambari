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
package org.apache.ambari.server.agent;

import org.apache.ambari.server.state.AlertState;

/**
 * Represents an alert that originates from an Agent.
 */
public class AgentAlert {

  private String name = null;
  private AlertState state = null;
  private String instance = null;
  private String label = null;
  private String text = null;

  /**
   * Public constructor for use by JSON parsers.
   */
  public AgentAlert() {
  }
  
  /**
   * Constructor used for testing
   */
  AgentAlert(String alertName, AlertState alertState) {
    name = alertName;
    state = alertState;
  }
 
  /**
   * @return the label
   */
  public String getLabel() {
    return label;
  }
  
  /**
   * @return the text
   */
  public String getText() {
    return text;
  }
 
  /**
   * @return the state
   */
  public AlertState getState() {
    return state;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }
  
  /**
   * @return instance specific information
   */
  public String getInstance() {
    return instance;
  }
  
}
