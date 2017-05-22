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

package org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl;

import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterKeyValueDescriptor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class FilterKeyValueDescriptorImpl extends FilterDescriptorImpl implements FilterKeyValueDescriptor {
  @Expose
  @SerializedName("field_split")
  private String fieldSplit;

  @Expose
  @SerializedName("value_split")
  private String valueSplit;

  @Expose
  @SerializedName("value_borders")
  private String valueBorders;

  public String getFieldSplit() {
    return fieldSplit;
  }

  public void setFieldSplit(String fieldSplit) {
    this.fieldSplit = fieldSplit;
  }

  public String getValueSplit() {
    return valueSplit;
  }

  public void setValueSplit(String valueSplit) {
    this.valueSplit = valueSplit;
  }

  public String getValueBorders() {
    return valueBorders;
  }

  public void setValueBorders(String valueBorders) {
    this.valueBorders = valueBorders;
  }
}
