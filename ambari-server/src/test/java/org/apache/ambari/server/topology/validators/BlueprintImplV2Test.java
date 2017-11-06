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
package org.apache.ambari.server.topology.validators;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.StackV2;
import org.apache.ambari.server.controller.StackV2Factory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.BlueprintImplV2;
import org.apache.ambari.server.topology.BlueprintV2Factory;
import org.apache.ambari.server.topology.HostGroupV2;
import org.apache.ambari.server.topology.HostGroupV2Impl;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

public class BlueprintImplV2Test {

  static String BLUEPRINTV2_JSON;
  static String BLUEPRINTV2_2_JSON;


  @BeforeClass
  public static void setUpClass() throws Exception {
    BLUEPRINTV2_JSON = Resources.toString(Resources.getResource("blueprintv2/blueprintv2.json"), Charsets.UTF_8);
    BLUEPRINTV2_2_JSON = Resources.toString(Resources.getResource("blueprintv2/blueprintv2_2.json"), Charsets.UTF_8);
  }

  @Test
  public void testSerialization() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule("CustomModel", Version.unknownVersion());
    SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
    resolver.addMapping(HostGroupV2.class, HostGroupV2Impl.class);
    module.setAbstractTypes(resolver);
    mapper.registerModule(module);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    BlueprintImplV2 bp = mapper.readValue(BLUEPRINTV2_JSON, BlueprintImplV2.class);
    bp.postDeserialization();
    // -- add stack --
    StackV2 hdpCore = new StackV2("HDPCORE", "3.0.0", "3.0.0.0-1", new HashMap<>(), new HashMap<>(), new HashMap<>(),
      new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    StackV2 analytics = new StackV2("ANALYTICS", "1.0.0", "1.0.0.0-1", new HashMap<>(), new HashMap<>(), new HashMap<>(),
      new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    bp.setStacks(ImmutableMap.of(new StackId("HDPCORE", "3.0.0"), hdpCore, new StackId("ANALYTICS", "1.0.0"), analytics));
    // ---------------
    String bpJson = mapper.writeValueAsString(bp);
    System.out.println(bpJson);
    System.out.println("\n\n====================================================================================\n\n");
    Map<String, Object> map = mapper.readValue(BLUEPRINTV2_JSON, HashMap.class);
    System.out.println(map);
    System.out.println("\n\n====================================================================================\n\n");
    String bpJson2 = mapper.writeValueAsString(map);
    System.out.println(bpJson2);
    System.out.println("\n\n====================================================================================\n\n");
    BlueprintImplV2 bp2 = mapper.readValue(bpJson2, BlueprintImplV2.class);
    System.out.println(bp2);
  }

  @Test
  public void testSerialization2() throws Exception {
    StackV2 hdpCore = new StackV2("HDPCORE", "3.0.0", "3.0.0.0-1", new HashMap<>(), new HashMap<>(), new HashMap<>(),
      new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());    StackV2Factory stackFactory = mock(StackV2Factory.class);
    when(stackFactory.create(anyString(), anyString())).thenReturn(hdpCore);
    BlueprintV2Factory bpFactory = BlueprintV2Factory.create(stackFactory);
    bpFactory.setPrettyPrintJson(true);
    BlueprintImplV2 bp = (BlueprintImplV2)bpFactory.convertFromJson(BLUEPRINTV2_2_JSON);
    String bpSerialized = bpFactory.convertToJson(bp);
    System.out.println(bpSerialized);
    bp = (BlueprintImplV2)bpFactory.convertFromJson(bpSerialized);
  }


}