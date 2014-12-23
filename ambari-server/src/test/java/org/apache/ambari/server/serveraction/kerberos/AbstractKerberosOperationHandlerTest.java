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
import org.apache.ambari.server.AmbariException;
import org.apache.commons.codec.binary.Base64;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractKerberosOperationHandlerTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  protected final KerberosOperationHandler handler;

  protected AbstractKerberosOperationHandlerTest(KerberosOperationHandler handler) {
    this.handler = handler;
  }

  @Before
  public void startUp() throws AmbariException {
    handler.open(new KerberosCredential(), "EXAMPLE.COM");
  }

  @After
  public void cleanUp() throws AmbariException {
    handler.close();
  }

  @Test
  public void testCreateSecurePassword() throws Exception {
    KerberosOperationHandler handler2 = new KerberosOperationHandler() {

      @Override
      public void open(KerberosCredential administratorCredentials, String defaultRealm) throws AmbariException {
        setAdministratorCredentials(administratorCredentials);
        setDefaultRealm(defaultRealm);
      }

      @Override
      public void close() throws AmbariException {

      }

      @Override
      public boolean principalExists(String principal) throws AmbariException {
        return false;
      }

      @Override
      public Integer createServicePrincipal(String principal, String password) throws AmbariException {
        return 0;
      }

      @Override
      public Integer setPrincipalPassword(String principal, String password) throws AmbariException {
        return 0;
      }

      @Override
      public boolean removeServicePrincipal(String principal) throws AmbariException {
        return false;
      }
    };

    String password1 = handler.createSecurePassword();
    Assert.assertNotNull(password1);
    Assert.assertEquals(KerberosOperationHandler.SECURE_PASSWORD_LENGTH, password1.length());

    String password2 = handler2.createSecurePassword();
    Assert.assertNotNull(password2);
    Assert.assertEquals(KerberosOperationHandler.SECURE_PASSWORD_LENGTH, password2.length());

    // Make sure the passwords are different... if they are the same, that indicated the random
    // number generators are generating using the same pattern and that is not secure.
    Assert.assertFalse((password1.equals(password2)));
  }

  @Test
  public void testCreateSecurePasswordWithSize() throws Exception {
    String password;

    password = handler.createSecurePassword(10);
    Assert.assertNotNull(password);
    Assert.assertEquals(10, password.length());

    password = handler.createSecurePassword(0);
    Assert.assertNotNull(password);
    Assert.assertEquals(KerberosOperationHandler.SECURE_PASSWORD_LENGTH, password.length());

    password = handler.createSecurePassword(-20);
    Assert.assertNotNull(password);
    Assert.assertEquals(KerberosOperationHandler.SECURE_PASSWORD_LENGTH, password.length());
  }

  @Test
  public void testCreateKeytabFileOneAtATime() throws Exception {
    File file = folder.newFile();
    final String principal1 = "principal1@REALM.COM";
    final String principal2 = "principal2@REALM.COM";
    int count;

    Assert.assertTrue(handler.createKeytabFile(principal1, handler.createSecurePassword(), 0, file));

    Keytab keytab = Keytab.read(file);
    Assert.assertNotNull(keytab);

    List<KeytabEntry> entries = keytab.getEntries();
    Assert.assertNotNull(entries);
    Assert.assertFalse(entries.isEmpty());

    count = entries.size();

    for (KeytabEntry entry : entries) {
      Assert.assertEquals(principal1, entry.getPrincipalName());
    }

    Assert.assertTrue(handler.createKeytabFile(principal2, handler.createSecurePassword(), 0, file));

    keytab = Keytab.read(file);
    Assert.assertNotNull(keytab);

    entries = keytab.getEntries();
    Assert.assertNotNull(entries);
    Assert.assertFalse(entries.isEmpty());

    Assert.assertEquals(count * 2, entries.size());
  }

  @Test
  public void testEnsureKeytabFileContainsNoDuplicates() throws Exception {
    File file = folder.newFile();
    final String principal1 = "principal1@REALM.COM";
    final String principal2 = "principal2@REALM.COM";
    Set<String> seenEntries = new HashSet<String>();

    Assert.assertTrue(handler.createKeytabFile(principal1, handler.createSecurePassword(), 0, file));
    Assert.assertTrue(handler.createKeytabFile(principal2, handler.createSecurePassword(), 0, file));

    // Attempt to add duplicate entries
    Assert.assertTrue(handler.createKeytabFile(principal2, handler.createSecurePassword(), 0, file));

    Keytab keytab = Keytab.read(file);
    Assert.assertNotNull(keytab);

    List<KeytabEntry> entries = keytab.getEntries();
    Assert.assertNotNull(entries);
    Assert.assertFalse(entries.isEmpty());

    for (KeytabEntry entry : entries) {
      String seenEntry = String.format("%s|%s", entry.getPrincipalName(), entry.getKey().getKeyType().toString());
      Assert.assertFalse(seenEntries.contains(seenEntry));
      seenEntries.add(seenEntry);
    }
  }

  @Test
  public void testCreateKeytabFileExceptions() throws Exception {
    File file = folder.newFile();
    final String principal1 = "principal1@REALM.COM";

    try {
      handler.createKeytabFile(null, handler.createSecurePassword(), 0, file);
      Assert.fail("AmbariException not thrown with null principal");
    } catch (Throwable t) {
      Assert.assertEquals(AmbariException.class, t.getClass());
    }

    try {
      handler.createKeytabFile(principal1, null, null, file);
      Assert.fail("AmbariException not thrown with null password");
    } catch (Throwable t) {
      Assert.assertEquals(AmbariException.class, t.getClass());
    }

    try {
      handler.createKeytabFile(principal1, handler.createSecurePassword(), 0, null);
      Assert.fail("AmbariException not thrown with null file");
    } catch (Throwable t) {
      Assert.assertEquals(AmbariException.class, t.getClass());
    }
  }

  @Test
  public void testCreateKeytabFileFromBase64EncodedData() throws Exception {
    File file = folder.newFile();
    final String principal = "principal@REALM.COM";

    Assert.assertTrue(handler.createKeytabFile(principal, handler.createSecurePassword(), 0, file));

    FileInputStream fis = new FileInputStream(file);
    byte[] data = new byte[(int) file.length()];

    Assert.assertEquals(data.length, fis.read(data));
    fis.close();

    File f = handler.createKeytabFile(Base64.encodeBase64String(data));

    try {
      Keytab keytab = Keytab.read(f);
      Assert.assertNotNull(keytab);

      List<KeytabEntry> entries = keytab.getEntries();
      Assert.assertNotNull(entries);
      Assert.assertFalse(entries.isEmpty());

      for (KeytabEntry entry : entries) {
        Assert.assertEquals(principal, entry.getPrincipalName());
      }
    } finally {
      if (!f.delete()) {
        f.deleteOnExit();
      }
    }
  }
}
