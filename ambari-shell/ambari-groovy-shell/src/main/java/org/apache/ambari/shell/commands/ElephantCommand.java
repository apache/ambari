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
package org.apache.ambari.shell.commands;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.stereotype.Component;

/**
 * Draws an elephant to the console.
 */
@Component
public class ElephantCommand implements CommandMarker {

  /**
   * Checks whether the hello command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("hello")
  public boolean isCommandAvailable() {
    return true;
  }

  /**
   * Prints an elephant to the console.
   *
   * @return elephant
   */
  @CliCommand(value = "hello", help = "Prints a simple elephant to the console")
  public String elephant() throws IOException {
    String res = null;
    InputStream is = null;
    try{
    is = getClass().getResourceAsStream("/elephant.txt");
    res = IOUtils.toString(is);
    } finally {
      if (is != null) {
        is.close();
      }
    }
    return res;
  }
}