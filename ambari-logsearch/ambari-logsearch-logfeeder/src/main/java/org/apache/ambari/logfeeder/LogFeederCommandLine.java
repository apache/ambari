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

package org.apache.ambari.logfeeder;

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
import java.util.List;

public class LogFeederCommandLine {

  private static final Logger LOG = LoggerFactory.getLogger(LogFeederCommandLine.class);
  
  private static final String TEST_COMMAND = "test";
  private static final String TEST_LOG_ENTRY_OPTION = "test-log-entry";
  private static final String TEST_SHIPPER_CONFIG_OPTION = "test-shipper-config";
  private static final String TEST_GLOBAL_CONFIG_OPTION = "test-global-config";
  private static final String TEST_LOG_ID_OPTION = "test-log-id";
  
  private static final String COMMAND_LINE_SYNTAX = "java org.apache.ambari.logfeeder.LogFeeder -(monitor|test) [args]";
  
  private CommandLine cli;

  public LogFeederCommandLine(String[] args) {
    Options options = new Options();
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.setDescPadding(10);
    helpFormatter.setWidth(200);

    Option helpOption = Option.builder("h")
      .longOpt("help")
      .desc("Print commands")
      .build();

    Option testOption = Option.builder("t")
      .longOpt(TEST_COMMAND)
      .desc("Test if log entry is parseable")
      .build();

    Option testLogEntryOption = Option.builder("tle")
      .longOpt(TEST_LOG_ENTRY_OPTION)
      .hasArg()
      .desc("Log entry to test if it's parseable")
      .build();

    Option testShipperConfOption = Option.builder("tsc")
      .longOpt(TEST_SHIPPER_CONFIG_OPTION)
      .hasArg()
      .desc("Shipper configuration file for testing if log entry is parseable")
      .build();

    Option testGlobalConfOption = Option.builder("tgc")
      .longOpt(TEST_GLOBAL_CONFIG_OPTION)
      .hasArg()
      .desc("Global configuration files (comma separated list) for testing if log entry is parseable")
      .build();

    Option testLogIdOption = Option.builder("tli")
      .longOpt(TEST_LOG_ID_OPTION)
      .hasArg()
      .desc("The id of the log to test")
      .build();

    options.addOption(helpOption);
    options.addOption(testOption);
    options.addOption(testLogEntryOption);
    options.addOption(testShipperConfOption);
    options.addOption(testGlobalConfOption);
    options.addOption(testLogIdOption);

    try {
      CommandLineParser cmdLineParser = new DefaultParser();
      cli = cmdLineParser.parse(options, args);

      if (cli.hasOption('h')) {
        helpFormatter.printHelp(COMMAND_LINE_SYNTAX, options);
        System.exit(0);
      }
      String command = "";
      if (cli.hasOption("t")) {
        command = TEST_COMMAND;
        validateRequiredOptions(cli, command, testLogEntryOption, testShipperConfOption);
      } else {
        LOG.info("Start application in monitor mode ");
      }
    } catch (Exception e) {
      LOG.info("Error parsing command line parameters: {}. LogFeeder will be started in monitoring mode.", e.getMessage());
    }
  }

  private static void validateRequiredOptions(CommandLine cli, String command, Option... optionsToValidate) {
    List<String> requiredOptions = new ArrayList<>();
    for (Option opt : optionsToValidate) {
      if (!cli.hasOption(opt.getOpt())) {
        requiredOptions.add(opt.getOpt());
      }
    }
    if (!requiredOptions.isEmpty()) {
      throw new IllegalArgumentException(
        String.format("The following options required for '%s' : %s", command, StringUtils.join(requiredOptions, ",")));
    }
  }
  
  public boolean isTest() {
    return cli != null && cli.hasOption('t');
  }
  
  public String getTestLogEntry() {
    return cli.getOptionValue("tle");
  }
  
  public String getTestShipperConfig() {
    return cli.getOptionValue("tsc");
  }
  
  public String getTestGlobalConfigs() {
    return cli.getOptionValue("tgc");
  }
  
  public String getTestLogId() {
    return cli.getOptionValue("tli");
  }
}
