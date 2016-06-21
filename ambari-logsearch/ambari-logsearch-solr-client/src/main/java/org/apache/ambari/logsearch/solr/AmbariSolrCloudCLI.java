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

package org.apache.ambari.logsearch.solr;

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

  private static final int ZK_CLIENT_TIMEOUT = 10000;
  private static final int ZK_CLIENT_CONNECT_TIMEOUT = 10000;
  private static final String CREATE_COLLECTION_COMMAND = "create-collection";
  private static final String UPLOAD_CONFIG_COMMAND = "upload-config";
  private static final String DOWNLOAD_CONFIG_COMMAND = "download-config";
  private static final String CONFIG_CHECK_COMMAND = "check-config";
  private static final String CREATE_SHARD_COMMAND = "create-shard";
  private static final String CMD_LINE_SYNTAX =
    "\n./solrCloudCli.sh --create-collection -z host1:2181,host2:2181/ambari-solr -c collection -cs conf_set"
      + "\n./solrCloudCli.sh --upload-config -z host1:2181,host2:2181/ambari-solr -d /tmp/myonfig_dir -cs config_set"
      + "\n./solrCloudCli.sh --download-config -z host1:2181,host2:2181/ambari-solr -cs config_set -d /tmp/myonfig_dir"
      + "\n./solrCloudCli.sh --check-config -z host1:2181,host2:2181/ambari-solr -cs config_set"
      + "\n./solrCloudCli.sh --create-shard -z host1:2181,host2:2181/ambari-solr -c collection -sn myshard\n";

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

    final Option createShardOption = Option.builder("csh")
      .longOpt(CREATE_SHARD_COMMAND)
      .desc("Create shard in Solr (command)")
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
      .desc("Zookeeper quorum [and a Znode]")
      .numberOfArgs(1)
      .argName("host:port,host:port[/ambari-solr]")
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

    options.addOption(helpOption);
    options.addOption(retryOption);
    options.addOption(intervalOption);
    options.addOption(zkConnectStringOption);
    options.addOption(configSetOption);
    options.addOption(configDirOption);
    options.addOption(collectionOption);
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
      } else {
        List<String> commands = Arrays.asList(CREATE_COLLECTION_COMMAND, CREATE_SHARD_COMMAND, UPLOAD_CONFIG_COMMAND,
          DOWNLOAD_CONFIG_COMMAND, CONFIG_CHECK_COMMAND);
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
        .withSolrZkClient(ZK_CLIENT_TIMEOUT, ZK_CLIENT_CONNECT_TIMEOUT);

      AmbariSolrCloudClient solrCloudClient = null;
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
            .withSolrZkClient(ZK_CLIENT_TIMEOUT, ZK_CLIENT_CONNECT_TIMEOUT)
            .build();
          solrCloudClient.createShard(shardName);
          break;
        default:
          throw new AmbariSolrCloudClientException(String.format("Not found command: '%s'", command));
      }
    } catch (Exception e) {
      helpFormatter.printHelp(
        CMD_LINE_SYNTAX, options);
      exit(1, e.getMessage());
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
