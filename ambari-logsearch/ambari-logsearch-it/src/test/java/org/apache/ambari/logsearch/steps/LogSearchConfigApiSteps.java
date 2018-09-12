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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputConfig;
import org.apache.ambari.logsearch.config.json.model.inputconfig.impl.InputConfigGson;
import org.apache.ambari.logsearch.config.json.model.inputconfig.impl.InputConfigImpl;
import org.apache.ambari.logsearch.domain.StoryDataRegistry;
import org.hamcrest.Matchers;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import com.google.gson.Gson;

public class LogSearchConfigApiSteps {
  private String response;
  private InputConfig inputConfig;

  @When("LogSearch api request sent: $url")
  public String sendApiRequest(String url) {
    response = StoryDataRegistry.INSTANCE.logsearchClient().get(url);
    return response;
  }

  @When("Update input config of $inputConfigType path to $logFilePath at $url")
  public void changeAndPut(String inputConfigType, String logFilePath, String url) {
    String putRequest = response.replace(inputConfig.getInput().get(0).getPath(), logFilePath);
    String putResponse = StoryDataRegistry.INSTANCE.logsearchClient().put(
            url, putRequest);
    assertThat(putResponse, is(""));

    String getResponse = sendApiRequest(url);
    checkInputConfig(getResponse, inputConfigType, logFilePath);
  }

  @When("Update input config with data $jsonString at $url")
  public void updateWithInvalidJson(String jsonString, String url) {
    response = StoryDataRegistry.INSTANCE.logsearchClient().put(url, jsonString);
  }

  @Then("Result is an input.config of $inputConfigType with log file path $logFilePath")
  public void checkInputConfig(String inputConfigType, String logFilePath) {
    checkInputConfig(response, inputConfigType, logFilePath);
  }

  public void checkInputConfig(String result, String type, String path) {
    inputConfig = InputConfigGson.gson.fromJson(response, InputConfigImpl.class);
    assertThat(inputConfig.getInput(), is(not(Matchers.nullValue())));
    assertThat(inputConfig.getInput(), hasSize(1));
    assertThat(inputConfig.getInput().get(0).getType(), is(type));
    assertThat(inputConfig.getInput().get(0).getPath(), is(path));
  }

  @Then("Result is status code $statusCode")
  public void checkStatus(int statusCode) {
    System.out.println("************" + response);
    StatusMessage statusMessage = new Gson().fromJson(response, StatusMessage.class);
    assertThat(statusMessage.getStatusCode(), is(statusCode));
  }
}
