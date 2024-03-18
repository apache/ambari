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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class LSServerPostMapValuesListSerializer extends JsonSerializer<LSServerPostMapValuesList> {
  @Override
  public void serialize(LSServerPostMapValuesList value, JsonGenerator jgen, SerializerProvider provider)
      throws IOException, JsonProcessingException {
    jgen.writeStartArray();
    for (LSServerPostMapValues postMapValues : value.getMappersList()) {
      jgen.writeStartObject();
      for (LSServerMapField mapField : postMapValues.getMappers()) {
        jgen.writeObjectField(mapField.getName(), mapField);
      }
      jgen.writeEndObject();
    }
    
    jgen.writeEndArray();
  }
}
