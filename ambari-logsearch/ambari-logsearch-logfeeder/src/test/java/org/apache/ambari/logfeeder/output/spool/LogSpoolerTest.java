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

    final File mockFile = setupInputFileExpectations();
    LogSpoolerContext logSpoolerContext = new LogSpoolerContext(mockFile);
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext, new LogSpoolerFileComparator(), LogicalOperator.EQUAL))).
        andReturn(false);

    replay(spoolWriter, rolloverCondition, mockFile);

    LogSpooler logSpooler = new LogSpooler(spoolDirectory, SOURCE_FILENAME_PREFIX,
        rolloverCondition, rolloverHandler) {
      @Override
      protected PrintWriter initializeSpoolWriter(File spoolFile) throws IOException {
        return spoolWriter;
      }

      @Override
      protected File initializeSpoolFile() {
        return mockFile;
      }
    };
    logSpooler.add("log event");

    verify(spoolWriter);
  }

  private File setupInputFileExpectations() {
    final File mockFile = mock(File.class);
    expect(mockFile.length()).andReturn(10240L);
    return mockFile;
  }

  @Test
  public void shouldIncrementSpooledEventsCount() {

    final PrintWriter spoolWriter = mock(PrintWriter.class);
    spoolWriter.println("log event");

    final File mockFile = setupInputFileExpectations();
    LogSpoolerContext logSpoolerContext = new LogSpoolerContext(mockFile);
    logSpoolerContext.logEventSpooled();
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext, new LogSpoolerEventCountComparator(), LogicalOperator.EQUAL))).
        andReturn(false);

    replay(spoolWriter, rolloverCondition, mockFile);

    LogSpooler logSpooler = new LogSpooler(spoolDirectory, SOURCE_FILENAME_PREFIX,
        rolloverCondition, rolloverHandler) {
      @Override
      protected PrintWriter initializeSpoolWriter(File spoolFile) throws IOException {
        return spoolWriter;
      }

      @Override
      protected File initializeSpoolFile() {
        return mockFile;
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

    final File mockFile = setupInputFileExpectations();
    LogSpoolerContext logSpoolerContext = new LogSpoolerContext(mockFile);
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext, new LogSpoolerFileComparator(), LogicalOperator.EQUAL))).
        andReturn(true);
    rolloverHandler.handleRollover(mockFile);

    replay(spoolWriter, rolloverCondition, rolloverHandler, mockFile);

    LogSpooler logSpooler = new LogSpooler(spoolDirectory, SOURCE_FILENAME_PREFIX,
        rolloverCondition, rolloverHandler) {

      @Override
      protected PrintWriter initializeSpoolWriter(File spoolFile) throws IOException {
        return spoolWriter;
      }

      @Override
      protected File initializeSpoolFile() {
        return mockFile;
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

    final File mockFile1 = setupInputFileExpectations();
    final File mockFile2 = setupInputFileExpectations();

    LogSpoolerContext logSpoolerContext1 = new LogSpoolerContext(mockFile1);
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext1, new LogSpoolerFileComparator(), LogicalOperator.EQUAL))
    ).andReturn(true);

    LogSpoolerContext logSpoolerContext2 = new LogSpoolerContext(mockFile2);
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext2, new LogSpoolerFileComparator(), LogicalOperator.EQUAL))
    ).andReturn(false);

    rolloverHandler.handleRollover(mockFile1);

    replay(spoolWriter1, spoolWriter2, rolloverCondition, rolloverHandler, mockFile1, mockFile2);

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
      protected File initializeSpoolFile() {
        if (!wasRolledOver) {
          return mockFile1;
        } else {
          return mockFile2;
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

    final File mockFile = setupInputFileExpectations();
    LogSpoolerContext logSpoolerContext = new LogSpoolerContext(mockFile);
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext, new LogSpoolerFileComparator(), LogicalOperator.EQUAL))
    ).andReturn(true);
    rolloverHandler.handleRollover(mockFile);

    replay(spoolWriter, rolloverCondition, rolloverHandler, mockFile);

    LogSpooler logSpooler = new LogSpooler(spoolDirectory, SOURCE_FILENAME_PREFIX,
        rolloverCondition, rolloverHandler) {

      @Override
      protected PrintWriter initializeSpoolWriter(File spoolFile) throws IOException {
        return spoolWriter;
      }

      @Override
      protected File initializeSpoolFile() {
        return mockFile;
      }
    };
    logSpooler.add("log event");

    verify(rolloverHandler);
  }

  // Rollover twice - the second rollover should work if the "rolloverInProgress"
  // flag is being reset correctly. Third file expectations being setup due
  // to auto-initialization.
  @Test
  public void shouldResetRolloverInProgressFlag() {
    final PrintWriter spoolWriter1 = mock(PrintWriter.class);
    final PrintWriter spoolWriter2 = mock(PrintWriter.class);
    final PrintWriter spoolWriter3 = mock(PrintWriter.class);
    spoolWriter1.println("log event1");
    spoolWriter2.println("log event2");
    spoolWriter1.flush();
    spoolWriter1.close();
    spoolWriter2.flush();
    spoolWriter2.close();

    final File mockFile1 = setupInputFileExpectations();
    final File mockFile2 = setupInputFileExpectations();
    final File mockFile3 = setupInputFileExpectations();

    LogSpoolerContext logSpoolerContext1 = new LogSpoolerContext(mockFile1);
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext1, new LogSpoolerFileComparator(), LogicalOperator.EQUAL))
    ).andReturn(true);

    LogSpoolerContext logSpoolerContext2 = new LogSpoolerContext(mockFile2);
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext2, new LogSpoolerFileComparator(), LogicalOperator.EQUAL))
    ).andReturn(true);

    rolloverHandler.handleRollover(mockFile1);
    rolloverHandler.handleRollover(mockFile2);

    replay(spoolWriter1, spoolWriter2, rolloverCondition, rolloverHandler, mockFile1, mockFile2, mockFile3);

    LogSpooler logSpooler = new LogSpooler(spoolDirectory, SOURCE_FILENAME_PREFIX,
        rolloverCondition, rolloverHandler) {
      private int currentFileNum;

      @Override
      protected PrintWriter initializeSpoolWriter(File spoolFile) throws IOException {
        PrintWriter spoolWriter = null;
        switch (currentFileNum) {
          case 0:
            spoolWriter = spoolWriter1;
            break;
          case 1:
            spoolWriter = spoolWriter2;
            break;
          case 2:
            spoolWriter = spoolWriter3;
            break;
        }
        currentFileNum++;
        return spoolWriter;
      }

      @Override
      protected File initializeSpoolFile() {
        switch (currentFileNum) {
          case 0:
            return mockFile1;
          case 1:
            return mockFile2;
          case 2:
            return mockFile3;
          default:
            return null;
        }
      }
    };
    logSpooler.add("log event1");
    logSpooler.add("log event2");

    verify(spoolWriter1, spoolWriter2, rolloverCondition);
  }

  @Test
  public void shouldNotRolloverZeroLengthFiles() {
    final PrintWriter spoolWriter = mock(PrintWriter.class);
    spoolWriter.println("log event");
    spoolWriter.flush();
    spoolWriter.close();

    final File mockFile = mock(File.class);
    expect(mockFile.length()).andReturn(0L);

    LogSpoolerContext logSpoolerContext = new LogSpoolerContext(mockFile);
    expect(rolloverCondition.shouldRollover(
        cmp(logSpoolerContext, new LogSpoolerFileComparator(), LogicalOperator.EQUAL))).
        andReturn(true);

    replay(spoolWriter, rolloverCondition, mockFile);

    LogSpooler logSpooler = new LogSpooler(spoolDirectory, SOURCE_FILENAME_PREFIX,
        rolloverCondition, rolloverHandler) {

      @Override
      protected PrintWriter initializeSpoolWriter(File spoolFile) throws IOException {
        return spoolWriter;
      }

      @Override
      protected File initializeSpoolFile() {
        return mockFile;
      }
    };
    logSpooler.add("log event");

    verify(mockFile);
  }

  class LogSpoolerFileComparator implements Comparator<LogSpoolerContext> {
    @Override
    public int compare(LogSpoolerContext o1, LogSpoolerContext o2) {
      return o1.getActiveSpoolFile()==o2.getActiveSpoolFile() ? 0 : -1;
    }
  }

  class LogSpoolerEventCountComparator implements Comparator<LogSpoolerContext> {
    @Override
    public int compare(LogSpoolerContext o1, LogSpoolerContext o2) {
      return (int)(o1.getNumEventsSpooled()-o2.getNumEventsSpooled());
    }
  }

}
