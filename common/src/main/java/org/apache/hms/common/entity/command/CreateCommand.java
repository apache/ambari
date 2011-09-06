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

package org.apache.hms.common.entity.command;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.hms.common.entity.manifest.ClusterManifest;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD) 
@XmlType(name="", propOrder = {})
public class CreateCommand extends Command {
  @XmlElement
  protected String clusterName;
  @XmlElement
  protected List<String> hosts;
  @XmlElement
  protected String[] packages;

  public CreateCommand() {
    this.cmd = CmdType.CREATE;
  }
  
  public CreateCommand(boolean dryRun, String clusterName, List<String> hosts, String[] packages) {
    this.cmd = CmdType.CREATE;
    this.dryRun = dryRun;
    this.clusterName = clusterName;
    this.hosts = hosts;
    this.packages = packages;
  }
  
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }
  
  public void setHosts(List<String> hosts) {
    this.hosts = hosts;
  }
  
  public void setPackages(String[] packages) {
    this.packages = packages;
  }
  
  public String getClusterName() {
    return clusterName;
  }
  
  public List<String> getHosts() {
    return hosts;
  }
  
  public String[] getPackages() {
    return packages;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(super.toString());
    sb.append(", cluster-name=");
    sb.append(clusterName);
    sb.append(", packages=");
    if (packages != null) {
      for (String p : packages) {
        sb.append(p);
        sb.append(" ");
      }
    }
    sb.append(", hosts=");
    if (hosts != null) {
      for (String s : hosts) {
        sb.append(s);
        sb.append(" ");
      }
    }
    return sb.toString();
  }
}
