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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple resource implementation.
 */
public class ResourceImpl implements Resource {

  /**
   * The resource type.
   */
  private final Type type;

  /**
   * Tree of categories/properties.
   * Each category is a sub node and each node contains a map of properties(n/v pairs).
   */
  private final TreeNode<Map<String, Object>> m_treeProperties =
      new TreeNodeImpl<Map<String, Object>>(null, new HashMap<String, Object>(), null);


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a resource of the given type.
   *
   * @param type  the resource type
   */
  public ResourceImpl(Type type) {
    this.type = type;
  }


  // ----- Resource ----------------------------------------------------------

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public TreeNode<Map<String, Object>> getProperties() {
    return m_treeProperties;
  }

  @Override
  public Map<String, Map<String, Object>> getPropertiesMap() {
    Map<String, Map<String, Object>> mapProps = new HashMap<String, Map<String, Object>>();
    addNodeToMap(m_treeProperties, mapProps, null);

    return mapProps;
  }

  @Override
  public void setProperty(String id, Object value) {
    String category = PropertyHelper.getPropertyCategory(id);
    TreeNode<Map<String, Object>> node;
    if (category == null) {
      node = m_treeProperties;
    } else {
      node = m_treeProperties.getChild(category);
      if (node == null) {
        String[] tokens = category.split("/");
        node = m_treeProperties;
        for (String t : tokens) {
          TreeNode<Map<String, Object>> child = node.getChild(t);
          if (child == null) {
            child = node.addChild(new HashMap<String, Object>(), t);
          }
          node = child;
        }
      }
    }
    node.getObject().put(PropertyHelper.getPropertyName(id), value);
  }

  @Override
  public Object getPropertyValue(String id) {
    String category = PropertyHelper.getPropertyCategory(id);
    TreeNode<Map<String, Object>> node = (category == null) ? m_treeProperties :
        m_treeProperties.getChild(category);

    return node == null ? null : node.getObject().get(PropertyHelper.getPropertyName(id));
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("Resource : ").append(type).append("\n");
    sb.append("Properties:\n");

    printPropertyNode(m_treeProperties, sb, null, "  ");

    return sb.toString();
  }


  // ----- class private methods ---------------------------------------------

  /**
   * Recursively prints the properties for a given node and it's children to a StringBuffer.
   *
   * @param node      the node to print properties for
   * @param sb        the SringBuffer to print to
   * @param category  the absolute category name
   * @param indent    the indent to be used
   */
  private void printPropertyNode(TreeNode<Map<String, Object>> node, StringBuilder sb, String category, String indent) {
    if (node.getParent() != null) {
      category = category == null ? node.getName() : category + '/' + node.getName();
      sb.append(indent).append("Category: ").append(category).append('\n');
      indent += "  ";
    }
    for (Map.Entry<String, Object> entry : node.getObject().entrySet()) {
      sb.append(indent).append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
    }

    for (TreeNode<Map<String, Object>> n : node.getChildren()) {
      printPropertyNode(n, sb, category, indent);
    }
  }

  /**
   * Add the node properties to the specified map.
   * Makes recursive calls for each child node.
   *
   * @param node      the node whose properties are to be added
   * @param mapProps  the map that the props are to be added to
   * @param path      the current category hierarchy
   */
  private void addNodeToMap(TreeNode<Map<String, Object>> node, Map<String, Map<String, Object>> mapProps, String path) {
    path = path == null ? node.getName() : path + "/" + node.getName();
    mapProps.put(path, node.getObject());

    for (TreeNode<Map<String, Object>> child : node.getChildren()) {
      addNodeToMap(child, mapProps, path);
    }
  }
}
