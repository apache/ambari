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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonFilter;

@JsonFilter("propertiesfilter")
public class ServiceInfo {
  private String name;
    private String version;
    private String user;
    private String comment;
  private List<PropertyInfo> properties;
  private List<ComponentInfo> components;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public List<PropertyInfo> getProperties() {
    if (properties == null) properties = new ArrayList<PropertyInfo>();
    return properties;
  }

  public List<ComponentInfo> getComponents() {
    if (components == null) components = new ArrayList<ComponentInfo>();
    return components;
  }

  public boolean isClientOnlyService() {
    if (components == null || components.isEmpty()) {
      return false;
    }
    for (ComponentInfo compInfo : components) {
      if (!compInfo.isClient()) {
        return false;
      }
    }
    return true;
  }

  public ComponentInfo getClientComponent() {
    if (components == null || components.isEmpty()) {
      return null;
    }
    for (ComponentInfo compInfo : components) {
      if (compInfo.isClient()) {
        return compInfo;
      }
    }
    return components.get(0);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Service name:" + name + "\nversion:" + version +
        "\nuser:" + user + "\ncomment:" + comment);
//    if(properties != null)
//    for (PropertyInfo property : properties) {
//      sb.append("\tProperty name=" + property.getName() +
    //"\nproperty value=" + property.getValue() + "\ndescription=" + property.getDescription());
//    }
    for(ComponentInfo component : components){
      sb.append("\n\n\nComponent:\n");
      sb.append("name="+ component.getName());
      sb.append("\tcategory="+ component.getCategory() );
    }

    return sb.toString();
  }
}
