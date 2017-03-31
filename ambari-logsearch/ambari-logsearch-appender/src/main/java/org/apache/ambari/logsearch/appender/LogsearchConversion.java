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

  private static final String NEW_LINE = System.getProperty("line.separator");

  public LogsearchConversion() {
  }

  public String format(LoggingEvent event) {
    String outputStr = createOutput(event);
    return outputStr + NEW_LINE;
  }

  public String createOutput(LoggingEvent event) {
    Output output = new Output();
    
    output.setLevel(event.getLevel().toString());
    output.setFile(event.getLocationInformation().getFileName());
    output.setLineNumber(Integer.parseInt(event.getLocationInformation().getLineNumber()));
    output.setLogtime(Long.toString(event.getTimeStamp()));
    output.setLoggerName(event.getLoggerName());
    output.setThreadName(event.getThreadName());
    output.setLogMessage(getLogMessage(event));
    
    return output.toJson();
  }

  public String getLogMessage(LoggingEvent event) {
    String logMessage = event.getMessage() != null ? event.getMessage().toString() : "";

    if (event.getThrowableInformation() != null && event.getThrowableInformation().getThrowable() != null) {
      logMessage += NEW_LINE;
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      event.getThrowableInformation().getThrowable().printStackTrace(pw);
      logMessage += sw.toString();
    }

    return logMessage;
  }

  @Override
  public boolean ignoresThrowable() {
    return false;
  }
}
