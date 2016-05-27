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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

/**
 * Tests for repository definitions.
 */
public class VersionDefinitionTest {

  private static File file = new File("src/test/resources/version_definition_test.xml");

  @Test
  public void testLoadingString() throws Exception {
    String xmlString = FileUtils.readFileToString(file);
    VersionDefinitionXml xml = VersionDefinitionXml.load(xmlString);

    validateXml(xml);
  }

  @Test
  public void testLoadingUrl() throws Exception {
    VersionDefinitionXml xml = VersionDefinitionXml.load(file.toURI().toURL());

    validateXml(xml);
  }

  private void validateXml(VersionDefinitionXml xml) throws Exception {
    assertNotNull(xml.release);
    assertEquals(RepositoryType.PATCH, xml.release.repositoryType);
    assertEquals("HDP-2.3", xml.release.stackId);
    assertEquals("2.3.4.1", xml.release.version);
    assertEquals("2.3.4.[1-9]", xml.release.compatibleWith);
    assertEquals("http://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.3.4/", xml.release.releaseNotes);

    assertEquals(4, xml.manifestServices.size());
    assertEquals("HDFS-271", xml.manifestServices.get(0).serviceId);
    assertEquals("HDFS", xml.manifestServices.get(0).serviceName);
    assertEquals("2.7.1", xml.manifestServices.get(0).version);
    assertEquals("10", xml.manifestServices.get(0).versionId);

    assertEquals(3, xml.availableServices.size());
    assertEquals("HDFS-271", xml.availableServices.get(0).serviceIdReference);
    assertEquals(0, xml.availableServices.get(0).components.size());

    assertEquals("HIVE-110", xml.availableServices.get(2).serviceIdReference);
    assertEquals(1, xml.availableServices.get(2).components.size());

    assertNotNull(xml.repositoryInfo);
    assertEquals(2, xml.repositoryInfo.getOses().size());

    assertEquals("redhat6", xml.repositoryInfo.getOses().get(0).getFamily());
    assertEquals(2, xml.repositoryInfo.getOses().get(0).getRepos().size());
    assertEquals("http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.3.0.0",
        xml.repositoryInfo.getOses().get(0).getRepos().get(0).getBaseUrl());
    assertEquals("HDP-2.3", xml.repositoryInfo.getOses().get(0).getRepos().get(0).getRepoId());
    assertEquals("HDP", xml.repositoryInfo.getOses().get(0).getRepos().get(0).getRepoName());
    assertNull(xml.repositoryInfo.getOses().get(0).getPackageVersion());
  }

  @Test
  public void testAllServices() throws Exception {

    File f = new File("src/test/resources/version_definition_test_all_services.xml");

    VersionDefinitionXml xml = VersionDefinitionXml.load(f.toURI().toURL());

    StackInfo stack = new StackInfo() {
      @Override
      public ServiceInfo getService(String name) {
        return null;
      }
    };

    // the file does not define available services
    assertEquals(4, xml.manifestServices.size());
    assertEquals(3, xml.getAvailableServices(stack).size());
  }

  @Test
  public void testStackManifest() throws Exception {

    File f = new File("src/test/resources/version_definition_test_all_services.xml");

    VersionDefinitionXml xml = VersionDefinitionXml.load(f.toURI().toURL());

    StackInfo stack = new StackInfo() {
      private Map<String, ServiceInfo> m_services = new HashMap<String, ServiceInfo>() {{
        put("HDFS", makeService("HDFS"));
        put("HBASE", makeService("HBASE"));
        put("HIVE", makeService("HIVE"));
        put("YARN", makeService("YARN"));
      }};

      @Override
      public ServiceInfo getService(String name) {
        return m_services.get(name);
      }

      @Override
      public synchronized Collection<ServiceInfo> getServices() {
        return m_services.values();
      }

    };

    List<ManifestServiceInfo> stackServices = xml.getStackServices(stack);

    // the file does not define available services
    assertEquals(4, xml.manifestServices.size());
    assertEquals(3, xml.getAvailableServices(stack).size());
    assertEquals(4, stackServices.size());

    boolean foundHdfs = false;
    boolean foundYarn = false;
    boolean foundHive = false;

    for (ManifestServiceInfo msi : stackServices) {
      if ("HDFS".equals(msi.m_name)) {
        foundHdfs = true;
        assertEquals("HDFS Display", msi.m_display);
        assertEquals("HDFS Comment", msi.m_comment);
        assertEquals(1, msi.m_versions.size());
        assertEquals("2.7.1", msi.m_versions.iterator().next());
      } else if ("YARN".equals(msi.m_name)) {
        foundYarn = true;
        assertEquals(1, msi.m_versions.size());
        assertEquals("1.1.1", msi.m_versions.iterator().next());
      } else if ("HIVE".equals(msi.m_name)) {
        foundHive = true;
        assertEquals(2, msi.m_versions.size());
        assertTrue(msi.m_versions.contains("1.1.0"));
        assertTrue(msi.m_versions.contains("2.0.0"));
      }
    }

    assertTrue(foundHdfs);
    assertTrue(foundYarn);
    assertTrue(foundHive);
  }

