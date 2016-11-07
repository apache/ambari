/*
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package org.apache.ambari.view.hawq;


import java.util.Map;

/**
 * JsonApi resource -- represents an object in JSON-API format.
 */
class JsonApiResource {

    /**
     * The object id.
     */
    private String id;

    /**
     * The object type.
     */
    private String type;

    /**
     * The query attributes.
     */
    private Map<String, Object> attributes;

    /**
     * Get the object id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the object id.
     *
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the object type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the object type.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the object attributes.
     *
     * @return the object attributes
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Set the object attributes.
     *
     * @param attributes the object attributes
     */
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

}
