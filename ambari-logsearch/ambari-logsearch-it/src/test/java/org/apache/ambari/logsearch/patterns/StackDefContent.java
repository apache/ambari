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
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

public class StackDefContent implements Log4jContent {
  public static final XPathFactory X_PATH_FACTORY = XPathFactory.newInstance();
  public static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

  private final File file;
  private final String propertyName;

  public StackDefContent(File file, String propertyName) {
    this.file = file;
    this.propertyName = propertyName;
  }

  @Override
  public String loadContent() {
    try {
      DocumentBuilder builder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
      Document doc;
      try (FileInputStream fileInputStream = new FileInputStream(file)) {
        doc = builder.parse(fileInputStream);
      }
      XPath xpath = X_PATH_FACTORY.newXPath();
      XPathExpression expr = xpath.compile("/configuration/property[name/text()='" + propertyName + "']/value/text()");
      return expr.evaluate(doc);
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
