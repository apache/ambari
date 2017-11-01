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

import org.apache.ambari.metrics.adservice.resource.{AnomalyResource, RootResource}
import org.apache.ambari.metrics.adservice.service.{ADQueryService, ADQueryServiceImpl}

import com.codahale.metrics.health.HealthCheck
import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder

import io.dropwizard.setup.Environment

class AnomalyDetectionAppModule(config: AnomalyDetectionAppConfig, env: Environment) extends AbstractModule {
  override def configure() {
    bind(classOf[AnomalyDetectionAppConfig]).toInstance(config)
    bind(classOf[Environment]).toInstance(env)
    val healthCheckBinder = Multibinder.newSetBinder(binder(), classOf[HealthCheck])
    healthCheckBinder.addBinding().to(classOf[DefaultHealthCheck])
    bind(classOf[AnomalyResource])
    bind(classOf[RootResource])
    bind(classOf[ADQueryService]).to(classOf[ADQueryServiceImpl])
  }
}
