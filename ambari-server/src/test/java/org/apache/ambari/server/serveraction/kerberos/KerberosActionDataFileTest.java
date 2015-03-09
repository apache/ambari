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
import java.util.Iterator;
import java.util.Map;

/**
 * This is a test to see how well the KerberosActionDataFileBuilder and KerberosActionDataFileReader
 * work when the data temporaryDirectory is opened, close, reopened, and appended to.
 */
public class KerberosActionDataFileTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testKerberosActionDataFile() throws Exception {
    File file = folder.newFile();
    Assert.assertNotNull(file);

    // Write the data
    KerberosActionDataFileBuilder builder = new KerberosActionDataFileBuilder(file);
    Assert.assertFalse(builder.isClosed());

    for (int i = 0; i < 10; i++) {
      builder.addRecord("hostName" + i, "serviceName" + i, "serviceComponentName" + i,
          "principal" + i, "principal_type" + i, "principalConfiguration" + i, "keytabFilePath" + i,
          "keytabFileOwnerName" + i, "keytabFileOwnerAccess" + i,
          "keytabFileGroupName" + i, "keytabFileGroupAccess" + i,
          "keytabFileConfiguration" + i, "false");
    }

    // Add some odd characters
    builder.addRecord("hostName's", "serviceName#", "serviceComponentName\"",
        "principal", "principal_type", "principalConfiguration", "keytabFilePath",
        "'keytabFileOwnerName'", "<keytabFileOwnerAccess>",
        "\"keytabFileGroupName\"", "keytab,File,Group,Access",
        "\"keytab,'File',Configuration\"", "false");

    builder.close();
    Assert.assertTrue(builder.isClosed());

    // Read the data...
    KerberosActionDataFileReader reader = new KerberosActionDataFileReader(file);
    Assert.assertFalse(reader.isClosed());

    Iterator<Map<String, String>> iterator = reader.iterator();
    Assert.assertNotNull(iterator);

    // Test iterator
    int i = 0;
    while (iterator.hasNext()) {
      Map<String, String> record = iterator.next();

      if (i < 10) {
        Assert.assertEquals("hostName" + i, record.get(KerberosActionDataFile.HOSTNAME));
        Assert.assertEquals("serviceName" + i, record.get(KerberosActionDataFile.SERVICE));
        Assert.assertEquals("serviceComponentName" + i, record.get(KerberosActionDataFile.COMPONENT));
        Assert.assertEquals("principal" + i, record.get(KerberosActionDataFile.PRINCIPAL));
        Assert.assertEquals("principal_type" + i, record.get(KerberosActionDataFile.PRINCIPAL_TYPE));
        Assert.assertEquals("principalConfiguration" + i, record.get(KerberosActionDataFile.PRINCIPAL_CONFIGURATION));
        Assert.assertEquals("keytabFilePath" + i, record.get(KerberosActionDataFile.KEYTAB_FILE_PATH));
        Assert.assertEquals("keytabFileOwnerName" + i, record.get(KerberosActionDataFile.KEYTAB_FILE_OWNER_NAME));
        Assert.assertEquals("keytabFileOwnerAccess" + i, record.get(KerberosActionDataFile.KEYTAB_FILE_OWNER_ACCESS));
        Assert.assertEquals("keytabFileGroupName" + i, record.get(KerberosActionDataFile.KEYTAB_FILE_GROUP_NAME));
        Assert.assertEquals("keytabFileGroupAccess" + i, record.get(KerberosActionDataFile.KEYTAB_FILE_GROUP_ACCESS));
        Assert.assertEquals("keytabFileConfiguration" + i, record.get(KerberosActionDataFile.KEYTAB_FILE_CONFIGURATION));
        Assert.assertEquals("false", record.get(KerberosActionDataFile.KEYTAB_FILE_IS_CACHABLE));
      } else {
        Assert.assertEquals("hostName's", record.get(KerberosActionDataFile.HOSTNAME));
        Assert.assertEquals("serviceName#", record.get(KerberosActionDataFile.SERVICE));
        Assert.assertEquals("serviceComponentName\"", record.get(KerberosActionDataFile.COMPONENT));
        Assert.assertEquals("principal", record.get(KerberosActionDataFile.PRINCIPAL));
        Assert.assertEquals("principal_type", record.get(KerberosActionDataFile.PRINCIPAL_TYPE));
        Assert.assertEquals("principalConfiguration", record.get(KerberosActionDataFile.PRINCIPAL_CONFIGURATION));
        Assert.assertEquals("keytabFilePath", record.get(KerberosActionDataFile.KEYTAB_FILE_PATH));
        Assert.assertEquals("'keytabFileOwnerName'", record.get(KerberosActionDataFile.KEYTAB_FILE_OWNER_NAME));
        Assert.assertEquals("<keytabFileOwnerAccess>", record.get(KerberosActionDataFile.KEYTAB_FILE_OWNER_ACCESS));
        Assert.assertEquals("\"keytabFileGroupName\"", record.get(KerberosActionDataFile.KEYTAB_FILE_GROUP_NAME));
        Assert.assertEquals("keytab,File,Group,Access", record.get(KerberosActionDataFile.KEYTAB_FILE_GROUP_ACCESS));
        Assert.assertEquals("\"keytab,'File',Configuration\"", record.get(KerberosActionDataFile.KEYTAB_FILE_CONFIGURATION));
        Assert.assertEquals("false", record.get(KerberosActionDataFile.KEYTAB_FILE_IS_CACHABLE));
      }

      i++;
    }

