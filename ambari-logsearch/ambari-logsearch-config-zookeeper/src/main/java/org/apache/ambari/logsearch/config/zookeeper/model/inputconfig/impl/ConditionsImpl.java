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

import org.apache.ambari.logsearch.config.api.ShipperConfigElementDescription;
import org.apache.ambari.logsearch.config.api.ShipperConfigTypeDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.Conditions;

import com.google.gson.annotations.Expose;

@ShipperConfigTypeDescription(
  name = "Conditions",
  description = "Describes the conditions that should be met in order to match a filter to an input element.\n" +
                "\n" +
                "It has the following attributes:"
)
public class ConditionsImpl implements Conditions {
  @ShipperConfigElementDescription(
    path = "/filter/[]/conditions/fields",
    type = "json object",
    description = "The fields in the input element of which's value should be met."
  )
  @Expose
  private FieldsImpl fields;

  public FieldsImpl getFields() {
    return fields;
  }

  public void setFields(FieldsImpl fields) {
    this.fields = fields;
  }
}
