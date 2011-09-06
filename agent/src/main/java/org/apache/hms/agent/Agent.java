/*
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

package org.apache.hms.agent;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.common.util.ExceptionUtil;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.xml.XmlConfiguration;

import com.sun.jersey.spi.container.servlet.ServletContainer;

public class Agent {
  private static Log log = LogFactory.getLog(Agent.class);
  
  private static Agent instance = null;
  private Server server = null;
  private static URL serverConf = null;

  public static Agent getInstance() {
    if(instance==null) {
      instance = new Agent();
    }
    return instance;
  }

  public void start() {
    try {
      System.out.close();
      System.err.close();
      instance = this;
      run();
    } catch(Exception e) {
      log.error(ExceptionUtil.getStackTrace(e));
      System.exit(-1);
    }
  }

  public void run() {
    server = new Server(4080);

    XmlConfiguration configuration;
    try {
      Context root = new Context(server, "/", Context.SESSIONS);
      ServletHolder sh = new ServletHolder(ServletContainer.class);
      sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
      sh.setInitParameter("com.sun.jersey.config.property.packages", "org.apache.hms.agent.rest");
      root.addServlet(sh, "/*");
      server.setStopAtShutdown(true);
      server.start();
    } catch (Exception e) {
      log.error(ExceptionUtil.getStackTrace(e));
    }
  }

  public void stop() throws Exception {
    try {
      server.stop();
    } catch (Exception e) {
      log.error(ExceptionUtil.getStackTrace(e));
    }
  }

  public static void main(String[] args) {
    Agent agent = Agent.getInstance();
    agent.start();
  }

}
