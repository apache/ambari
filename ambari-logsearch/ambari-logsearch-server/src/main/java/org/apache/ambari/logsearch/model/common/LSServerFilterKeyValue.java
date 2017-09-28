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

package org.apache.ambari.logsearch.model.common;

import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterKeyValueDescriptor;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;

@ApiModel
public class LSServerFilterKeyValue extends LSServerFilter {
  @JsonProperty("field_split")
  private String fieldSplit;

  @JsonProperty("value_split")
  private String valueSplit;

  @JsonProperty("value_borders")
  private String valueBorders;

  public LSServerFilterKeyValue() {}

  public LSServerFilterKeyValue(FilterDescriptor filterDescriptor) {
    super(filterDescriptor);
    FilterKeyValueDescriptor filterKeyValueDescriptor = (FilterKeyValueDescriptor)filterDescriptor;
    this.fieldSplit = filterKeyValueDescriptor.getFieldSplit();
    this.valueSplit = filterKeyValueDescriptor.getValueSplit();
    this.valueBorders = filterKeyValueDescriptor.getValueBorders();
  }

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
