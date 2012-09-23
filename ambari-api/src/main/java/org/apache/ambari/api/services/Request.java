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

package org.apache.ambari.api.services;

import org.apache.ambari.api.resource.ResourceDefinition;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public interface Request {

  public enum RequestType {
    GET,
    PUT,
    POST,
    DELETE
  }

  public enum ResponseType {JSON}

  public ResourceDefinition getResource();

  public URI getURI();

  public RequestType getRequestType();

  public int getAPIVersion();

  public Map<String, List<String>> getQueryParameters();

  public Map<String, List<String>> getQueryPredicates();

  public Set<String> getPartialResponseFields();

  public Set<String> getExpandEntities();

  public Map<String, List<String>> getHeaders();

  public String getBody();

  public Serializer getSerializer();

  //todo: temporal information.  For now always specify in PR for each field.  Could use *[...] ?
  //public Map<String, TemporalData> getTemporalFields();
}
