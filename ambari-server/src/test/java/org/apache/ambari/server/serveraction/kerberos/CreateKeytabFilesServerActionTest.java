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

package org.apache.ambari.server.serveraction.kerberos;

import junit.framework.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class CreateKeytabFilesServerActionTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void testEnsureAmbariOnlyAccess() throws Exception {
    File directory = testFolder.newFolder();
    Assert.assertNotNull(directory);

    new CreateKeytabFilesServerAction().ensureAmbariOnlyAccess(directory);

    // The directory is expected to have the following permissions: rwx------ (700)
    Assert.assertTrue(directory.canRead());
    Assert.assertTrue(directory.canWrite());
    Assert.assertTrue(directory.canExecute());

    File file = File.createTempFile("temp_", "", directory);
    Assert.assertNotNull(file);
    Assert.assertTrue(file.exists());

    new CreateKeytabFilesServerAction().ensureAmbariOnlyAccess(file);

    // The file is expected to have the following permissions: rw------- (600)
    Assert.assertTrue(file.canRead());
    Assert.assertTrue(file.canWrite());
    Assert.assertFalse(file.canExecute());
  }
}