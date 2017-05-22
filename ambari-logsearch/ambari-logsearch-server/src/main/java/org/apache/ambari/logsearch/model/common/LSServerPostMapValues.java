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

import org.apache.ambari.logsearch.config.api.model.inputconfig.MapDateDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldCopyDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldNameDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldValueDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.PostMapValues;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonSerialize(using = LSServerPostMapValuesSerializer.class)
public class LSServerPostMapValues {
  private List<LSServerMapField> mappers;
  
  public LSServerPostMapValues(PostMapValues pmv) {
    mappers = new ArrayList<>();
    for (MapFieldDescriptor mapFieldDescriptor : pmv.getMappers()) {
      if (mapFieldDescriptor instanceof MapDateDescriptor) {
        mappers.add(new LSServerMapDate((MapDateDescriptor)mapFieldDescriptor));
      } else if (mapFieldDescriptor instanceof MapFieldCopyDescriptor) {
        mappers.add(new LSServerMapFieldCopy((MapFieldCopyDescriptor)mapFieldDescriptor));
      } else if (mapFieldDescriptor instanceof MapFieldNameDescriptor) {
        mappers.add(new LSServerMapFieldName((MapFieldNameDescriptor)mapFieldDescriptor));
      } else if (mapFieldDescriptor instanceof MapFieldValueDescriptor) {
        mappers.add(new LSServerMapFieldValue((MapFieldValueDescriptor)mapFieldDescriptor));
      }
    }
  }

  public List<LSServerMapField> getMappers() {
    return mappers;
  }

  public void setMappers(List<LSServerMapField> mappers) {
    this.mappers = mappers;
  }
}
