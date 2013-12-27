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
import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*", "org.apache.log4j.*"})
@PrepareForTest({ MasterKeyServiceImpl.class })
public class MasterKeyServiceTest extends TestCase {
  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();
  private String fileDir;
  private static final Log LOG = LogFactory.getLog
    (MasterKeyServiceTest.class);

  @Override
  protected void setUp() throws Exception {
    tmpFolder.create();
    fileDir = tmpFolder.newFolder("keys").getAbsolutePath();
    LOG.info("Setting temp folder to: " + fileDir);
  }

  @Test
  public void testInitiliazeMasterKey() throws Exception {
    MasterKeyService ms = new MasterKeyServiceImpl("ThisisSomePassPhrase",
      fileDir + File.separator + "master", true);
    Assert.assertTrue(ms.isMasterKeyInitialized());
    File f = new File(fileDir + File.separator + "master");
    Assert.assertTrue(f.exists());
    // Re-initialize master from file
    MasterKeyService ms1 = new MasterKeyServiceImpl(fileDir + File.separator
      + "master", true);
    Assert.assertTrue(ms1.isMasterKeyInitialized());
    Assert.assertEquals("ThisisSomePassPhrase", new String(ms1.getMasterSecret
      ()));
  }

  @Test
  public void testReadFromEnvAsKey() throws Exception {
    Map<String, String> mapRet = new HashMap<String, String>();
    mapRet.put(Configuration.MASTER_KEY_ENV_PROP, "ThisisSomePassPhrase");
    mockStatic(System.class);
    expect(System.getenv()).andReturn(mapRet);
    replayAll();
    MasterKeyService ms = new MasterKeyServiceImpl();
    verifyAll();
    Assert.assertTrue(ms.isMasterKeyInitialized());
    Assert.assertNotNull(ms.getMasterSecret());
    Assert.assertEquals("ThisisSomePassPhrase",
      new String(ms.getMasterSecret()));
  }

  @Test
  public void testReadFromEnvAsPath() throws Exception {
    // Create a master key
    MasterKeyService ms = new MasterKeyServiceImpl("ThisisSomePassPhrase",
      fileDir + File.separator + "master", true);
    Assert.assertTrue(ms.isMasterKeyInitialized());
    File f = new File(fileDir + File.separator + "master");
    Assert.assertTrue(f.exists());

    Map<String, String> mapRet = new HashMap<String, String>();
    mapRet.put(Configuration.MASTER_KEY_LOCATION, f.getAbsolutePath());
    mockStatic(System.class);
    expect(System.getenv()).andReturn(mapRet);
    replayAll();
    ms = new MasterKeyServiceImpl();
    verifyAll();
    Assert.assertTrue(ms.isMasterKeyInitialized());
    Assert.assertNotNull(ms.getMasterSecret());
    Assert.assertEquals("ThisisSomePassPhrase",
      new String(ms.getMasterSecret()));
    Assert.assertFalse(f.exists());
  }

  @Override
  protected void tearDown() throws Exception {
    tmpFolder.delete();
  }

}
