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
import java.io.StringWriter;
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
import javax.xml.bind.Marshaller;
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
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.repository.AvailableVersion.Component;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.RepositoryXml.Os;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Class that wraps a repository definition file.
 */
@XmlRootElement(name="repository-version")
@XmlAccessorType(XmlAccessType.FIELD)
public class VersionDefinitionXml {

  public static String SCHEMA_LOCATION = "version_definition.xsd";


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
  private Map<String, AvailableService> m_availableMap;

  @XmlTransient
  private List<ManifestServiceInfo> m_manifest = null;

  @XmlTransient
  private boolean m_stackDefault = false;

  @XmlTransient
  private Map<String, String> m_packageVersions = null;


  /**
   * @param stack the stack info needed to lookup service and component display names
   * @return a collection of AvailableServices used for web service consumption.  This
   * collection is either the subset of the manifest, or the manifest itself if no services
   * are specified as "available".
   */
  public synchronized Collection<AvailableService> getAvailableServices(StackInfo stack) {
    if (null == m_availableMap) {
      Map<String, ManifestService> manifests = buildManifest();
      m_availableMap = new HashMap<>();

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

    return m_availableMap.values();
  }

  /**
   * Gets if the version definition was built as the default for a stack
   * @return {@code true} if default for a stack
   */
  public boolean isStackDefault() {
    return m_stackDefault;
  }

  /**
   * Gets the list of stack services, applying information from the version definition.
   * @param stack the stack for which to get the information
   * @return the list of {@code ManifestServiceInfo} instances for each service in the stack
   */
  public synchronized List<ManifestServiceInfo> getStackServices(StackInfo stack) {

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
          manifestVersions.get(si.getName()) : Collections.singleton(
              null == si.getVersion() ? "" : si.getVersion());

      m_manifest.add(new ManifestServiceInfo(si.getName(), si.getDisplayName(),
          si.getComment(), versions));
    }

    return m_manifest;
  }

  /**
   * Gets the package version for an OS family
   * @param osFamily  the os family
   * @return the package version, or {@code null} if not found
   */
  public String getPackageVersion(String osFamily) {
    if (null == m_packageVersions) {
      m_packageVersions = new HashMap<>();

      for (Os os : repositoryInfo.getOses()) {
        m_packageVersions.put(os.getFamily(), os.getPackageVersion());
      }
    }

    return m_packageVersions.get(osFamily);
  }



