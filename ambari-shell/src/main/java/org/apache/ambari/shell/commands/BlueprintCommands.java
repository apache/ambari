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
package org.apache.ambari.shell.commands;

import static org.apache.ambari.shell.support.TableRenderer.renderMultiValueMap;
import static org.apache.ambari.shell.support.TableRenderer.renderSingleMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.shell.model.AmbariContext;
import org.apache.ambari.shell.model.Hints;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

/**
 * Blueprint related commands used in the shell.
 *
 * @see org.apache.ambari.groovy.client.AmbariClient
 */
@Component
public class BlueprintCommands implements CommandMarker {

  private AmbariClient client;
  private AmbariContext context;

  @Autowired
  public BlueprintCommands(AmbariClient client, AmbariContext context) {
    this.client = client;
    this.context = context;
  }

  /**
   * Checks whether the blueprints command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("blueprint list")
  public boolean isBlueprintListCommandAvailable() {
    return context.areBlueprintsAvailable();
  }

  /**
   * Prints all the blueprints.
   *
   * @return list of blueprints
   */
  @CliCommand(value = "blueprint list", help = "Lists all known blueprints")
  public String listBlueprints() {
    return renderSingleMap(client.getBlueprintsMap(), "BLUEPRINT", "STACK");
  }

  /**
   * Checks whether the blueprint show command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator(value = "blueprint show")
  public boolean isBlueprintShowCommandAvailable() {
    return context.areBlueprintsAvailable();
  }

  /**
   * Shows the requested blueprint's details.
   *
   * @param id id of the blueprint
   * @return blueprint as formatted table
   */
  @CliCommand(value = "blueprint show", help = "Shows the blueprint by its id")
  public String showBlueprint(
    @CliOption(key = "id", mandatory = true, help = "Id of the blueprint") String id) {
    return renderMultiValueMap(client.getBlueprintMap(id), "HOSTGROUP", "COMPONENT");
  }

  /**
   * Checks whether the blueprint add command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator(value = "blueprint add")
  public boolean isBlueprintAddCommandAvailable() {
    return true;
  }

  /**
   * Adds a blueprint to the Ambari server either through an URL or from a file.
   * If both specified the file takes precedence.
   *
   * @param url  -optional, URL containing the blueprint json
   * @param file - optional, file containing the blueprint json
   * @return status message
   */
  @CliCommand(value = "blueprint add", help = "Add a new blueprint with either --url or --file")
  public String addBlueprint(
    @CliOption(key = "url", mandatory = false, help = "URL of the blueprint to download from") String url,
    @CliOption(key = "file", mandatory = false, help = "File which contains the blueprint") File file) {
    String message;
    try {
      String json = file == null ? readContent(url) : readContent(file);
      if (json != null) {
        client.addBlueprint(json);
        message = "Blueprint added";
        context.setHint(Hints.BUILD_CLUSTER);
        context.setBlueprintsAvailable(true);
      } else {
        message = "No blueprint specified";
      }
    } catch (Exception e) {
      message = "Cannot add blueprint: " + e.getMessage();
    }
    return message;
  }

  /**
   * Checks whether the blueprint defaults command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator(value = "blueprint defaults")
  public boolean isBlueprintDefaultsAddCommandAvailable() {
    return !context.areBlueprintsAvailable();
  }

  /**
   * Adds two default blueprints to the Ambari server.
   *
   * @return status message
   */
  @CliCommand(value = "blueprint defaults", help = "Adds the default blueprints to Ambari")
  public String addBlueprint() {
    String message = "Default blueprints added";
    try {
      client.addDefaultBlueprints();
      context.setHint(Hints.BUILD_CLUSTER);
      context.setBlueprintsAvailable(true);
    } catch (Exception e) {
      message = "Failed to add the default blueprints: " + e.getMessage();
    }
    return message;
  }

  private String readContent(File file) {
    String content = null;
    try {
      content = IOUtils.toString(new FileInputStream(file));
    } catch (IOException e) {
      // not important
    }
    return content;
  }

  private String readContent(String url) {
    String content = null;
    try {
      content = IOUtils.toString(new URL(url));
    } catch (IOException e) {
      // not important
    }
    return content;
  }
}
