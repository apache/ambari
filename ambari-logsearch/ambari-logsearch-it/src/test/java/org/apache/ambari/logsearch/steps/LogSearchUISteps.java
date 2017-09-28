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

import junit.framework.Assert;
import org.apache.ambari.logsearch.domain.StoryDataRegistry;
import org.apache.ambari.logsearch.web.Home;
import org.jbehave.core.annotations.AfterScenario;
import org.jbehave.core.annotations.AfterStory;
import org.jbehave.core.annotations.BeforeScenario;
import org.jbehave.core.annotations.BeforeStories;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.jbehave.web.selenium.WebDriverProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class LogSearchUISteps extends AbstractLogSearchSteps {

  private static final Logger LOG = LoggerFactory.getLogger(LogSearchUISteps.class);

  private final WebDriverProvider driverProvider;

  private Home home;

  public LogSearchUISteps(WebDriverProvider driverProvider) {
    this.driverProvider = driverProvider;
  }

  @BeforeScenario
  public void initHomePage() {
    home = new Home(driverProvider);
    LOG.info("Init home page: {}", home.getCurrentUrl());
  }

  @AfterScenario
  public void deleteCookies() {
    LOG.info("Delete all cookies...");
    home.manage().deleteAllCookies();
  }

  @BeforeStories
  public void beforeStories() throws Exception {
    initDockerContainer();
    LOG.info("Initialize web driver...");
    StoryDataRegistry.INSTANCE.getWebDriverProvider().initialize();
    LOG.info("Web driver details: {}",  StoryDataRegistry.INSTANCE.getWebDriverProvider().get().toString());
  }

  @AfterStory
  public void closePage() throws Exception {
    LOG.info("Closing web driver");
    StoryDataRegistry.INSTANCE.getWebDriverProvider().end();
  }

  @Given("open logsearch home page")
  public void initBrowser() {
    LOG.info("Delete all cookies...");
    home.manage().deleteAllCookies();
    LOG.info("Open home page: {}", home.getCurrentUrl());
    home.open();
  }

  @When("login with $username / $password")
  public void login(@Named("username") String userName, @Named("password") String password) {
    LOG.info("Type username: {}", userName);
    home.findElement(By.id("username")).sendKeys(userName);
    LOG.info("Type password: {}", password);
    home.findElement(By.id("password")).sendKeys(password);
    LOG.info("Click on Sign In button.");
    home.findElement(By.cssSelector("login-form > div > form > button")).click();
    closeTourPopup();
  }

  @Then("page contains text: '$text'")
  public void contains(@Named("text") String text) {
    LOG.info("Check page contains text: '{}'", text);
    home.found(text);
  }

  @Then("page does not contain text: '$text'")
  public void notContains(@Named("text") String text) {
    LOG.info("Check page does not contain text: '{}'", text);
    home.notFound(text);
  }

  @When("wait $seconds seconds")
  public void waitSeconds(@Named("second") String second) {
    LOG.info("Wait {} seconds...", second);
    home.manage().timeouts().implicitlyWait(Integer.parseInt(second), TimeUnit.SECONDS);
  }

  @When("click on element: $xpath (xpath)")
  public void clickOnElementByXPath(@Named("xpath") String xPath) {
    LOG.info("Click on element by xpath: '{}'", xPath);
    driverProvider.get().findElement(By.xpath(xPath)).click();
  }

  @When("click on element: $id (id)")
  public void clickOnElementById(@Named("id") String id) {
    LOG.info("Click on element by id: '{}'", id);
    driverProvider.get().findElement(By.xpath(id)).click();
  }

  @When("click on element: $css (css selector)")
  public void clickOnElementByCssSelector(@Named("css") String cssSelector) {
    LOG.info("Click on element by css selector: '{}'", cssSelector);
    driverProvider.get().findElement(By.cssSelector(cssSelector)).click();
  }

  @Then("element exists with xpath: $xpath")
  public void findByXPath(@Named("xpath") String xPath) {
    LOG.info("Find element by xpath: '{}'", xPath);
    Assert.assertNotNull(home.findElement(By.xpath(xPath)));
  }

  @Then("element exists with xpath: $id")
  public void findById(@Named("id") String id) {
    LOG.info("Find element by id: '{}'", id);
    Assert.assertNotNull(home.findElement(By.id(id)));
  }

  @Then("element exists with css selector: $css")
  public void findByCssSelector(@Named("css") String cssSelector) {
    LOG.info("Find element by css selector: '{}'", cssSelector);
    Assert.assertNotNull(home.findElement(By.cssSelector(cssSelector)));
  }

  @Then("element text equals '$text', with xpath $xpath")
  public void equalsByXPath(@Named("text") String text, @Named("xpath") String xPath) {
    LOG.info("Check text of the element (xpath: '{}') equals with '{}'", xPath, text);
    Assert.assertEquals(text, home.findElement(By.xpath(xPath)).getText());
  }

  @Then("element text equals '$text' with id $id")
  public void equalsyId(@Named("text") String text, @Named("id") String id) {
    LOG.info("Check text of the element (id: '{}') equals with '{}'", id, text);
    Assert.assertEquals(text, home.findElement(By.id(id)).getText());
  }

  @Then("element text equals '$text' with css selector $css")
  public void equalsCssSelector(@Named("text") String text, @Named("css") String cssSelector) {
    LOG.info("Check text of the element (css selector: '{}') equals with '{}'", cssSelector, text);
    Assert.assertEquals(text, home.findElement(By.cssSelector(cssSelector)).getText());
  }

  @Then("element does not exist with xpath: $xpath")
  public void doNotFindByXPath(@Named("xpath") String xPath) {
    try {
      LOG.info("Check that element does not exist with xpath: {}", xPath);
      home.findElement(By.xpath(xPath));
      Assert.fail(String.format("Element is found. xPath: '%s'", xPath));
    } catch (NoSuchElementException e) {
      // success
    }
  }

  @Then("element does not exist with xpath: $id")
  public void doNotFindById(@Named("id") String id) {
    try {
      LOG.info("Check that element does not exist with id: {}", id);
      home.findElement(By.xpath(id));
      Assert.fail(String.format("Element is found. id: '%s'", id));
    } catch (NoSuchElementException e) {
      // success
    }
  }

  @Then("element does not exist with css selector: $css")
  public void doNotFindByCssSelector(@Named("css") String cssSelector) {
    try {
      LOG.info("Check that element does not exist with css selector: {}", cssSelector);
      home.findElement(By.xpath(cssSelector));
      Assert.fail(String.format("Element is found. css selector: '%s'", cssSelector));
    } catch (NoSuchElementException e) {
      // success
    }
  }

  private void closeTourPopup() {
    LOG.info("Close Tour popup if needed.");
    try {
      home.findElement(By.cssSelector("div.modal-footer > button.btn.btn-default")).click();
    } catch (NoSuchElementException ex) {
      // do nothing - no popup
    }
  }
}
