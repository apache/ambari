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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * Used to represent an execution that should occur on an agent.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="execute")
public class ExecuteTask extends Task {

  @XmlTransient
  private Task.Type type = Task.Type.EXECUTE;

  /**
   * Command to run under normal conditions.
   */
  @XmlElement(name="command")
  public String command;

  /**
   * Run the command only if this condition is first met.
   */
  @XmlElement(name="first")
  public String first;

  /**
   * Run the command unless this condition is met.
   */
  @XmlElement(name="unless")
  public String unless;

  /**
   * Command to run if a failure occurs.
   */
  @XmlElement(name="onfailure")
  public String onfailure;

  /**
   * Run the command up to x times, until it succeeds.
   */
  @XmlElement(name="upto")
  public String upto;

  /**
   * If "until" is specified, then sleep this many seconds between attempts.
   */
  @XmlElement(name="every")
  public String every;

  /**
   * Comma delimited list of return codes to ignore
   */
  @XmlElement(name="ignore")
  public String ignore;

  @Override
  public Task.Type getType() {
    return type;
  }
}