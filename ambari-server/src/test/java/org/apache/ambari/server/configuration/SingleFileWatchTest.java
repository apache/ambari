/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.configuration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.awaitility.Awaitility.await;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SingleFileWatchTest {
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();
  private SingleFileWatch watchDog;
  private File fileToWatch;
  private volatile int numberOfEventsReceived = 0;

  @Before
  public void setUp() throws Exception {
    fileToWatch = tmp.newFile();
    watchDog = new SingleFileWatch(fileToWatch, file -> numberOfEventsReceived++);
    watchDog.start();
    await().atMost(3, SECONDS).until(watchDog::isStarted);
  }

  @After
  public void tearDown() throws Exception {
    watchDog.stop();
  }

  @Test
  public void testTriggersEventsOnMultipleFileChange() throws Exception {
    assumeTrue(SystemUtils.IS_OS_LINUX); // the OSX implementation of WatchService is really slow
    changeFile("change1");
    await().atMost(15, SECONDS).until(() -> numberOfEventsReceived == 1);
    changeFile("change2");
    await().atMost(15, SECONDS).until(() -> numberOfEventsReceived == 2);
  }

  private void changeFile(String content) throws IOException {
    writeStringToFile(fileToWatch, content, "UTF-8");
  }
}