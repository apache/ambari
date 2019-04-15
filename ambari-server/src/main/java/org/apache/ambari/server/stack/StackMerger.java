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
package org.apache.ambari.server.stack;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.ldap.LdapModule;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.QuickLinksConfigurationInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.ThemeInfo;
import org.apache.ambari.server.state.quicklinks.QuickLinks;
import org.apache.ambari.server.state.stack.ConfigurationXml;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.apache.ambari.server.state.stack.StackMetainfoXml;
import org.apache.ambari.server.state.stack.StackRoleCommandOrder;
import org.apache.ambari.server.state.theme.Theme;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Utility tool for merging stack hierarchy and generate flattened stack definitions for legal stacks
 */
public class StackMerger {

  private static final String MERGED_STACKS_ROOT = "mergedStacksRoot";
  private static final String STACKS_ARG = "stacks";

  private static final Logger LOG = LoggerFactory.getLogger
    (StackMerger.class);

  private PersistService persistService;
  private DBAccessor dbAccessor;
  private Injector injector;

  private static final ObjectMapper mapper = new ObjectMapper();
  private File commonServicesRoot;
  private File stackRoot;
  private StackManager stackManager;
  private Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @Inject
  public StackMerger(Injector injector) throws Exception {
    this.injector = injector;
    AmbariMetaInfo metaInfo = injector.getInstance(AmbariMetaInfo.class);
    metaInfo.init();
    this.stackRoot = metaInfo.getStackRoot();
    this.commonServicesRoot = metaInfo.getCommonServicesRoot();
    this.stackManager = metaInfo.getStackManager();
  }

  /**
   * Extension of audit logger module
   */
  public static class StackMergerAuditModule extends AuditLoggerModule {

    public StackMergerAuditModule() throws Exception {
    }

    @Override
    protected void configure() {
      super.configure();
    }
  }

  /**
   * Context object that encapsulates values passed in as arguments to the {@link StackMerger} class.
   */
  private static class StackMergerContext {
    private String mergedStacksRoot;
    private HashSet<StackId> stackIds;

    public StackMergerContext(String mergedStacksRoot, HashSet<StackId> stackIds) {
      this.stackIds = stackIds;
      this.mergedStacksRoot = mergedStacksRoot;
    }

    public HashSet<StackId> getStackIds() {
      return stackIds;
    }

    public String getMergedStacksRoot() {
      return mergedStacksRoot;
    }
  }

  private static Options getOptions() {
    Options options = new Options();
    options.addOption(Option.builder().longOpt(STACKS_ARG).desc(
            "Comma-separated list of stacks to be merged and exported").required().type(String.class).hasArg().valueSeparator(' ').build());
    options.addOption(Option.builder().longOpt(MERGED_STACKS_ROOT).desc(
            "Root directory where the merged stacks should be exported").required().type(String.class).hasArg().valueSeparator(' ').build());
    return options;
  }

  private static StackMergerContext processArguments(String... args) throws Exception {
    CommandLineParser cmdLineParser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();

    CommandLine line = cmdLineParser.parse(getOptions(), args);
    String mergedStacksRoot = (String) line.getParsedOptionValue(MERGED_STACKS_ROOT);
    String stacksStr = (String) line.getParsedOptionValue(STACKS_ARG);
    HashSet<StackId> stackIds = new HashSet<>();
    for (String s : stacksStr.split(",")) {
      stackIds.add(new StackId(s));
    }
    return new StackMergerContext(mergedStacksRoot, stackIds);
  }

  public void mergeStacks(StackMergerContext ctx) throws Exception {
    File mergeRoot = new File(ctx.mergedStacksRoot);
    for(StackId stackId : ctx.stackIds) {
      mergeStack(mergeRoot, stackId);
    }
  }

