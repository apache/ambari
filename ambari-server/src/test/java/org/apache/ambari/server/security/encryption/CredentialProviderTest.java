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
package org.apache.ambari.server.security.encryption;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;

public class CredentialProviderTest extends TestCase {
  private String keystore_dir;
  private static final Log LOG = LogFactory.getLog
    (CredentialProviderTest.class);

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Override
  protected void setUp() throws Exception {
    tmpFolder.create();
    keystore_dir = tmpFolder.getRoot().getAbsolutePath();
  }

  private void createMasterKey(String dir) {
    File f = new File(dir + System.getProperty("file" +
      ".separator") + Configuration.MASTER_KEY_FILENAME_DEFAULT);
    if (!f.exists()) {
      try {
        f.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    MasterKeyService ms = new MasterKeyServiceImpl("blahblah!",
      f.getAbsolutePath(), true);
    if (!ms.isMasterKeyInitialized()) {
      throw new ExceptionInInitializerError("Cannot create master key.");
    }
  }

  @Test
  public void testInitialization() throws Exception {
    CredentialProvider cr = null;
    String msFile = keystore_dir + System.getProperty("file" +
      ".separator") + Configuration.MASTER_KEY_FILENAME_DEFAULT;
    try {
      new CredentialProvider(null, null, true);
      fail("Expected an exception");
    } catch (Throwable t) {
      Assert.assertTrue(t instanceof IllegalArgumentException);
    }
    try {
      new CredentialProvider(null, msFile, true);
      fail("Expected an exception");
    } catch (Throwable t) {
      Assert.assertTrue(t instanceof AmbariException);
    }
    // Without master key persisted
    cr = new CredentialProvider("blahblah!", msFile, false);
    Assert.assertNotNull(cr);
    Assert.assertNotNull(cr.getKeystoreService());
    // With master key persisted
    createMasterKey(keystore_dir);
    cr = new CredentialProvider(null, msFile, true);
    Assert.assertNotNull(cr);
    Assert.assertNotNull(cr.getKeystoreService());
  }

  @Test
  public void testIsAliasString() {
    String test  = "cassablanca";
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
    String msFile = keystore_dir + System.getProperty("file" +
      ".separator") + Configuration.MASTER_KEY_FILENAME_DEFAULT;
    // With master key persisted
    createMasterKey(keystore_dir);
    CredentialProvider cr = new CredentialProvider(null, msFile, true);
    Assert.assertNotNull(cr);
    Assert.assertNotNull(cr.getKeystoreService());

    try {
      cr.addAliasToCredentialStore("", "xyz");
      fail("Expected an exception");
    } catch (Throwable t) {
      Assert.assertTrue(t instanceof IllegalArgumentException);
    }

    try {
      cr.addAliasToCredentialStore("xyz", null);
      fail("Expected an exception");
    } catch (Throwable t) {
      Assert.assertTrue(t instanceof IllegalArgumentException);
    }

    cr.addAliasToCredentialStore("myalias", "mypassword");
    Assert.assertEquals("mypassword", new String(cr.getPasswordForAlias
      ("myalias")));
  }

  @Override
  protected void tearDown() throws Exception {
    tmpFolder.delete();
  }
}
