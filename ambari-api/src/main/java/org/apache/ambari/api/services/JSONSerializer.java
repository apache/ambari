/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.api.services;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import java.io.IOException;

/**
 * Provides JSON serialization.
 */
public class JSONSerializer implements Serializer {
  /**
   * Serialize the result into JSON representation.
   *
   * @param result the object to serialize
   * @return the JSON representation of the result
   */
  @Override
  public Object serialize(Object result) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    mapper.configure(SerializationConfig.Feature.USE_ANNOTATIONS, true);
    try {

      return mapper.writeValueAsString(result);
    } catch (IOException e) {
      //todo
      throw new RuntimeException("Unable to serialize json: " + e, e);
    }
  }
}
