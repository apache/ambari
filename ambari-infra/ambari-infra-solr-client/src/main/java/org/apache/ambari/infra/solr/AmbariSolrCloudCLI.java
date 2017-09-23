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

package org.apache.ambari.infra.solr;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AmbariSolrCloudCLI {

  private static final Logger LOG = LoggerFactory.getLogger(AmbariSolrCloudCLI.class);

  private static final int ZK_CLIENT_TIMEOUT = 15000;
  private static final int ZK_CLIENT_CONNECT_TIMEOUT = 15000;
  private static final String CREATE_COLLECTION_COMMAND = "create-collection";
  private static final String UPLOAD_CONFIG_COMMAND = "upload-config";
  private static final String DOWNLOAD_CONFIG_COMMAND = "download-config";
  private static final String CONFIG_CHECK_COMMAND = "check-config";
  private static final String CREATE_SHARD_COMMAND = "create-shard";
  private static final String CREATE_ZNODE = "create-znode";
  private static final String SET_CLUSTER_PROP = "cluster-prop";
  private static final String SETUP_KERBEROS_PLUGIN = "setup-kerberos-plugin";
  private static final String CHECK_ZNODE = "check-znode";
  private static final String SECURE_ZNODE_COMMAND = "secure-znode";
  private static final String UNSECURE_ZNODE_COMMAND = "unsecure-znode";
  private static final String SECURE_SOLR_ZNODE_COMMAND = "secure-solr-znode";
  private static final String SECURITY_JSON_LOCATION = "security-json-location";
  private static final String REMOVE_ADMIN_HANDLERS = "remove-admin-handlers";
  private static final String CMD_LINE_SYNTAX =
    "\n./solrCloudCli.sh --create-collection -z host1:2181,host2:2181/ambari-solr -c collection -cs conf_set"
      + "\n./solrCloudCli.sh --upload-config -z host1:2181,host2:2181/ambari-solr -d /tmp/myconfig_dir -cs config_set"
      + "\n./solrCloudCli.sh --download-config -z host1:2181,host2:2181/ambari-solr -cs config_set -d /tmp/myonfig_dir"
      + "\n./solrCloudCli.sh --check-config -z host1:2181,host2:2181/ambari-solr -cs config_set"
      + "\n./solrCloudCli.sh --create-shard -z host1:2181,host2:2181/ambari-solr -c collection -sn myshard"
      + "\n./solrCloudCli.sh --remove-admin-handlers -z host1:2181,host2:2181/ambari-solr -c collection"
      + "\n./solrCloudCli.sh --create-znode -z host1:2181,host2:2181 -zn /ambari-solr"
      + "\n./solrCloudCli.sh --check-znode -z host1:2181,host2:2181 -zn /ambari-solr"
      + "\n./solrCloudCli.sh --cluster-prop -z host1:2181,host2:2181/ambari-solr -cpn urlScheme -cpn http"
      + "\n./solrCloudCli.sh --secure-znode -z host1:2181,host2:2181 -zn /ambari-solr -su logsearch,atlas,ranger --jaas-file /etc/myconf/jaas_file"
      + "\n./solrCloudCli.sh --unsecure-znode -z host1:2181,host2:2181 -zn /ambari-solr --jaas-file /etc/myconf/jaas_file"
      + "\n./solrCloudCli.sh --secure-solr-znode -z host1:2181,host2:2181 -zn /ambari-solr -su logsearch,atlas,ranger --jaas-file /etc/myconf/jaas_file"
      + "\n./solrCloudCli.sh --setup-kerberos-plugin -z host1:2181,host2:2181 -zn /ambari-solr --security-json-location /etc/infra-solr/conf/security.json\n ";

  public static void main(String[] args) {
    Options options = new Options();
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.setDescPadding(10);
    helpFormatter.setWidth(200);

    final Option helpOption = Option.builder("h")
      .longOpt("help")
      .desc("Print commands")
      .build();

    final Option createCollectionOption = Option.builder("cc")
      .longOpt(CREATE_COLLECTION_COMMAND)
      .desc("Create collection in Solr (command)")
      .build();

    final Option uploadConfigurationOption = Option.builder("uc")
      .longOpt(UPLOAD_CONFIG_COMMAND)
      .desc("Upload configuration set to Zookeeper (command)")
      .build();

    final Option downloadConfigOption = Option.builder("dc")
      .longOpt(DOWNLOAD_CONFIG_COMMAND)
      .desc("Download configuration set from Zookeeper (command)")
      .build();

    final Option checkConfigOption = Option.builder("chc")
      .longOpt(CONFIG_CHECK_COMMAND)
      .desc("Check configuration exists in Zookeeper (command)")
      .build();

    final Option checkZnodeOption = Option.builder("chz")
      .longOpt(CHECK_ZNODE)
      .desc("Check znode exists in Zookeeper (command)")
      .build();

    final Option createShardOption = Option.builder("csh")
      .longOpt(CREATE_SHARD_COMMAND)
      .desc("Create shard in Solr (command)")
      .build();

    final Option setClusterPropOption = Option.builder("cp")
      .longOpt(SET_CLUSTER_PROP)
      .desc("Set cluster property (command)")
      .build();

    final Option createZnodeOption = Option.builder("cz")
      .longOpt(CREATE_ZNODE)
      .desc("Create Znode (command)")
      .build();

    final Option setupKerberosPluginOption = Option.builder("skp")
      .longOpt(SETUP_KERBEROS_PLUGIN)
      .desc("Setup kerberos plugin in security.json (command)")
      .build();

    final Option secureSolrZnodeOption = Option.builder("ssz")
      .longOpt(SECURE_SOLR_ZNODE_COMMAND)
      .desc("Set acls for solr znode (command)")
      .build();

    final Option secureZnodeOption = Option.builder("sz")
      .longOpt(SECURE_ZNODE_COMMAND)
      .desc("Set acls for znode (command)")
      .build();

    final Option unsecureZnodeOption = Option.builder("uz")
      .longOpt(UNSECURE_ZNODE_COMMAND)
      .desc("Disable security for znode (command)")
      .build();

    final Option removeAdminHandlerOption = Option.builder("rah")
      .longOpt(REMOVE_ADMIN_HANDLERS)
      .desc("Remove AdminHandlers request handler from solrconfig.xml (command)")
      .build();

    final Option shardNameOption = Option.builder("sn")
      .longOpt("shard-name")
      .desc("Name of the shard for create-shard command")
      .numberOfArgs(1)
      .argName("my_new_shard")
      .build();

    final Option disableShardingOption = Option.builder("ns")
      .longOpt("no-sharding")
      .desc("Sharding not used when creating collection")
      .build();

    final Option zkConnectStringOption = Option.builder("z")
      .longOpt("zookeeper-connect-string")
      .desc("Zookeeper quorum [and Znode (optional)]")
      .numberOfArgs(1)
      .argName("host:port,host:port[/ambari-solr]")
      .build();

    final Option znodeOption = Option.builder("zn")
      .longOpt("znode")
      .desc("Zookeeper ZNode")
      .numberOfArgs(1)
      .argName("/ambari-solr")
      .build();

    final Option collectionOption = Option.builder("c")
      .longOpt("collection")
      .desc("Collection name")
      .numberOfArgs(1)
      .argName("collection name")
      .build();

    final Option configSetOption = Option.builder("cs")
      .longOpt("config-set")
      .desc("Configuration set")
      .numberOfArgs(1)
      .argName("config_set")
      .build();

    final Option configDirOption = Option.builder("d")
      .longOpt("config-dir")
      .desc("Configuration directory")
      .numberOfArgs(1)
      .argName("config_dir")
      .build();

    final Option shardsOption = Option.builder("s")
      .longOpt("shards")
      .desc("Number of shards")
      .numberOfArgs(1)
      .argName("shard number")
      .type(Integer.class)
      .build();

    final Option replicationOption = Option.builder("r")
      .longOpt("replication")
      .desc("Replication factor")
      .numberOfArgs(1)
      .argName("replication factor")
      .type(Integer.class)
      .build();

    final Option retryOption = Option.builder("rt")
      .longOpt("retry")
      .desc("Number of retries for access Solr [default:10]")
      .numberOfArgs(1)
      .argName("number of retries")
      .type(Integer.class)
      .build();

    final Option intervalOption = Option.builder("i")
      .longOpt("interval")
      .desc("Interval for retry logic in sec [default:5]")
      .numberOfArgs(1)
      .argName("interval")
      .type(Integer.class)
      .build();

    final Option maxShardsOption = Option.builder("m")
      .longOpt("max-shards")
      .desc("Max number of shards per node (default: replication * shards)")
      .numberOfArgs(1)
      .argName("max number of shards")
      .build();

    final Option routerNameOption = Option.builder("rn")
      .longOpt("router-name")
      .desc("Router name for collection [default:implicit]")
      .numberOfArgs(1)
      .argName("router_name")
      .build();

    final Option routerFieldOption = Option.builder("rf")
      .longOpt("router-field")
      .desc("Router field for collection [default:_router_field_]")
      .numberOfArgs(1)
      .argName("router_field")
      .build();

    final Option jaasFileOption = Option.builder("jf")
      .longOpt("jaas-file")
      .desc("Location of the jaas-file to communicate with kerberized Solr")
      .numberOfArgs(1)
      .argName("jaas_file")
      .build();

    final Option keyStoreLocationOption = Option.builder("ksl")
      .longOpt("key-store-location")
      .desc("Location of the key store used to communicate with Solr using SSL")
      .numberOfArgs(1)
      .argName("key store location")
      .build();

    final Option keyStorePasswordOption = Option.builder("ksp")
      .longOpt("key-store-password")
      .desc("Key store password used to communicate with Solr using SSL")
      .numberOfArgs(1)
      .argName("key store password")
      .build();

    final Option keyStoreTypeOption = Option.builder("kst")
      .longOpt("key-store-type")
      .desc("Type of the key store used to communicate with Solr using SSL")
      .numberOfArgs(1)
      .argName("key store type")
      .build();

    final Option trustStoreLocationOption = Option.builder("tsl")
      .longOpt("trust-store-location")
      .desc("Location of the trust store used to communicate with Solr using SSL")
      .numberOfArgs(1)
      .argName("trust store location")
      .build();

    final Option trustStorePasswordOption = Option.builder("tsp")
      .longOpt("trust-store-password")
      .desc("Trust store password used to communicate with Solr using SSL")
      .numberOfArgs(1)
      .argName("trust store password")
      .build();

    final Option trustStoreTypeOption = Option.builder("tst")
      .longOpt("trust-store-type")
      .desc("Type of the trust store used to communicate with Solr using SSL")
      .numberOfArgs(1)
      .argName("trust store type")
      .build();

    final Option propNameOption = Option.builder("cpn")
      .longOpt("property-name")
      .desc("Cluster property name")
      .numberOfArgs(1)
      .argName("cluster prop name")
      .build();

    final Option propValueOption = Option.builder("cpv")
      .longOpt("property-value")
      .desc("Cluster property value")
      .numberOfArgs(1)
      .argName("cluster prop value")
      .build();

    final Option copyFromZnodeOption = Option.builder("cfz")
      .longOpt("copy-from-znode")
      .desc("Copy-from-znode")
      .numberOfArgs(1)
      .argName("/ambari-solr-secure")
      .build();

    final Option saslUsersOption = Option.builder("su")
      .longOpt("sasl-users")
      .desc("Sasl users (comma separated list)")
      .numberOfArgs(1)
      .argName("atlas,ranger,logsearch-solr")
      .build();

    final Option securityJsonLocationOption = Option.builder("sjl")
      .longOpt(SECURITY_JSON_LOCATION)
      .desc("Local security.json path")
      .numberOfArgs(1)
      .argName("security.json location")
      .build();

    final Option secureOption = Option.builder("sec")
      .longOpt("secure")
      .desc("Flag for enable/disable kerberos (with --setup-kerberos or --setup-kerberos-plugin)")
      .build();

    options.addOption(helpOption);
    options.addOption(retryOption);
    options.addOption(removeAdminHandlerOption);
    options.addOption(intervalOption);
    options.addOption(zkConnectStringOption);
    options.addOption(configSetOption);
    options.addOption(configDirOption);
    options.addOption(collectionOption);
    options.addOption(secureZnodeOption);
    options.addOption(unsecureZnodeOption);
    options.addOption(secureSolrZnodeOption);
    options.addOption(shardsOption);
    options.addOption(replicationOption);
    options.addOption(maxShardsOption);
    options.addOption(routerNameOption);
    options.addOption(routerFieldOption);
    options.addOption(shardNameOption);
    options.addOption(disableShardingOption);
    options.addOption(createCollectionOption);
    options.addOption(downloadConfigOption);
    options.addOption(uploadConfigurationOption);
    options.addOption(checkConfigOption);
    options.addOption(createShardOption);
    options.addOption(jaasFileOption);
    options.addOption(keyStoreLocationOption);
    options.addOption(keyStorePasswordOption);
    options.addOption(keyStoreTypeOption);
    options.addOption(trustStoreLocationOption);
    options.addOption(trustStorePasswordOption);
    options.addOption(trustStoreTypeOption);
    options.addOption(setClusterPropOption);
    options.addOption(propNameOption);
    options.addOption(propValueOption);
    options.addOption(createZnodeOption);
    options.addOption(znodeOption);
    options.addOption(secureOption);
    options.addOption(copyFromZnodeOption);
    options.addOption(saslUsersOption);
    options.addOption(checkZnodeOption);
    options.addOption(setupKerberosPluginOption);
    options.addOption(securityJsonLocationOption);

    AmbariSolrCloudClient solrCloudClient = null;

    try {
      CommandLineParser cmdLineParser = new DefaultParser();
      CommandLine cli = cmdLineParser.parse(options, args);

      if(cli.hasOption('h')) {
        helpFormatter.printHelp("sample", options);
        exit(0, null);
      }
      String command = "";
      if (cli.hasOption("cc")) {
        command = CREATE_COLLECTION_COMMAND;
        validateRequiredOptions(cli, command, zkConnectStringOption, collectionOption, configSetOption);
      } else if (cli.hasOption("uc")) {
        command = UPLOAD_CONFIG_COMMAND;
        validateRequiredOptions(cli, command, zkConnectStringOption, configSetOption, configDirOption);
      } else if (cli.hasOption("dc")) {
        command = DOWNLOAD_CONFIG_COMMAND;
        validateRequiredOptions(cli, command, zkConnectStringOption, configSetOption, configDirOption);
      } else if (cli.hasOption("csh")) {
        command = CREATE_SHARD_COMMAND;
        validateRequiredOptions(cli, command, zkConnectStringOption, collectionOption, shardNameOption);
      } else if (cli.hasOption("chc")) {
        command = CONFIG_CHECK_COMMAND;
        validateRequiredOptions(cli, command, zkConnectStringOption, configSetOption);
      } else if (cli.hasOption("cp")) {
        command = SET_CLUSTER_PROP;
        validateRequiredOptions(cli, command, zkConnectStringOption, propNameOption, propValueOption);
      } else if (cli.hasOption("cz")) {
        command = CREATE_ZNODE;
        validateRequiredOptions(cli, command, zkConnectStringOption, znodeOption);
      } else if (cli.hasOption("chz")){
        command = CHECK_ZNODE;
        validateRequiredOptions(cli, command, zkConnectStringOption, znodeOption);
      } else if (cli.hasOption("skp")) {
        command = SETUP_KERBEROS_PLUGIN;
        validateRequiredOptions(cli, command, zkConnectStringOption, znodeOption);
      } else if (cli.hasOption("sz")) {
        command = SECURE_ZNODE_COMMAND;
        validateRequiredOptions(cli, command, zkConnectStringOption, znodeOption, jaasFileOption, saslUsersOption);
      } else if (cli.hasOption("ssz")) {
        command = SECURE_SOLR_ZNODE_COMMAND;
        validateRequiredOptions(cli, command, zkConnectStringOption, znodeOption, jaasFileOption, saslUsersOption);
      } else if (cli.hasOption("uz")) {
        command = UNSECURE_ZNODE_COMMAND;
        validateRequiredOptions(cli, command, zkConnectStringOption, znodeOption, jaasFileOption);
      } else if (cli.hasOption("rah")) {
        command = REMOVE_ADMIN_HANDLERS;
        validateRequiredOptions(cli, command, zkConnectStringOption, collectionOption);
      } else {
        List<String> commands = Arrays.asList(CREATE_COLLECTION_COMMAND, CREATE_SHARD_COMMAND, UPLOAD_CONFIG_COMMAND,
          DOWNLOAD_CONFIG_COMMAND, CONFIG_CHECK_COMMAND, SET_CLUSTER_PROP, CREATE_ZNODE, SECURE_ZNODE_COMMAND, UNSECURE_ZNODE_COMMAND,
          SECURE_SOLR_ZNODE_COMMAND, CHECK_ZNODE, SETUP_KERBEROS_PLUGIN, REMOVE_ADMIN_HANDLERS);
        helpFormatter.printHelp(CMD_LINE_SYNTAX, options);
        exit(1, String.format("One of the supported commands is required (%s)", StringUtils.join(commands, "|")));
      }

      String zkConnectString = cli.getOptionValue('z');
      String collection = cli.getOptionValue('c');
      String configSet = cli.getOptionValue("cs");
      String configDir = cli.getOptionValue("d");
      int shards = cli.hasOption('s') ? Integer.parseInt(cli.getOptionValue('s')) : 1;
      int replication = cli.hasOption('r') ? Integer.parseInt(cli.getOptionValue('r')) : 1;
      int retry = cli.hasOption("rt") ? Integer.parseInt(cli.getOptionValue("rt")) : 5;
      int interval = cli.hasOption('i') ? Integer.parseInt(cli.getOptionValue('i')) : 10;
      int maxShards = cli.hasOption('m') ? Integer.parseInt(cli.getOptionValue('m')) : shards * replication;
      String routerName = cli.hasOption("rn") ? cli.getOptionValue("rn") : null;
      String routerField = cli.hasOption("rf") ? cli.getOptionValue("rf") : null;
      String shardName = cli.hasOption("sn") ? cli.getOptionValue("sn") : null;
      boolean isSplitting = !cli.hasOption("ns");
      String jaasFile = cli.hasOption("jf") ? cli.getOptionValue("jf") : null;
      String keyStoreLocation = cli.hasOption("ksl") ? cli.getOptionValue("ksl") : null;
      String keyStorePassword = cli.hasOption("ksp") ? cli.getOptionValue("ksp") : null;
      String keyStoreType = cli.hasOption("kst") ? cli.getOptionValue("kst") : null;
      String trustStoreLocation = cli.hasOption("tsl") ? cli.getOptionValue("tsl") : null;
      String trustStorePassword = cli.hasOption("tsp") ? cli.getOptionValue("tsp") : null;
      String trustStoreType = cli.hasOption("tst") ? cli.getOptionValue("tst") : null;
      String clusterPropName = cli.hasOption("cpn") ? cli.getOptionValue("cpn") : null;
      String clusterPropValue = cli.hasOption("cpv") ? cli.getOptionValue("cpv") : null;
      String znode = cli.hasOption("zn") ? cli.getOptionValue("zn") : null;
      boolean isSecure = cli.hasOption("sec");
      String saslUsers = cli.hasOption("su") ? cli.getOptionValue("su") : "";
      String securityJsonLocation = cli.hasOption("sjl") ? cli.getOptionValue("sjl") : "";

      AmbariSolrCloudClientBuilder clientBuilder = new AmbariSolrCloudClientBuilder()
        .withZkConnectString(zkConnectString)
        .withCollection(collection)
        .withConfigSet(configSet)
        .withShards(shards)
        .withReplication(replication)
        .withMaxShardsPerNode(maxShards)
        .withRetry(retry)
        .withInterval(interval)
        .withRouterName(routerName)
        .withRouterField(routerField)
        .withJaasFile(jaasFile) // call before creating SolrClient
        .withSplitting(isSplitting)
        .withSolrZkClient(ZK_CLIENT_TIMEOUT, ZK_CLIENT_CONNECT_TIMEOUT)
        .withKeyStoreLocation(keyStoreLocation)
        .withKeyStorePassword(keyStorePassword)
        .withKeyStoreType(keyStoreType)
        .withTrustStoreLocation(trustStoreLocation)
        .withTrustStorePassword(trustStorePassword)
        .withTrustStoreType(trustStoreType)
        .withClusterPropName(clusterPropName)
        .withClusterPropValue(clusterPropValue)
        .withSecurityJsonLocation(securityJsonLocation)
        .withZnode(znode)
        .withSecure(isSecure)
        .withSaslUsers(saslUsers);

      switch (command) {
        case CREATE_COLLECTION_COMMAND:
          solrCloudClient = clientBuilder
            .withSolrCloudClient()
            .build();
          solrCloudClient.createCollection();
          break;
        case UPLOAD_CONFIG_COMMAND:
          solrCloudClient = clientBuilder
            .withConfigDir(configDir)
            .build();
          solrCloudClient.uploadConfiguration();
          break;
        case DOWNLOAD_CONFIG_COMMAND:
          solrCloudClient = clientBuilder
            .withConfigDir(configDir)
            .build();
          solrCloudClient.downloadConfiguration();
          break;
        case CONFIG_CHECK_COMMAND:
          solrCloudClient = clientBuilder.build();
          boolean configExists = solrCloudClient.configurationExists();
          if (!configExists) {
            exit(1, null);
          }
          break;
        case CREATE_SHARD_COMMAND:
          solrCloudClient = clientBuilder
            .withSolrCloudClient()
            .build();
          solrCloudClient.createShard(shardName);
          break;
        case SET_CLUSTER_PROP:
          solrCloudClient = clientBuilder.build();
          solrCloudClient.setClusterProp();
          break;
        case CREATE_ZNODE:
          solrCloudClient = clientBuilder.build();
          solrCloudClient.createZnode();
          break;
        case CHECK_ZNODE:
          solrCloudClient = clientBuilder.build();
          boolean znodeExists = solrCloudClient.isZnodeExists(znode);
          if (!znodeExists) {
            exit(1, String.format("'%s' znode does not exist. Solr is responsible to create the ZNode, " +
              "check Solr started successfully or not", znode));
          }
          break;
        case SETUP_KERBEROS_PLUGIN:
          solrCloudClient = clientBuilder.build();
          solrCloudClient.setupKerberosPlugin();
          break;
        case SECURE_ZNODE_COMMAND:
          solrCloudClient = clientBuilder.build();
          solrCloudClient.secureZnode();
          break;
        case UNSECURE_ZNODE_COMMAND:
          solrCloudClient = clientBuilder.build();
          solrCloudClient.unsecureZnode();
          break;
        case SECURE_SOLR_ZNODE_COMMAND:
          solrCloudClient = clientBuilder.build();
          solrCloudClient.secureSolrZnode();
        case REMOVE_ADMIN_HANDLERS:
          solrCloudClient = clientBuilder.build();
          solrCloudClient.removeAdminHandlerFromCollectionConfig();
          break;
        default:
          throw new AmbariSolrCloudClientException(String.format("Not found command: '%s'", command));
      }
    } catch (Exception e) {
      helpFormatter.printHelp(
        CMD_LINE_SYNTAX, options);
      exit(1, e.getMessage());
    } finally {
      if (solrCloudClient != null && solrCloudClient.getSolrZkClient() != null) {
        solrCloudClient.getSolrZkClient().close();
      }
    }
    exit(0, null);
  }

  private static void validateRequiredOptions(CommandLine cli, String command, Option... optionsToValidate)
    throws AmbariSolrCloudClientException {
    List<String> requiredOptions = new ArrayList<>();
    for (Option opt : optionsToValidate) {
      if (!cli.hasOption(opt.getOpt())) {
        requiredOptions.add(opt.getOpt());
      }
    }
    if (!requiredOptions.isEmpty()) {
      throw new AmbariSolrCloudClientException(
        String.format("The following options required for '%s' : %s",
          command, StringUtils.join(requiredOptions, ",")));
    }
  }

  private static void exit(int exitCode, String message) {
    if (message != null){
      LOG.error(message);
    }
    LOG.info("Return code: {}", exitCode);
    System.exit(exitCode);
  }
}
