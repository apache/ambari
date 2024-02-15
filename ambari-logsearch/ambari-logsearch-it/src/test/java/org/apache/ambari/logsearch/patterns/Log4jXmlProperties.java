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

public class Log4jXmlProperties {
  public static Log4jXmlProperties unwrapFrom(File file) {
    return unwrapFrom(file, "content");
  }

  public static Log4jXmlProperties unwrapFrom(File file, String contentPropertyName) {
    return new Log4jXmlProperties(
            new StackDefContent(file, contentPropertyName),
            (xmlPropertyName) -> "/configuration/properties/property[@name='" + xmlPropertyName + "']/text()");
  }

  public Log4jXmlProperties(Log4jContent content, LayoutQuery layoutQuery) {
    this.content = content;
    this.layoutQuery = layoutQuery;
  }

  private final Log4jContent content;
  private final LayoutQuery layoutQuery;

  public String getLayout(String propertyName) {
    return Log4jXml.getLayout(content, layoutQuery, propertyName);
  }
}
