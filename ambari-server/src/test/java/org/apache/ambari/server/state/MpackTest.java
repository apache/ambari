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
package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

public class MpackTest {
  @Test
  public void testMpacks() {
    Mpack mpack = new Mpack();
    mpack.setName("name");
    mpack.setMpackId((long)100);
    mpack.setDescription("desc");
    mpack.setVersion("3.0");
    mpack.setMpackUri("abc.tar.gz");
    mpack.setRegistryId(new Long(100));

    Assert.assertEquals("name", mpack.getName());
    Assert.assertEquals(new Long(100), mpack.getMpackId());
    Assert.assertEquals("desc", mpack.getDescription());
    Assert.assertEquals("abc.tar.gz", mpack.getMpackUri());
    Assert.assertEquals(new Long(100), mpack.getRegistryId());

  }

  @Test
  public void testMpacksUsingGson() {
    String mpackJsonContents = "{\n" +
            "  \"name\" : \"hdf-ambari-mpack\",\n" +
            "  \"version\": \"3.0.0.0-111\",\n" +
            "  \"description\" : \"HDF 3.0.0 Ambari Management Pack\",\n" +
            "  \"prerequisites\": {\n" +
            "    \"min-ambari-version\" : \"3.0.0.0\"\n" +
            "  },\n" +
            "  \"packlets\": [\n" +
            "    {\n" +
            "      \"type\" : \"service-packlet\",\n" +
            "      \"name\" : \"NIFI\",\n" +
            "      \"version\" : \"1.2.0.0-123\",\n" +
            "      \"source_location\": \"packlets/NIFI-1.2.0.0-123.tar.gz\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\" : \"service-packlet\",\n" +
            "      \"name\" : \"STREAMLINE\",\n" +
            "      \"version\" : \"1.0.0.0-100\",\n" +
            "      \"source_location\": \"packlets/STREAMLINE-1.0.0.0-100.tar.gz\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    HashMap<String, String> expectedPrereq = new HashMap<>();
    expectedPrereq.put("min-ambari-version","3.0.0.0");
    ArrayList<Module> expectedPacklets = new ArrayList<>();
    Module nifi = new Module();
    //nifi.setType(.PackletType.SERVICE_PACKLET);
    nifi.setVersion("1.2.0.0-123");
    nifi.setDefinition("NIFI-1.2.0.0-123.tar.gz");
    nifi.setName("NIFI");
    Module streamline = new Module();
    streamline.setName("STREAMLINE");
    //streamline.setType(Module.PackletType.SERVICE_PACKLET);
    streamline.setDefinition("STREAMLINE-1.0.0.0-100.tar.gz");
    streamline.setVersion("1.0.0.0-100");
    expectedPacklets.add(nifi);
    expectedPacklets.add(streamline);

    Gson gson = new Gson();
    Mpack mpack = gson.fromJson(mpackJsonContents, Mpack.class);
    Assert.assertEquals("hdf-ambari-mpack", mpack.getName());
    Assert.assertEquals("3.0.0.0-111", mpack.getVersion());
    Assert.assertEquals("HDF 3.0.0 Ambari Management Pack", mpack.getDescription());
    Assert.assertEquals(expectedPrereq, mpack.getPrerequisites());
    Assert.assertEquals(expectedPacklets.toString(), mpack.getModules().toString());
  }

}