  @Test
  public void testSerialization() throws Exception {

    File f = new File("src/test/resources/version_definition_test_all_services.xml");

    VersionDefinitionXml xml = VersionDefinitionXml.load(f.toURI().toURL());

    String xmlString = xml.toXml();

    xml = VersionDefinitionXml.load(xmlString);

    assertNotNull(xml.release.build);
    assertEquals("1234", xml.release.build);
  }


  @Test
  public void testMerger() throws Exception {
    File f = new File("src/test/resources/version_definition_test_all_services.xml");

    VersionDefinitionXml xml1 = VersionDefinitionXml.load(f.toURI().toURL());
    VersionDefinitionXml xml2 = VersionDefinitionXml.load(f.toURI().toURL());

    assertEquals(2, xml1.repositoryInfo.getOses().size());
    assertEquals(2, xml2.repositoryInfo.getOses().size());

    // make xml1 have only redhat6 (remove redhat7) without a package version
    RepositoryXml.Os target = null;
    for (RepositoryXml.Os os : xml1.repositoryInfo.getOses()) {
      if (os.getFamily().equals("redhat7")) {
        target = os;
      }
    }
    assertNotNull(target);
    xml1.repositoryInfo.getOses().remove(target);

    // make xml2 have only redhat7 (remove redhat6) with a package version
    target = null;
    for (RepositoryXml.Os os : xml2.repositoryInfo.getOses()) {
      if (os.getFamily().equals("redhat6")) {
        target = os;
      } else {
        Field field = RepositoryXml.Os.class.getDeclaredField("packageVersion");
        field.setAccessible(true);
        field.set(os, "2_3_4_2");
      }
    }
    assertNotNull(target);
    xml2.repositoryInfo.getOses().remove(target);
    xml2.release.version = "2.3.4.2";
    xml2.release.build = "2468";

    assertEquals(1, xml1.repositoryInfo.getOses().size());
    assertEquals(1, xml2.repositoryInfo.getOses().size());

    VersionDefinitionXml.Merger builder = new VersionDefinitionXml.Merger();
    VersionDefinitionXml xml3 = builder.merge();

    assertNull(xml3);

    builder.add(xml1.release.version, xml1);
    builder.add("", xml2);
    xml3 = builder.merge();

    assertNotNull(xml3);
    assertNull("Merged definition cannot have a build", xml3.release.build);
    assertEquals(xml3.release.version, "2.3.4.1");

    RepositoryXml.Os redhat6 = null;
    RepositoryXml.Os redhat7 = null;
    assertEquals(2, xml3.repositoryInfo.getOses().size());
    for (RepositoryXml.Os os : xml3.repositoryInfo.getOses()) {
      if (os.getFamily().equals("redhat6")) {
        redhat6 = os;
      } else if (os.getFamily().equals("redhat7")) {
        redhat7 = os;
      }
    }
    assertNotNull(redhat6);
    assertNotNull(redhat7);
    assertNull(redhat6.getPackageVersion());
    assertEquals("2_3_4_2", redhat7.getPackageVersion());

    // !!! extra test to make sure it serializes
    xml3.toXml();
  }

  @Test
  public void testLoadingBadNewLine() throws Exception {
    List<?> lines = FileUtils.readLines(file);

    // crude
    StringBuilder builder = new StringBuilder();
    for (Object line : lines) {
      String lineString = line.toString().trim();
      if (lineString.startsWith("<baseurl>")) {
        lineString = lineString.replace("<baseurl>", "");
        lineString = lineString.replace("</baseurl>", "");

        builder.append("<baseurl>\n");
        builder.append(lineString).append('\n');
        builder.append("</baseurl>\n");
      } else if (lineString.startsWith("<version>")) {
        lineString = lineString.replace("<version>", "");
        lineString = lineString.replace("</version>", "");

        builder.append("<version>\n");
        builder.append(lineString).append('\n');
        builder.append("</version>\n");
      } else {
        builder.append(line.toString().trim()).append('\n');
      }
    }

    VersionDefinitionXml xml = VersionDefinitionXml.load(builder.toString());

    validateXml(xml);
  }

  @Test
  public void testPackageVersion() throws Exception {
    File f = new File("src/test/resources/hbase_version_test.xml");

    VersionDefinitionXml xml = VersionDefinitionXml.load(f.toURI().toURL());

    String xmlString = xml.toXml();

    xml = VersionDefinitionXml.load(xmlString);

    assertNotNull(xml.release.build);
    assertEquals("3396", xml.release.build);
    assertEquals("redhat6", xml.repositoryInfo.getOses().get(0).getFamily());
    assertEquals("2_3_4_0_3396", xml.repositoryInfo.getOses().get(0).getPackageVersion());
    assertNotNull(xml.getPackageVersion("redhat6"));
    assertEquals("2_3_4_0_3396", xml.getPackageVersion("redhat6"));
    assertNull(xml.getPackageVersion("suse11"));

  }


  private static ServiceInfo makeService(final String name) {
    return new ServiceInfo() {
      @Override
      public String getName() {
        return name;
      }
      @Override
      public String getDisplayName() {
        return name + " Display";
      }
      @Override
      public String getVersion() {
        return "1.1.1";
      }
      @Override
      public String getComment() {
        return name + " Comment";
      }

    };
  }

}
