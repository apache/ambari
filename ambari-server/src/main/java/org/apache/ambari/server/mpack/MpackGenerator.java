/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.mpack;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stack.ServiceDirectory;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.Packlet;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.QuickLinksConfigurationInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.ThemeInfo;
import org.apache.ambari.server.state.quicklinks.QuickLinks;
import org.apache.ambari.server.state.repository.ManifestServiceInfo;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.ConfigurationXml;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.apache.ambari.server.state.stack.StackMetainfoXml;
import org.apache.ambari.server.state.stack.StackRoleCommandOrder;
import org.apache.ambari.server.state.theme.Theme;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Generate management pack from stack definition
 */
public class MpackGenerator {
  private static final Logger LOG = LoggerFactory.getLogger
    (MpackGenerator.class);

  private static final ObjectMapper mapper = new ObjectMapper();
  private File commonServicesRoot;
  private File stackRoot;
  private File mpacksRoot;
  private StackId srcStackId;
  private StackId dstStackId;
  private StackManager stackManager;
  private VersionDefinitionXml vdf;
  private Gson gson = new GsonBuilder().setPrettyPrinting().create();

  /**
   * {@link MpackGenerator} constructor
   * @param stackRoot           Stack root directory
   * @param commonServicesRoot  Common services root directory
   * @param srcStackId          Source stack id
   * @param vdfUrl              Version Definition File
   * @param mpacksRoot          Management pack root directory
   * @throws Exception
   */
  public MpackGenerator(File stackRoot, File commonServicesRoot, StackId srcStackId, URL vdfUrl, File mpacksRoot)
    throws Exception {
    this.commonServicesRoot = commonServicesRoot;
    this.stackRoot = stackRoot;
    this.mpacksRoot = mpacksRoot;
    this.srcStackId = srcStackId;
    this.stackManager = new StackManager(stackRoot, commonServicesRoot, false);
    this.vdf = VersionDefinitionXml.load(vdfUrl);
    this.dstStackId = new StackId(vdf.release.stackId);
  }

