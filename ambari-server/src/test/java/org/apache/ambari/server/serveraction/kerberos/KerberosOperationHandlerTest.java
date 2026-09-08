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

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KeyTab;

import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.commons.codec.binary.Base64;
import org.apache.kerby.kerberos.kerb.keytab.Keytab;
import org.apache.kerby.kerberos.kerb.keytab.KeytabEntry;
import org.apache.kerby.kerberos.kerb.type.base.EncryptionType;
import org.apache.kerby.kerberos.kerb.type.base.NameType;
import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import junit.framework.Assert;

public abstract class KerberosOperationHandlerTest extends EasyMockSupport {

  static final String DEFAULT_ADMIN_PRINCIPAL = "admin";
  static final String DEFAULT_ADMIN_PASSWORD = "hadoop";
  static final String DEFAULT_REALM = "EXAMPLE.COM";
  static final PrincipalKeyCredential DEFAULT_ADMIN_CREDENTIALS = new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD);
  static final Map<String, String> DEFAULT_KERBEROS_ENV_MAP;

  static {
    Map<String, String> map = new HashMap<>();
    map.put(KerberosOperationHandler.KERBEROS_ENV_ENCRYPTION_TYPES, "aes des3-cbc-sha1 rc4 des-cbc-md5");
    map.put(IPAKerberosOperationHandler.KERBEROS_ENV_KDC_HOSTS, "localhost");
    map.put(IPAKerberosOperationHandler.KERBEROS_ENV_ADMIN_SERVER_HOST, "localhost");
    DEFAULT_KERBEROS_ENV_MAP = Collections.unmodifiableMap(map);
  }

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testOpenSucceeded() throws Exception {
    KerberosOperationHandler handler = createMockedHandler();

    setupOpenSuccess(handler);

    replayAll();

    handler.open(getAdminCredentials(), DEFAULT_REALM, getKerberosEnv());

    verifyAll();

    Assert.assertTrue(handler.isOpen());
  }

  @Test
  public void testOpenFailed() throws Exception {
    KerberosOperationHandler handler = createMockedHandler();

    setupOpenFailure(handler);

    replayAll();

    try {
      handler.open(getAdminCredentials(), DEFAULT_REALM, getKerberosEnv());
      Assert.fail("KerberosAdminAuthenticationException expected");
    } catch (KerberosAdminAuthenticationException e) {
      // This is expected
    }

    verifyAll();

    Assert.assertFalse(handler.isOpen());
  }

  @Test(expected = KerberosPrincipalAlreadyExistsException.class)
  public void testCreateUserPrincipalPrincipalAlreadyExists() throws Exception {
    testCreatePrincipalPrincipalAlreadyExists(false);
  }

  @Test(expected = KerberosPrincipalAlreadyExistsException.class)
  public void testCreateServicePrincipalPrincipalAlreadyExists() throws Exception {
    testCreatePrincipalPrincipalAlreadyExists(true);
  }

  private void testCreatePrincipalPrincipalAlreadyExists(boolean service) throws Exception {
    KerberosOperationHandler handler = createMockedHandler();

    setupOpenSuccess(handler);
    setupPrincipalAlreadyExists(handler, service);

    replayAll();

    handler.open(getAdminCredentials(), DEFAULT_REALM, getKerberosEnv());
    handler.createPrincipal(createPrincipal(service), "password", service);
    handler.close();

    verifyAll();

  }


  @Test
  public void testUserPrincipalExistsNotFound() throws Exception {
    testPrincipalExistsNotFound(false);
  }

  @Test
  public void testServicePrincipalExistsNotFound() throws Exception {
    testPrincipalExistsNotFound(true);
  }

  private void testPrincipalExistsNotFound(boolean service) throws Exception {
    KerberosOperationHandler handler = createMockedHandler();

    setupOpenSuccess(handler);
    setupPrincipalDoesNotExist(handler, service);

    replayAll();

    handler.open(getAdminCredentials(), DEFAULT_REALM, getKerberosEnv());
    Assert.assertFalse(handler.principalExists(createPrincipal(service), service));
    handler.close();

    verifyAll();
  }

  @Test
  public void testUserPrincipalExistsFound() throws Exception {
    testPrincipalExistsFound(false);
  }

  @Test
  public void testServicePrincipalExistsFound() throws Exception {
    testPrincipalExistsFound(true);
  }

  private void testPrincipalExistsFound(boolean service) throws Exception {
    KerberosOperationHandler handler = createMockedHandler();

    setupOpenSuccess(handler);
    setupPrincipalExists(handler, service);

    replayAll();

    handler.open(getAdminCredentials(), DEFAULT_REALM, getKerberosEnv());
    Assert.assertTrue(handler.principalExists(createPrincipal(service), service));
    handler.close();

    verifyAll();

  }

  @Test
  public void testCreateKeytabFileOneAtATime() throws Exception {
    KerberosOperationHandler handler = createHandler();
    File file = folder.newFile();
    final String principal1 = "principal1@REALM.COM";
    final String principal2 = "principal2@REALM.COM";
    int count;

    Assert.assertTrue(handler.createKeytabFile(principal1, "some password", 0, file));

    Keytab keytab = Keytab.loadKeytab(file);
    Assert.assertNotNull(keytab);

    List<KeytabEntry> entries = handler.getKeytabEntries(keytab);
    Assert.assertNotNull(entries);
    Assert.assertFalse(entries.isEmpty());

    count = entries.size();

    for (KeytabEntry entry : entries) {
      Assert.assertEquals(principal1, entry.getPrincipal().getName());
    }

    Assert.assertTrue(handler.createKeytabFile(principal2, "some password", 0, file));

    keytab = Keytab.loadKeytab(file);
    Assert.assertNotNull(keytab);

    entries = handler.getKeytabEntries(keytab);
    Assert.assertNotNull(entries);
    Assert.assertFalse(entries.isEmpty());

    Assert.assertEquals(count * 2, entries.size());
  }

  @Test
  public void testEnsureKeytabFileContainsNoDuplicates() throws Exception {
    KerberosOperationHandler handler = createHandler();
    File file = folder.newFile();
    final String principal1 = "principal1@REALM.COM";
    final String principal2 = "principal2@REALM.COM";
    Set<String> seenEntries = new HashSet<>();

    Assert.assertTrue(handler.createKeytabFile(principal1, "some password", 0, file));
    Assert.assertTrue(handler.createKeytabFile(principal2, "some password", 0, file));

    // Attempt to add duplicate entries
    Assert.assertTrue(handler.createKeytabFile(principal2, "some password", 0, file));

    Keytab keytab = Keytab.loadKeytab(file);
    Assert.assertNotNull(keytab);

    List<KeytabEntry> entries = handler.getKeytabEntries(keytab);
    Assert.assertNotNull(entries);
    Assert.assertFalse(entries.isEmpty());

    for (KeytabEntry entry : entries) {
      String seenEntry = String.format("%s|%s", entry.getPrincipal().getName(), entry.getKey().getKeyType().toString());
      Assert.assertFalse(seenEntries.contains(seenEntry));
      seenEntries.add(seenEntry);
    }
  }

  @Test
  public void testCreateKeytabFileExceptions() throws Exception {
    KerberosOperationHandler handler = createHandler();
    File file = folder.newFile();
    final String principal1 = "principal1@REALM.COM";

    try {
      handler.createKeytabFile(null, "some password", 0, file);
      Assert.fail("KerberosOperationException not thrown with null principal");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }

    try {
      handler.createKeytabFile(principal1, null, null, file);
      Assert.fail("KerberosOperationException not thrown with null password");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }

    try {
      handler.createKeytabFile(principal1, "some password", 0, null);
      Assert.fail("KerberosOperationException not thrown with null file");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }
  }

  @Test
  public void testCreateKeytabFileFromBase64EncodedData() throws Exception {
    KerberosOperationHandler handler = createHandler();
    File file = folder.newFile();
    final String principal = "principal@REALM.COM";

    Assert.assertTrue(handler.createKeytabFile(principal, "some password", 0, file));

    FileInputStream fis = new FileInputStream(file);
    byte[] data = new byte[(int) file.length()];

    Assert.assertEquals(data.length, fis.read(data));
    fis.close();

    File f = handler.createKeytabFile(Base64.encodeBase64String(data));
    if (f != null) {
      try {
        Keytab keytab = Keytab.loadKeytab(f);
        Assert.assertNotNull(keytab);

        List<KeytabEntry> entries = handler.getKeytabEntries(keytab);
        Assert.assertNotNull(entries);
        Assert.assertFalse(entries.isEmpty());

        for (KeytabEntry entry : entries) {
          Assert.assertEquals(principal, entry.getPrincipal().getName());
        }
      } finally {
        if (!f.delete()) {
          f.deleteOnExit();
        }
      }
    }
  }

  @Test
  public void testMergeKeytabs() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    Keytab keytab1 = handler.createKeytab("principal@EXAMPLE.COM", "password", 1);
    Keytab keytab2 = handler.createKeytab("principal@EXAMPLE.COM", "password1", 1);
    Keytab keytab3 = handler.createKeytab("principal1@EXAMPLE.COM", "password", 4);

    Keytab merged;

    merged = handler.mergeKeytabs(keytab1, keytab2);
    Assert.assertEquals(handler.getKeytabEntries(keytab1).size(), handler.getKeytabEntries(merged).size());

    merged = handler.mergeKeytabs(keytab1, keytab3);
    Assert.assertEquals(handler.getKeytabEntries(keytab1).size() + handler.getKeytabEntries(keytab3).size(),
        handler.getKeytabEntries(merged).size());

    merged = handler.mergeKeytabs(keytab2, keytab3);
    Assert.assertEquals(handler.getKeytabEntries(keytab2).size() + handler.getKeytabEntries(keytab3).size(),
        handler.getKeytabEntries(merged).size());

    merged = handler.mergeKeytabs(keytab2, merged);
    Assert.assertEquals(handler.getKeytabEntries(keytab2).size() + handler.getKeytabEntries(keytab3).size(),
        handler.getKeytabEntries(merged).size());
  }

  @Test
  public void testCreateKeytabPreservesKeyDerivationAndJdkFormatCompatibility() throws Exception {
    KerberosOperationHandler handler = createHandler();
    handler.setKeyEncryptionTypes(EnumSet.of(
        EncryptionType.AES256_CTS_HMAC_SHA1_96,
        EncryptionType.AES128_CTS_HMAC_SHA1_96,
        EncryptionType.DES3_CBC_SHA1_KD,
        EncryptionType.RC4_HMAC,
        EncryptionType.DES_CBC_MD5));

    String principal = "service/host.example.com@EXAMPLE.COM";
    Keytab keytab = handler.createKeytab(principal, "correct horse battery staple", 7);
    List<KeytabEntry> entries = handler.getKeytabEntries(keytab);

    Map<EncryptionType, String> expectedKeys = new HashMap<>();
    expectedKeys.put(EncryptionType.AES256_CTS_HMAC_SHA1_96,
        "17e4caf893cc1e339ad02dbc4c1174b4e5a847614abf5a12d763aebcee10f67f");
    expectedKeys.put(EncryptionType.AES128_CTS_HMAC_SHA1_96, "7a22a28993125fad78648d72338a93f2");
    expectedKeys.put(EncryptionType.DES3_CBC_SHA1_KD, "ce7ad53db67cba854508f1d97ccef2f15ec8ef455d2562f4");
    expectedKeys.put(EncryptionType.RC4_HMAC, "1b9d5effd34ac283c8efe2eacaea8bbc");
    expectedKeys.put(EncryptionType.DES_CBC_MD5, "d5b0e3c7e5e6c1dc");

    Assert.assertEquals(expectedKeys.size(), entries.size());
    for (KeytabEntry entry : entries) {
      Assert.assertEquals(principal, entry.getPrincipal().getName());
      Assert.assertEquals(NameType.NT_PRINCIPAL, entry.getPrincipal().getNameType());
      Assert.assertEquals(7, entry.getKvno());
      Assert.assertEquals(findExpectedKey(expectedKeys, entry.getKey().getKeyType().getValue()),
          HexFormat.of().formatHex(entry.getKey().getKeyData()));
    }

    File keytabFile = folder.newFile();
    keytab.store(keytabFile);
    KerberosKey[] jdkKeys = KeyTab.getInstance(keytabFile)
        .getKeys(new KerberosPrincipal(principal));
    Assert.assertEquals(2, jdkKeys.length);
    for (KerberosKey key : jdkKeys) {
      Assert.assertEquals(7, key.getVersionNumber());
      String expectedKey = findExpectedKey(expectedKeys, key.getKeyType());
      Assert.assertNotNull(expectedKey);
      Assert.assertEquals(expectedKey, HexFormat.of().formatHex(key.getEncoded()));
    }
    Assert.assertEquals(
        new HashSet<>(Arrays.asList(EncryptionType.AES128_CTS_HMAC_SHA1_96.getValue(),
            EncryptionType.AES256_CTS_HMAC_SHA1_96.getValue())),
        new HashSet<>(Arrays.asList(jdkKeys[0].getKeyType(), jdkKeys[1].getKeyType())));
  }

  private static String findExpectedKey(Map<EncryptionType, String> expectedKeys, int encryptionType) {
    return expectedKeys.entrySet().stream()
        .filter(entry -> entry.getKey().getValue() == encryptionType)
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  @Test
  public void testCreateKeytabDerivationPreservesPrincipalCaseInSalt() throws Exception {
    KerberosOperationHandler handler = createHandler();
    handler.setKeyEncryptionTypes(EnumSet.of(EncryptionType.AES128_CTS_HMAC_SHA1_96));

    List<KeytabEntry> mixedCaseEntries = handler.getKeytabEntries(handler.createKeytab(
        "service/Host.Example.Com@EXAMPLE.COM", "correct horse battery staple", 1));
    List<KeytabEntry> lowerRealmEntries = handler.getKeytabEntries(handler.createKeytab(
        "service/host.example.com@example.com", "correct horse battery staple", 1));

    Assert.assertEquals("8d41ea04fdb638f05b02806ae39a37d4",
        HexFormat.of().formatHex(mixedCaseEntries.get(0).getKey().getKeyData()));
    Assert.assertEquals("0ca0cdba7276dd08d8695c3c9f73a683",
        HexFormat.of().formatHex(lowerRealmEntries.get(0).getKey().getKeyData()));
    Assert.assertFalse(Arrays.equals(mixedCaseEntries.get(0).getKey().getKeyData(),
        lowerRealmEntries.get(0).getKey().getKeyData()));
  }

  @Test
  public void testCreateKeytabPreservesLegacyEightBitKvno() throws Exception {
    KerberosOperationHandler handler = createHandler();
    handler.setKeyEncryptionTypes(EnumSet.of(EncryptionType.AES128_CTS_HMAC_SHA1_96));
    int[] requestedKvnos = {127, 128, 255, 256};
    int[] expectedKvnos = {127, 128, 255, 0};

    for (int i = 0; i < requestedKvnos.length; i++) {
      String principal = "user" + requestedKvnos[i] + "@EXAMPLE.COM";
      Keytab keytab = handler.createKeytab(principal, "password", requestedKvnos[i]);
      Assert.assertEquals(expectedKvnos[i], handler.getKeytabEntries(keytab).get(0).getKvno());

      File keytabFile = folder.newFile();
      keytab.store(keytabFile);
      KerberosKey[] jdkKeys = KeyTab.getInstance(keytabFile)
          .getKeys(new KerberosPrincipal(principal));
      Assert.assertEquals(1, jdkKeys.length);
      Assert.assertEquals(expectedKvnos[i], jdkKeys[0].getVersionNumber());
    }
  }

  @Test
  public void testReadsLegacyApacheDsKeytabAndWritesJdkCompatibleKeytab() throws Exception {
    String principal = "service/host.example.com@EXAMPLE.COM";
    byte[] legacyKeytab = Base64.decodeBase64(
        "BQIAAABHAAIAC0VYQU1QTEUuQ09NAAdzZXJ2aWNlABBob3N0LmV4YW1wbGUuY29tAAAAAWVT8QAHABEAEHoioomTEl+teGSNcjOKk/I=");
    File legacyFile = folder.newFile();
    Files.write(legacyFile.toPath(), legacyKeytab);

    KerberosOperationHandler handler = createHandler();
    Keytab keytab = Keytab.loadKeytab(legacyFile);
    List<KeytabEntry> entries = handler.getKeytabEntries(keytab);
    Assert.assertEquals(1, entries.size());
    KeytabEntry entry = entries.get(0);
    Assert.assertEquals(principal, entry.getPrincipal().getName());
    Assert.assertEquals(NameType.NT_PRINCIPAL, entry.getPrincipal().getNameType());
    Assert.assertEquals(1_700_000_000_000L, entry.getTimestamp().getTime());
    Assert.assertEquals(7, entry.getKvno());
    Assert.assertEquals(EncryptionType.AES128_CTS_HMAC_SHA1_96.getValue(),
        entry.getKey().getKeyType().getValue());
    Assert.assertEquals("7a22a28993125fad78648d72338a93f2",
        HexFormat.of().formatHex(entry.getKey().getKeyData()));

    File migratedFile = folder.newFile();
    keytab.store(migratedFile);
    KerberosKey[] jdkKeys = KeyTab.getInstance(migratedFile)
        .getKeys(new KerberosPrincipal(principal));
    Assert.assertEquals(1, jdkKeys.length);
    Assert.assertEquals(7, jdkKeys[0].getVersionNumber());
    Assert.assertEquals("7a22a28993125fad78648d72338a93f2",
        HexFormat.of().formatHex(jdkKeys[0].getEncoded()));
  }

  @Test
  public void testTranslateEncryptionTypes() throws Exception {
    KerberosOperationHandler handler = createHandler();

    Assert.assertEquals(
        new HashSet<EncryptionType>() {{
          add(EncryptionType.AES256_CTS_HMAC_SHA1_96);
          add(EncryptionType.AES128_CTS_HMAC_SHA1_96);
          add(EncryptionType.DES3_CBC_SHA1_KD);
          add(EncryptionType.DES_CBC_MD5);
          add(EncryptionType.DES_CBC_MD4);
          add(EncryptionType.DES_CBC_CRC);
          add(EncryptionType.NONE);
        }},
        handler.translateEncryptionTypes("aes256-cts-hmac-sha1-96\n aes128-cts-hmac-sha1-96\tdes3-cbc-sha1 arcfour-hmac-md5 " +
            "camellia256-cts-cmac camellia128-cts-cmac des-cbc-crc des-cbc-md5 des-cbc-md4", "\\s+")
    );

    Assert.assertEquals(
        new HashSet<EncryptionType>() {{
          add(EncryptionType.AES256_CTS_HMAC_SHA1_96);
          add(EncryptionType.AES128_CTS_HMAC_SHA1_96);
        }},
        handler.translateEncryptionTypes("aes", " ")
    );

    Assert.assertEquals(
        new HashSet<EncryptionType>() {{
          add(EncryptionType.AES256_CTS_HMAC_SHA1_96);
        }},
        handler.translateEncryptionTypes("aes-256", " ")
    );

    Assert.assertEquals(
        new HashSet<EncryptionType>() {{
          add(EncryptionType.DES3_CBC_SHA1_KD);
        }},
        handler.translateEncryptionTypes("des3", " ")
    );
  }

  @Test(expected = KerberosOperationException.class)
  public void testTranslateWrongEncryptionTypes() throws Exception {
    KerberosOperationHandler handler = createHandler();
    handler.translateEncryptionTypes("aes-255", " ");
  }

  @Test
  public void testEscapeCharacters() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    HashSet<Character> specialCharacters = new HashSet<Character>() {
      {
        add('/');
        add(',');
        add('\\');
        add('#');
        add('+');
        add('<');
        add('>');
        add(';');
        add('"');
        add('=');
        add(' ');
      }
    };

    Assert.assertEquals("\\/\\,\\\\\\#\\+\\<\\>\\;\\\"\\=\\ ", handler.escapeCharacters("/,\\#+<>;\"= ", specialCharacters, '\\'));
    Assert.assertNull(handler.escapeCharacters(null, specialCharacters, '\\'));
    Assert.assertEquals("", handler.escapeCharacters("", specialCharacters, '\\'));
    Assert.assertEquals("nothing_special_here", handler.escapeCharacters("nothing_special_here", specialCharacters, '\\'));
    Assert.assertEquals("\\/\\,\\\\\\#\\+\\<\\>\\;\\\"\\=\\ ", handler.escapeCharacters("/,\\#+<>;\"= ", specialCharacters, '\\'));

    Assert.assertEquals("nothing<>special#here!", handler.escapeCharacters("nothing<>special#here!", null, '\\'));
    Assert.assertEquals("nothing<>special#here!", handler.escapeCharacters("nothing<>special#here!", Collections.emptySet(), '\\'));
    Assert.assertEquals("nothing<>special#here!", handler.escapeCharacters("nothing<>special#here!", Collections.singleton('?'), '\\'));
    Assert.assertEquals("\\A's are special!", handler.escapeCharacters("A's are special!", Collections.singleton('A'), '\\'));
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testAdminCredentialsNullPrincipal() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    PrincipalKeyCredential credentials = new PrincipalKeyCredential(null, "password");
    handler.setAdministratorCredential(credentials);
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testAdminCredentialsEmptyPrincipal() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    PrincipalKeyCredential credentials = new PrincipalKeyCredential("", "password");
    handler.setAdministratorCredential(credentials);
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testAdminCredentialsNullCredential() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    PrincipalKeyCredential credentials = new PrincipalKeyCredential("principal", (char[]) null);
    handler.setAdministratorCredential(credentials);
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testAdminCredentialsEmptyCredential1() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    PrincipalKeyCredential credentials = new PrincipalKeyCredential("principal", "");
    handler.setAdministratorCredential(credentials);
  }

  @Test
  public void testSetExecutableSearchPaths() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    handler.setExecutableSearchPaths((String) null);
    Assert.assertNull(handler.getExecutableSearchPaths());

    handler.setExecutableSearchPaths((String[]) null);
    Assert.assertNull(handler.getExecutableSearchPaths());

    handler.setExecutableSearchPaths("");
    Assert.assertNotNull(handler.getExecutableSearchPaths());
    Assert.assertEquals(0, handler.getExecutableSearchPaths().length);

    handler.setExecutableSearchPaths(new String[0]);
    Assert.assertNotNull(handler.getExecutableSearchPaths());
    Assert.assertEquals(0, handler.getExecutableSearchPaths().length);

    handler.setExecutableSearchPaths(new String[]{""});
    Assert.assertNotNull(handler.getExecutableSearchPaths());
    Assert.assertEquals(1, handler.getExecutableSearchPaths().length);

    handler.setExecutableSearchPaths("/path1, path2, path3/");
    Assert.assertNotNull(handler.getExecutableSearchPaths());
    Assert.assertEquals(3, handler.getExecutableSearchPaths().length);
    Assert.assertEquals("/path1", handler.getExecutableSearchPaths()[0]);
    Assert.assertEquals("path2", handler.getExecutableSearchPaths()[1]);
    Assert.assertEquals("path3/", handler.getExecutableSearchPaths()[2]);

    handler.setExecutableSearchPaths("/path1, path2, ,path3/");
    Assert.assertNotNull(handler.getExecutableSearchPaths());
    Assert.assertEquals(3, handler.getExecutableSearchPaths().length);
    Assert.assertEquals("/path1", handler.getExecutableSearchPaths()[0]);
    Assert.assertEquals("path2", handler.getExecutableSearchPaths()[1]);
    Assert.assertEquals("path3/", handler.getExecutableSearchPaths()[2]);

    handler.setExecutableSearchPaths(new String[]{"/path1", "path2", "path3/"});
    Assert.assertNotNull(handler.getExecutableSearchPaths());
    Assert.assertEquals(3, handler.getExecutableSearchPaths().length);
    Assert.assertEquals("/path1", handler.getExecutableSearchPaths()[0]);
    Assert.assertEquals("path2", handler.getExecutableSearchPaths()[1]);
    Assert.assertEquals("path3/", handler.getExecutableSearchPaths()[2]);
  }

  protected abstract KerberosOperationHandler createMockedHandler() throws KerberosOperationException;

  protected abstract void setupOpenSuccess(KerberosOperationHandler handler) throws Exception;

  protected abstract void setupOpenFailure(KerberosOperationHandler handler) throws Exception;

  protected abstract void setupPrincipalAlreadyExists(KerberosOperationHandler handler, boolean service) throws Exception;

  protected abstract void setupPrincipalDoesNotExist(KerberosOperationHandler handler, boolean service) throws Exception;

  protected abstract void setupPrincipalExists(KerberosOperationHandler handler, boolean service) throws Exception;

  protected abstract Map<String, String> getKerberosEnv();

  protected PrincipalKeyCredential getAdminCredentials() {
    return DEFAULT_ADMIN_CREDENTIALS;
  }

  private KerberosOperationHandler createHandler() throws KerberosOperationException {
    KerberosOperationHandler handler = new KerberosOperationHandler() {

      @Override
      public void open(PrincipalKeyCredential administratorCredentials, String defaultRealm, Map<String, String> kerberosConfiguration) throws KerberosOperationException {
        setAdministratorCredential(administratorCredentials);
        setDefaultRealm(defaultRealm);
        setExecutableSearchPaths("/usr/bin, /usr/kerberos/bin, /usr/sbin");
      }

      @Override
      public void close() throws KerberosOperationException {

      }

      @Override
      public boolean principalExists(String principal, boolean service) throws KerberosOperationException {
        return false;
      }

      @Override
      public Integer createPrincipal(String principal, String password, boolean service) throws KerberosOperationException {
        return 0;
      }

      @Override
      public Integer setPrincipalPassword(String principal, String password, boolean service) throws KerberosOperationException {
        return 0;
      }

      @Override
      public boolean removePrincipal(String principal, boolean service) throws KerberosOperationException {
        return false;
      }
    };

    handler.open(new PrincipalKeyCredential("admin/admin", "hadoop"), "EXAMPLE.COM", null);
    return handler;
  }

  private String createPrincipal(boolean service) {
    return String.format("%s@%s", (service) ? "service/host" : "user", DEFAULT_REALM);
  }
}
