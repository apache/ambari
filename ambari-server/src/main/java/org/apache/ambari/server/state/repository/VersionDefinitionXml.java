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
package org.apache.ambari.server.state.repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.commons.io.IOUtils;

/**
 * Class that wraps a repository definition file.
 */
@XmlRootElement(name="repository-version")
@XmlAccessorType(XmlAccessType.FIELD)
public class VersionDefinitionXml {

  /**
   * Release details.
   */
  @XmlElement(name = "release")
  public Release release;

  /**
   * The manifest of ALL services available in this repository.
   */
  @XmlElementWrapper(name="manifest")
  @XmlElement(name="service")
  public List<ManifestService> manifestServices = new ArrayList<>();

  /**
   * For PATCH and SERVICE repositories, this dictates what is available for upgrade
   * from the manifest.
   */
  @XmlElementWrapper(name="available-services")
  @XmlElement(name="service")
  public List<AvailableServiceReference> availableServices = new ArrayList<>();

  /**
   * Represents the repository details.  This is reused from stack repo info.
   */
  @XmlElement(name="repository-info")
  public RepositoryXml repositoryInfo;

  /**
   * Parses a URL for a definition XML file into the object graph.
   * @param   url the URL to load.  Can be a file URL reference also.
   * @return  the definition
   */
  public static VersionDefinitionXml load(URL url) throws Exception {

    InputStream stream = null;
    try {
      stream = url.openStream();
      return load(stream);
    } finally {
      IOUtils.closeQuietly(stream);
    }
  }

  /**
   * Parses an xml string.  Used when the xml is in the database.
   * @param   xml the xml string
   * @return  the definition
   */
  public static VersionDefinitionXml load(String xml) throws Exception {
    return load(new ByteArrayInputStream(xml.getBytes("UTF-8")));
  }

  /**
   * Parses a stream into an object graph
   * @param stream  the stream
   * @return the definition
   * @throws Exception
   */
  private static VersionDefinitionXml load(InputStream stream) throws Exception {

    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    XMLStreamReader xmlReader = xmlFactory.createXMLStreamReader(stream);

    xmlReader.nextTag();
    String xsdName = xmlReader.getAttributeValue(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "noNamespaceSchemaLocation");

    JAXBContext ctx = JAXBContext.newInstance(VersionDefinitionXml.class);
    Unmarshaller unmarshaller = ctx.createUnmarshaller();

    if (null != xsdName) {
      InputStream xsdStream = VersionDefinitionXml.class.getClassLoader().getResourceAsStream(xsdName);

      if (null == xsdStream) {
        throw new Exception(String.format("Could not load XSD identified by '%s'", xsdName));
      }

      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = factory.newSchema(new StreamSource(xsdStream));
      unmarshaller.setSchema(schema);
    }

    return (VersionDefinitionXml) unmarshaller.unmarshal(xmlReader);
  }


}
