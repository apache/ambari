/**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package org.apache.ambari.metrics.adservice.app

import javax.ws.rs.Path
import javax.ws.rs.container.{ContainerRequestFilter, ContainerResponseFilter}

import org.apache.ambari.metrics.adservice.app.GuiceInjector.{withInjector, wrap}
import org.glassfish.jersey.filter.LoggingFilter

import com.codahale.metrics.health.HealthCheck
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.dropwizard.Application
import io.dropwizard.setup.Environment

class AnomalyDetectionApp extends Application[AnomalyDetectionAppConfig] {
  override def getName = "anomaly-detection-service"

  override def run(t: AnomalyDetectionAppConfig, env: Environment): Unit = {
    configure(t, env)
  }

  def configure(config: AnomalyDetectionAppConfig, env: Environment) {
    withInjector(new AnomalyDetectionAppModule(config, env)) { injector =>
      injector.instancesWithAnnotation(classOf[Path]).foreach { r => env.jersey().register(r) }
      injector.instancesOfType(classOf[HealthCheck]).foreach { h => env.healthChecks.register(h.getClass.getName, h) }
      injector.instancesOfType(classOf[ContainerRequestFilter]).foreach { f => env.jersey().register(f) }
      injector.instancesOfType(classOf[ContainerResponseFilter]).foreach { f => env.jersey().register(f) }
    }
    env.jersey.register(jacksonJaxbJsonProvider)
    env.jersey.register(new LoggingFilter)
  }

  private def jacksonJaxbJsonProvider: JacksonJaxbJsonProvider = {
    val provider = new JacksonJaxbJsonProvider()
    val objectMapper = new ObjectMapper()
    objectMapper.registerModule(DefaultScalaModule)
    objectMapper.registerModule(new JodaModule)
    objectMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false)
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    objectMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true)
    provider.setMapper(objectMapper)
    provider
  }

  override def bootstrapLogging(): Unit = {}
}


object AnomalyDetectionApp {
  def main(args: Array[String]): Unit = new AnomalyDetectionApp().run(args: _*)
}
