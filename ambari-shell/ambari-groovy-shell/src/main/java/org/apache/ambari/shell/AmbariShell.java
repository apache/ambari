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
package org.apache.ambari.shell;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.shell.model.AmbariContext;
import org.apache.ambari.shell.model.Hints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.CommandLine;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.shell.event.ShellStatus;
import org.springframework.shell.event.ShellStatusListener;

/**
 * Shell bootstrap.
 */
@Configuration
@ComponentScan(basePackageClasses = {AmbariShell.class})
public class AmbariShell implements CommandLineRunner, ShellStatusListener {

  @Autowired
  private CommandLine commandLine;
  @Autowired
  private JLineShellComponent shell;
  @Autowired
  private AmbariContext context;
  @Autowired
  private AmbariClient client;

  @Override
  public void run(String... arg) throws Exception {
    String[] shellCommandsToExecute = commandLine.getShellCommandsToExecute();
    if (shellCommandsToExecute != null) {
      for (String cmd : shellCommandsToExecute) {
        if (!shell.executeScriptLine(cmd)) {
          break;
        }
      }
    } else {
      shell.addShellStatusListener(this);
      shell.start();
      shell.promptLoop();
      shell.waitForComplete();
    }
  }

  @Override
  public void onShellStatusChange(ShellStatus oldStatus, ShellStatus newStatus) {
    if (newStatus.getStatus() == ShellStatus.Status.STARTED) {
      try {
        String cluster = client.getClusterName();
        boolean available = client.isBlueprintAvailable();
        if (cluster == null) {
          if (available) {
            context.setHint(Hints.BUILD_CLUSTER);
          } else {
            context.setHint(Hints.ADD_BLUEPRINT);
          }
        } else {
          context.setHint(Hints.PROGRESS);
        }
        context.setCluster(cluster);
        context.setBlueprintsAvailable(available);
      } catch (Exception e) {
        System.out.println(e.getMessage());
        shell.executeCommand("quit");
      }
    }
  }


  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println(
        "\nAmbari Shell: Interactive command line tool for managing Apache Ambari.\n\n" +
          "Usage:\n" +
          "  java -jar ambari-shell.jar                  : Starts Ambari Shell in interactive mode.\n" +
          "  java -jar ambari-shell.jar --cmdfile=<FILE> : Ambari Shell executes commands read from the file.\n\n" +
          "Options:\n" +
          "  --ambari.host=<HOSTNAME>       Hostname of the Ambari Server [default: localhost].\n" +
          "  --ambari.port=<PORT>           Port of the Ambari Server [default: 8080].\n" +
          "  --ambari.user=<USER>           Username of the Ambari admin [default: admin].\n" +
          "  --ambari.password=<PASSWORD>   Password of the Ambari admin [default: admin].\n\n" +
          "Note:\n" +
          "  At least one option is mandatory."
      );
      System.exit(1);
    }
    new SpringApplicationBuilder(AmbariShell.class).showBanner(false).run(args);
  }
}
