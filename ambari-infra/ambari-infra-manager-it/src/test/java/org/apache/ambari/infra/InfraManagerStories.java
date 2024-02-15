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
package org.apache.ambari.infra;

import org.apache.ambari.infra.steps.ExportJobsSteps;
import org.apache.commons.lang.StringUtils;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.io.LoadFromRelativeFile;
import org.jbehave.core.io.StoryFinder;
import org.jbehave.core.io.StoryLoader;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.reporters.Format;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.jbehave.core.steps.ParameterConverters;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.jbehave.core.io.CodeLocations.codeLocationFromClass;

public class InfraManagerStories extends JUnitStories {
  private static final String BACKEND_STORIES_LOCATION_PROPERTY = "backend.stories.location";
  private static final String STORY_SUFFIX = ".story";

  @Override
  public Configuration configuration() {
    return new MostUsefulConfiguration()
            .useStoryLoader(getStoryLoader(BACKEND_STORIES_LOCATION_PROPERTY, this.getClass()))
            .useParameterConverters(new ParameterConverters().addConverters(new OffsetDateTimeConverter()))
            .useStoryReporterBuilder(
                    new StoryReporterBuilder().withFailureTrace(true).withDefaultFormats().withFormats(Format.CONSOLE, Format.TXT));
  }

  private static StoryLoader getStoryLoader(String property, Class clazz) {
    boolean useExternalStoryLocation = useExternalStoryLocation(property);
    if (useExternalStoryLocation) {
      try {
        return new LoadFromRelativeFile(new URL("file://" + System.getProperty(property)));
      } catch (Exception e) {
        throw new RuntimeException("Cannot load story files from url: file://" + System.getProperty(property));
      }
    } else {
      return new LoadFromClasspath(clazz);
    }
  }

  @Override
  public InjectableStepsFactory stepsFactory() {
    return new InstanceStepsFactory(configuration(), new ExportJobsSteps());
  }

  @Override
  protected List<String> storyPaths() {
    return findStories(BACKEND_STORIES_LOCATION_PROPERTY, STORY_SUFFIX, this.getClass());
  }

  private static List<String> findStories(String property, String suffix, Class clazz) {
    if (useExternalStoryLocation(property)) {
      return findStoriesInFolder(System.getProperty(property), suffix);
    } else {
      return new StoryFinder()
              .findPaths(codeLocationFromClass(clazz).getFile(), singletonList(String.format("**/*%s", suffix)), null);
    }
  }

  private static List<String> findStoriesInFolder(String folderAbsolutePath, String suffix) {
    List<String> results = new ArrayList<>();
    File folder = new File(folderAbsolutePath);
    File[] listOfFiles = folder.listFiles();
    if (listOfFiles != null) {
      for (File file : listOfFiles) {
        if (file.getName().endsWith(suffix)) {
          results.add(file.getName());
        }
      }
    }
    return results;
  }

  private static boolean useExternalStoryLocation(String property) {
    String storyLocationProp = System.getProperty(property);
    return StringUtils.isNotEmpty(storyLocationProp) && !"NONE".equals(storyLocationProp);
  }

}