  /**
   * Merge stack hierarchy to create flattened stack definition
   * @throws Exception
   */
  public void mergeStack(File mergeRoot, StackId srcStackId) throws Exception {
    StackId destStackId = srcStackId;
    String stackName = destStackId.getStackName();
    String stackVersion = destStackId.getStackVersion();
    File mergedStackDir = new File(mergeRoot.getAbsolutePath()
            + File.separator + stackName + File.separator + stackVersion);

    LOG.info("===========================================================");
    LOG.info("Source Stacks Root: " + stackRoot);
    LOG.info("Common Services Root: " + commonServicesRoot);
    LOG.info("Merged Stacks Root: " + mergeRoot);
    LOG.info("Source Stack Id: " + srcStackId);
    LOG.info("Destination Stack Id: " + destStackId);
    LOG.info("Merged Stack Path " + mergedStackDir);
    LOG.info("===========================================================");

    // Create merged stack directory
    if (!mergeRoot.exists()) {
      mergeRoot.mkdirs();
    }
    if (mergedStackDir.exists()) {
      FileUtils.deleteDirectory(mergedStackDir);
    }
    mergedStackDir.mkdirs();

    // Create services directory
    File servicesDir = new File(mergedStackDir.getAbsolutePath() + File.separator + "services");
    if (servicesDir.exists()) {
      servicesDir.delete();
    }
    servicesDir.mkdir();

    // Export role command order
    StackInfo srcStackInfo = stackManager.getStack(srcStackId.getStackName(), srcStackId.getStackVersion());
    StackRoleCommandOrder stackRoleCommandOrder = srcStackInfo.getRoleCommandOrder();
    FileWriter stackRCOFile = new FileWriter(
      mergedStackDir.getAbsolutePath() + File.separator + StackDirectory.RCO_FILE_NAME);
    mapper.writerWithDefaultPrettyPrinter().writeValue(stackRCOFile, stackRoleCommandOrder.getContent());

    // Export stack-level configs (example: cluster-env)
    File stackConfigDir = new File(
      mergedStackDir.getAbsolutePath() + File.separator + StackDirectory.SERVICE_CONFIG_FOLDER_NAME);
    exportConfigs(srcStackInfo.getProperties(), stackConfigDir);

    // Export stack metainfo.xml
    StackMetainfoXml stackMetainfoXml = new StackMetainfoXml();
    stackMetainfoXml.setMinJdk(srcStackInfo.getMinJdk());
    stackMetainfoXml.setMaxJdk(srcStackInfo.getMaxJdk());
    // Flattened stack, so set extends to null
    stackMetainfoXml.setExtendsVersion(null);
    StackMetainfoXml.Version version = new StackMetainfoXml.Version();
    version.setActive(srcStackInfo.isActive());
    version.setUpgrade(srcStackInfo.getMinUpgradeVersion());
    stackMetainfoXml.setVersion(version);

    JAXBContext ctx = JAXBContext.newInstance(StackMetainfoXml.class);
    Marshaller marshaller = ctx.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    FileOutputStream stackMetainfoFileStream = new FileOutputStream(
            mergedStackDir.getAbsolutePath() + File.separator + "metainfo.xml");
    marshaller.marshal(stackMetainfoXml, stackMetainfoFileStream);
    stackMetainfoFileStream.flush();
    stackMetainfoFileStream.close();

    // Export repoinfo.xml
    RepositoryXml repositoryXml =  srcStackInfo.getRepositoryXml();
    ctx = JAXBContext.newInstance(RepositoryXml.class);
    marshaller = ctx.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    File reposDir = new File(mergedStackDir.getAbsolutePath() + File.separator + "repos");
    if (!reposDir.exists()) {
      reposDir.mkdir();
    }
    FileOutputStream repoXmlFileStream = new FileOutputStream(
      reposDir.getAbsolutePath() + File.separator + "repoinfo.xml");
    marshaller.marshal(repositoryXml, repoXmlFileStream);
    repoXmlFileStream.flush();
    repoXmlFileStream.close();

    // Copy cluster property files (example: stacks/HDP/2.0.6/properties/stack_tools.json)
    File destPropertiesDir = new File(mergedStackDir.getAbsoluteFile() + File.separator + "properties");
    if(!destPropertiesDir.exists()) {
      destPropertiesDir.mkdir();
    }

    String srcStackName = srcStackId.getStackName();
    String currentStackVersion = srcStackId.getStackVersion();
    while (!StringUtils.isEmpty(currentStackVersion)) {
      StackInfo currentStackInfo = stackManager.getStack(srcStackName, currentStackVersion);
      File srcPropertiesDir = new File(stackRoot.getAbsolutePath()
              + File.separator + srcStackName + File.separator + currentStackVersion + File.separator + "properties");
      if (srcPropertiesDir.exists() && srcPropertiesDir.isDirectory()) {
        for (File srcPropertiesFile : srcPropertiesDir.listFiles()) {
          File destPropertiesFile = new File(destPropertiesDir.getAbsolutePath()
                  + File.separator + srcPropertiesFile.getName());
          if (!destPropertiesFile.exists()) {
            FileUtils.copyFile(srcPropertiesFile, destPropertiesFile);
          }
        }
      }
      currentStackVersion = currentStackInfo.getParentStackVersion();
    }

    // Copy Kerberos pre-configuration file (example: stacks/HDP/2.6/kerberos_preconfigure.json
    if(srcStackInfo.getKerberosDescriptorPreConfigurationFileLocation() != null) {
      File srcKerberosPreConfigFile = new File(srcStackInfo.getKerberosDescriptorPreConfigurationFileLocation());
      File destKerberosPreConfigFile = new File(mergedStackDir.getAbsoluteFile()
              + File.separator + StackDirectory.KERBEROS_DESCRIPTOR_PRECONFIGURE_FILE_NAME);
      if (!destKerberosPreConfigFile.exists()) {
        FileUtils.copyFile(srcKerberosPreConfigFile, destKerberosPreConfigFile);
      }
    }

    // Copy upgrade packs
    if(srcStackInfo.getUpgradesFolder() != null) {
      File srcUpgradesFile = new File(srcStackInfo.getUpgradesFolder());
      File destUpgradesFile = new File(mergedStackDir.getAbsoluteFile()
              + File.separator + StackDirectory.UPGRADE_PACK_FOLDER_NAME);
      FileUtils.copyDirectory(srcUpgradesFile, destUpgradesFile);
    }

    // Export all stack advisors in the stack hierarchy
    File stackAdvisorsDir = new File(mergedStackDir.getAbsolutePath() + File.separator + "stack-advisors");
    if(!stackAdvisorsDir.exists()) {
      stackAdvisorsDir.mkdir();
    }

    currentStackVersion = srcStackId.getStackVersion();
    String baseStackAdvisor = null;
    String baseStackAdvisorModule = null;
    while (!StringUtils.isEmpty(currentStackVersion)) {
      // Copy all inherited stack advisors from source stack to "stack-advisors" folder
      StackInfo currentStackInfo = stackManager.getStack(srcStackName, currentStackVersion);
      File srcStackAdvisor = new File(stackRoot.getAbsolutePath() + File.separator
              + srcStackName + File.separator + currentStackVersion + File.separator + "services" + File.separator
              + "stack_advisor.py");
      if(srcStackAdvisor.exists()) {
        if(baseStackAdvisor == null) {
          baseStackAdvisor = srcStackName.toUpperCase() + currentStackVersion.replace(".", "") + "StackAdvisor";
          baseStackAdvisorModule = "stack_advisor_" + srcStackName.toLowerCase() + currentStackVersion.replace(".", "");
        }
        File destStackAdvisor = new File(
          stackAdvisorsDir.getAbsolutePath() + File.separator + "stack_advisor_" + srcStackName.toLowerCase()
            + currentStackVersion.replace(".", "") + ".py");
        FileUtils.copyFile(srcStackAdvisor, destStackAdvisor);
      }
      currentStackVersion = currentStackInfo.getParentStackVersion();
    }

    // Define top-level stack advisor for merged stack
    if(baseStackAdvisor != null) {
      String topLevelStackAdvisorName = destStackId.getStackName().toUpperCase() + destStackId.getStackVersion().replace(".", "") + "StackAdvisor";
      if(baseStackAdvisor.equalsIgnoreCase(topLevelStackAdvisorName)) {
        // Use top level stack advisor from source stack as top level stack advisor for merged stack
        String srcPath = stackAdvisorsDir.getAbsolutePath() + File.separator + baseStackAdvisorModule + ".py";
        String destPath = servicesDir.getAbsolutePath() + File.separator + "stack_advisor.py";
        Files.move(Paths.get(srcPath), Paths.get(destPath));
      } else {
        // Create top level stack advisor for merged stack
        FileWriter fileWriter = new FileWriter(
          servicesDir.getAbsolutePath() + File.separator + "stack_advisor.py");
        BufferedWriter bw = new BufferedWriter(fileWriter);
        bw.write("from " + baseStackAdvisorModule + " import *");
        bw.newLine();
        bw.write("class " + topLevelStackAdvisorName + "(" + baseStackAdvisor + ")");
        bw.newLine();
        bw.write("  pass");
        bw.newLine();
        bw.flush();
        fileWriter.flush();
        bw.close();
        fileWriter.close();
      }
    }

    // Export all service definitions
    for (String serviceName : srcStackInfo.getServiceNames()) {

      ServiceInfo serviceInfo = srcStackInfo.getService(serviceName);
      ServiceInfo clonedServiceInfo =  (ServiceInfo) serviceInfo.clone();
      // Flattening the stack, so set cloned service's parent to null
      clonedServiceInfo.setParent(null);

      // Create service root directory
      File serviceDir = new File(
        servicesDir.getAbsolutePath() + File.separator + serviceName);
      if (!serviceDir.exists()) {
        serviceDir.mkdir();
      }

      // Export service metainfo.xml
      ServiceMetainfoXml serviceMetainfoXml = new ServiceMetainfoXml();
      serviceMetainfoXml.setSchemaVersion(clonedServiceInfo.getSchemaVersion());
      List<ServiceInfo> serviceInfos = Collections.singletonList(clonedServiceInfo);
      serviceMetainfoXml.setServices(serviceInfos);
      ctx = JAXBContext.newInstance(ServiceMetainfoXml.class);
      marshaller = ctx.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      FileOutputStream serviceMetainfoFileStream = new FileOutputStream(
              serviceDir.getAbsolutePath() + File.separator + "metainfo.xml");
      StringWriter sw = new StringWriter();
      marshaller.marshal(serviceMetainfoXml, serviceMetainfoFileStream);
      marshaller.marshal(serviceMetainfoXml, sw);
      serviceMetainfoFileStream.flush();
      serviceMetainfoFileStream.close();

      // Export mertrics.json
      File srcMetricsFile = serviceInfo.getMetricsFile();
      exportFile(srcMetricsFile, serviceDir);

      // Export widgets.json
      File srcWidgetsFile = serviceInfo.getWidgetsDescriptorFile();
      exportFile(srcWidgetsFile, serviceDir);

      // Export alerts.json
      File srcAlertsFile = serviceInfo.getAlertsFile();
      exportFile(srcAlertsFile, serviceDir);

      // Export kerberos.json
      File srcKerberosFile = serviceInfo.getKerberosDescriptorFile();
      exportFile(srcKerberosFile, serviceDir);

      // Export quicklinks
      for (Map.Entry<String, QuickLinksConfigurationInfo> entry : serviceInfo.getQuickLinksConfigurationsMap()
        .entrySet()) {
        QuickLinksConfigurationInfo quickLinksConfigurationInfo = entry.getValue();
        String quickLinksFileName = quickLinksConfigurationInfo.getFileName();
        for (Map.Entry<String, QuickLinks> quickLinksEntry : quickLinksConfigurationInfo
          .getQuickLinksConfigurationMap().entrySet()) {
          File quickLinksDir = new File(
                  serviceDir.getAbsolutePath() + File.separator + serviceInfo
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
                  serviceDir.getAbsolutePath() + File.separator + serviceInfo.getThemesDir());
          if (!themesDir.exists()) {
            themesDir.mkdir();
          }
          FileWriter themesFileWriter = new FileWriter(
            themesDir.getAbsolutePath() + File.separator + themeFileName, true);
          mapper.writerWithDefaultPrettyPrinter().writeValue(themesFileWriter, themeEntry.getValue());
        }
      }

      // Export package folder (python scripts)
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
                serviceDir.getAbsolutePath() + File.separator
            + ServiceDirectory.PACKAGE_FOLDER_NAME);
        FileUtils.copyDirectory(srcPackageFile, destPackageFile);
      }

