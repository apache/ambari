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
package org.apache.ambari.infra;

import static java.lang.System.currentTimeMillis;

import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestUtil {
  private static final Logger logger = LogManager.getLogger(TestUtil.class);

  public static void doWithin(int sec, String actionName, BooleanSupplier predicate) {
    doWithin(sec, actionName, () -> {
      if (!predicate.getAsBoolean())
        throw new RuntimeException("Predicate was false!");
    });
  }

  public static void doWithin(int sec, String actionName, Runnable runnable) {
    long start = currentTimeMillis();
    Exception exception;
    while (true) {
      try {
        runnable.run();
        return;
      }
      catch (Exception e) {
        exception = e;
      }

      if (currentTimeMillis() - start > sec * 1000) {
        throw new AssertionError(String.format("Unable to perform action '%s' within %d seconds", actionName, sec), exception);
      }
      else {
        logger.info("Performing action '{}' failed. retrying...", actionName);
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
  }

  public static String getDockerHost() {
    return System.getProperty("docker.host") != null ? System.getProperty("docker.host") : "localhost";
  }

  public static void runCommand(String[] command) {
    try {
      logger.info("Exec command: {}", StringUtils.join(command, " "));
      Process process = Runtime.getRuntime().exec(command);
      String stdout = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
      logger.info("Exec command result {}", stdout);
    } catch (Exception e) {
      throw new RuntimeException("Error during execute shell command: ", e);
    }
  }
}
