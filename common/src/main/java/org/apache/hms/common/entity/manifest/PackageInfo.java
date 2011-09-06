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

package org.apache.hms.common.entity.manifest;

import javax.xml.bind.annotation.XmlElement;
import org.apache.hms.common.entity.RestSource;

public class PackageInfo extends RestSource {
  @XmlElement
  private String name;
  @XmlElement
  private String[] relocate;
  
  public String getName() {
    return name;
  }
  
  public String[] getRelocate() {
    return relocate;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public void setRelocate(String[] relocate) {
    this.relocate = relocate;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("package: ");
    sb.append(name);
    return sb.toString();
  }
  
  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    return name.equals(((PackageInfo) obj).getName());
  }
  
  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
