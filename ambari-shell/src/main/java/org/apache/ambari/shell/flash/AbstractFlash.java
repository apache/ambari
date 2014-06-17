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
package org.apache.ambari.shell.flash;

import static java.lang.Thread.sleep;

import java.util.logging.Level;

import org.springframework.shell.core.JLineShellComponent;

/**
 * Base class for showing flash messages.
 */
public abstract class AbstractFlash implements Runnable {

  private static final int SLEEP_TIME = 1500;
  private volatile boolean stop;
  private FlashType flashType;
  private JLineShellComponent shell;

  protected AbstractFlash(JLineShellComponent shell, FlashType flashType) {
    this.shell = shell;
    this.flashType = flashType;
  }

  @Override
  public void run() {
    while (!stop) {
      String text = null;
      try {
        text = getText();
        if (text.isEmpty()) {
          stop = true;
        }
        sleep(SLEEP_TIME);
      } catch (Exception e) {
        // ignore
      } finally {
        shell.flash(Level.SEVERE, text == null ? "" : text, flashType.getName());
      }
    }
  }

  /**
   * Returns the actual text of the flash messages. To remove the flash
   * return an empty string.
   *
   * @return message
   */
  public abstract String getText();
}
