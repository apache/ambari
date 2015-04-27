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

import java.net.URL;
import java.net.URLClassLoader;


/**
 * Class loader used to load classes and resources from a search path of URLs referring to both JAR files
 * and directories.  The URLs will be searched in the order specified for classes and resources before
 * searching the parent class loader.
 */
public class ViewClassLoader extends URLClassLoader {

  // ----- Constructors ------------------------------------------------------

  /**
   * Constructs a new ViewClassLoader for the given URLs using a default parent class loader.
   * The URLs will be searched in the order specified for classes and resources before searching
   * the parent class loader.
   *
   * @param urls  the URLs from which to load classes and resources
   */
  public ViewClassLoader(URL[] urls) {
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
  public ViewClassLoader(ClassLoader parent, URL[] urls) {
    super(new URL[]{}, selectParentClassLoader(parent));

    for (URL url : urls) {
      addURL(url);
    }
  }


  // ----- ClassLoader -------------------------------------------------------

  @Override
  public synchronized URL getResource(String name) {
    URL resource = this.findResource(name);

    if (resource == null) {
      ClassLoader parentClassLoader = getParent();
      if (parentClassLoader != null) {
        resource = parentClassLoader.getResource(name);
      }
    }
    return resource;
  }

  @Override
  protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class clazz = findLoadedClass(name);

    if (clazz == null) {
      try {
        clazz = this.findClass(name);
      } catch (ClassNotFoundException e) {
        ClassLoader parentClassLoader = getParent();
        if (parentClassLoader != null) {
          clazz = parentClassLoader.loadClass(name);
        }

        if (clazz == null) {
          throw e;
        }
      }
    }

    if (resolve) {
      resolveClass(clazz);
    }
    return clazz;
  }


  // ----- helper methods ----------------------------------------------------

  // Get an appropriate parent class loader.
  private static ClassLoader selectParentClassLoader(ClassLoader parentClassLoader) {

    if (parentClassLoader == null) {

      parentClassLoader = Thread.currentThread().getContextClassLoader();

      if (parentClassLoader == null) {
        parentClassLoader = ViewClassLoader.class.getClassLoader();
      }
    }
    return parentClassLoader;
  }
}
