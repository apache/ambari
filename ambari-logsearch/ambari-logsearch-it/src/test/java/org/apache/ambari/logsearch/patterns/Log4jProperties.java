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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

public class Log4jProperties {
  public static Log4jProperties loadFrom(File file) {
    return new Log4jProperties(() -> {
      try {
        return FileUtils.readFileToString(file, Charset.defaultCharset());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public static Log4jProperties unwrapFrom(File file) {
    return new Log4jProperties(new StackDefContent(file, "content"));
  }

  public static Log4jProperties unwrapFrom(File file, String propertyName) {
    return new Log4jProperties(new StackDefContent(file, propertyName));
  }

  private final Log4jContent content;

  public Log4jProperties(Log4jContent content) {
    this.content = content;
  }

  public String getLayout(String appenderName) {
    Properties properties = new Properties();
    try (StringReader reader = new StringReader(content.loadContent())) {
      properties.load(reader);
      return properties.getProperty("log4j.appender." + appenderName + ".layout.ConversionPattern");
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
