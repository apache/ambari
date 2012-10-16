/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services.serializers;

import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.api.util.TreeNode;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.util.DefaultPrettyPrinter;

import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * JSON serializer.
 * Responsible for representing a result as JSON.
 */
public class JsonSerializer implements ResultSerializer {

  /**
   * Factory used to create JSON generator.
   */
  JsonFactory m_factory = new JsonFactory();

  /**
   * Generator which writes JSON.
   */
  JsonGenerator m_generator;

  @Override
  public Object serialize(Result result, UriInfo uriInfo) {
    try {
      ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      m_generator = createJsonGenerator(bytesOut);

      DefaultPrettyPrinter p = new DefaultPrettyPrinter();
      p.indentArraysWith(new DefaultPrettyPrinter.Lf2SpacesIndenter());
      m_generator.setPrettyPrinter(p);

      processNode(result.getResultTree());

      m_generator.close();
      return bytesOut.toString("UTF-8");
    } catch (IOException e) {
      //todo: exception handling
      throw new RuntimeException("Unable to serialize to json: " + e, e);
    }
  }

  private void processNode(TreeNode<Resource> node) throws IOException {
    String name = node.getName();
    Resource r = node.getObject();

    if (r == null) {
      if (name != null) {
        if (node.getParent() == null) {
          m_generator.writeStartObject();
          writeHref(node);
        }
        m_generator.writeArrayFieldStart(name);
      }
    } else {
      m_generator.writeStartObject();
      writeHref(node);
      // resource props
      handleResourceProperties(r.getCategories());
    }

    for (TreeNode<Resource> child : node.getChildren()) {
      processNode(child);
    }

    if (r == null) {
      if (name != null) {
        m_generator.writeEndArray();
        if (node.getParent() == null) {
          m_generator.writeEndObject();
        }
      }
    } else {
      m_generator.writeEndObject();
    }
  }

  private void handleResourceProperties(Map<String, Map<String, Object>> mapCatProps) throws IOException {
    for (Map.Entry<String, Map<String, Object>> categoryEntry : mapCatProps.entrySet()) {
      String category = categoryEntry.getKey();
      Map<String, Object> mapProps = categoryEntry.getValue();
      if (category != null) {
        m_generator.writeFieldName(category);
        m_generator.writeStartObject();
      }

      for (Map.Entry<String, Object> propEntry : mapProps.entrySet()) {
        m_generator.writeObjectField(propEntry.getKey(), propEntry.getValue());
      }

      if (category != null) {
        m_generator.writeEndObject();
      }
    }
  }

  private JsonGenerator createJsonGenerator(ByteArrayOutputStream baos) throws IOException {
    JsonGenerator generator = m_factory.createJsonGenerator(new OutputStreamWriter(baos,
        Charset.forName("UTF-8").newEncoder()));

    DefaultPrettyPrinter p = new DefaultPrettyPrinter();
    p.indentArraysWith(new DefaultPrettyPrinter.Lf2SpacesIndenter());
    generator.setPrettyPrinter(p);

    return generator;
  }

  private void writeHref(TreeNode<Resource> node) throws IOException {
    String hrefProp = node.getProperty("href");
    if (hrefProp != null) {
      m_generator.writeStringField("href", hrefProp);
    }
  }
}
