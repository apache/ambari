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

import org.apache.ambari.logsearch.config.api.model.inputconfig.PostMapValues;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonSerialize(using = LSServerPostMapValuesListSerializer.class)
@JsonDeserialize(using = LSServerPostMapValuesListDeserializer.class)
public class LSServerPostMapValuesList {
  @Valid
  @NotNull
  private List<LSServerPostMapValues> mapperLists;
  
  public LSServerPostMapValuesList() {}
  
  public LSServerPostMapValuesList(List<? extends PostMapValues> list) {
    mapperLists = new ArrayList<>();
    for (PostMapValues postMapValues : list) {
      mapperLists.add(new LSServerPostMapValues(postMapValues));
    }
  }
  
  public List<LSServerPostMapValues> getMappersList() {
    return mapperLists;
  }

  public void setMappersList(List<LSServerPostMapValues> mapperLists) {
    this.mapperLists = mapperLists;
  }
}
