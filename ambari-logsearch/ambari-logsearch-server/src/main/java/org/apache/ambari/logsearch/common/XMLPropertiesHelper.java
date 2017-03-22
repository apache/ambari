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

package org.apache.ambari.logsearch.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.springframework.util.DefaultPropertiesPersister;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLPropertiesHelper extends DefaultPropertiesPersister {
  private static Logger logger = Logger.getLogger(XMLPropertiesHelper.class);

  public XMLPropertiesHelper() {
  }

  @Override
  public void loadFromXml(Properties properties, InputStream inputStream)
      throws IOException {
    try {
      DocumentBuilderFactory xmlDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
      xmlDocumentBuilderFactory.setIgnoringComments(true);
      xmlDocumentBuilderFactory.setNamespaceAware(true);
      DocumentBuilder xmlDocumentBuilder = xmlDocumentBuilderFactory.newDocumentBuilder();
      Document xmlDocument = xmlDocumentBuilder.parse(inputStream);
      if (xmlDocument != null) {
        xmlDocument.getDocumentElement().normalize();
        NodeList nList = xmlDocument.getElementsByTagName("property");
        if (nList != null) {
          for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
              Element eElement = (Element) nNode;
              String propertyName = "";
              String propertyValue = "";
              if (eElement.getElementsByTagName("name") != null && eElement.getElementsByTagName("name").item(0) != null) {
                propertyName = eElement.getElementsByTagName("name").item(0).getTextContent().trim();
              }
              if (eElement.getElementsByTagName("value") != null && eElement.getElementsByTagName("value").item(0) != null) {
                propertyValue = eElement.getElementsByTagName("value").item(0).getTextContent().trim();
              }
              if (propertyName != null && !propertyName.isEmpty()) {
                properties.put(propertyName, propertyValue);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error("Error loading xml properties ", e);
    }
  }

}