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
package org.apache.ambari.tools.hadoop;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ManagedDependencyClientTest {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String BINDING = "59646bd5-39eb-414e-a07e-f5469d369687";
  private static final String OPERATION = "43d3dad3-d0b0-4ba2-a9ae-2ccf7ab758f2";
  private static final String ROOT = "/apps/ambari-managed/hbase/" + BINDING + "/root";
  @Rule
  public TemporaryFolder directory = new TemporaryFolder();
  private RawLocalFileSystem fs;
  private JsonNode identity;

  @Before
  public void setup() throws Exception {
    File base = directory.getRoot();
    fs = new RawLocalFileSystem() {
      @Override
      public File pathToFile(Path path) {
        return new File(base, path.toUri().getPath().replaceFirst("^/", ""));
      }
    };
    fs.initialize(URI.create("file:///"), new Configuration());
    identity = JSON.valueToTree(Map.of("bindingId", BINDING, "operationId", OPERATION,
        "epoch", 1, "immutableRequestHash", "sha256:" + "a".repeat(64)));
    fs.mkdirs(new Path(ROOT));
  }

  @Test
  public void observesRealMetadataAndNonemptyRoundTripWithoutLeavingProbe() throws Exception {
    Map<String, Object> status = execute("STAT", ROOT);
    assertEquals(true, status.get("exists"));
    assertEquals("DIRECTORY", status.get("type"));
    assertEquals(System.getProperty("user.name"), status.get("owner"));
    assertTrue(status.get("mode") instanceof Integer);
    Map<String, Object> result = execute("PROBE", ROOT);
    assertEquals(Map.of("readWriteVerified", true, "cleaned", true,
        "bytes", ManagedDependencyClient.PROBE_BYTES), result);
    assertFalse(fs.exists(new Path(ROOT, ".ambari-managed-probe-" + OPERATION)));
    assertEquals(result, execute("PROBE", ROOT));
  }

  @Test
  public void absentMetadataIsDistinctFromReadOrAuthorizationFailure() throws Exception {
    assertEquals(Map.of("exists", false), execute("STAT", ROOT + "/absent"));
    try {
      execute("COUNT", ROOT + "/absent");
      fail("An observation failure cannot become an empty directory");
    } catch (IOException expected) {
      assertFalse(fs.exists(new Path(ROOT + "/absent")));
    }
  }

  @Test
  public void mismatchedExistingProbeIsPreservedAndCannotBecomeSuccess() throws Exception {
    Path probe = new Path(ROOT, ".ambari-managed-probe-" + OPERATION);
    byte[] foreign = "foreign data".getBytes(StandardCharsets.UTF_8);
    try (FSDataOutputStream output = fs.create(probe, false)) {
      output.write(foreign);
    }
    try {
      execute("PROBE", ROOT);
      fail("Unexpected data must not be accepted or removed");
    } catch (IOException expected) {
      try (var input = fs.open(probe)) {
        assertArrayEquals(foreign, input.readAllBytes());
      }
    }
  }

  @Test
  public void foreignBindingMutationIsRejectedBeforeAnyWrite() throws Exception {
    String foreign = ROOT.replace(BINDING, OPERATION);
    try {
      ManagedDependencyClient.execute(fs, "MKDIR", identity,
          JSON.valueToTree(Map.of("path", foreign, "mode", 0700)));
      fail("Foreign binding mutation must be denied");
    } catch (IllegalArgumentException expected) {
      assertFalse(fs.exists(new Path(foreign)));
    }
  }

  private Map<String, Object> execute(String operation, String path) throws Exception {
    return ManagedDependencyClient.execute(fs, operation, identity,
        JSON.valueToTree(Map.of("path", path)));
  }
}
