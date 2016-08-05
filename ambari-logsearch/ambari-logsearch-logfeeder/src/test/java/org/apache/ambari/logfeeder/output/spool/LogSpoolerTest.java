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

package org.apache.ambari.logfeeder.output.spool;

import org.easymock.EasyMockRule;
import org.easymock.LogicalOperator;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;

import static org.easymock.EasyMock.*;

public class LogSpoolerTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  private String spoolDirectory;
  private static final String SOURCE_FILENAME_PREFIX = "hdfs-namenode.log";
  private static final String FILE_SUFFIX = "currentFile";

  @Mock
  private RolloverCondition rolloverCondition;

  @Mock
  private RolloverHandler rolloverHandler;

  @Before
  public void setup() {
    spoolDirectory = testFolder.getRoot().getAbsolutePath();
  }

  @Test
  public void shouldSpoolEventToFile() {
    final PrintWriter spoolWriter = mock(PrintWriter.class);
    spoolWriter.println("log event");

    final File spoolFile = new File(spoolDirectory, SOURCE_FILENAME_PREFIX + FILE_SUFFIX);
    LogSpoolerContext logSpoolerContext = new LogSpoolerContext(spoolFile);
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext, new LogSpoolerFileComparator(), LogicalOperator.EQUAL))).
        andReturn(false);

    replay(spoolWriter, rolloverCondition);

    LogSpooler logSpooler = new LogSpooler(spoolDirectory, SOURCE_FILENAME_PREFIX,
        rolloverCondition, rolloverHandler) {
      @Override
      protected PrintWriter initializeSpoolWriter(File spoolFile) throws IOException {
        return spoolWriter;
      }

      @Override
      protected String getCurrentFileName() {
        return SOURCE_FILENAME_PREFIX + FILE_SUFFIX;
      }
    };
    logSpooler.add("log event");

    verify(spoolWriter);
  }

  @Test
  public void shouldIncrementSpooledEventsCount() {

    final PrintWriter spoolWriter = mock(PrintWriter.class);
    spoolWriter.println("log event");

    final File spoolFile = new File(spoolDirectory, SOURCE_FILENAME_PREFIX + FILE_SUFFIX);
    LogSpoolerContext logSpoolerContext = new LogSpoolerContext(spoolFile);
    logSpoolerContext.logEventSpooled();
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext, new LogSpoolerEventCountComparator(), LogicalOperator.EQUAL))).
        andReturn(false);

    replay(spoolWriter, rolloverCondition);

    LogSpooler logSpooler = new LogSpooler(spoolDirectory, SOURCE_FILENAME_PREFIX,
        rolloverCondition, rolloverHandler) {
      @Override
      protected PrintWriter initializeSpoolWriter(File spoolFile) throws IOException {
        return spoolWriter;
      }

      @Override
      protected String getCurrentFileName() {
        return SOURCE_FILENAME_PREFIX + FILE_SUFFIX;
      }
    };
    logSpooler.add("log event");

    verify(rolloverCondition);
  }

  @Test
  public void shouldCloseCurrentSpoolFileOnRollOver() {
    final PrintWriter spoolWriter = mock(PrintWriter.class);
    spoolWriter.println("log event");
    spoolWriter.flush();
    spoolWriter.close();

    File spoolFile = new File(spoolDirectory, SOURCE_FILENAME_PREFIX + FILE_SUFFIX);
    LogSpoolerContext logSpoolerContext = new LogSpoolerContext(spoolFile);
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext, new LogSpoolerFileComparator(), LogicalOperator.EQUAL))).
        andReturn(true);
    rolloverHandler.handleRollover(spoolFile);

    replay(spoolWriter, rolloverCondition, rolloverHandler);

    LogSpooler logSpooler = new LogSpooler(spoolDirectory, SOURCE_FILENAME_PREFIX,
        rolloverCondition, rolloverHandler) {

      @Override
      protected PrintWriter initializeSpoolWriter(File spoolFile) throws IOException {
        return spoolWriter;
      }

      @Override
      protected String getCurrentFileName() {
        return SOURCE_FILENAME_PREFIX + FILE_SUFFIX;
      }
    };
    logSpooler.add("log event");

    verify(spoolWriter);
  }

  @Test
  public void shouldReinitializeFileOnRollover() {
    final PrintWriter spoolWriter1 = mock(PrintWriter.class);
    final PrintWriter spoolWriter2 = mock(PrintWriter.class);
    spoolWriter1.println("log event1");
    spoolWriter2.println("log event2");
    spoolWriter1.flush();
    spoolWriter1.close();

    File spoolFile1 = new File(spoolDirectory, SOURCE_FILENAME_PREFIX + FILE_SUFFIX + "_1");
    File spoolFile2 = new File(spoolDirectory, SOURCE_FILENAME_PREFIX + FILE_SUFFIX + "_2");

    LogSpoolerContext logSpoolerContext1 = new LogSpoolerContext(spoolFile1);
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext1, new LogSpoolerFileComparator(), LogicalOperator.EQUAL))
    ).andReturn(true);

    LogSpoolerContext logSpoolerContext2 = new LogSpoolerContext(spoolFile2);
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext2, new LogSpoolerFileComparator(), LogicalOperator.EQUAL))
    ).andReturn(false);

    rolloverHandler.handleRollover(spoolFile1);

    replay(spoolWriter1, spoolWriter2, rolloverCondition, rolloverHandler);

    LogSpooler logSpooler = new LogSpooler(spoolDirectory, SOURCE_FILENAME_PREFIX,
        rolloverCondition, rolloverHandler) {
      private boolean wasRolledOver;

      @Override
      protected PrintWriter initializeSpoolWriter(File spoolFile) throws IOException {
        if (!wasRolledOver) {
          wasRolledOver = true;
          return spoolWriter1;
        } else {
          return spoolWriter2;
        }
      }

      @Override
      protected String getCurrentFileName() {
        if (!wasRolledOver) {
          return SOURCE_FILENAME_PREFIX + FILE_SUFFIX + "_1";
        } else {
          return SOURCE_FILENAME_PREFIX + FILE_SUFFIX + "_2";
        }
      }
    };
    logSpooler.add("log event1");
    logSpooler.add("log event2");

    verify(spoolWriter1, spoolWriter2, rolloverCondition);
  }

  @Test
  public void shouldCallRolloverHandlerOnRollover() {
    final PrintWriter spoolWriter = mock(PrintWriter.class);
    spoolWriter.println("log event");
    spoolWriter.flush();
    spoolWriter.close();

    File spoolFile = new File(spoolDirectory, SOURCE_FILENAME_PREFIX + FILE_SUFFIX);
    LogSpoolerContext logSpoolerContext = new LogSpoolerContext(spoolFile);
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext, new LogSpoolerFileComparator(), LogicalOperator.EQUAL))
    ).andReturn(true);
    rolloverHandler.handleRollover(spoolFile);

    replay(spoolWriter, rolloverCondition, rolloverHandler);

    LogSpooler logSpooler = new LogSpooler(spoolDirectory, SOURCE_FILENAME_PREFIX,
        rolloverCondition, rolloverHandler) {

      @Override
      protected PrintWriter initializeSpoolWriter(File spoolFile) throws IOException {
        return spoolWriter;
      }

      @Override
      protected String getCurrentFileName() {
        return SOURCE_FILENAME_PREFIX + FILE_SUFFIX;
      }
    };
    logSpooler.add("log event");

    verify(rolloverHandler);
  }

  class LogSpoolerFileComparator implements Comparator<LogSpoolerContext> {
    @Override
    public int compare(LogSpoolerContext o1, LogSpoolerContext o2) {
      return o1.getActiveSpoolFile().compareTo(o2.getActiveSpoolFile());
    }
  }

  class LogSpoolerEventCountComparator implements Comparator<LogSpoolerContext> {
    @Override
    public int compare(LogSpoolerContext o1, LogSpoolerContext o2) {
      return (int)(o1.getNumEventsSpooled()-o2.getNumEventsSpooled());
    }
  }

}