      // Export merged service-level configs
      File configDir = new File(
              serviceDir.getAbsolutePath() + File.separator + serviceInfo.getConfigDir());
      exportConfigs(serviceInfo.getProperties(), configDir);

      // Copy service property files (example: common-services/KERBEROS/1.10.3-10/properties/krb5_conf.j2)
      File destServicePropertiesDir = new File(serviceDir.getAbsolutePath() + File.separator + "properties");
      if(!destServicePropertiesDir.exists()) {
        destServicePropertiesDir.mkdir();
      }

      srcStackName = srcStackId.getStackName();
      currentStackVersion = srcStackId.getStackVersion();
      boolean foundExplicitParent = false;
      while (!StringUtils.isEmpty(currentStackVersion)) {
        StackInfo currentStackInfo = stackManager.getStack(srcStackName, currentStackVersion);
        ServiceInfo currentServiceInfo = currentStackInfo.getService(serviceName);
        if(currentServiceInfo != null) {
          File srcServicePropertiesDir = new File(stackRoot.getAbsolutePath() + File.separator
                  + srcStackName + File.separator + currentStackVersion + File.separator
                  + "services" + File.separator + serviceName + File.separator + "properties");
          if (srcServicePropertiesDir.exists() && srcServicePropertiesDir.isDirectory()) {
            for (File srcServicePropertiesFile : srcServicePropertiesDir.listFiles()) {
              File destServicePropertiesFile = new File(destServicePropertiesDir.getAbsolutePath()
                      + File.separator + srcServicePropertiesFile.getName());
              if (!destServicePropertiesFile.exists()) {
                FileUtils.copyFile(srcServicePropertiesFile, destServicePropertiesFile);
              }
            }
          }
          String currentServiceParent =  currentServiceInfo.getParent();
          // Check for explicit service inheritance
          if (StringUtils.isNotEmpty(currentServiceParent)) {
            foundExplicitParent = true;
          }
        }
        if(foundExplicitParent) {
          // Stop traversing the stack hierarchy if explicit parent defined
          break;
        } else {
          currentStackVersion = currentStackInfo.getParentStackVersion();
        }
      }

