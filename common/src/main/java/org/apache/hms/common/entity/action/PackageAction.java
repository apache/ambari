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

import org.apache.hms.common.entity.manifest.PackageInfo;

/**
 * Action describes what package to install or remove from a node.
 *
 */
@XmlRootElement
@XmlType(propOrder = { "packages", "dryRun" })
public class PackageAction extends Action {
  @XmlElement
  private PackageInfo[] packages;
  @XmlElement(name="dry-run")
  private boolean dryRun = false;
  
  public PackageInfo[] getPackages() {
    return packages;
  }
  
  public boolean getDryRun() {
    return dryRun;
  }

  public void setPackages(PackageInfo[] packages) {
    this.packages = packages;
  }
  
  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(super.toString());
    sb.append(", dry-run=");
    sb.append(dryRun);
    sb.append(", packages=");
    if (packages != null) {
      for(PackageInfo p : packages) {
        sb.append(p);
        sb.append(" ");
      }
    }
    return sb.toString();
  }
}
