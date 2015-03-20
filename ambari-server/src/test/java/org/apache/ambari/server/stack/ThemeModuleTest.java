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

package org.apache.ambari.server.stack;

import org.apache.ambari.server.state.ThemeInfo;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ThemeModuleTest {
  private String parentTheme = "{\n" +
    "  \"Theme\": {\n" +
    "    \"name\": \"default\",\n" +
    "    \"description\": \"Default theme for HDFS service\",\n" +
    "    \"subObject\": {\n" +
    "      \"primitiveInt\" : 10,\n" +
    "      \"primitiveStr\" : \"str\",\n" +
    "      \"array1\" : [1,2,3],\n" +
    "      \"array2\" : [1,2,3]\n" +
    "    }\n" +
    "  }\n" +
    "}";
  private String childTheme = "{\n" +
    "  \"Theme\": {\n" +
    "    \"description\": \"inherited theme\",\n" +
    "    \"subObject\": {\n" +
    "      \"primitiveInt\" : 12,\n" +
    "      \"primitiveStr\" : \"newStr\",\n" +
    "      \"array1\" : [1,2,3,4,5],\n" +
    "      \"array2\" : null,\n" +
    "      \"subObject2\" : {\"1\":\"1\"}\n" +
    "    }\n" +
    "  }\n" +
    "}";

  private ObjectMapper mapper = new ObjectMapper();
  TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};


  @Test
  public void testResolve() throws Exception {

    ThemeInfo parentInfo = new ThemeInfo();
    parentInfo.setThemeMap(mapper.<Map<String, Object>>readValue(parentTheme, typeRef));
    ThemeModule parentModule = new ThemeModule(parentInfo);

    ThemeInfo childInfo = new ThemeInfo();
    childInfo.setThemeMap(mapper.<Map<String, Object>>readValue(childTheme, typeRef));
    ThemeModule childModule = new ThemeModule(childInfo);

    childModule.resolve(parentModule, null, null);

    Map descriptionMap = ((Map) childInfo.getThemeMap().get("Theme"));
    Map subObjectMap = (Map) descriptionMap.get("subObject");

    assertTrue(StringUtils.equals((String) descriptionMap.get("description"), "inherited theme"));
    assertTrue(descriptionMap.containsKey("name"));
    assertFalse(subObjectMap.containsKey("array2"));
    assertEquals(subObjectMap.get("primitiveInt"), 12);
    assertEquals(subObjectMap.get("primitiveStr"), "newStr");
    assertEquals(((List)subObjectMap.get("array1")).size(), 5);


  }
}