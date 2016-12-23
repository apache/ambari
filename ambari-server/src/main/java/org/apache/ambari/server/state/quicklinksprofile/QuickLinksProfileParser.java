/*
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

package org.apache.ambari.server.state.quicklinksprofile;

import java.io.IOException;
import java.net.URL;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.deser.std.StdDeserializer;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.node.ObjectNode;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

/**
 * Loads and parses JSON quicklink profiles.
 */
public class QuickLinksProfileParser {
  private final ObjectMapper mapper = new ObjectMapper();

  public QuickLinksProfileParser() {
    SimpleModule module =
        new SimpleModule("Quick Links Parser", new Version(1, 0, 0, null));
    module.addDeserializer(Filter.class, new QuickLinksFilterDeserializer());
    mapper.registerModule(module);
  }


  public QuickLinksProfile parse(byte[] input) throws IOException {
    return mapper.readValue(input, QuickLinksProfile.class);
  }

  public QuickLinksProfile parse(URL url) throws IOException {
    return parse(Resources.toByteArray(url));
  }
}

/**
 * Custom deserializer is needed to handle filter polymorphism.
 */
class QuickLinksFilterDeserializer extends StdDeserializer<Filter> {
  private static final String PARSE_ERROR_MESSAGE =
      "A filter is not allowed to declare both link_name and link_attribute at the same time.";

  QuickLinksFilterDeserializer() {
    super(Filter.class);
  }

  /**
   * Filter polymorphism is handled here. If a filter object in the JSON document has:
   * <ul>
   *   <li>a {@code link_attribute} field, it will parsed as {@link LinkAttributeFilter}</li>
   *   <li>a {@code link_name} field, it will be parsed as {@link LinkNameFilter}</li>
   *   <li>both {@code link_attribute} and {@code link_name}, it will throw a {@link JsonParseException}</li>
   *   <li>neither of the above fields, it will be parsed as {@link AcceptAllFilter}</li>
   * </ul>
   *
   * @throws JsonParseException if ambiguous filter definitions are found, or any JSON syntax error.
   */
  @Override
  public Filter deserialize (JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
    ObjectMapper mapper = (ObjectMapper) parser.getCodec();
    ObjectNode root = (ObjectNode) mapper.readTree(parser);
    Class<? extends Filter> filterClass = null;
    for (String fieldName: ImmutableList.copyOf(root.getFieldNames())) {
      switch(fieldName) {
        case LinkAttributeFilter.LINK_ATTRIBUTE:
          if (null != filterClass) {
            throw new JsonParseException(PARSE_ERROR_MESSAGE, parser.getCurrentLocation());
          }
          filterClass = LinkAttributeFilter.class;
          break;
        case LinkNameFilter.LINK_NAME:
          if (null != filterClass) {
            throw new JsonParseException(PARSE_ERROR_MESSAGE, parser.getCurrentLocation());
          }
          filterClass = LinkNameFilter.class;
          break;
      }
    }
    if (null == filterClass) {
      filterClass = AcceptAllFilter.class;
    }
    return mapper.readValue(root, filterClass);
  }
}