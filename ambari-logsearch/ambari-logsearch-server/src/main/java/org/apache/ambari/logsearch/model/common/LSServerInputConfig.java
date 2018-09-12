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

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterGrokDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterJsonDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterKeyValueDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputConfig;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputFileBaseDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputS3FileDescriptor;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputSocketDescriptor;

@ApiModel
public class LSServerInputConfig {
  @Valid
  @NotNull
  @ApiModelProperty
  @JsonDeserialize(using = LSServerInputDeserializer.class)
  private List<LSServerInput> input;
  
  @Valid
  @NotNull
  @ApiModelProperty
  @JsonDeserialize(using = LSServerFilterDeserializer.class)
  private List<LSServerFilter> filter;
  
  public LSServerInputConfig() {}
  
  public LSServerInputConfig(InputConfig inputConfig) {
    input = new ArrayList<>();
    for (InputDescriptor inputDescriptor : inputConfig.getInput()) {
      if (inputDescriptor instanceof InputFileBaseDescriptor) {
        LSServerInput inputItem = new LSServerInputFile(inputDescriptor);
        input.add(inputItem);
      } else if (inputDescriptor instanceof InputS3FileDescriptor) {
        LSServerInput inputItem = new LSServerInputS3File(inputDescriptor);
        input.add(inputItem);
      } else if (inputDescriptor instanceof InputSocketDescriptor) {
        LSServerInput inputItem = new LSServerInputSocket(inputDescriptor);
        input.add(inputItem);
      }
    }
    
    filter = new ArrayList<>();
    for (FilterDescriptor filterDescriptor : inputConfig.getFilter()) {
      if (filterDescriptor instanceof FilterGrokDescriptor) {
        LSServerFilter filterItem = new LSServerFilterGrok(filterDescriptor);
        filter.add(filterItem);
      } else if (filterDescriptor instanceof FilterKeyValueDescriptor) {
        LSServerFilter filterItem = new LSServerFilterKeyValue(filterDescriptor);
        filter.add(filterItem);
      } else if (filterDescriptor instanceof FilterJsonDescriptor) {
        LSServerFilter filterItem = new LSServerFilterJson(filterDescriptor);
        filter.add(filterItem);
      }
    }
  }

  public List<LSServerInput> getInput() {
    return input;
  }

  public void setInput(List<LSServerInput> input) {
    this.input = input;
  }

  public List<LSServerFilter> getFilter() {
    return filter;
  }

  public void setFilter(List<LSServerFilter> filter) {
    this.filter = filter;
  }
}
