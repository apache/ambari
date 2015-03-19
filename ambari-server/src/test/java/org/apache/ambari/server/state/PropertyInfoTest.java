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
package org.apache.ambari.server.state;

import org.apache.ambari.server.state.stack.StackMetainfoXml;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class PropertyInfoTest {

  @Test
  public void testProperty() {
    PropertyInfo property = new PropertyInfo();
    property.setName("name");
    property.setValue("value");
    property.setDescription("desc");
    property.setFilename("filename");
    PropertyDependencyInfo pdi = new PropertyDependencyInfo("type", "name");
    property.getDependsOnProperties().add(pdi);

    assertEquals("name", property.getName());
    assertEquals("value", property.getValue());
    assertEquals("desc", property.getDescription());
    assertEquals("filename", property.getFilename());

    assertEquals(1, property.getDependsOnProperties().size());
    assertTrue(property.getDependsOnProperties().contains(pdi));
  }

  @Test
  public void testAttributes() throws Exception {
    PropertyInfo property = new PropertyInfo();

    List<Element> elements = new ArrayList<Element>();
    Element e1 = createNiceMock(Element.class);
    Element e2 = createNiceMock(Element.class);
    Node n1 = createNiceMock(Node.class);
    Node n2 = createNiceMock(Node.class);

    elements.add(e1);
    elements.add(e2);

    // set mock expectations
    expect(e1.getTagName()).andReturn("foo").anyTimes();
    expect(e1.getFirstChild()).andReturn(n1).anyTimes();
    expect(n1.getNodeValue()).andReturn("value1").anyTimes();

    expect(e2.getTagName()).andReturn("bar").anyTimes();
    expect(e2.getFirstChild()).andReturn(n2).anyTimes();
    expect(n2.getNodeValue()).andReturn("value2").anyTimes();

    replay(e1, e2, n1, n2);

    // set attributes
    Field f = property.getClass().getDeclaredField("propertyAttributes");
    f.setAccessible(true);
    f.set(property, elements);

    Map<String, String> attributes = property.getAttributesMap();
    assertEquals(2, attributes.size());
    assertEquals("value1", attributes.get("foo"));
    assertEquals("value2", attributes.get("bar"));
  }
}