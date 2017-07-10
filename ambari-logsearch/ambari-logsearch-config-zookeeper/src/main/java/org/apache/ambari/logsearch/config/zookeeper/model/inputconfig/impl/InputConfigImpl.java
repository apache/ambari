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

import java.util.List;

import org.apache.ambari.logsearch.config.api.ShipperConfigElementDescription;
import org.apache.ambari.logsearch.config.api.ShipperConfigTypeDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputConfig;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;

import com.google.gson.annotations.Expose;

@ShipperConfigTypeDescription(
  name = "Input Config",
  description = "The input configurations are stored in json files. Each of them are describing the processing of the log files of a service.\n" +
                "\n" +
                "The json contains two elements:"
)
public class InputConfigImpl implements InputConfig {
  @ShipperConfigElementDescription(
    path = "/input",
    type = "list of json objects",
    description = "A list of input descriptions"
  )
  @Expose
  private List<InputDescriptorImpl> input;

  @ShipperConfigElementDescription(
    path = "/filter",
    type = "list of json objects",
    description = "A list of filter descriptions"
  )
  @Expose
  private List<FilterDescriptorImpl> filter;

  @Override
  public List<? extends InputDescriptor> getInput() {
    return input;
  }

  public void setInput(List<InputDescriptorImpl> input) {
    this.input = input;
  }

  @Override
  public List<? extends FilterDescriptor> getFilter() {
    return filter;
  }

  public void setFilter(List<FilterDescriptorImpl> filter) {
    this.filter = filter;
  }
}
