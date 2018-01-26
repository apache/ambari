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

package org.apache.ambari.server.state;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


/**
 * Represents info about command script
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CommandScriptDefinition {


  private String script = null;


  private Type scriptType = Type.PYTHON;

  /**
   * Timeout is given in seconds. Default value of 0 is used if not
   * overridden at xml file
   */
  private int timeout = 0;

  private String clientComponentType = null;


  public String getScript() {
    return script;
  }

  public Type getScriptType() {
    return scriptType;
  }

  public int getTimeout() {
    return timeout;
  }

  public String getClientComponentType() {
    return clientComponentType;
  }

  public enum Type {
    PYTHON
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (! (obj instanceof CommandScriptDefinition)) {
      return false;
    }

    CommandScriptDefinition rhs = (CommandScriptDefinition) obj;
    return new EqualsBuilder().
            append(script, rhs.script).
            append(scriptType, rhs.scriptType).
            append(timeout, rhs.timeout).
            append(clientComponentType, rhs.clientComponentType).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 31).
            append(script).
            append(scriptType).
            append(timeout).
            append(clientComponentType).toHashCode();
  }
}
