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

package org.apache.hms.common.entity.action;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Generic scripting action for describing parameters for running 
 * arbitrary unix command on a node.
 *
 */
@XmlRootElement
@XmlType(propOrder = { "script", "parameters" })
public class ScriptAction extends Action {
  @XmlElement
  private String script;
  @XmlElement(name="parameters")
  private String[] parameters;
  
  public String getScript() {
    return script;
  }
  
  public String[] getParameters() {
    return parameters;
  }
  
  public void setScript(String script) {
    this.script =script;
  }
  
  public void setParameters(String[] parameters) {
    this.parameters = parameters;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(super.toString());
    sb.append(", script=");
    sb.append(script);
    sb.append(", parameters=");
    if (parameters != null) {
      for (String p : parameters) {
        sb.append(p);
        sb.append(" ");
      }
    }
    if (role != null) {
      sb.append(", role=");
      sb.append(role);
    }
    return sb.toString();
  }
}
