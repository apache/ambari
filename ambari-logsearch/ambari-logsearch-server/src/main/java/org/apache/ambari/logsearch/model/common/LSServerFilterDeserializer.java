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
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class LSServerFilterDeserializer extends JsonDeserializer<List<LSServerFilter>> {
  @Override
  public List<LSServerFilter> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    ObjectCodec oc = jp.getCodec();
    JsonNode node = oc.readTree(jp);
    
    List<LSServerFilter> filters = new ArrayList<>();
    for (JsonNode filterNode : node) {
      if (filterNode.get("filter") == null) {
        throw new IllegalArgumentException("Each filter element must have a field called 'filter' declaring it's type");
      }
      switch (filterNode.get("filter").asText()) {
        case "grok" :
          filters.add(oc.treeToValue((TreeNode)filterNode, LSServerFilterGrok.class));
          break;
        case "keyvalue" :
          filters.add(oc.treeToValue((TreeNode)filterNode, LSServerFilterKeyValue.class));
          break;
        case "json" :
          filters.add(oc.treeToValue((TreeNode)filterNode, LSServerFilterJson.class));
          break;
      }
    }
    
    return filters;
  }
}
