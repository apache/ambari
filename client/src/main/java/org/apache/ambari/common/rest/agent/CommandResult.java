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

package org.apache.ambari.common.rest.agent;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * Data model for Ambari Agent to report the execution result of the command
 * to Ambari controller.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
public class CommandResult {
  public CommandResult() {
  }
  
  public CommandResult(int exitCode, String stdout, String stderr) {
    this.exitCode = exitCode;
    this.stdout = stdout;
    this.stderr = stderr;
  }
  
  @XmlElement
  private int exitCode;
  @XmlElement
  private String stdout;
  @XmlElement
  private String stderr;

  public int getExitCode() {
    return exitCode;
  }
  
  public String getStdOut() {
    return this.stdout;
  }
  
  public String getStdErr() {
    return this.stderr;
  }
  
  public void setExitCode(int exitCode) {
    this.exitCode = exitCode;
  }
  
  public void setStdOut(String stdout) {
    this.stdout = stdout;
  }
  
  public void setStdErr(String stderr) {
    this.stderr = stderr;
  }
}