      if(foundExplicitParent) {
        StackInfo currentStackInfo = stackManager.getStack(srcStackName, currentStackVersion);
        ServiceInfo currentServiceInfo = currentStackInfo.getService(serviceName);
        String currentServiceParent =  currentServiceInfo.getParent();
        if(currentServiceParent.split(StackManager.PATH_DELIMITER)[0].equalsIgnoreCase(StackManager.COMMON_SERVICES)) {
          String[] parentToks = currentServiceParent.split(StackManager.PATH_DELIMITER);
          File srcServicePropertiesDir = new File(commonServicesRoot.getAbsolutePath() + File.separator
                  + parentToks[1] + File.separator + parentToks[2] + File.separator + "properties");
          if (srcServicePropertiesDir.exists() && srcServicePropertiesDir.isDirectory()) {
            for (File srcServicePropertiesFile : srcServicePropertiesDir.listFiles()) {
              File destServicePropertiesFile = new File(destServicePropertiesDir.getAbsolutePath()
                      + File.separator + srcServicePropertiesFile.getName());
              if (!destServicePropertiesFile.exists()) {
                FileUtils.copyFile(srcServicePropertiesFile, destServicePropertiesFile);
              }
            }
          }
          // TODO : Not traversing common-services hierarchy for now
        }  else {
          // TODO : Not handling explicit inheritance outside of common-services for now
        }
      }

