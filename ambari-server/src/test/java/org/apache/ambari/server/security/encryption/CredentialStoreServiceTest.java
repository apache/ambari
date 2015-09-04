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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class CredentialStoreServiceTest {

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    tmpFolder.create();
  }

  @After
  public void cleanUp() throws Exception {
    tmpFolder.delete();
  }

  @Test
  public void testFileBasedCredentialStoreService_AddCredentialToStoreWithPersistMaster() throws Exception {
    addCredentialToStoreWithPersistMasterTest(new FileBasedCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testFileBasedCredentialStoreService_AddCredentialToStore() throws Exception {
    addCredentialToStoreTest(new FileBasedCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testFileBasedCredentialStoreService_GetCredential() throws Exception {
    getCredentialTest(new FileBasedCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testFileBasedCredentialStoreService_RemoveCredential() throws Exception {
    removeCredentialTest(new FileBasedCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testInMemoryCredentialStoreService_AddCredentialToStoreWithPersistMaster() throws Exception {
    addCredentialToStoreWithPersistMasterTest(new InMemoryCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testInMemoryCredentialStoreService_AddCredentialToStore() throws Exception {
    addCredentialToStoreTest(new InMemoryCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testInMemoryCredentialStoreService_GetCredential() throws Exception {
    getCredentialTest(new InMemoryCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testInMemoryCredentialStoreService_RemoveCredential() throws Exception {
    removeCredentialTest(new InMemoryCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testInMemoryCredentialStoreService_CredentialExpired() throws Exception {
    getExpiredCredentialTest(new InMemoryCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  private void addCredentialToStoreWithPersistMasterTest(CredentialStoreServiceFactory credentialStoreServiceFactory,
                                                         MasterKeyServiceFactory masterKeyServiceFactory) throws Exception {
    File directory = tmpFolder.getRoot();

    String masterKey = "ThisIsSomeSecretPassPhrase1234";
    File masterKeyFile = new File(directory, "master");

    MasterKeyService masterKeyService = masterKeyServiceFactory.createPersisted(masterKeyFile, masterKey);
    CredentialStoreService credentialStoreService = credentialStoreServiceFactory.create(directory, masterKeyService);

    String password = "mypassword";
    credentialStoreService.addCredential("myalias", password.toCharArray());
    char[] credential = credentialStoreService.getCredential("myalias");
    Assert.assertEquals(password, new String(credential));

    Assert.assertTrue(masterKeyFile.exists());
  }

  private void addCredentialToStoreTest(CredentialStoreServiceFactory credentialStoreServiceFactory,
                                        MasterKeyServiceFactory masterKeyServiceFactory) throws Exception {
    File directory = tmpFolder.getRoot();

    String masterKey = "ThisIsSomeSecretPassPhrase1234";
    File masterKeyFile = new File(directory, "master");

    MasterKeyService masterKeyService = masterKeyServiceFactory.create(masterKey);
    CredentialStoreService credentialStoreService = credentialStoreServiceFactory.create(directory, masterKeyService);

    String password = "mypassword";
    credentialStoreService.addCredential("password", password.toCharArray());
    char[] credential = credentialStoreService.getCredential("password");
    Assert.assertEquals(password, new String(credential));

    credentialStoreService.addCredential("null_password", null);
    Assert.assertNull(credentialStoreService.getCredential("null_password"));

    credentialStoreService.addCredential("empty_password", new char[0]);
    Assert.assertNull(credentialStoreService.getCredential("empty_password"));

    Assert.assertFalse(masterKeyFile.exists());
  }

  private void getCredentialTest(CredentialStoreServiceFactory credentialStoreServiceFactory,
                                 MasterKeyServiceFactory masterKeyServiceFactory) throws Exception {
    File directory = tmpFolder.getRoot();

    String masterKey = "ThisIsSomeSecretPassPhrase1234";

    MasterKeyService masterKeyService = masterKeyServiceFactory.create(masterKey);
    CredentialStoreService credentialStoreService = credentialStoreServiceFactory.create(directory, masterKeyService);

    Assert.assertNull(credentialStoreService.getCredential(""));
    Assert.assertNull(credentialStoreService.getCredential(null));

    String password = "mypassword";
    credentialStoreService.addCredential("myalias", password.toCharArray());
    char[] credential = credentialStoreService.getCredential("myalias");
    Assert.assertEquals(password, new String(credential));

    Assert.assertNull(credentialStoreService.getCredential("does_not_exist"));
  }

  private void getExpiredCredentialTest(CredentialStoreServiceFactory credentialStoreServiceFactory,
                                 MasterKeyServiceFactory masterKeyServiceFactory) throws Exception {
    File directory = tmpFolder.getRoot();

    String masterKey = "ThisIsSomeSecretPassPhrase1234";

    MasterKeyService masterKeyService = masterKeyServiceFactory.create(masterKey);
    CredentialStoreService credentialStoreService = credentialStoreServiceFactory.create(directory, masterKeyService);

    String password = "mypassword";
    credentialStoreService.addCredential("myalias", password.toCharArray());
    Assert.assertEquals(password, new String(credentialStoreService.getCredential("myalias")));

    Thread.sleep(250);
    Assert.assertEquals(password, new String(credentialStoreService.getCredential("myalias")));

    Thread.sleep(550);
    Assert.assertNull(password, credentialStoreService.getCredential("myalias"));

  }

  private void removeCredentialTest(CredentialStoreServiceFactory credentialStoreServiceFactory,
                                    MasterKeyServiceFactory masterKeyServiceFactory) throws Exception {
    File directory = tmpFolder.getRoot();

    String masterKey = "ThisIsSomeSecretPassPhrase1234";

    MasterKeyService masterKeyService = masterKeyServiceFactory.create(masterKey);
    CredentialStoreService credentialStoreService = credentialStoreServiceFactory.create(directory, masterKeyService);

    String password = "mypassword";
    credentialStoreService.addCredential("myalias", password.toCharArray());

    char[] credential = credentialStoreService.getCredential("myalias");
    Assert.assertEquals(password, new String(credential));

    credentialStoreService = credentialStoreServiceFactory.create(directory, masterKeyService);
    credentialStoreService.setMasterKeyService(masterKeyService);

    credentialStoreService.removeCredential("myalias");
    Assert.assertNull(credentialStoreService.getCredential("myalias"));

    credentialStoreService.removeCredential("does_not_exist");
  }

  private interface CredentialStoreServiceFactory {
    CredentialStoreService create(File directory, MasterKeyService masterKeyService);
  }

  private class FileBasedCredentialStoreServiceFactory implements CredentialStoreServiceFactory {
    @Override
    public CredentialStoreService create(File directory, MasterKeyService masterKeyService) {
      CredentialStoreService credentialStoreService = new FileBasedCredentialStoreService(directory.getAbsolutePath());
      credentialStoreService.setMasterKeyService(masterKeyService);
      return credentialStoreService;
    }
  }

  private class InMemoryCredentialStoreServiceFactory implements CredentialStoreServiceFactory {
    @Override
    public CredentialStoreService create(File directory, MasterKeyService masterKeyService) {
      CredentialStoreService credentialStoreService = new InMemoryCredentialStoreService(500, TimeUnit.MILLISECONDS, true);
      credentialStoreService.setMasterKeyService(masterKeyService);
      return credentialStoreService;
    }
  }

  private interface MasterKeyServiceFactory {
    MasterKeyService create(String masterKey);

    MasterKeyService createPersisted(File masterKeyFile, String masterKey);
  }

  private class DefaultMasterKeyServiceFactory implements MasterKeyServiceFactory {

    @Override
    public MasterKeyService create(String masterKey) {
      return new MasterKeyServiceImpl(masterKey);
    }

    @Override
    public MasterKeyService createPersisted(File masterKeyFile, String masterKey) {
      MasterKeyServiceImpl.initializeMasterKeyFile(masterKeyFile, masterKey);
      return new MasterKeyServiceImpl(masterKeyFile);
    }
  }

}
