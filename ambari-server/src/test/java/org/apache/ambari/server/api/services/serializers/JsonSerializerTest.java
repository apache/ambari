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

package org.apache.ambari.server.api.services.serializers;

import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import javax.ws.rs.core.UriInfo;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

/**
 * JSONSerializer unit tests
 */
public class JsonSerializerTest {
  @Test
  public void testSerialize() throws Exception {
    UriInfo uriInfo = createMock(UriInfo.class);
    Resource resource = createMock(Resource.class);
    //Resource resource2 = createMock(Resource.class);

    Result result = new ResultImpl(true);
    result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
    TreeNode<Resource> tree = result.getResultTree();
    //tree.setName("items");
    TreeNode<Resource> child = tree.addChild(resource, "resource1");
    //child.addChild(resource2, "sub-resource");

    // resource properties
    HashMap<String, Object> mapRootProps = new HashMap<String, Object>();
    mapRootProps.put("prop1", "value1");
    mapRootProps.put("prop2", "value2");

    HashMap<String, Object> mapCategoryProps = new HashMap<String, Object>();
    mapCategoryProps.put("catProp1", "catValue1");
    mapCategoryProps.put("catProp2", "catValue2");

    TreeNode<Map<String, Object>> treeProps1 = new TreeNodeImpl<Map<String, Object>>(
        null, mapRootProps, null);

    treeProps1.addChild(mapCategoryProps, "category");

//    // resource2 properties
//    HashMap<String, Object> map2RootProps = new HashMap<String, Object>();
//    map2RootProps.put("2prop1", "2value1");
//
//    HashMap<String, Object> map2CategoryProps = new HashMap<String, Object>();
//    map2CategoryProps.put("2catProp1", "2catValue1");
//
//    TreeNode<Map<String, Object>> treeProps2 = new TreeNodeImpl<Map<String, Object>>(
//        null, map2RootProps, null);
//
//    treeProps2.addChild(mapCategoryProps, "little-category");


    //expectations
    expect(resource.getProperties()).andReturn(treeProps1).anyTimes();
    expect(resource.getType()).andReturn(Resource.Type.Cluster).anyTimes();

//    expect(resource2.getProperties()).andReturn(treeProps2).anyTimes();
//    expect(resource2.getType()).andReturn(Resource.Type.Service).anyTimes();

    replay(uriInfo, resource/*, resource2*/);

    //execute test
    Object o = new JsonSerializer().serialize(result);

    String expected = "{\n" +
        "  \"prop2\" : \"value2\",\n" +
        "  \"prop1\" : \"value1\",\n" +
        "  \"category\" : {\n" +
        "    \"catProp1\" : \"catValue1\",\n" +
        "    \"catProp2\" : \"catValue2\"\n" +
        "  }\n" +
        "}";

    assertEquals(expected, o);

    verify(uriInfo, resource/*, resource2*/);
  }
}
