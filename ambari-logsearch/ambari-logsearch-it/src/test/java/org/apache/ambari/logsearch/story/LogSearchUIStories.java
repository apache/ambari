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
package org.apache.ambari.logsearch.story;

import org.apache.ambari.logsearch.domain.StoryDataRegistry;
import org.apache.ambari.logsearch.steps.LogSearchDockerSteps;
import org.apache.ambari.logsearch.steps.LogSearchUISteps;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.Embeddable;
import org.jbehave.core.embedder.executors.SameThreadExecutors;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.jbehave.web.selenium.RemoteWebDriverProvider;
import org.jbehave.web.selenium.SeleniumConfiguration;
import org.jbehave.web.selenium.SeleniumContext;
import org.jbehave.web.selenium.WebDriverProvider;
import org.jbehave.web.selenium.WebDriverScreenshotOnFailure;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.Arrays;
import java.util.List;

import static org.jbehave.core.io.CodeLocations.codeLocationFromClass;
import static org.jbehave.core.reporters.Format.CONSOLE;
import static org.jbehave.core.reporters.Format.HTML;
import static org.jbehave.core.reporters.Format.TXT;
import static org.jbehave.core.reporters.Format.XML;

public class LogSearchUIStories extends JUnitStories {

  private WebDriverProvider driverProvider;
  private SeleniumContext context;

  private static final String UI_STORIES_LOCATION_PROPERTY = "ui.stories.location";
  private static final String STORY_SUFFIX = ".ui.story";

  public LogSearchUIStories() {
    String dockerHost = System.getProperty("docker.host") != null ? System.getProperty("docker.host") : "localhost";
    System.setProperty("REMOTE_WEBDRIVER_URL", String.format("http://%s:4444/wd/hub", dockerHost));
    DesiredCapabilities capability = DesiredCapabilities.firefox();
    capability.setPlatform(Platform.LINUX);
    capability.setVersion("45.8.0");
    driverProvider = new RemoteWebDriverProvider(capability);
    StoryDataRegistry.INSTANCE.setWebDriverProvider(driverProvider);
    context = new SeleniumContext();
    configuredEmbedder().useExecutorService(new SameThreadExecutors().create(configuredEmbedder().embedderControls()));
  }

  @Override
  public Configuration configuration() {
    Class<? extends Embeddable> embeddableClass = this.getClass();
    return new SeleniumConfiguration()
      .useSeleniumContext(context)
      .useWebDriverProvider(driverProvider)
      .useStoryLoader(LogSearchStoryLocator.getStoryLoader(UI_STORIES_LOCATION_PROPERTY, this.getClass()))
      .useStoryReporterBuilder(new StoryReporterBuilder()
        .withCodeLocation(codeLocationFromClass(embeddableClass))
        .withDefaultFormats()
        .withFormats(CONSOLE, TXT, HTML, XML));
  }

  @Override
  public InjectableStepsFactory stepsFactory() {
    Configuration configuration = configuration();
    return new InstanceStepsFactory(configuration, new LogSearchDockerSteps(), new LogSearchUISteps(driverProvider),
      new WebDriverScreenshotOnFailure(driverProvider, configuration.storyReporterBuilder()));
  }

  @Override
  protected List<String> storyPaths() {
    return LogSearchStoryLocator.findStories(UI_STORIES_LOCATION_PROPERTY, STORY_SUFFIX, this.getClass());
  }
}
