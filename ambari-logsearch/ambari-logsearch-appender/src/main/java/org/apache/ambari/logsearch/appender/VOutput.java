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

public class VOutput extends VBase {

  private String level;
  private String file;
  private String thread_name;
  private int line_number;
  private String log_message;
  private String logger_name;
  private String logtime;

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public String getThread_name() {
    return thread_name;
  }

  public void setThread_name(String thread_name) {
    this.thread_name = thread_name;
  }

  public int getLine_number() {
    return line_number;
  }

  public void setLine_number(int line_number) {
    this.line_number = line_number;
  }

  public String getLog_message() {
    return log_message;
  }

  public void setLog_message(String log_message) {
    this.log_message = log_message;
  }

  public String getLogger_name() {
    return logger_name;
  }

  public void setLogger_name(String logger_name) {
    this.logger_name = logger_name;
  }

  public String getLogtime() {
    return logtime;
  }

  public void setLogtime(String logtime) {
    this.logtime = logtime;
  }

}
