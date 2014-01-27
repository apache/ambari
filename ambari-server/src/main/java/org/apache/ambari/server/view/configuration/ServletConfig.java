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

package org.apache.ambari.server.view.configuration;

import javax.servlet.http.HttpServlet;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 *  View servlet mapping.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ServletConfig {
  /**
   * The servlet name.
   */
  @XmlElement(name="servlet-name")
  private String name;

  /**
   * The servlet class name.
   */
  @XmlElement(name="servlet-path")
  private String path;

  /**
   * The servlet class.
   */
  private Class<? extends HttpServlet> servletClass = null;

  /**
   * The servlet name.
   *
   * @return the servlet name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the servlet class.
   *
   * @param cl  the class loader
   *
   * @return the servlet class
   *
   * @throws ClassNotFoundException if the class can not be loaded
   */
  public Class<? extends HttpServlet> getServletClass(ClassLoader cl) throws ClassNotFoundException {
    if (servletClass == null) {
      servletClass = cl.loadClass(path).asSubclass(HttpServlet.class);
    }
    return servletClass;
  }
}
