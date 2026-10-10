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
package org.apache.ambari.server.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.List;

import org.apache.ambari.server.metadata.ActionMetadata;
import org.junit.Test;

public class StackCandidateContextTest {
  @Test
  public void candidateServiceChecksDoNotMutateActiveMetadata() {
    ActionMetadata metadata = new ActionMetadata();
    metadata.addServiceCheckAction("OLD_SERVICE");
    StackContext candidate = new StackContext(null, metadata, null, true);
    candidate.registerServiceCheck("NGINX");
    assertNull(metadata.getServiceCheckAction("NGINX"));
    assertEquals("OLD_SERVICE_SERVICE_CHECK", metadata.getServiceCheckAction("OLD_SERVICE"));
    candidate.publishServiceChecks();
    assertEquals("NGINX_SERVICE_CHECK", metadata.getServiceCheckAction("NGINX"));
    assertNull(metadata.getServiceCheckAction("OLD_SERVICE"));
    assertTrue(metadata.getActions("OLD_SERVICE").isEmpty());
    candidate.publishServiceChecks();
    assertEquals(List.of("NGINX_SERVICE_CHECK"), metadata.getActions("NGINX"));
  }

  @Test
  public void isolatedResolutionDoesNotStartRemoteRepositoryDiscovery() {
    StackContext candidate = new StackContext(null, new ActionMetadata(), null, true);
    candidate.registerRepoUpdateTask(URI.create("http://127.0.0.1:1/not-a-candidate-input"), null);
    candidate.executeRepoTasks();
    assertTrue(candidate.haveAllRepoTasksCompleted());
  }

  @Test
  public void bindingSnapshotRejectsMultipleVersionsOfOneProvider() {
    StackResolutionContext.Binding first = new StackResolutionContext.Binding("GENERIC", "1.0", "NGINX", "1.0");
    StackResolutionContext.Binding next = new StackResolutionContext.Binding("GENERIC", "1.0", "NGINX", "2.0");
    assertThrows(IllegalArgumentException.class,
        () -> new StackResolutionContext("snapshot", List.of(first, next)));
    assertEquals(1, new StackResolutionContext("snapshot", List.of(first)).bindings().size());
  }
}
