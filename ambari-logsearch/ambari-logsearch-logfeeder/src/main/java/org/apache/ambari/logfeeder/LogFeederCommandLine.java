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

import com.google.gson.GsonBuilder;
import org.apache.ambari.logfeeder.common.LogEntryParseTester;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogFeederCommandLine {
  
  private static final String TEST_COMMAND = "test";
  private static final String TEST_LOG_ENTRY_OPTION = "test-log-entry";
  private static final String TEST_SHIPPER_CONFIG_OPTION = "test-shipper-config";
  private static final String TEST_GLOBAL_CONFIG_OPTION = "test-global-config";
  private static final String TEST_LOG_ID_OPTION = "test-log-id";
  
  private static final String COMMAND_LINE_SYNTAX = "java org.apache.ambari.logfeeder.LogFeederCommandLine --test [args]";

  public static void main(String[] args) {
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
      CommandLine cli = cmdLineParser.parse(options, args);

      if (cli.hasOption('h')) {
        helpFormatter.printHelp(COMMAND_LINE_SYNTAX, options);
        System.exit(0);
      }
      String command = "";
      if (cli.hasOption("t")) {
        command = TEST_COMMAND;
        validateRequiredOptions(cli, command, testLogEntryOption, testShipperConfOption);
      }
      test(cli);
    } catch (Exception e) {
      e.printStackTrace();
      helpFormatter.printHelp(COMMAND_LINE_SYNTAX, options);
      System.exit(1);
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

  private static void test(CommandLine cli) {
    try {
      LogManager.shutdown();
      String testLogEntry = cli.getOptionValue("tle");
      String testShipperConfig = FileUtils.readFileToString(new File(cli.getOptionValue("tsc")), Charset.defaultCharset());
      List<String> testGlobalConfigs = new ArrayList<>();
      for (String testGlobalConfigFile : cli.getOptionValue("tgc").split(",")) {
        testGlobalConfigs.add(FileUtils.readFileToString(new File(testGlobalConfigFile), Charset.defaultCharset()));
      }
      String testLogId = cli.getOptionValue("tli");
      Map<String, Object> result = new LogEntryParseTester(testLogEntry, testShipperConfig, testGlobalConfigs, testLogId).parse();
      String parsedLogEntry = new GsonBuilder().setPrettyPrinting().create().toJson(result);
      System.out.println("The result of the parsing is:\n" + parsedLogEntry);
    } catch (Exception e) {
      System.out.println("Exception occurred, could not test if log entry is parseable");
      e.printStackTrace(System.out);
    }
  }
}
