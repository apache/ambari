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
package org.apache.ambari.shell.configuration;

import org.apache.ambari.groovy.client.AmbariClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.shell.CommandLine;
import org.springframework.shell.SimpleShellCommandLineOptions;
import org.springframework.shell.commands.ExitCommands;
import org.springframework.shell.commands.HelpCommands;
import org.springframework.shell.commands.ScriptCommands;
import org.springframework.shell.commands.VersionCommands;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.shell.plugin.HistoryFileNameProvider;
import org.springframework.shell.plugin.support.DefaultHistoryFileNameProvider;

/**
 * Spring bean definitions.
 */
@Configuration
public class ShellConfiguration {

  @Value("${ambari.host:localhost}")
  private String host;

  @Value("${ambari.port:8080}")
  private String port;

  @Value("${ambari.user:admin}")
  private String user;

  @Value("${ambari.password:admin}")
  private String password;

  @Value("${cmdfile:}")
  private String cmdFile;

  @Bean
  AmbariClient createAmbariClient() {
    return new AmbariClient(host, port, user, password);
  }

  @Bean
  static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @Bean
  HistoryFileNameProvider defaultHistoryFileNameProvider() {
    return new DefaultHistoryFileNameProvider();
  }

  @Bean(name = "shell")
  JLineShellComponent shell() {
    return new JLineShellComponent();
  }

  @Bean
  CommandLine commandLine() throws Exception {
    String[] args = cmdFile.length() > 0 ? new String[]{"--cmdfile", cmdFile} : new String[0];
    return SimpleShellCommandLineOptions.parseCommandLine(args);
  }

  @Bean
  ThreadPoolExecutorFactoryBean getThreadPoolExecutorFactoryBean() {
    return new ThreadPoolExecutorFactoryBean();
  }

  @Bean
  ObjectMapper getObjectMapper() {
    return new ObjectMapper();
  }

  @Bean
  CommandMarker exitCommand() {
    return new ExitCommands();
  }

  @Bean
  CommandMarker versionCommands() {
    return new VersionCommands();
  }

  @Bean
  CommandMarker helpCommands() {
    return new HelpCommands();
  }

  @Bean
  CommandMarker scriptCommands() {
    return new ScriptCommands();
  }
}