  /**
   * Helper method to use a {@link ManifestService} to generate the available services structure
   * @param ms          the ManifestService instance
   * @param stack       the stack object
   * @param components  the set of components for the service
   */
  private void addToAvailable(ManifestService ms, StackInfo stack, Set<String> components) {
    ServiceInfo service = stack.getService(ms.serviceName);

    if (!m_availableMap.containsKey(ms.serviceName)) {
      String display = (null == service) ? ms.serviceName: service.getDisplayName();

      m_availableMap.put(ms.serviceName, new AvailableService(ms.serviceName, display));
    }

    AvailableService as = m_availableMap.get(ms.serviceName);
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
   * Returns the XML representation of this instance.
   */
  public String toXml() throws Exception {

    JAXBContext ctx = JAXBContext.newInstance(VersionDefinitionXml.class);
    Marshaller marshaller = ctx.createMarshaller();
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    InputStream xsdStream = VersionDefinitionXml.class.getClassLoader().getResourceAsStream(xsdLocation);

    if (null == xsdStream) {
      throw new Exception(String.format("Could not load XSD identified by '%s'", xsdLocation));
    }

    try {
      Schema schema = factory.newSchema(new StreamSource(xsdStream));
      marshaller.setSchema(schema);
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.setProperty("jaxb.noNamespaceSchemaLocation", xsdLocation);

      StringWriter w = new StringWriter();
      marshaller.marshal(this, w);

      return w.toString();
    } finally {
      IOUtils.closeQuietly(xsdStream);
    }
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

    if (null == xsdName) {
      throw new IllegalArgumentException("Provided XML does not have a Schema defined using 'noNamespaceSchemaLocation'");
    }

    InputStream xsdStream = VersionDefinitionXml.class.getClassLoader().getResourceAsStream(xsdName);

    if (null == xsdStream) {
      throw new Exception(String.format("Could not load XSD identified by '%s'", xsdName));
    }

    JAXBContext ctx = JAXBContext.newInstance(VersionDefinitionXml.class);
    Unmarshaller unmarshaller = ctx.createUnmarshaller();

    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = factory.newSchema(new StreamSource(xsdStream));
    unmarshaller.setSchema(schema);

    try {
      VersionDefinitionXml xml = (VersionDefinitionXml) unmarshaller.unmarshal(xmlReader);
      xml.xsdLocation = xsdName;
      return xml;
    } finally {
      IOUtils.closeQuietly(xsdStream);
    }
  }

  /**
   * Builds a Version Definition that is the default for the stack
   * @param stack
   * @return the version definition
   */
  public static VersionDefinitionXml build(StackInfo stackInfo) {

    VersionDefinitionXml xml = new VersionDefinitionXml();
    xml.m_stackDefault = true;
    xml.release = new Release();
    xml.repositoryInfo = new RepositoryXml();
    xml.xsdLocation = SCHEMA_LOCATION;

    StackId stackId = new StackId(stackInfo.getName(), stackInfo.getVersion());

    xml.release.repositoryType = RepositoryType.STANDARD;
    xml.release.stackId = stackId.toString();
    xml.release.version = stackInfo.getVersion();
    xml.release.releaseNotes = "NONE";
    xml.release.display = stackId.toString();

    for (ServiceInfo si : stackInfo.getServices()) {
      ManifestService ms = new ManifestService();
      ms.serviceName = si.getName();
      ms.version = StringUtils.trimToEmpty(si.getVersion());
      ms.serviceId = ms.serviceName + "-" + ms.version.replace(".", "");
      xml.manifestServices.add(ms);
    }

    if (null != stackInfo.getRepositoryXml()) {
      xml.repositoryInfo.getOses().addAll(stackInfo.getRepositoryXml().getOses());
    }

    try {
      xml.toXml();
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }

    return xml;
  }

  /**
   * Used to facilitate merging when multiple version definitions are provided.  Ambari
   * represents them as a unified entity.  Since there is no knowledge of which one is
   * "correct" - the first one is used for the release meta-info.
   */
  public static class Merger {
    private VersionDefinitionXml m_xml = new VersionDefinitionXml();
    private boolean m_seeded = false;

    public Merger() {
      m_xml.release = new Release();
      m_xml.repositoryInfo = new RepositoryXml();
    }

    /**
     * Adds definition to this one.
     * @param version the version the definition represents
     * @param xml the definition object
     */
    public void add(String version, VersionDefinitionXml xml) {
      if (!m_seeded) {
        m_xml.xsdLocation = xml.xsdLocation;

        StackId stackId = new StackId(xml.release.stackId);

        m_xml.release.build = null; // could be combining builds, so this is invalid
        m_xml.release.compatibleWith = xml.release.compatibleWith;
        m_xml.release.display = stackId.getStackName() + "-" + xml.release.version;
        m_xml.release.repositoryType = RepositoryType.STANDARD; // assumption since merging only done for new installs
        m_xml.release.releaseNotes = xml.release.releaseNotes;
        m_xml.release.stackId = xml.release.stackId;
        m_xml.release.version = version;
        m_xml.manifestServices.addAll(xml.manifestServices);

        m_seeded = true;
      }

      m_xml.repositoryInfo.getOses().addAll(xml.repositoryInfo.getOses());
    }

    /**
     * @return the merged definition file
     */
    public VersionDefinitionXml merge() {
      return m_seeded ? m_xml : null;
    }
  }

}