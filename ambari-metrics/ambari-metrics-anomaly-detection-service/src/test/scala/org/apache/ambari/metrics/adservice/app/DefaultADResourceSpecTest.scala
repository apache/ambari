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

import javax.ws.rs.client.Client
import javax.ws.rs.core.MediaType.APPLICATION_JSON

import org.apache.ambari.metrics.adservice.app.DropwizardAppRuleHelper.withAppRunning
import org.glassfish.jersey.client.ClientProperties.{CONNECT_TIMEOUT, READ_TIMEOUT}
import org.glassfish.jersey.client.{ClientConfig, JerseyClientBuilder}
import org.glassfish.jersey.filter.LoggingFilter
import org.glassfish.jersey.jaxb.internal.XmlJaxbElementProvider
import org.joda.time.DateTime
import org.scalatest.{FunSpec, Matchers}

import com.google.common.io.Resources

class DefaultADResourceSpecTest extends FunSpec with Matchers {

  describe("/topNAnomalies") {
    it("Must return default message") {
      withAppRunning(classOf[AnomalyDetectionApp], Resources.getResource("config.yml").getPath) { rule =>
        val json = client.target(s"http://localhost:${rule.getLocalPort}/topNAnomalies")
          .request().accept(APPLICATION_JSON).buildGet().invoke(classOf[String])
        val now = DateTime.now.toString("MM-dd-yyyy hh:mm")
        assert(json == "{\"message\":\"Anomaly Detection Service!\"," + "\"today\":\"" + now + "\"}")
      }
    }
  }

  def client: Client = {
    val config = new ClientConfig()
    config.register(classOf[LoggingFilter])
    config.register(classOf[XmlJaxbElementProvider.App])
    config.property(CONNECT_TIMEOUT, 5000)
    config.property(READ_TIMEOUT, 10000)
    JerseyClientBuilder.createClient(config)
  }
}
