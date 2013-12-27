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

package org.apache.ambari.server.state;

import javax.xml.bind.annotation.*;

import org.apache.ambari.server.controller.StackServiceComponentResponse;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ComponentInfo {
  private String name;
  private String category;
  private boolean deleted;

  /**
  * Added at schema ver 2
  */
  private CommandScriptDefinition commandScript;

  /**
   * Added at schema ver 2
   */
  @XmlElementWrapper(name="customCommands")
  @XmlElements(@XmlElement(name="customCommand"))
  private List<CustomCommandDefinition> customCommands;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public boolean isClient() {
    return "CLIENT".equals(category);
  }

  public boolean isMaster() {
    return "MASTER".equals(category);
  }

  public StackServiceComponentResponse convertToResponse() {
    return new StackServiceComponentResponse(getName(), getCategory(), isClient(), isMaster());
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public CommandScriptDefinition getCommandScript() {
    return commandScript;
  }

  public List<CustomCommandDefinition> getCustomCommands() {
    if (customCommands == null) {
      customCommands = new ArrayList<CustomCommandDefinition>();
    }
    return customCommands;
  }

  public boolean isCustomCommand(String commandName) {
    if (customCommands != null && commandName != null) {
      for (CustomCommandDefinition cc: customCommands) {
        if (commandName.equals(cc.getName())){
          return true;
        }
      }
    }
    return false;
  }
}
