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

package org.apache.ambari.logfeeder.input;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.logfeeder.conf.LogEntryCacheConfig;
import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.plugin.filter.Filter;
import org.apache.ambari.logfeeder.plugin.input.InputMarker;
import org.apache.ambari.logfeeder.plugin.manager.InputManager;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.InputFileDescriptorImpl;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class InputFileTest {
  private static final Logger LOG = Logger.getLogger(InputFileTest.class);

  private static final String TEST_DIR_NAME = "/logfeeder_test_dir/";
  private static final File TEST_DIR = new File(FileUtils.getTempDirectoryPath() + TEST_DIR_NAME);

  private static final String TEST_LOG_FILE_CONTENT = "2016-03-10 14:09:38,278 INFO  datanode.DataNode (DataNode.java:<init>(418)) - File descriptor passing is enabled.\n"
      + "2016-03-10 14:09:38,278 INFO  datanode.DataNode (DataNode.java:<init>(429)) - Configured hostname is c6401.ambari.apache.org\n"
      + "2016-03-10 14:09:38,294 INFO  datanode.DataNode (DataNode.java:startDataNode(1127)) - Starting DataNode with maxLockedMemory = 0\n"
      + "2016-03-10 14:09:38,340 INFO  datanode.DataNode (DataNode.java:initDataXceiver(921)) - Opened streaming server at /0.0.0.0:50010\n"
      + "2016-03-10 14:09:38,343 INFO  datanode.DataNode (DataXceiverServer.java:<init>(76)) - Balancing bandwith is 6250000 bytes/s\n"
      + "2016-03-10 14:09:38,343 INFO  datanode.DataNode (DataXceiverServer.java:<init>(77)) - Number threads for balancing is 5\n"
      + "2016-03-10 14:09:38,345 INFO  datanode.DataNode (DataXceiverServer.java:<init>(76)) - Balancing bandwith is 6250000 bytes/s\n"
      + "2016-03-10 14:09:38,346 INFO  datanode.DataNode (DataXceiverServer.java:<init>(77)) - Number threads for balancing is 5\n";

  private static final String[] TEST_LOG_FILE_ROWS = TEST_LOG_FILE_CONTENT.split("\n");
  private InputFile inputFile;
  private List<String> rows = new ArrayList<>();

  private InputFileMarker testInputMarker;

  private LogFeederProps logFeederProps;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void initDir() throws IOException {
    if (!TEST_DIR.exists()) {
      TEST_DIR.mkdir();
    }
    FileUtils.cleanDirectory(TEST_DIR);
  }

  @Before
  public void setUp() throws Exception {
    logFeederProps = new LogFeederProps();
    LogEntryCacheConfig logEntryCacheConfig = new LogEntryCacheConfig();
    logEntryCacheConfig.setCacheEnabled(false);
    logEntryCacheConfig.setCacheLastDedupEnabled(false);
    logEntryCacheConfig.setCacheSize(10);
    logFeederProps.setLogEntryCacheConfig(logEntryCacheConfig);
    testInputMarker = new InputFileMarker(inputFile, "", 0);
  }

  public void init(String path) throws Exception {
    InputFileDescriptorImpl inputFileDescriptor = new InputFileDescriptorImpl();
    inputFileDescriptor.setSource("file");
    inputFileDescriptor.setTail(true);
    inputFileDescriptor.setGenEventMd5(true);
    inputFileDescriptor.setType("hdfs_datanode");
    inputFileDescriptor.setRowtype("service");
    inputFileDescriptor.setPath(path);

    Filter capture = new Filter<LogFeederProps>() {
      @Override
      public void init(LogFeederProps logFeederProps) {
      }

      @Override
      public String getShortDescription() {
        return null;
      }

      @Override
      public void apply(String inputStr, InputMarker inputMarker) {
        rows.add(inputStr);
        if (rows.size() % 3 == 0)
          inputFile.setDrain(true);
      }
    };

    inputFile = new InputFile();
    inputFile.loadConfig(inputFileDescriptor);
    inputFile.addFilter(capture);
    inputFile.init(logFeederProps);
  }

  @Test
  public void testInputFile_process3Rows() throws Exception {
    LOG.info("testInputFile_process3Rows()");

    File checkPointDir = createCheckpointDir("process3_checkpoint");
    File testFile = createFile("process3.log");

    init(testFile.getAbsolutePath());

    InputManager inputManager = EasyMock.createStrictMock(InputManager.class);
    EasyMock.expect(inputManager.getCheckPointFolderFile()).andReturn(checkPointDir);
    EasyMock.replay(inputManager);
    inputFile.setInputManager(inputManager);

    inputFile.isReady();
    inputFile.start();

    assertEquals("Amount of the rows is incorrect", rows.size(), 3);
    for (int row = 0; row < 3; row++)
      assertEquals("Row #" + (row + 1) + " not correct", TEST_LOG_FILE_ROWS[row], rows.get(row));

    EasyMock.verify(inputManager);
  }

  @Test
  public void testInputFile_noLogPath() throws Exception {
    LOG.info("testInputFile_noLogPath()");

    expectedException.expect(NullPointerException.class);

    init(null);
    inputFile.isReady();
  }

  @After
  public void tearDown() throws Exception {
    rows.clear();
  }

  @AfterClass
  public static void cleanUp() throws Exception {
    FileUtils.deleteDirectory(TEST_DIR);
  }

  private File createFile(String filename) throws IOException {
    File newFile = new File(FileUtils.getTempDirectoryPath() + TEST_DIR_NAME + filename);
    FileUtils.writeStringToFile(newFile, TEST_LOG_FILE_CONTENT, Charset.defaultCharset());
    return newFile;
  }

  private File createCheckpointDir(String dirname) {
    File newDir = new File(TEST_DIR + "/" + dirname);
    if (!newDir.exists()) {
      newDir.mkdir();
    }
    return newDir;
  }
}
