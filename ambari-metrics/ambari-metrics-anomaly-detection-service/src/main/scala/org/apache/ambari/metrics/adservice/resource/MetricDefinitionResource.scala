/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.metrics.adservice.resource

import javax.ws.rs._
import javax.ws.rs.core.MediaType.APPLICATION_JSON
import javax.ws.rs.core.Response

import org.apache.ambari.metrics.adservice.metadata.{MetricKey, MetricSourceDefinition}
import org.apache.ambari.metrics.adservice.service.MetricDefinitionService
import org.apache.commons.lang.StringUtils

import com.google.inject.Inject

@Path("/metric-definition")
class MetricDefinitionResource {

  @Inject
  var metricDefinitionService: MetricDefinitionService = _

  @GET
  @Produces(Array(APPLICATION_JSON))
  @Path("/{name}")
  def defaultGet(@PathParam("name") definitionName: String): Response  = {

    if (StringUtils.isEmpty(definitionName)) {
      Response.ok.entity(Map("message" -> "Definition name cannot be empty. Use query parameter 'name'")).build()
    }
    val metricSourceDefinition = metricDefinitionService.getDefinitionByName(definitionName)
    if (metricSourceDefinition != null) {
      Response.ok.entity(metricSourceDefinition).build()
    } else {
      Response.ok.entity(Map("message" -> "Definition not found")).build()
    }
  }

  @GET
  @Produces(Array(APPLICATION_JSON))
  def getAllMetricDefinitions: Response  = {
    val metricSourceDefinitionMap: List[MetricSourceDefinition] = metricDefinitionService.getDefinitions
    Response.ok.entity(metricSourceDefinitionMap).build()
  }

  @GET
  @Path("/keys")
  @Produces(Array(APPLICATION_JSON))
  def getMetricKeys: Response  = {
    val metricKeyMap:  Map[String, Set[MetricKey]] = metricDefinitionService.getMetricKeys
    Response.ok.entity(metricKeyMap).build()
  }

  @POST
  @Produces(Array(APPLICATION_JSON))
  def defaultPost(definition: MetricSourceDefinition) : Response = {
    if (definition == null) {
      Response.ok.entity(Map("message" -> "Definition content cannot be empty.")).build()
    }
    val success : Boolean = metricDefinitionService.addDefinition(definition)
    if (success) {
      Response.ok.entity(Map("message" -> "Definition saved")).build()
    } else {
      Response.ok.entity(Map("message" -> "Definition could not be saved")).build()
    }
  }

  @PUT
  @Produces(Array(APPLICATION_JSON))
  def defaultPut(definition: MetricSourceDefinition) : Response = {
    if (definition == null) {
      Response.ok.entity(Map("message" -> "Definition content cannot be empty.")).build()
    }
    val success : Boolean = metricDefinitionService.updateDefinition(definition)
    if (success) {
      Response.ok.entity(Map("message" -> "Definition updated")).build()
    } else {
      Response.ok.entity(Map("message" -> "Definition could not be updated")).build()
    }
  }

  @DELETE
  @Produces(Array(APPLICATION_JSON))
  @Path("/{name}")
  def defaultDelete(@PathParam("name") definitionName: String): Response  = {

    if (StringUtils.isEmpty(definitionName)) {
      Response.ok.entity(Map("message" -> "Definition name cannot be empty. Use query parameter 'name'")).build()
    }
    val success: Boolean = metricDefinitionService.deleteDefinitionByName(definitionName)
    if (success) {
      Response.ok.entity(Map("message" -> "Definition deleted")).build()
    } else {
      Response.ok.entity(Map("message" -> "Definition could not be deleted")).build()
    }
  }
}
