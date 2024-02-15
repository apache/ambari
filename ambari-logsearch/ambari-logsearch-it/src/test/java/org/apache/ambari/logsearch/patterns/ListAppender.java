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
package org.apache.ambari.logsearch.patterns;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;

public class ListAppender extends AppenderSkeleton {

  private final List<String> logList;

  public ListAppender() {
    logList = new ArrayList<>();
  }

  @Override
  protected void append(LoggingEvent event) {
    StringWriter stringWriter = new StringWriter();
    WriterAppender writerAppender = new WriterAppender(layout, stringWriter);
    writerAppender.append(event);
    logList.add(stringWriter.toString());
  }

  @Override
  public void close() {

  }

  @Override
  public boolean requiresLayout() {
    return true;
  }

  public List<String> getLogList() {
    return logList;
  }
}
