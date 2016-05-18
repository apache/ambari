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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.xml.bind.annotation.*;

/**
 * Represents the customCommand tag at service/component metainfo
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BulkCommandDefinition {

  private String displayName;
  private String masterComponent;

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName){
    this.displayName = displayName;
  }

  public String getMasterComponent() {
    return masterComponent;
  }

  public void setMasterComponent(String masterComponent){
    this.masterComponent = masterComponent;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (! (obj instanceof BulkCommandDefinition)) {
      return false;
    }

    BulkCommandDefinition rhs = (BulkCommandDefinition) obj;
    return new EqualsBuilder()
        .append(masterComponent, rhs.masterComponent)
        .append(displayName, rhs.displayName).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 31).append(displayName).append(masterComponent).toHashCode();
  }
}