  /**
   * Generate management pack
   * @throws Exception
   */
  public void generateMpack() throws Exception {
    String dstStackName = dstStackId.getStackName();
    String version = vdf.release.version;
    String build = vdf.release.build;

    System.out.println("===========================================================");
    System.out.println("Source Stack Id: " + srcStackId);
    System.out.println("Destination Stack Id: " + dstStackId);
    System.out.println("===========================================================");

    String mpackName = dstStackName.toLowerCase() + "-ambari-mpack" + "-" + version + "-" + build;
    File mpackRootDir = new File(mpacksRoot.getAbsolutePath() + File.separator + mpackName);
    if (!mpacksRoot.exists()) {
      mpacksRoot.mkdirs();
    }
    if (mpackRootDir.exists()) {
      FileUtils.deleteDirectory(mpackRootDir);
    }
    mpackRootDir.mkdir();

    File mpackPackletsDir = new File(mpackRootDir.getAbsolutePath() + File.separator + "packlets");
    if (mpackPackletsDir.exists()) {
      mpackPackletsDir.delete();
    }
    mpackPackletsDir.mkdir();

    StackInfo srcStackInfo = stackManager.getStack(srcStackId.getStackName(), srcStackId.getStackVersion());
    StackRoleCommandOrder stackRoleCommandOrder = srcStackInfo.getRoleCommandOrder();
    FileWriter stackRCOFile = new FileWriter(
      mpackRootDir.getAbsolutePath() + File.separator + AmbariMetaInfo.RCO_FILE_NAME);
    mapper.writerWithDefaultPrettyPrinter().writeValue(stackRCOFile, stackRoleCommandOrder.getContent());

    // Export stack configs
    File stackConfigDir = new File(
      mpackRootDir.getAbsolutePath() + File.separator + AmbariMetaInfo.SERVICE_CONFIG_FOLDER_NAME);
    exportConfigs(srcStackInfo.getProperties(), stackConfigDir);

    // Export widgets.json
    exportFile(new File(srcStackInfo.getWidgetsDescriptorFileLocation()), mpackRootDir);

    // Export kerberos.json
    exportFile(new File(srcStackInfo.getKerberosDescriptorFileLocation()), mpackRootDir);

    // Export repoinfo.xml
    RepositoryXml repositoryXml =  srcStackInfo.getRepositoryXml();
    JAXBContext ctx = JAXBContext.newInstance(RepositoryXml.class);
    Marshaller marshaller = ctx.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    File reposDir = new File(mpackRootDir.getAbsolutePath() + File.separator + "repos");
    if (!reposDir.exists()) {
      reposDir.mkdir();
    }
    FileOutputStream repoXmlFileStream = new FileOutputStream(
      reposDir.getAbsolutePath() + File.separator + "repoinfo.xml");
    marshaller.marshal(repositoryXml, repoXmlFileStream);
    repoXmlFileStream.flush();
    repoXmlFileStream.close();

    // Copy JSON cluster property files (example: stacks/HDP/2.0.6/properties/stack_tools.json)
    File destPropertiesDir = new File(mpackRootDir.getAbsoluteFile() + File.separator + "properties");
    if(!destPropertiesDir.exists()) {
      destPropertiesDir.mkdir();
    }
    String srcStackName = srcStackId.getStackName();
    String currentStackVersion = srcStackId.getStackVersion();

    while (!StringUtils.isEmpty(currentStackVersion)) {
      StackInfo currentStackInfo = stackManager.getStack(srcStackName, currentStackVersion);
      File srcPropertiesDir = new File(stackRoot.getAbsolutePath() + File.separator + srcStackName + File.separator + currentStackVersion + File.separator + "properties");
      if (srcPropertiesDir.exists() && srcPropertiesDir.isDirectory()) {
        for (File srcPropertiesFile : srcPropertiesDir.listFiles()) {
          File destPropertiesFile = new File(destPropertiesDir.getAbsolutePath() + File.separator + srcPropertiesFile.getName());
          if (!destPropertiesFile.exists()) {
            FileUtils.copyFile(srcPropertiesFile, destPropertiesFile);
          }
        }
      }
      currentStackVersion = currentStackInfo.getParentStackVersion();
    }

    // Copy stack hooks folder
    String srcStackHooksFolder = srcStackInfo.getStackHooksFolder();
    srcStackHooksFolder = stackRoot.getAbsolutePath() + File.separator + srcStackHooksFolder;
    File srcStackHooksFile = new File(srcStackHooksFolder);
    if (srcStackHooksFile != null && srcStackHooksFile.exists()) {
      File destStackHooksFile = new File(
        mpackRootDir.getAbsolutePath() + File.separator
          + srcStackHooksFile.getName());
      FileUtils.copyDirectory(srcStackHooksFile, destStackHooksFile);
    }

    // Export stack metainfo.xml
    String parentStackVersion = srcStackInfo.getParentStackVersion();
    StackMetainfoXml stackMetainfoXml = new StackMetainfoXml();
    stackMetainfoXml.setMinJdk(srcStackInfo.getMinJdk());
    stackMetainfoXml.setMaxJdk(srcStackInfo.getMaxJdk());
    StackMetainfoXml.Version ver = new StackMetainfoXml.Version();
    ver.setActive(srcStackInfo.isActive());
    stackMetainfoXml.setVersion(ver);
    ctx = JAXBContext.newInstance(StackMetainfoXml.class);
    marshaller = ctx.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    FileOutputStream stackMetainfoFileStream = new FileOutputStream(
      mpackRootDir.getAbsolutePath() + File.separator + "metainfo.xml");
    marshaller.marshal(stackMetainfoXml, stackMetainfoFileStream);
    stackMetainfoFileStream.flush();
    stackMetainfoFileStream.close();

    // Export stack advisors
    File stackAdvisorsDir = new File(mpackRootDir.getAbsolutePath() + File.separator + "stack-advisor");
    if(!stackAdvisorsDir.exists()) {
      stackAdvisorsDir.mkdir();
    }

    currentStackVersion = srcStackId.getStackVersion();
    String baseStackAdvisor = null;
    String baseStackAdvisorModule = null;
    while (!StringUtils.isEmpty(currentStackVersion)) {
      // Copy all inherited stack advisors from source stack to "stack-advisor" folder
      StackInfo currentStackInfo = stackManager.getStack(srcStackName, currentStackVersion);
      File srcStackAdvisor = new File(stackRoot.getAbsolutePath() + File.separator + srcStackName + File.separator + currentStackVersion + File.separator + "services" + File.separator + "stack_advisor.py");
      if(srcStackAdvisor.exists()) {
        if(baseStackAdvisor == null) {
          baseStackAdvisor = srcStackName.toUpperCase() + currentStackVersion.replace(".", "") + "StackAdvisor";
          baseStackAdvisorModule = "stack_advisor_" + srcStackName.toLowerCase() + currentStackVersion.replace(".", "");
        }
        File dstStackAdvisor = new File(
          stackAdvisorsDir.getAbsolutePath() + File.separator + "stack_advisor_" + srcStackName.toLowerCase()
            + currentStackVersion.replace(".", "") + ".py");
        FileUtils.copyFile(srcStackAdvisor, dstStackAdvisor);
      }
      currentStackVersion = currentStackInfo.getParentStackVersion();
    }
    if(baseStackAdvisor != null) {
      File mpackServicesDir = new File(mpackRootDir.getAbsolutePath() + File.separator + "services");
      if (!mpackServicesDir.exists()) {
        mpackServicesDir.mkdir();
      }
      String mpackStackAdvisorName = dstStackId.getStackName().toUpperCase() + dstStackId.getStackVersion().replace(".", "") + "StackAdvisor";
      if(baseStackAdvisor.equalsIgnoreCase(mpackStackAdvisorName)) {
        // Use top level stack advisor from source stack as mpack stack advisor
        String srcPath = stackAdvisorsDir.getAbsolutePath() + File.separator + baseStackAdvisorModule + ".py";
        String dstPath = mpackServicesDir.getAbsolutePath() + File.separator + "stack_advisor.py";
        Files.move(Paths.get(srcPath), Paths.get(dstPath));
      } else {
        // Create mpack stack advisor that inherits from top level stack advisor from source stack
        FileWriter fileWriter = new FileWriter(
          mpackServicesDir.getAbsolutePath() + File.separator + "stack_advisor.py");
        BufferedWriter bw = new BufferedWriter(fileWriter);
        bw.write("from " + baseStackAdvisorModule + " import *");
        bw.newLine();
        bw.write("class " + mpackStackAdvisorName + "(" + baseStackAdvisor + ")");
        bw.newLine();
        bw.write("  pass");
        bw.newLine();
        bw.flush();
        fileWriter.flush();
        bw.close();
        fileWriter.close();
      }
    }

    Mpack mpack = new Mpack();
    mpack.setName(dstStackName);
    mpack.setVersion(vdf.release.getFullVersion());
    mpack.setStackId(dstStackId.getStackId());
    mpack.setDescription(dstStackName + " Ambari Management Pack");
    Map<String, String> prereqs = new HashMap<>();
    prereqs.put("min-ambari-version", "3.0.0.0");
    mpack.setPrerequisites(prereqs);
    List<Packlet> packlets = new ArrayList<>();
    mpack.setPacklets(packlets);

    for (ManifestServiceInfo manifestServiceInfo : vdf.getStackServices(srcStackInfo, true /* skipMissingServices = true */)) {
      ServiceInfo serviceInfo = srcStackInfo.getService(manifestServiceInfo.getName());
      String serviceName = manifestServiceInfo.getName();
      String serviceVersion = (String) manifestServiceInfo.getVersions().toArray()[0];
      ServiceInfo clonedServiceInfo =  (ServiceInfo) serviceInfo.clone();
      clonedServiceInfo.setVersion(serviceVersion);
      clonedServiceInfo.setParent(null);
      if (serviceInfo.getMetricsFile() != null) {
        clonedServiceInfo.setMetricsFileName(serviceInfo.getMetricsFile().getName());
      }
      if( serviceInfo.getWidgetsDescriptorFile() != null) {
        clonedServiceInfo.setWidgetsFileName(serviceInfo.getWidgetsDescriptorFile().getName());
      }

      System.out.println("Processing service=" + serviceInfo.getName() + ", version=" + serviceVersion);
      System.out.println("Service Parent : " + serviceInfo.getParent());
      String packletDirName = serviceName + "-packlet-" + serviceVersion;
      String packletTarName = packletDirName + ".tar.gz";
      File packletDir = new File(
        mpackPackletsDir.getAbsolutePath() + File.separator + packletDirName);
      if (!packletDir.exists()) {
        packletDir.mkdir();
      }
      Packlet packlet = new Packlet();
      packlet.setType(Packlet.PackletType.SERVICE_PACKLET);
      packlet.setName(serviceName);
      packlet.setVersion(serviceVersion);
      packlet.setServiceId(serviceVersion);
      packlet.setSourceLocation("packlets" + File.separator + packletTarName);
      packlets.add(packlet);

      // Export service metainfo.xml
      ServiceMetainfoXml serviceMetainfoXml = new ServiceMetainfoXml();
      serviceMetainfoXml.setSchemaVersion(clonedServiceInfo.getSchemaVersion());
      List<ServiceInfo> serviceInfos = Collections.singletonList(clonedServiceInfo);
      serviceMetainfoXml.setServices(serviceInfos);
      ctx = JAXBContext.newInstance(ServiceMetainfoXml.class);
      marshaller = ctx.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      FileOutputStream serviceMetainfoFileStream = new FileOutputStream(
        packletDir.getAbsolutePath() + File.separator + "metainfo.xml");
      marshaller.marshal(serviceMetainfoXml, serviceMetainfoFileStream);
      serviceMetainfoFileStream.flush();
      serviceMetainfoFileStream.close();

      // Export mertrics.json
      File srcMetricsFile = serviceInfo.getMetricsFile();
      exportFile(srcMetricsFile, packletDir);

      // Export widgets.json
      File srcWidgetsFile = serviceInfo.getWidgetsDescriptorFile();
      exportFile(srcWidgetsFile, packletDir);

      // Export alerts.json
      File srcAlertsFile = serviceInfo.getAlertsFile();
      exportFile(srcAlertsFile, packletDir);

      // Export kerberos.json
      File srcKerberosFile = serviceInfo.getKerberosDescriptorFile();
      exportFile(srcKerberosFile, packletDir);

      // Export quicklinks
      for (Map.Entry<String, QuickLinksConfigurationInfo> entry : serviceInfo.getQuickLinksConfigurationsMap()
        .entrySet()) {
        QuickLinksConfigurationInfo quickLinksConfigurationInfo = entry.getValue();
        String quickLinksFileName = quickLinksConfigurationInfo.getFileName();
        for (Map.Entry<String, QuickLinks> quickLinksEntry : quickLinksConfigurationInfo
          .getQuickLinksConfigurationMap().entrySet()) {
          File quickLinksDir = new File(
            packletDir.getAbsolutePath() + File.separator + serviceInfo
              .getQuickLinksConfigurationsDir());
          if (!quickLinksDir.exists()) {
            quickLinksDir.mkdir();
          }
          FileWriter quickLinksFileWriter = new FileWriter(
            quickLinksDir.getAbsolutePath() + File.separator + quickLinksFileName, true);
          mapper.writerWithDefaultPrettyPrinter()
            .writeValue(quickLinksFileWriter, quickLinksEntry.getValue());
        }
      }

      // Export themes
      for (Map.Entry<String, ThemeInfo> entry : serviceInfo.getThemesMap().entrySet()) {
        ThemeInfo themeInfo = entry.getValue();
        String themeFileName = themeInfo.getFileName();
        for (Map.Entry<String, Theme> themeEntry : themeInfo.getThemeMap().entrySet()) {
          File themesDir = new File(
            packletDir.getAbsolutePath() + File.separator + serviceInfo.getThemesDir());
          if (!themesDir.exists()) {
            themesDir.mkdir();
          }
          FileWriter themesFileWriter = new FileWriter(
            themesDir.getAbsolutePath() + File.separator + themeFileName, true);
          mapper.writerWithDefaultPrettyPrinter().writeValue(themesFileWriter, themeEntry.getValue());
        }
      }

      // Export package folder
      String srcPackageFolder = serviceInfo.getServicePackageFolder();
      if (srcPackageFolder.startsWith("common-services")) {
        srcPackageFolder = srcPackageFolder
          .replace("common-services", commonServicesRoot.getAbsolutePath());
      } else {
        srcPackageFolder = srcPackageFolder.replace("stacks", stackRoot.getAbsolutePath());
      }
      File srcPackageFile = new File(srcPackageFolder);
      if (srcPackageFile != null && srcPackageFile.exists()) {
        File destPackageFile = new File(
          packletDir.getAbsolutePath() + File.separator
            + ServiceDirectory.PACKAGE_FOLDER_NAME);
        FileUtils.copyDirectory(srcPackageFile, destPackageFile);
      }

      // Export merged configs
      File configDir = new File(
        packletDir.getAbsolutePath() + File.separator + serviceInfo.getConfigDir());
      exportConfigs(serviceInfo.getProperties(), configDir);

      // Copy service advisor
      File srcServiceAdvisor = serviceInfo.getAdvisorFile();
      File destServiceAdvisor = new File(packletDir.getAbsolutePath() + File.separator + "service_advisor.py");
      if(srcServiceAdvisor != null && srcServiceAdvisor.exists()) {
        FileUtils.copyFile(srcServiceAdvisor, destServiceAdvisor);
      }

      // TODO: Export upgrade packs

      // Create packlet tarball
      createTarGzip(packletDir.getAbsolutePath());
      if(packletDir.exists()) {
        FileUtils.deleteDirectory(packletDir);
      }
    }

    // Create mpack.json
    String mpackFilePath = mpackRootDir.getAbsolutePath() + File.separator + "mpack.json";
    FileWriter mpackFileWriter = new FileWriter(mpackFilePath);
    gson.toJson(mpack, Mpack.class, mpackFileWriter);
    mpackFileWriter.flush();
    mpackFileWriter.close();

    // Create mpack tarball
    createTarGzip(mpackRootDir.getAbsolutePath());
    if(mpackRootDir.exists()) {
      FileUtils.deleteDirectory(mpackRootDir);
    }
  }

