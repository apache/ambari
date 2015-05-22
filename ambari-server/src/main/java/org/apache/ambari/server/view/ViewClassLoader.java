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

package org.apache.ambari.server.view;

import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.IOException;
import java.net.URL;

/**
 * Class loader used to load classes and resources from a search path of URLs referring to both JAR files
 * and directories.  The URLs will be searched in the order specified for classes and resources before
 * searching the parent class loader.
 */
public class ViewClassLoader extends WebAppClassLoader {

  // ----- Constructors ------------------------------------------------------

  /**
   * Constructs a new ViewClassLoader for the given URLs using a default parent class loader.
   * The URLs will be searched in the order specified for classes and resources before searching
   * the parent class loader.
   *
   * @param urls  the URLs from which to load classes and resources
   */
  public ViewClassLoader(URL[] urls) throws IOException {
    this(null, urls);
  }

  /**
   * Constructs a new ViewClassLoader for the given URLs.
   * The URLs will be searched in the order specified for classes and resources before searching the specified
   * parent class loader.
   *
   * @param parent  the parent class loader
   * @param urls    the URLs from which to load classes and resources
   */
  public ViewClassLoader(ClassLoader parent, URL[] urls) throws IOException {
    super(parent, getInitContext());

    for (URL url : urls) {
      addURL(url);
    }
  }


  // ----- helper methods ----------------------------------------------------

  // Get a context to initialize the class loader.
  private static WebAppContext getInitContext() {
    // For now we are using defaults or setting the values for things like parent loader priority and
    // system classes.  In the future we may allow overrides at the view level.
    WebAppContext webAppContext = new WebAppContext();

    // add com.google.inject as system classes to allow for injection in view components using the google annotation
    webAppContext.addSystemClass("com.google.inject.");
    // add org.slf4j as system classes to avoid linkage errors
    webAppContext.addSystemClass("org.slf4j.");

    return webAppContext;
  }
}
