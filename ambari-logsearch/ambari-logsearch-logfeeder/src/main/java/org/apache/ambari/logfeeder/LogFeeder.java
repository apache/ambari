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
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.system.ApplicationPidFileWriter;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootApplication(
  scanBasePackages = {"org.apache.ambari.logfeeder"}
)
public class LogFeeder {

  public static void main(String[] args) {
    LogFeederCommandLine cli = new LogFeederCommandLine(args);
    if (cli.isTest()) {
      test(cli);
    }
    String pidFile = System.getenv("PID_FILE") == null ? "logfeeder.pid" : System.getenv("PID_FILE");
    new SpringApplicationBuilder(LogFeeder.class)
      .bannerMode(Banner.Mode.OFF)
      .listeners(new ApplicationPidFileWriter(pidFile))
      .run(args);
  }

  private static void test(LogFeederCommandLine cli) {
    try {
      LogManager.shutdown();
      String testLogEntry = cli.getTestLogEntry();
      String testShipperConfig = FileUtils.readFileToString(new File(cli.getTestShipperConfig()), Charset.defaultCharset());
      List<String> testGlobalConfigs = new ArrayList<>();
      for (String testGlobalConfigFile : cli.getTestGlobalConfigs().split(",")) {
        testGlobalConfigs.add(FileUtils.readFileToString(new File(testGlobalConfigFile), Charset.defaultCharset()));
      }
      String testLogId = cli.getTestLogId();
      Map<String, Object> result = new LogEntryParseTester(testLogEntry, testShipperConfig, testGlobalConfigs, testLogId).parse();
      String parsedLogEntry = new GsonBuilder().setPrettyPrinting().create().toJson(result);
      System.out.println("The result of the parsing is:\n" + parsedLogEntry);
    } catch (Exception e) {
      System.out.println("Exception occurred, could not test if log entry is parseable");
      e.printStackTrace(System.out);
    }
  }
}
