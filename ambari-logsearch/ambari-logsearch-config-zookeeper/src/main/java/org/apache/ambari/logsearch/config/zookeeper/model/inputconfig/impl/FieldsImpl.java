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

import java.util.Set;

import org.apache.ambari.logsearch.config.api.ShipperConfigElementDescription;
import org.apache.ambari.logsearch.config.api.ShipperConfigTypeDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.Fields;

import com.google.gson.annotations.Expose;

@ShipperConfigTypeDescription(
    name = "Fields",
    description = "Describes a the fields which's value should be met in order to match a filter to an input element.\n" +
                  "\n" +
                  "It has the following attributes:"
  )
public class FieldsImpl implements Fields {
  @ShipperConfigElementDescription(
    path = "/filter/[]/conditions/fields/type",
    type = "list of strings",
    description = "The acceptable values for the type field in the input element.",
    examples = {"ambari_server", "\"spark_jobhistory_server\", \"spark_thriftserver\", \"livy_server\""}
  )
  @Expose
  private Set<String> type;

  public Set<String> getType() {
    return type;
  }

  public void setType(Set<String> type) {
    this.type = type;
  }
}
