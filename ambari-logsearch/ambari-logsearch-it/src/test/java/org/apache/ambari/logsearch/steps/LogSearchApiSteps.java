/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.common.io.Resources;
import org.apache.ambari.logsearch.domain.StoryDataRegistry;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class LogSearchApiSteps {

  private static Logger LOG = LoggerFactory.getLogger(LogSearchApiSteps.class);

  private String response;

  @When("LogSearch api query sent: <apiQuery>")
  public void sendApiQuery(@Named("apiQuery") String apiQuery) {
    JerseyClient jerseyClient = JerseyClientBuilder.createClient();
    HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basicBuilder()
      .credentials("admin", "admin")
      .build();
    jerseyClient.register(authFeature);

    String logsearchUrl = String.format("http://%s:%d%s",
      StoryDataRegistry.INSTANCE.getDockerHost(),
      StoryDataRegistry.INSTANCE.getLogsearchPort(),
      apiQuery);

    LOG.info("Url: {}", logsearchUrl);

    WebTarget target = jerseyClient.target(logsearchUrl);
    Invocation.Builder invocationBuilder =  target.request(MediaType.APPLICATION_JSON_TYPE);
    response = invocationBuilder.get().readEntity(String.class);
  }


  @Then("The api query result is <jsonResult>")
  public void verifyRestApiCall(@Named("jsonResult") String jsonResult) throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();
    Path jsonFilePath = new File(Resources.getResource("test-output/" + jsonResult).toURI()).toPath();
    String jsonExpected = new String(Files.readAllBytes(jsonFilePath));

    JsonNode expected = mapper.readTree(jsonExpected);
    JsonNode result = mapper.readTree(response);
    JsonNode patch = JsonDiff.asJson(expected, result);
    List<?> diffObjects = mapper.convertValue(patch, List.class);
    assertDiffs(diffObjects, expected);

  }

  @SuppressWarnings("unchecked")
  private void assertDiffs(List<?> diffObjects, JsonNode expected) {
    for (Object diffObj : diffObjects) {
      String path = ((Map<String, String>) diffObj).get("path");
      Assert.assertTrue(expected.at(path).isMissingNode());
    }
  }
}
