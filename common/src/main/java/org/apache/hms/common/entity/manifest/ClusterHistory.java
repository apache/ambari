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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.apache.hms.common.entity.RestSource;
import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@history")
public class ClusterHistory extends RestSource {
  @XmlElement
  private List<ClusterManifest> history;
  
  public List<ClusterManifest> getHistory() {
    return this.history;
  }
  
  public void setHistory(ArrayList<ClusterManifest> history) {
    this.history = history;
  }
  
  public void add(ClusterManifest cm) {
    if(history==null) {
      history = new ArrayList<ClusterManifest>();
    }
    history.add(cm);
  }
}
