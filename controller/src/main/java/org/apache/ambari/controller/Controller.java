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

package org.apache.ambari.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.ambari.common.util.DaemonWatcher;
import org.apache.ambari.common.util.ExceptionUtil;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;
import org.mortbay.resource.ResourceCollection;

import com.sun.jersey.spi.container.servlet.ServletContainer;

public class Controller {
  private static Log LOG = LogFactory.getLog(Controller.class);
  public static int CONTROLLER_PORT = 4080;
  private static Controller instance = new Controller();
  private Server server = null;
  public volatile boolean running = true; // true while controller runs
  private HeartbeatHandler hbHandler;
  
  public static Controller getInstance() {
    return instance;
  }
  
  public HeartbeatHandler getHeartbeatHandler() {
    return hbHandler;
  }
  
  public void run() {
    hbHandler = new HeartbeatHandler();      
    server = new Server(CONTROLLER_PORT);

    try {
      Context root = new Context(server, "/", Context.SESSIONS);
      String AMBARI_HOME = System.getenv("AMBARI_HOME");
      root.setBaseResource(new ResourceCollection(new Resource[]
        {
          Resource.newResource(AMBARI_HOME+"/webapps/")
        }));
      ServletHolder rootServlet = root.addServlet(DefaultServlet.class, "/");
      rootServlet.setInitOrder(1);
      
      ServletHolder sh = new ServletHolder(ServletContainer.class);
      sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", 
        "com.sun.jersey.api.core.PackagesResourceConfig");
      sh.setInitParameter("com.sun.jersey.config.property.packages", 
        "org.apache.ambari.controller.rest.resources");
      sh.setInitParameter("com.sun.jersey.config.property.WadlGeneratorConfig", 
        "org.apache.ambari.controller.rest.config.ExtendedWadlGeneratorConfig");
      root.addServlet(sh, "/rest/*");
      sh.setInitOrder(2);

      ServletHolder agent = new ServletHolder(ServletContainer.class);
      agent.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", 
        "com.sun.jersey.api.core.PackagesResourceConfig");
      agent.setInitParameter("com.sun.jersey.config.property.packages", 
        "org.apache.ambari.controller.rest.agent");
      agent.setInitParameter("com.sun.jersey.config.property.WadlGeneratorConfig", 
        "org.apache.ambari.controller.rest.config.PrivateWadlGeneratorConfig");
      root.addServlet(agent, "/agent/*");
      agent.setInitOrder(3);

      Constraint constraint = new Constraint();
      constraint.setName(Constraint.__BASIC_AUTH);;
      constraint.setRoles(new String[]{"user","admin","moderator"});
      constraint.setAuthenticate(true);
       
      ConstraintMapping cm = new ConstraintMapping();
      cm.setConstraint(constraint);
      cm.setPathSpec("/agent/*");
      
      SecurityHandler security = new SecurityHandler();
      security.setUserRealm(new HashUserRealm("Controller",
          System.getenv("AMBARI_CONF_DIR")+"/auth.conf"));
      security.setConstraintMappings(new ConstraintMapping[]{cm});

      root.addHandler(security);
      server.setStopAtShutdown(true);
      server.start();
    } catch (Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
    }
  }
  
  public void stop() throws Exception {
    try {
      server.stop();
    } catch (Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
    }
  }

  public static void main(String[] args) {
    DaemonWatcher.createInstance(System.getProperty("PID"), 9100);
    try {
      Controller controller = Controller.getInstance();
      if (controller != null) {
        controller.run();
      }
    } catch(Throwable t) {
      DaemonWatcher.bailout(1);
    }
  }
}
