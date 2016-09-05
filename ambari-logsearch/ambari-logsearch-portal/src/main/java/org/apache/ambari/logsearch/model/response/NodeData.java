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
package org.apache.ambari.logsearch.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Collection;

@ApiModel
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class NodeData {

  @ApiModelProperty
  private String name;

  @ApiModelProperty
  private String type;

  @ApiModelProperty
  private String value;

  @ApiModelProperty
  private Collection<NodeData> childs;

  @ApiModelProperty
  private Collection<NameValueData> logLevelCount;

  @ApiModelProperty
  @JsonProperty("isParent")
  private boolean parent;

  @ApiModelProperty
  @JsonProperty("isRoot")
  private boolean root;

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

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public boolean isRoot() {
    return root;
  }

  public void setRoot(boolean root) {
    this.root = root;
  }

  public Collection<NodeData> getChilds() {
    return childs;
  }

  public void setChilds(Collection<NodeData> childs) {
    this.childs = childs;
  }

  public Collection<NameValueData> getLogLevelCount() {
    return logLevelCount;
  }

  public void setLogLevelCount(Collection<NameValueData> logLevelCount) {
    this.logLevelCount = logLevelCount;
  }

  public boolean isParent() {
    return parent;
  }

  public void setParent(boolean parent) {
    this.parent = parent;
  }
}
