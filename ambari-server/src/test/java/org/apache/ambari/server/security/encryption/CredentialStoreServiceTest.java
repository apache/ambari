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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CredentialStoreServiceTest extends TestCase {
  private File keystore_dir;
  private CredentialStoreService credentialStoreService;
  private static final Log LOG = LogFactory.getLog
    (CredentialStoreServiceTest.class);

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Override
  protected void setUp() throws Exception {
    tmpFolder.create();
    keystore_dir = tmpFolder.newFolder("jcekeystore");
    LOG.debug("Setting default keystore_dir to " + keystore_dir);
    credentialStoreService = new
      CredentialStoreServiceImpl(keystore_dir.getAbsolutePath());
  }

  @Test
  public void testAddCredentialToStoreWithPersistMaster() throws Exception {
    String masterKey = "ThisissomeSecretPassPhrasse";
    String masterKeyLocation = keystore_dir.getAbsolutePath() + "/master";
    MasterKeyService masterKeyService = new MasterKeyServiceImpl(masterKey,
      masterKeyLocation, true);
    credentialStoreService.setMasterKeyService(masterKeyService);
    String password = "mypassword";
    credentialStoreService.addCredential("myalias", password);
    char[] credential = credentialStoreService.getCredential("myalias");
    Assert.assertEquals(password, new String(credential));
  }

  @Test
  public void testAddCredentialToStore() throws Exception {
    String masterKey = "ThisissomeSecretPassPhrasse";
    String masterKeyLocation = keystore_dir.getAbsolutePath() + "/master";
    MasterKeyService masterKeyService = new MasterKeyServiceImpl(masterKey,
      masterKeyLocation, false);
    credentialStoreService.setMasterKeyService(masterKeyService);
    String password = "mypassword";
    credentialStoreService.addCredential("myalias", password);
    char[] credential = credentialStoreService.getCredential("myalias");
    Assert.assertEquals(password, new String(credential));
    File f = new File(masterKeyLocation);
    Assert.assertFalse(f.exists());
  }

  @Test
  public void testGetCredential() throws Exception {
    String masterKey = "ThisissomeSecretPassPhrasse";
    String masterKeyLocation = keystore_dir.getAbsolutePath() + "/master";
    MasterKeyService masterKeyService = new MasterKeyServiceImpl(masterKey,
      masterKeyLocation, false);
    credentialStoreService.setMasterKeyService(masterKeyService);
    Assert.assertNull(credentialStoreService.getCredential(""));
    Assert.assertNull(credentialStoreService.getCredential(null));
    String password = "mypassword";
    credentialStoreService.addCredential("myalias", password);
    char[] credential = credentialStoreService.getCredential("myalias");
    Assert.assertEquals(password, new String(credential));
  }

  @Test
  public void testAliasParsing() throws Exception {
    String strPasswd = "${alias=ambari.password}";
    Pattern PASSWORD_ALIAS_PATTERN = Pattern.compile
      ("\\$\\{alias=[\\w\\.]+\\}");
    Matcher matcher = PASSWORD_ALIAS_PATTERN.matcher(strPasswd);
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("ambari.password", strPasswd.substring(strPasswd
      .indexOf("=")
      + 1, strPasswd.length() - 1));
  }

  @Override
  protected void tearDown() throws Exception {
    tmpFolder.delete();
  }
}