      // Copy service advisor
      File srcServiceAdvisor = serviceInfo.getAdvisorFile();
      File destServiceAdvisor = new File(serviceDir.getAbsolutePath() + File.separator + "service_advisor.py");
      if(srcServiceAdvisor != null && srcServiceAdvisor.exists()) {
        FileUtils.copyFile(srcServiceAdvisor, destServiceAdvisor);
      }
    }

    // Delete *.pyc, *.pyo, archive.zip
    FileUtils.listFiles(mergedStackDir, new String[]{"pyc", "pyo", "zip"}, true).forEach(File::delete);
    LOG.info("Merged Stack " + destStackId + " has been successfully exported at " + mergedStackDir);
  }

  /**
   * Export file
   * @param srcFile     Source File Path
   * @param destRootDir Target root directory
   * @throws Exception
   */
  public static void exportFile(File srcFile, File destRootDir) throws Exception {
    if (srcFile != null && srcFile.exists()) {
      Path srcPath = Paths.get(srcFile.getAbsolutePath());
      Path destPath = Paths.get(
        destRootDir.getAbsolutePath() + File.separator + srcFile.getName());
      Files.copy(srcPath, destPath, StandardCopyOption.COPY_ATTRIBUTES,
        StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Export configs
   * @param properties List of config properties
   * @param configDir Configuration directory
   * @throws Exception
   */
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
   * Main method for merging stack definitions
   *
   * Usage:
   *   java -cp /etc/ambari-server/conf:/usr/lib/ambari-server/*:/usr/share/java/postgresql-jdbc.jar
   *        org.apache.ambari.server.stack.StackMerger --mergedStacksRoot=/tmp/merged-root --stacks=HDP-2.5,HDP-2.6
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    StackMergerContext ctx = processArguments(args);

    LOG.info("********* Initializing Stack Merger *********");
    Injector injector = Guice.createInjector(new ControllerModule(), new StackMergerAuditModule(), new LdapModule());
    GuiceJpaInitializer jpaInitializer = injector.getInstance(GuiceJpaInitializer.class);
    jpaInitializer.setInitialized(injector.getInstance(AmbariEventPublisher.class));
    StackMerger stackMerger = injector.getInstance(StackMerger.class);
    LOG.info("********* Stack Merger Initialized *********");
    stackMerger.mergeStacks(ctx);
    LOG.info("********* Stack Merger Finished *********");

    System.exit(0);
  }
}
