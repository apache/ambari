/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logsearch.view;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class VNode {

  private String name;

  private String type;
  
  private String value;

  private boolean isRoot;

  private Collection<VNode> childs;

  private Collection<VNameValue> logLevelCount;

  private boolean isParent;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isRoot() {
    return isRoot;
  }

  public void setRoot(boolean isRoot) {
    this.isRoot = isRoot;
  }

  public Collection<VNode> getChilds() {
    return childs;
  }

  public void setChilds(Collection<VNode> childs) {
    this.childs = childs;
  }

  public boolean isParent() {
    return isParent;
  }

  public void setParent(boolean isParent) {
    this.isParent = isParent;
  }

  public Collection<VNameValue> getLogLevelCount() {
    return logLevelCount;
  }

  public void setLogLevelCount(Collection<VNameValue> logLevelCount) {
    this.logLevelCount = logLevelCount;
  }
  
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    String str = "VNode={";
    str += "name={" + name + "} ";
    str += "value={" + value + "} ";
    str += "type={" + type + "} ";
    str += "isRoot={" + isRoot + "} ";
    str += "isParent={" + isParent + "} ";
    str += "logLevelCount={" + logLevelCount + "} ";
    str += "childs={" + childs + "} ";
    str += "}";
    return str;
  }

}
