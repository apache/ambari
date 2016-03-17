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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.repository.AvailableVersion.Component;
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
  List<ManifestService> manifestServices = new ArrayList<>();

  /**
   * For PATCH and SERVICE repositories, this dictates what is available for upgrade
   * from the manifest.
   */
  @XmlElementWrapper(name="available-services")
  @XmlElement(name="service")
  List<AvailableServiceReference> availableServices = new ArrayList<>();

  /**
   * Represents the repository details.  This is reused from stack repo info.
   */
  @XmlElement(name="repository-info")
  public RepositoryXml repositoryInfo;

  /**
   * The xsd location.  Never {@code null}.
   */
  @XmlTransient
  public String xsdLocation;

  @XmlTransient
  private Map<String, AvailableService> availableMap;

  @XmlTransient
  private List<ManifestServiceInfo> m_manifest = null;


  /**
   * @param stack the stack info needed to lookup service and component display names
   * @return a collection of AvailableServices used for web service consumption.  This
   * collection is either the subset of the manifest, or the manifest if no services
   * are specified as "available".
   */
  public Collection<AvailableService> getAvailableServices(StackInfo stack) {
    if (null == availableMap) {
      Map<String, ManifestService> manifests = buildManifest();
      availableMap = new HashMap<>();

      if (availableServices.isEmpty()) {
        for (ManifestService ms : manifests.values()) {
          addToAvailable(ms, stack, Collections.<String>emptySet());
        }

      } else {
        for (AvailableServiceReference ref : availableServices) {
          ManifestService ms = manifests.get(ref.serviceIdReference);

          addToAvailable(ms, stack, ref.components);
        }
      }
    }

    return availableMap.values();
  }

  /**
   * Gets the list of stack services, applying information from the version definition.
   * @param stack the stack for which to get the information
   * @return the list of {@code ManifestServiceInfo} instances for each service in the stack
   */
  public List<ManifestServiceInfo> getStackServices(StackInfo stack) {

    if (null != m_manifest) {
      return m_manifest;
    }

    Map<String, Set<String>> manifestVersions = new HashMap<>();

    for (ManifestService manifest : manifestServices) {
      String name = manifest.serviceName;

      if (!manifestVersions.containsKey(name)) {
        manifestVersions.put(manifest.serviceName, new TreeSet<String>());
      }

      manifestVersions.get(manifest.serviceName).add(manifest.version);
    }

    m_manifest = new ArrayList<>();

    for (ServiceInfo si : stack.getServices()) {
      Set<String> versions = manifestVersions.containsKey(si.getName()) ?
          manifestVersions.get(si.getName()) : Collections.singleton("");

      m_manifest.add(new ManifestServiceInfo(si.getName(), si.getDisplayName(),
          si.getComment(), versions));
    }

    return m_manifest;
  }


  /**
   * Helper method to use a {@link ManifestService} to generate the available services structure
   * @param ms          the ManifestService instance
   * @param stack       the stack object
   * @param components  the set of components for the service
   */
  private void addToAvailable(ManifestService ms, StackInfo stack, Set<String> components) {
    ServiceInfo service = stack.getService(ms.serviceName);

    if (!availableMap.containsKey(ms.serviceName)) {
      String display = (null == service) ? ms.serviceName: service.getDisplayName();

      availableMap.put(ms.serviceName, new AvailableService(ms.serviceName, display));
    }

    AvailableService as = availableMap.get(ms.serviceName);
    as.getVersions().add(new AvailableVersion(ms.version, ms.versionId,
        buildComponents(service, components)));
  }


  /**
   * @return the list of manifest services to a map for easier access.
   */
  private Map<String, ManifestService> buildManifest() {
    Map<String, ManifestService> normalized = new HashMap<>();

    for (ManifestService ms : manifestServices) {
      normalized.put(ms.serviceId, ms);
    }
    return normalized;
  }

  /**
   * @param service the service containing components
   * @param components the set of components in the service
   * @return the set of components name/display name pairs
   */
  private Set<Component> buildComponents(ServiceInfo service, Set<String> components) {
    Set<Component> set = new HashSet<>();

    for (String component : components) {
      ComponentInfo ci = service.getComponentByName(component);
      String display = (null == ci) ? component : ci.getDisplayName();
      set.add(new Component(component, display));
    }

    return set;
  }



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

    VersionDefinitionXml xml = (VersionDefinitionXml) unmarshaller.unmarshal(xmlReader);
    xml.xsdLocation = xsdName;

    return xml;
  }




}
