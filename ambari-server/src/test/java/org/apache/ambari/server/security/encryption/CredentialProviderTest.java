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
package org.apache.ambari.server.security.encryption;

import java.io.File;
import java.io.IOException;

import org.apache.ambari.server.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import junit.framework.Assert;

public class CredentialProviderTest {
  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    tmpFolder.create();
  }

  private void createMasterKey() throws IOException {
    File f = tmpFolder.newFile(Configuration.MASTER_KEY_FILENAME_DEFAULT);
    Assert.assertTrue(MasterKeyServiceImpl.initializeMasterKeyFile(f, "blahblah!"));
    MasterKeyService ms = new MasterKeyServiceImpl(f);
    if (!ms.isMasterKeyInitialized()) {
      throw new ExceptionInInitializerError("Cannot create master key.");
    }
  }

  @Test
  public void testInitialization() throws Exception {
    CredentialProvider cr;
    File msFile = tmpFolder.newFile(Configuration.MASTER_KEY_FILENAME_DEFAULT);
    File mksFile = tmpFolder.newFile(Configuration.MASTER_KEYSTORE_FILENAME_DEFAULT);
    try {
      new CredentialProvider(null, null, true, null);
      Assert.fail("Expected an exception");
    } catch (Throwable t) {
      Assert.assertTrue(t instanceof IllegalArgumentException);
    }
    // Without master key persisted
    cr = new CredentialProvider("blahblah!", msFile, false, mksFile);
    Assert.assertNotNull(cr);
    Assert.assertNotNull(cr.getKeystoreService());
    // With master key persisted
    msFile.delete();
    mksFile.delete();
    createMasterKey();
    cr = new CredentialProvider(null, msFile, true, mksFile);
    Assert.assertNotNull(cr);
    Assert.assertNotNull(cr.getKeystoreService());
  }

  @Test
  public void testIsAliasString() {
    String test = "cassablanca";
    Assert.assertFalse(CredentialProvider.isAliasString(test));
    test = "${}";
    Assert.assertFalse(CredentialProvider.isAliasString(test));
    test = "{}";
    Assert.assertFalse(CredentialProvider.isAliasString(test));
    test = "{cassablanca}";
    Assert.assertFalse(CredentialProvider.isAliasString(test));
    test = "${cassablanca}";
    Assert.assertFalse(CredentialProvider.isAliasString(test));
    test = "${alias=cassablanca}";
    Assert.assertTrue(CredentialProvider.isAliasString(test));
  }

  @Test
  public void testCredentialStore() throws Exception {
    File msFile = tmpFolder.newFile(Configuration.MASTER_KEY_FILENAME_DEFAULT);
    File mksFile = tmpFolder.newFile(Configuration.MASTER_KEYSTORE_FILENAME_DEFAULT);

    // With master key persisted
    createMasterKey();
    CredentialProvider cr = new CredentialProvider(null, msFile, true, mksFile);
    Assert.assertNotNull(cr);
    Assert.assertNotNull(cr.getKeystoreService());

    try {
      cr.addAliasToCredentialStore("", "xyz");
      Assert.fail("Expected an exception");
    } catch (Throwable t) {
      Assert.assertTrue(t instanceof IllegalArgumentException);
    }

    try {
      cr.addAliasToCredentialStore("xyz", null);
      Assert.fail("Expected an exception");
    } catch (Throwable t) {
      Assert.assertTrue(t instanceof IllegalArgumentException);
    }

    cr.addAliasToCredentialStore("myalias", "mypassword");
    Assert.assertEquals("mypassword", new String(cr.getPasswordForAlias
        ("myalias")));
  }

  @After
  public void tearDown() throws Exception {
    tmpFolder.delete();
  }
}
