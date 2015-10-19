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

package org.apache.ambari.server.stack;

import org.apache.ambari.server.state.stack.ConfigUpgradePack;
import org.apache.ambari.server.state.stack.ConfigurationXml;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.apache.ambari.server.state.stack.StackMetainfoXml;
import org.apache.ambari.server.state.stack.UpgradePack;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides functionality to unmarshal stack definition files to their
 * corresponding object representations.
 */
class ModuleFileUnmarshaller {
  /**
   * Map of class to JAXB context
   */
  private static final Map<Class<?>, JAXBContext> jaxbContexts = new HashMap<Class<?>, JAXBContext>();

  /**
   * Unmarshal a file to it's corresponding object type.
   *
   * @param clz   class of the object representation
   * @param file  file to unmarshal
   *
   * @return object representation of the specified file
   * @throws JAXBException if unable to unmarshal the file
   */
  public <T> T unmarshal(Class<T> clz, File file) throws JAXBException {
    Unmarshaller u = jaxbContexts.get(clz).createUnmarshaller();

    return clz.cast(u.unmarshal(file));
  }

  /**
   * statically register the JAXB contexts
   */
  static {
    try {
      // three classes define the top-level element "metainfo", so we need 3 contexts.
      JAXBContext ctx = JAXBContext.newInstance(StackMetainfoXml.class, RepositoryXml.class,
          ConfigurationXml.class, UpgradePack.class, ConfigUpgradePack.class);

      jaxbContexts.put(StackMetainfoXml.class, ctx);
      jaxbContexts.put(RepositoryXml.class, ctx);
      jaxbContexts.put(ConfigurationXml.class, ctx);
      jaxbContexts.put(UpgradePack.class, ctx);
      jaxbContexts.put(ConfigUpgradePack.class, ctx);
      jaxbContexts.put(ServiceMetainfoXml.class, JAXBContext.newInstance(ServiceMetainfoXml.class));
    } catch (JAXBException e) {
      throw new RuntimeException (e);
    }
  }
}
