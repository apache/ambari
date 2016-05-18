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
package org.apache.ambari.shell.support;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class TableRendererTest {

  @Test
  public void testRenderMultiValueMap() throws IOException {
    Map<String, List<String>> map = new LinkedHashMap<>();
    map.put("ZOOKEEPER", Collections.singletonList("ZOOKEEPER_SERVER"));
    map.put("MAPREDUCE2", Collections.singletonList("HISTORYSERVER"));
    map.put("HDFS", Collections.singletonList("DATANODE"));
    assertEquals(IOUtils.toString(new FileInputStream(new File("src/test/resources/2columns"))),
      TableRenderer.renderMultiValueMap(map, "SERVICE", "COMPONENT"));
  }

  @Test
  public void testRenderMapValueMap() throws IOException {
    Map<String, Map<String, String>> map = new LinkedHashMap<>();
    map.put("ZOOKEEPER", Collections.singletonMap("ZOOKEEPER_SERVER", "INSTALLED"));
    map.put("MAPREDUCE2", Collections.singletonMap("HISTORYSERVER", "STARTED"));
    map.put("HDFS", Collections.singletonMap("DATANODE", "STARTED"));
    assertEquals(IOUtils.toString(new FileInputStream(new File("src/test/resources/3columns"))),
      TableRenderer.renderMapValueMap(map, "SERVICE", "COMPONENT", "STATE"));
  }
}
