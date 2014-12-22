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


package org.apache.ambari.server.agent;


import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.serveraction.kerberos.KerberosActionDataFile;
import org.apache.ambari.server.serveraction.kerberos.KerberosActionDataFileBuilder;
import org.apache.ambari.server.serveraction.kerberos.KerberosServerAction;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests injectKeytab method of HeartBeatHandler
 */
public class HeartBeatHandlerInjectKeytabTest  {

  String dataDir;
  
  @Before
  public void setup() throws Exception {
      File temporaryDirectory;
      File indexFile;
      KerberosActionDataFileBuilder kerberosActionDataFileBuilder = null;

      try {
          temporaryDirectory = File.createTempFile(".ambari_", ".d");
      } catch (IOException e) {
          throw new AmbariException("Unexpected error", e);
      }

      // Convert the temporary file into a temporary directory...
      if (!temporaryDirectory.delete() || !temporaryDirectory.mkdirs()) {
          throw new AmbariException("Failed to create temporary directory");
      }

      dataDir = temporaryDirectory.getAbsolutePath();

      indexFile = new File(temporaryDirectory, KerberosActionDataFile.DATA_FILE_NAME);
      kerberosActionDataFileBuilder = new KerberosActionDataFileBuilder(indexFile);

      kerberosActionDataFileBuilder.addRecord("c6403.ambari.apache.org", "HDFS", "DATANODE",
              "dn/_HOST@_REALM", "hdfs-site/dfs.namenode.kerberos.principal",
              "/etc/security/keytabs/dn.service.keytab",
              "hdfs", "r", "hadoop", "", "hdfs-site/dfs.namenode.keytab.file");

      kerberosActionDataFileBuilder.close();
      File hostDirectory = new File(dataDir, "c6403.ambari.apache.org");

      // Ensure the host directory exists...
      if (hostDirectory.exists() || hostDirectory.mkdirs()) {
          File file = new File(hostDirectory, DigestUtils.sha1Hex("/etc/security/keytabs/dn.service.keytab"));
          if (!file.exists()) {
              file.createNewFile();
          }

          FileWriter fw = new FileWriter(file.getAbsoluteFile());
          BufferedWriter bw = new BufferedWriter(fw);
          bw.write("hello");
          bw.close();
      }
  }

    @Test
    public void testInjectKeytabApplicableHost() throws Exception {

        ExecutionCommand executionCommand = new ExecutionCommand();

        Map<String, String> hlp = new HashMap<String, String>();
        hlp.put("custom_command", "SET_KEYTAB");
        executionCommand.setHostLevelParams(hlp);

        Map<String, String> commandparams = new HashMap<String, String>();
        commandparams.put(KerberosServerAction.DATA_DIRECTORY, dataDir);
        executionCommand.setCommandParams(commandparams);

        HeartBeatHandler.injectKeytab(executionCommand, "c6403.ambari.apache.org");

        List<Map<String, String>> kcp = executionCommand.getKerberosCommandParams();

        Assert.assertEquals("c6403.ambari.apache.org", kcp.get(0).get(KerberosActionDataFile.HOSTNAME));
        Assert.assertEquals("HDFS", kcp.get(0).get(KerberosActionDataFile.SERVICE));
        Assert.assertEquals("DATANODE", kcp.get(0).get(KerberosActionDataFile.COMPONENT));
        Assert.assertEquals("dn/_HOST@_REALM", kcp.get(0).get(KerberosActionDataFile.PRINCIPAL));
        Assert.assertEquals("hdfs-site/dfs.namenode.kerberos.principal", kcp.get(0).get(KerberosActionDataFile.PRINCIPAL_CONFIGURATION));
        Assert.assertEquals("/etc/security/keytabs/dn.service.keytab", kcp.get(0).get(KerberosActionDataFile.KEYTAB_FILE_PATH));
        Assert.assertEquals("hdfs", kcp.get(0).get(KerberosActionDataFile.KEYTAB_FILE_OWNER_NAME));
        Assert.assertEquals("r", kcp.get(0).get(KerberosActionDataFile.KEYTAB_FILE_OWNER_ACCESS));
        Assert.assertEquals("hadoop", kcp.get(0).get(KerberosActionDataFile.KEYTAB_FILE_GROUP_NAME));
        Assert.assertEquals("", kcp.get(0).get(KerberosActionDataFile.KEYTAB_FILE_GROUP_ACCESS));
        Assert.assertEquals("hdfs-site/dfs.namenode.keytab.file", kcp.get(0).get(KerberosActionDataFile.KEYTAB_FILE_CONFIGURATION));

        Assert.assertEquals(Base64.encodeBase64String("hello".getBytes()), kcp.get(0).get(KerberosServerAction.KEYTAB_CONTENT_BASE64));

    }

  @Test
  public void testInjectKeytabNotApplicableHost() throws Exception {
      ExecutionCommand executionCommand = new ExecutionCommand();

      Map<String, String> hlp = new HashMap<String, String>();
      hlp.put("custom_command", "SET_KEYTAB");
      executionCommand.setHostLevelParams(hlp);

      Map<String, String> commandparams = new HashMap<String, String>();
      commandparams.put(KerberosServerAction.DATA_DIRECTORY, dataDir);
      executionCommand.setCommandParams(commandparams);

      HeartBeatHandler.injectKeytab(executionCommand, "c6400.ambari.apache.org");

      List<Map<String, String>> kcp = executionCommand.getKerberosCommandParams();
      Assert.assertTrue(kcp.isEmpty());

  }

}
