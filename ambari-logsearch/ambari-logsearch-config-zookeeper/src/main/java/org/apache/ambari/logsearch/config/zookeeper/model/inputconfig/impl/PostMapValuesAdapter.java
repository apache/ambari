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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldDescriptor;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

public class PostMapValuesAdapter implements JsonDeserializer<List<PostMapValuesImpl>>, JsonSerializer<List<PostMapValuesImpl>> {
  @Override
  public List<PostMapValuesImpl> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
    List<PostMapValuesImpl> vals = new ArrayList<>();
    if (json.isJsonArray()) {
      for (JsonElement e : json.getAsJsonArray()) {
        vals.add(createPostMapValues(e, context));
      }
    } else if (json.isJsonObject()) {
      vals.add(createPostMapValues(json, context));
    } else {
      throw new RuntimeException("Unexpected JSON type: " + json.getClass());
    }
    return vals;
  }

  private PostMapValuesImpl createPostMapValues(JsonElement e, JsonDeserializationContext context) {
    List<MapFieldDescriptor> mappers = new ArrayList<>();
    for (Map.Entry<String, JsonElement> m : e.getAsJsonObject().entrySet()) {
      switch (m.getKey()) {
        case "map_date":
          mappers.add((MapDateDescriptorImpl)context.deserialize(m.getValue(), MapDateDescriptorImpl.class));
          break;
        case "map_fieldcopy":
          mappers.add((MapFieldCopyDescriptorImpl)context.deserialize(m.getValue(), MapFieldCopyDescriptorImpl.class));
          break;
        case "map_fieldname":
          mappers.add((MapFieldNameDescriptorImpl)context.deserialize(m.getValue(), MapFieldNameDescriptorImpl.class));
          break;
        case "map_fieldvalue":
          mappers.add((MapFieldValueDescriptorImpl)context.deserialize(m.getValue(), MapFieldValueDescriptorImpl.class));
          break;
        case "map_anonymize":
          mappers.add((MapAnonymizeDescriptorImpl)context.deserialize(m.getValue(), MapAnonymizeDescriptorImpl.class));
          break;
        default:
          System.out.println("Unknown key: " + m.getKey());
      }
    }
    
    PostMapValuesImpl postMapValues = new PostMapValuesImpl();
    postMapValues.setMappers(mappers);
    return postMapValues;
  }

  @Override
  public JsonElement serialize(List<PostMapValuesImpl> src, Type typeOfSrc, JsonSerializationContext context) {
    if (src.size() == 1) {
      return createMapperObject(src.get(0), context);
    } else {
      JsonArray jsonArray = new JsonArray();
      for (PostMapValuesImpl postMapValues : src) {
        jsonArray.add(createMapperObject(postMapValues, context));
      }
      return jsonArray;
    }
  }

  private JsonElement createMapperObject(PostMapValuesImpl postMapValues, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();
    for (MapFieldDescriptor m : postMapValues.getMappers()) {
      jsonObject.add(((MapFieldDescriptorImpl)m).getJsonName(), context.serialize(m));
    }
    return jsonObject;
  }
}
