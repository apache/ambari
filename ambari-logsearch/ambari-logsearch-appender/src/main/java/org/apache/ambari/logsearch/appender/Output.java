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

import java.io.IOException;
import java.io.StringWriter;

import com.google.gson.stream.JsonWriter;

class Output {

  private String level;
  private String file;
  private String threadName;
  private int lineNumber;
  private String loggerName;
  private String logtime;
  private String logMessage;

  void setLevel(String level) {
    this.level = level;
  }

  void setFile(String file) {
    this.file = file;
  }

  void setThreadName(String threadName) {
    this.threadName = threadName;
  }

  void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }

  void setLoggerName(String loggerName) {
    this.loggerName = loggerName;
  }

  void setLogtime(String logtime) {
    this.logtime = logtime;
  }

  void setLogMessage(String logMessage) {
    this.logMessage = logMessage;
  }

  public String toJson() {
    StringWriter stringWriter = new StringWriter();
    
    try (JsonWriter writer = new JsonWriter(stringWriter)) {
      writer.beginObject();
      
      if (level != null) writer.name("level").value(level);
      if (file != null) writer.name("file").value(file);
      if (threadName != null) writer.name("thread_name").value(threadName);
      writer.name("line_number").value(lineNumber);
      if (loggerName != null) writer.name("logger_name").value(loggerName);
      if (logtime != null) writer.name("logtime").value(logtime);
      if (logMessage != null) writer.name("log_message").value(logMessage);
      
      writer.endObject();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    return stringWriter.toString();
  }
  
  @Override
  public String toString() {
    return toJson();
  }
}
