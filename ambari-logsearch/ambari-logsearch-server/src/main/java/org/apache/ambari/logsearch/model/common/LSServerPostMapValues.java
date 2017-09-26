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

import org.apache.ambari.logsearch.config.api.model.inputconfig.MapDateDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldCopyDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldNameDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldValueDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.PostMapValues;

import io.swagger.annotations.ApiModel;

@ApiModel
public class LSServerPostMapValues {
  @Valid
  @NotNull
  private List<LSServerMapField> mappers;
  
  public LSServerPostMapValues() {}
  
  public LSServerPostMapValues(PostMapValues pmv) {
    mappers = new ArrayList<>();
    for (MapFieldDescriptor mapFieldDescriptor : pmv.getMappers()) {
      mappers.add(convert(mapFieldDescriptor));
    }
  }

  private LSServerMapField convert(MapFieldDescriptor mapFieldDescriptor) {
    if (mapFieldDescriptor instanceof MapDateDescriptor) {
      return new LSServerMapDate((MapDateDescriptor)mapFieldDescriptor);
    } else if (mapFieldDescriptor instanceof MapFieldCopyDescriptor) {
      return new LSServerMapFieldCopy((MapFieldCopyDescriptor)mapFieldDescriptor);
    } else if (mapFieldDescriptor instanceof MapFieldNameDescriptor) {
      return new LSServerMapFieldName((MapFieldNameDescriptor)mapFieldDescriptor);
    } else if (mapFieldDescriptor instanceof MapFieldValueDescriptor) {
      return new LSServerMapFieldValue((MapFieldValueDescriptor)mapFieldDescriptor);
    }
    
    throw new IllegalArgumentException("Unknown mapper: " + mapFieldDescriptor.getClass());
  }
  
  public List<LSServerMapField> getMappers() {
    return mappers;
  }

  public void setMappers(List<LSServerMapField> mappers) {
    this.mappers = mappers;
  }
}
