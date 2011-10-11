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

package org.apache.ambari.common.rest.entities.agent;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ConfigFile {
  
  public ConfigFile() {  
  }
  
  public ConfigFile(String owner, String group, String permission, 
      String path, String umask, String data) {
    this.owner = owner;
    this.group = group;
    this.permission = permission;
    this.path = path;
    this.umask = umask;
    this.data = data;
  }
  
  @XmlElement
  private String data;
  @XmlElement
  private String umask;
  @XmlElement
  private String path;
  @XmlElement
  private String owner;
  @XmlElement
  private String group;
  @XmlElement
  private String permission;
  
  public String getData() {
    return data;
  }
  
  public String getUmask() {
    return umask;
  }
  
  public String getPath() {
    return path;
  }
  
  public String getOwner() {
    return owner;
  }
  
  public String getGroup() {
    return group;
  }
  
  public String getPermission() {
    return permission;
  }
  
  public void setData(String data) {
    this.data = data;
  }
  
  public void setUmask(String umask) {
    this.umask = umask;
  }
  
  public void setPath(String path) {
    this.path = path;
  }
  
  public void setOwner(String owner) {
    this.owner = owner;
  }
  
  public void setGroup(String group) {
    this.group = group;
  }
  
  public void setPermission(String permission) {
    this.permission = permission;
  }
}