  public static void exportFile(File srcFile, File destRootDir) throws Exception {
    if (srcFile != null && srcFile.exists()) {
      Path srcPath = Paths.get(srcFile.getAbsolutePath());
      Path destPath = Paths.get(
        destRootDir.getAbsolutePath() + File.separator + srcFile.getName());
      Files.copy(srcPath, destPath, StandardCopyOption.COPY_ATTRIBUTES,
        StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public void createTarGzip(String path) throws FileNotFoundException, IOException {
    File file = new File(path);
    if(!file.exists()) {
      throw new FileNotFoundException(path + " does not exist");
    }
    String parentDirName = file.getParent();
    String tarGzPath = parentDirName + File.separator + file.getName() + ".tar.gz";
    // Delete existing tarball
    File f = new File(tarGzPath);
    if(f.exists()) {
      f.delete();
    }
    FileOutputStream fOut = null;
    BufferedOutputStream bOut = null;
    GzipCompressorOutputStream gzOut = null;
    TarArchiveOutputStream tOut = null;
    try {
      fOut = new FileOutputStream(new File(tarGzPath));
      bOut = new BufferedOutputStream(fOut);
      gzOut = new GzipCompressorOutputStream(bOut);
      tOut = new TarArchiveOutputStream(gzOut);
      addFileToTarGz(tOut, path, "");
      System.out.println("Compressed " + path + " -> " + tarGzPath);
    } finally {
      if(tOut != null) {
        tOut.finish();
        tOut.close();
      }
      if(gzOut != null) {
        gzOut.close();
      }
      if(bOut != null) {
        bOut.close();
      }
      if(fOut != null) {
        fOut.close();
      }
    }

  }

  private void addFileToTarGz(TarArchiveOutputStream tOut, String path, String base)
    throws IOException
  {
    File f = new File(path);
    String entryName = base + f.getName();
    TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);
    tOut.putArchiveEntry(tarEntry);

    if (f.isFile()) {
      IOUtils.copy(new FileInputStream(f), tOut);
      tOut.closeArchiveEntry();
    } else {
      tOut.closeArchiveEntry();
      File[] children = f.listFiles();
      if (children != null) {
        for (File child : children) {
          addFileToTarGz(tOut, child.getAbsolutePath(), entryName + "/");
        }
      }
    }
  }

  public static void exportConfigs(List<PropertyInfo> properties, File configDir) throws Exception {
    if (!configDir.exists()) {
      configDir.mkdir();
    }

    Map<String, List<PropertyInfo>> configFilesMap = new HashMap<>();
    for (PropertyInfo propertyInfo : properties) {
      String fileName = propertyInfo.getFilename();
      if (!configFilesMap.containsKey(fileName)) {
        configFilesMap.put(fileName, new ArrayList<PropertyInfo>());
      }
      configFilesMap.get(fileName).add(propertyInfo);
    }

    for (Map.Entry<String, List<PropertyInfo>> entry : configFilesMap.entrySet()) {
      String fileName = entry.getKey();
      ConfigurationXml configXml = new ConfigurationXml();
      configXml.setProperties(entry.getValue());
      JAXBContext ctx = JAXBContext.newInstance(ConfigurationXml.class);
      Marshaller marshaller = ctx.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      FileOutputStream configFileStream = new FileOutputStream(
        configDir.getAbsolutePath() + File.separator + fileName);
      marshaller.marshal(configXml, configFileStream);
      configFileStream.flush();
      configFileStream.close();
    }
  }

  /**
   * Main method for generating mpack
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    System.out.println("Mpack Generator Started");
    String stackDir = args[0];
    String commonServicesDir = args[1];
    String srcStack = args[2];
    String vdfPath = args[3];
    String mpacksDir = args[4];
    MpackGenerator mpackGenerator = new MpackGenerator(
      new File(stackDir),
      new File(commonServicesDir),
      new StackId(srcStack),
      new URL(vdfPath),
      new File(mpacksDir));
    mpackGenerator.generateMpack();
    System.out.println("Mpack Generator Finished");
  }
}
