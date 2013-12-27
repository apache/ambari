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

package org.apache.ambari.server.bootstrap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.bootstrap.BootStrapStatus.BSStat;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * Test BootStrap Implementation.
 */
public class BootStrapTest extends TestCase {
  private static Log LOG = LogFactory.getLog(BootStrapTest.class);
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    temp.create();
  }

  @After
  public void tearDown() throws IOException {
    temp.delete();
  }

  @Test
  public void testRun() throws Exception {
    Properties properties = new Properties();
    String bootdir =  temp.newFolder("bootdir").toString();
    String metadetadir =  temp.newFolder("metadetadir").toString();
    String serverVersionFilePath =  temp.newFolder("serverVersionFilePath").toString();
    LOG.info("Bootdir is " + bootdir);
    LOG.info("Metadetadir is " + metadetadir);
    LOG.info("ServerVersionFilePath is " + serverVersionFilePath);
    properties.setProperty(Configuration.BOOTSTRAP_DIR,
       bootdir);
    properties.setProperty(Configuration.BOOTSTRAP_SCRIPT, "echo");
    properties.setProperty(Configuration.SRVR_KSTR_DIR_KEY, "target" + File.separator + "classes");
    properties.setProperty(Configuration.METADETA_DIR_PATH, metadetadir);
    properties.setProperty(Configuration.SERVER_VERSION_FILE, serverVersionFilePath);
    Configuration conf = new Configuration(properties);
    AmbariMetaInfo ambariMetaInfo = new AmbariMetaInfo(conf);
    BootStrapImpl impl = new BootStrapImpl(conf, ambariMetaInfo);
    impl.init();
    SshHostInfo info = new SshHostInfo();
    info.setSshKey("xyz");
    ArrayList<String> hosts = new ArrayList<String>();
    hosts.add("host1");
    hosts.add("host2");
    info.setHosts(hosts);
    info.setUser("user");
    info.setPassword("passwd");
    BSResponse response = impl.runBootStrap(info);
    LOG.info("Response id from bootstrap " + response.getRequestId());
    /* do a query */
    BootStrapStatus status = impl.getStatus(response.getRequestId());
    LOG.info("Status " + status.getStatus());
    int num = 0;
    while ((status.getStatus() != BSStat.SUCCESS) && (num < 10000)) {
        status = impl.getStatus(response.getRequestId());
        Thread.sleep(100);
        num++;
    }
    LOG.info("Status: log " + status.getLog() + " status=" + status.getStatus()
        );
    /* Note its an echo command so it should echo host1,host2 */
    Assert.assertTrue(status.getLog().contains("host1,host2"));
    Assert.assertEquals(BSStat.SUCCESS, status.getStatus());
    Assert.assertFalse(new File(bootdir + File.separator + "1" + File.separator + "sshKey").exists());
    Assert.assertFalse(new File(bootdir + File.separator + "1" + File.separator + "host_pass").exists());
  }


  @Test
  public void testPolling() throws Exception {
    File tmpFolder = temp.newFolder("bootstrap");
    /* create log and done files */
    FileUtils.writeStringToFile(new File(tmpFolder, "host1.done"), "0");
    FileUtils.writeStringToFile(new File(tmpFolder, "host1.log"), "err_log_1");
    FileUtils.writeStringToFile(new File(tmpFolder, "host2.done"), "1");
    FileUtils.writeStringToFile(new File(tmpFolder, "host2.log"), "err_log_2");

    List<String> listHosts = new ArrayList<String>();
    listHosts.add("host1");
    listHosts.add("host2");
    BSHostStatusCollector collector = new BSHostStatusCollector(tmpFolder,
        listHosts);
    collector.run();
    List<BSHostStatus> polledHostStatus = collector.getHostStatus();
    Assert.assertTrue(polledHostStatus.size() == 2);
    Assert.assertEquals(polledHostStatus.get(0).getHostName(), "host1");
    Assert.assertEquals(polledHostStatus.get(0).getLog(), "err_log_1");
    Assert.assertEquals(polledHostStatus.get(0).getStatus(), "DONE");
    Assert.assertEquals(polledHostStatus.get(1).getHostName(), "host2");
    Assert.assertEquals(polledHostStatus.get(1).getLog(), "err_log_2");
    Assert.assertEquals(polledHostStatus.get(1).getStatus(), "FAILED");


  }

}
