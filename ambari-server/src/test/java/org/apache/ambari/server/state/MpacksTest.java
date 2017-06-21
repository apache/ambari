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

import com.google.gson.Gson;
import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashMap;

public class MpacksTest {
  @Test
  public void testMpacks() {
    Mpacks mpacks = new Mpacks();
    mpacks.setName("name");
    mpacks.setMpackId((long)100);
    mpacks.setDescription("desc");
    mpacks.setVersion("3.0");
    mpacks.setMpacksUri("abc.tar.gz");
    mpacks.setRegistryId(new Long(100));

    Assert.assertEquals("name", mpacks.getName());
    Assert.assertEquals(new Long(100), mpacks.getMpackId());
    Assert.assertEquals("desc", mpacks.getDescription());
    Assert.assertEquals("abc.tar.gz", mpacks.getMpacksUri());
    Assert.assertEquals(new Long(100), mpacks.getRegistryId());

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
            "      \"source_dir\": \"packlets/NIFI-1.2.0.0-123.tar.gz\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\" : \"service-packlet\",\n" +
            "      \"name\" : \"STREAMLINE\",\n" +
            "      \"version\" : \"1.0.0.0-100\",\n" +
            "      \"source_dir\": \"packlets/STREAMLINE-1.0.0.0-100.tar.gz\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    HashMap<String, String> expectedPrereq = new HashMap<>();
    expectedPrereq.put("min-ambari-version","3.0.0.0");
    ArrayList<Packlet> expectedPacklets = new ArrayList<>();
    Packlet nifi = new Packlet();
    nifi.setType("service-packlet");
    nifi.setVersion("1.2.0.0-123");
    nifi.setSourceDir("packlets/NIFI-1.2.0.0-123.tar.gz");
    nifi.setName("NIFI");
    Packlet streamline = new Packlet();
    streamline.setName("STREAMLINE");
    streamline.setType("service-packlet");
    streamline.setSourceDir("packlets/STREAMLINE-1.0.0.0-100.tar.gz");
    streamline.setVersion("1.0.0.0-100");
    expectedPacklets.add(nifi);
    expectedPacklets.add(streamline);

    Gson gson = new Gson();
    Mpacks mpacks = gson.fromJson(mpackJsonContents, Mpacks.class);
    Assert.assertEquals("hdf-ambari-mpack",mpacks.getName());
    Assert.assertEquals("3.0.0.0-111", mpacks.getVersion());
    Assert.assertEquals("HDF 3.0.0 Ambari Management Pack",mpacks.getDescription());
    Assert.assertEquals(expectedPrereq, mpacks.getPrerequisites());
    Assert.assertEquals(expectedPacklets.toString(), mpacks.getPacklets().toString());
  }

}
