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

public class LSServerInputDeserializer extends JsonDeserializer<List<LSServerInput>> {
  @Override
  public List<LSServerInput> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    ObjectCodec oc = jp.getCodec();
    JsonNode node = oc.readTree(jp);
    
    List<LSServerInput> inputs = new ArrayList<>();
    for (JsonNode inputNode : node) {
    
      String source = null;
      if (inputNode.get("source") != null) {
        source = inputNode.get("source").asText();
      } else {
        source = (inputNode.get("s3_access_key") != null || inputNode.get("s3_secret_key") != null) ? "s3_file" : "file";
      }
      
      switch (source) {
        case "file" :
          inputs.add(oc.treeToValue((TreeNode)inputNode, LSServerInputFile.class));
          break;
        case "s3_file" :
          inputs.add(oc.treeToValue((TreeNode)inputNode, LSServerInputS3File.class));
          break;
      }
    }
    
    return inputs;
  }
}
