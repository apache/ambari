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

import static org.apache.ambari.logsearch.patterns.StackDefContent.DOCUMENT_BUILDER_FACTORY;
import static org.apache.ambari.logsearch.patterns.StackDefContent.X_PATH_FACTORY;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Document;

public class Log4jXml {
  public static Log4jXml unwrapFrom(File file) {
    return unwrapFrom(file, "content");
  }

  public static Log4jXml unwrapFrom(File file, String propertyName) {
    return new Log4jXml(
            new StackDefContent(file, propertyName),
            (appenderName) -> "/configuration/appender[@name='" + appenderName + "']/layout/param[@name='ConversionPattern']/@value");
  }

  private final Log4jContent content;
  private final LayoutQuery layoutQuery;

  public Log4jXml(Log4jContent content, LayoutQuery layoutQuery) {
    this.content = content;
    this.layoutQuery = layoutQuery;
  }

  public String getLayout(String appenderName) {
    return getLayout(content, layoutQuery, appenderName);
  }

  public static String getLayout(Log4jContent content, LayoutQuery layoutQuery, String parameterName) {
    try {
      DocumentBuilder builder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
      Document doc;
      try (InputStream stringReader = new ByteArrayInputStream(content.loadContent().getBytes(Charset.defaultCharset()))) {
        doc = builder.parse(stringReader);
      }
      XPath xpath = X_PATH_FACTORY.newXPath();
      XPathExpression expr = xpath.compile(layoutQuery.query(parameterName));
      return (String) expr.evaluate(doc, XPathConstants.STRING);
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
