/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logsearch.appender;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.spi.LoggingEvent;

public class LogsearchConversion extends EnhancedPatternLayout {
  //
  protected final int BUF_SIZE = 256;
  protected final int MAX_CAPACITY = 1024;

  private StringBuffer sbuf = new StringBuffer(BUF_SIZE);

  private String newLine = System.getProperty("line.separator");

  public LogsearchConversion() {
  }

  public String format(LoggingEvent event) {
    if (sbuf.capacity() > MAX_CAPACITY) {
      sbuf = new StringBuffer(BUF_SIZE);
    } else {
      sbuf.setLength(0);
    }
    String outputStr = createOutput(event);
    sbuf.append(outputStr + newLine);
    return sbuf.toString();
  }

  public String createOutput(LoggingEvent event) {
    VOutput vOutput = new VOutput();
    vOutput.setLevel(event.getLevel().toString());
    vOutput.setFile(event.getLocationInformation().getFileName());
    vOutput.setLine_number(Integer.parseInt(event.getLocationInformation().getLineNumber()));
    String logmsg = event.getMessage() != null ? event.getMessage().toString() : "";
    if (event.getThrowableInformation() != null && event.getThrowableInformation().getThrowable() != null) {
      logmsg += newLine + stackTraceToString(event.getThrowableInformation().getThrowable());
    }
    vOutput.setLog_message(logmsg);
    vOutput.setLogtime("" + event.getTimeStamp());
    vOutput.setLogger_name("" + event.getLoggerName());
    vOutput.setThread_name(event.getThreadName());
    return vOutput.toJson();
  }

  public String stackTraceToString(Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }
  
  @Override
  public boolean ignoresThrowable() {
    //set false to ignore exception stacktrace
    return false;
  }
}
