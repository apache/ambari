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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.apache.ambari.logsearch.steps.LogSearchApiSteps;
import org.apache.ambari.logsearch.steps.SolrSteps;
import org.apache.ambari.logsearch.steps.LogSearchDockerSteps;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.embedder.executors.SameThreadExecutors;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.io.StoryFinder;
import org.jbehave.core.io.StoryPathResolver;
import org.jbehave.core.io.UnderscoredCamelCaseResolver;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.junit.JUnitStory;
import org.jbehave.core.reporters.Format;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static org.jbehave.core.io.CodeLocations.codeLocationFromClass;

public class LogSearchBackendStories extends JUnitStories {

  @Override
  public Configuration configuration() {
    return new MostUsefulConfiguration()
      .useStoryLoader(new LoadFromClasspath(this.getClass()))
      .useStoryReporterBuilder(
        new StoryReporterBuilder().withFailureTrace(true).withDefaultFormats().withFormats(Format.CONSOLE, Format.TXT));
  }

  @Override
  public InjectableStepsFactory stepsFactory() {
    return new InstanceStepsFactory(configuration(),
      new LogSearchDockerSteps(),
      new SolrSteps(),
      new LogSearchApiSteps());
  }

  @Test
  public void run() throws Throwable {
    super.run();
  }

  @Override
  protected List<String> storyPaths() {
    List<String> backendStories = new StoryFinder()
      .findPaths(codeLocationFromClass(this.getClass()).getFile(), Arrays.asList("**/*.story"), null);
    return Lists.newArrayList(Collections2.filter(backendStories, new Predicate<String>() {
      @Override
      public boolean apply(String storyFileName) {
        return !storyFileName.endsWith("ui.story");
      }
    }));
  }

}
