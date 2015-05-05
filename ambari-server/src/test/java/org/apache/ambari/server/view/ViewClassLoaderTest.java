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

import junit.framework.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * ViewClassLoader test.
 */
public class ViewClassLoaderTest {

  @Test
  public void testGetResource() throws Exception {
    ClassLoader parentClassLoader = createMock(ClassLoader.class);
    URL parentResource = new File("parent-resource").toURI().toURL();

    expect(parentClassLoader.getResource("parent-resource")).andReturn(parentResource).once();

    replay(parentClassLoader);

    File file = new File("./src/test/resources");
    URL testURL = file.toURI().toURL();

    URL[] urls = new URL[]{testURL};

    ViewClassLoader classLoader = new ViewClassLoader(parentClassLoader, urls);

    URL url = classLoader.getResource("ambari.properties");

    Assert.assertNotNull(url);

    url = classLoader.getResource("parent-resource");

    Assert.assertNotNull(url);
    Assert.assertSame(parentResource, url);

    verify(parentClassLoader);
  }

  @Test
  public void testLoadClass() throws Exception {
    TestClassLoader parentClassLoader = createMock(TestClassLoader.class);
    Class parentClass = Object.class;

    expect(parentClassLoader.getPackage("org.apache.ambari.server.view")).andReturn(null).anyTimes();
    expect(parentClassLoader.loadClass("java.lang.Object")).andReturn(parentClass).anyTimes();
    expect(parentClassLoader.loadClass("ParentClass")).andReturn(parentClass).once();
    expect(parentClassLoader.loadClass("javax.xml.parsers.SAXParserFactory")).andReturn(parentClass).once();

    replay(parentClassLoader);

    File file = new File("./target/test-classes");
    URL testURL = file.toURI().toURL();

    URL[] urls = new URL[]{testURL};

    ViewClassLoader classLoader = new ViewClassLoader(parentClassLoader, urls);

    Class clazz = classLoader.loadClass("org.apache.ambari.server.view.ViewClassLoaderTest");

    Assert.assertNotNull(clazz);

    clazz = classLoader.loadClass("ParentClass");

    Assert.assertNotNull(clazz);
    Assert.assertSame(parentClass, clazz);

    clazz = classLoader.loadClass("javax.xml.parsers.SAXParserFactory");

    Assert.assertNotNull(clazz);
    Assert.assertSame(parentClass, clazz);

    verify(parentClassLoader);
  }

  public class TestClassLoader extends ClassLoader {
    @Override
    public Package getPackage(String s) {
      return super.getPackage(s);
    }
  }
}
