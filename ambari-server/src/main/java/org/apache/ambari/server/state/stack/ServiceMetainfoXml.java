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

import java.util.Collection;
import java.util.HashSet;
import org.apache.ambari.server.state.ServiceInfo;

import javax.xml.bind.annotation.*;
import java.util.List;
import java.util.Set;
import org.apache.ambari.server.stack.Validable;

/**
 * Represents the <code>$SERVICE_HOME/metainfo.xml</code> file.
 * Schema version: v2
 * May contain multiple service definitions
 */
@XmlRootElement(name="metainfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceMetainfoXml implements Validable{

  private String schemaVersion;

  @XmlElementWrapper(name="services")
  @XmlElements(@XmlElement(name="service"))
  private List<ServiceInfo> services;
  
  @XmlTransient
  private boolean valid = true;

  /**
   * 
   * @return valid xml flag
   */
  @Override
  public boolean isValid() {
    return valid;
  }

  /**
   * 
   * @param valid set validity flag
   */
  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  @XmlTransient
  private Set<String> errorSet = new HashSet<String>();
  
  @Override
  public void setErrors(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection getErrors() {
    return errorSet;
  } 

  @Override
  public void setErrors(Collection error) {
    this.errorSet.addAll(error);
  }  
  
  /**
   * @return the list of services for the metainfo file
   */
  public List<ServiceInfo> getServices() {
    return services;
  }
  
  public String getSchemaVersion() {
    return schemaVersion;
  }

  public void setSchemaVersion(String schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  
}
