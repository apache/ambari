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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .loadsimulator.util;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import java.io.IOException;

/**
 * Small wrapper that configures the ObjectMapper with some defaults.
 */
public class Json {
  private ObjectMapper myObjectMapper;

  /**
   * Creates default Json ObjectMapper that maps fields.
   */
  public Json() {
    this(false);
  }

  /**
   * Creates a Json ObjectMapper that maps fields and optionally pretty prints the
   * serialized objects.
   *
   * @param pretty a flag - if true the output will be pretty printed.
   */
  public Json(boolean pretty) {
    myObjectMapper = new ObjectMapper();
    myObjectMapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
    if (pretty) {
      myObjectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    }
  }

  public String serialize(Object o) throws IOException {
    return myObjectMapper.writeValueAsString(o);
  }

  public <T> T deserialize(String content, Class<T> paramClass) throws IOException {
    return myObjectMapper.readValue(content, paramClass);
  }

}
