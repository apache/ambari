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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class LSServerPostMapValuesListDeserializer extends JsonDeserializer<LSServerPostMapValuesList> {
  @Override
  public LSServerPostMapValuesList deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    ObjectCodec oc = jp.getCodec();
    JsonNode node = oc.readTree(jp);
    
    List<LSServerPostMapValues> mappersList = new ArrayList<>();
    for (JsonNode childNode : node) {
      List<LSServerMapField> mappers = new ArrayList<>();
      for (Iterator<Map.Entry<String, JsonNode>> i = childNode.fields(); i.hasNext();) {
        Map.Entry<String, JsonNode> mapperData = i.next();
        String mapperType = mapperData.getKey();
        JsonNode mapperProperties = mapperData.getValue();
        switch (mapperType) {
          case "map_date" :
            LSServerMapDate mapDate = oc.treeToValue((TreeNode)mapperProperties, LSServerMapDate.class);
            mappers.add(mapDate);
            break;
          case "map_fieldname" :
            LSServerMapFieldName mapFieldName = oc.treeToValue((TreeNode)mapperProperties, LSServerMapFieldName.class);
            mappers.add(mapFieldName);
            break;
          case "map_fieldvalue" :
            LSServerMapFieldValue mapFieldValue = oc.treeToValue((TreeNode)mapperProperties, LSServerMapFieldValue.class);
            mappers.add(mapFieldValue);
            break;
          case "map_fieldcopy" :
            LSServerMapFieldCopy mapFieldCopy = oc.treeToValue((TreeNode)mapperProperties, LSServerMapFieldCopy.class);
            mappers.add(mapFieldCopy);
            break;
          case "map_anonymize" :
            LSServerMapFieldAnonymize mapAnonyimize = oc.treeToValue((TreeNode)mapperProperties, LSServerMapFieldAnonymize.class);
            mappers.add(mapAnonyimize);
            break;
        }
      }
      
      LSServerPostMapValues lsServerPostMapValues = new LSServerPostMapValues();
      lsServerPostMapValues.setMappers(mappers);
      mappersList.add(lsServerPostMapValues);
    }
    
    LSServerPostMapValuesList lsServerPostMapValuesList = new LSServerPostMapValuesList();
    lsServerPostMapValuesList.setMappersList(mappersList);
    return lsServerPostMapValuesList;
  }
}