    reader.close();
    Assert.assertTrue(reader.isClosed());
    reader.open();
    Assert.assertFalse(reader.isClosed());

    i = 0;
    for (Map<String, String> record : reader) {
      if (i < 10) {
        Assert.assertEquals("hostName" + i, record.get(KerberosActionDataFile.HOSTNAME));
        Assert.assertEquals("serviceName" + i, record.get(KerberosActionDataFile.SERVICE));
        Assert.assertEquals("serviceComponentName" + i, record.get(KerberosActionDataFile.COMPONENT));
        Assert.assertEquals("principal" + i, record.get(KerberosActionDataFile.PRINCIPAL));
        Assert.assertEquals("principal_type" + i, record.get(KerberosActionDataFile.PRINCIPAL_TYPE));
        Assert.assertEquals("principalConfiguration" + i, record.get(KerberosActionDataFile.PRINCIPAL_CONFIGURATION));
        Assert.assertEquals("keytabFilePath" + i, record.get(KerberosActionDataFile.KEYTAB_FILE_PATH));
        Assert.assertEquals("keytabFileOwnerName" + i, record.get(KerberosActionDataFile.KEYTAB_FILE_OWNER_NAME));
        Assert.assertEquals("keytabFileOwnerAccess" + i, record.get(KerberosActionDataFile.KEYTAB_FILE_OWNER_ACCESS));
        Assert.assertEquals("keytabFileGroupName" + i, record.get(KerberosActionDataFile.KEYTAB_FILE_GROUP_NAME));
        Assert.assertEquals("keytabFileGroupAccess" + i, record.get(KerberosActionDataFile.KEYTAB_FILE_GROUP_ACCESS));
        Assert.assertEquals("keytabFileConfiguration" + i, record.get(KerberosActionDataFile.KEYTAB_FILE_CONFIGURATION));
      } else {
        Assert.assertEquals("hostName's", record.get(KerberosActionDataFile.HOSTNAME));
        Assert.assertEquals("serviceName#", record.get(KerberosActionDataFile.SERVICE));
        Assert.assertEquals("serviceComponentName\"", record.get(KerberosActionDataFile.COMPONENT));
        Assert.assertEquals("principal", record.get(KerberosActionDataFile.PRINCIPAL));
        Assert.assertEquals("principal_type", record.get(KerberosActionDataFile.PRINCIPAL_TYPE));
        Assert.assertEquals("principalConfiguration", record.get(KerberosActionDataFile.PRINCIPAL_CONFIGURATION));
        Assert.assertEquals("keytabFilePath", record.get(KerberosActionDataFile.KEYTAB_FILE_PATH));
        Assert.assertEquals("'keytabFileOwnerName'", record.get(KerberosActionDataFile.KEYTAB_FILE_OWNER_NAME));
        Assert.assertEquals("<keytabFileOwnerAccess>", record.get(KerberosActionDataFile.KEYTAB_FILE_OWNER_ACCESS));
        Assert.assertEquals("\"keytabFileGroupName\"", record.get(KerberosActionDataFile.KEYTAB_FILE_GROUP_NAME));
        Assert.assertEquals("keytab,File,Group,Access", record.get(KerberosActionDataFile.KEYTAB_FILE_GROUP_ACCESS));
        Assert.assertEquals("\"keytab,'File',Configuration\"", record.get(KerberosActionDataFile.KEYTAB_FILE_CONFIGURATION));
      }

      i++;
    }

    reader.close();
    Assert.assertTrue(reader.isClosed());

    // Add an additional record
    builder.open();
    Assert.assertFalse(builder.isClosed());

    builder.addRecord("hostName", "serviceName", "serviceComponentName",
        "principal","principal_type", "principalConfiguration", "keytabFilePath",
        "keytabFileOwnerName", "keytabFileOwnerAccess",
        "keytabFileGroupName", "keytabFileGroupAccess",
        "keytabFileConfiguration", "true");

    builder.close();
    Assert.assertTrue(builder.isClosed());

    reader = new KerberosActionDataFileReader(file);
    Assert.assertFalse(reader.isClosed());

    i = 0;
    for (Map<String, String> record : reader) {
      i++;
    }

    Assert.assertEquals(12, i);

    reader.close();
    Assert.assertTrue(reader.isClosed());

    // Add an additional record
    builder = new KerberosActionDataFileBuilder(file);
    Assert.assertFalse(builder.isClosed());

    builder.addRecord("hostName", "serviceName", "serviceComponentName",
        "principal", "principal_type", "principalConfiguration", "keytabFilePath",
        "keytabFileOwnerName", "keytabFileOwnerAccess",
        "keytabFileGroupName", "keytabFileGroupAccess",
        "keytabFileConfiguration", "true");

    builder.close();
    Assert.assertTrue(builder.isClosed());

    reader.open();
    Assert.assertFalse(reader.isClosed());

    i = 0;
    for (Map<String, String> record : reader) {
      i++;
    }

    Assert.assertEquals(13, i);

    reader.close();
    Assert.assertTrue(reader.isClosed());

    // trying to iterate over a closed reader...
    i = 0;
    for (Map<String, String> record : reader) {
      i++;
    }
    Assert.assertEquals(0, i);

  }
}