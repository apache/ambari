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
package org.apache.ambari.server.state.stack;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ambari.server.state.ComponentInfo;

/**
 * Represents the <code>$SERVICE_HOME/metainfo.xml</code> file.
 */
@XmlRootElement(name="metainfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceMetainfoXml {
  private String user = null;
  private String comment = null;
  private String version = null;
  private boolean deleted = false;
  
  @XmlElementWrapper(name="components")
  @XmlElements(@XmlElement(name="component"))
  private List<ComponentInfo> components;

  @XmlElementWrapper(name="configuration-dependencies")
  @XmlElement(name="config-type")
  private List<String> configDependencies;
  /**
   * @return the user
   */
  public String getUser() {
    return user;
  }
  
  /**
   * @return the comment 
   */
  public String getComment() {
    return comment;
  }
  
  /**
   * @return the version
   */
  public String getVersion() {
    return version;
  }
  
  /**
   * @return the list of components for the service
   */
  public List<ComponentInfo> getComponents() {
    return components;
  }
  
  /**
   * @return if the service is to be deleted from the parent stack
   */
  public boolean isDeleted() {
    return deleted;
  }

  public List<String> getConfigDependencies() {
    return configDependencies;
  }

  public void setConfigDependencies(List<String> configDependencies) {
    this.configDependencies = configDependencies;
  }
}
