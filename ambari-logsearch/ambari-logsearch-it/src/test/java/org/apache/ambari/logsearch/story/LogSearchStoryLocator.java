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

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.io.LoadFromRelativeFile;
import org.jbehave.core.io.StoryFinder;
import org.jbehave.core.io.StoryLoader;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.jbehave.core.io.CodeLocations.codeLocationFromClass;

/**
 * Helper class for loading story files from the classpath or externally - based on system properties
 */
public class LogSearchStoryLocator {

  private LogSearchStoryLocator() {
  }

  /**
   * Get the proper story loader based on story location property (if empty or NONE - use story loading from classpath)
   * @param property Story location property (absolute path - folder)
   * @param clazz Class of the *Stories object
   */
  public static StoryLoader getStoryLoader(String property, Class clazz) {
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


  /**
   * Find stories based on story location property, if the property is not set or NONE, then the story files will be loaded from the classpath
   * @param property Story location property (absolute path - folder)
   * @param suffix Story suffix for specific stories - i.e. : .ui.story
   * @param clazz Class of the *Stories object
   */
  public static List<String> findStories(String property, String suffix, Class clazz) {
    List<String> stories = null;
    if (useExternalStoryLocation(property)) {
      stories = findStoriesInFolder(System.getProperty(property), suffix);
    } else {
      stories = new StoryFinder()
        .findPaths(codeLocationFromClass(clazz).getFile(), Arrays.asList(String.format("**/*%s", suffix)), null);
    }
    return stories;
  }

  private static List<String> findStoriesInFolder(String folderAbsolutePath, String suffix) {
    List<String> results = Lists.newArrayList();
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
