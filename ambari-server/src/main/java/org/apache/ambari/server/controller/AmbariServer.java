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

package org.apache.ambari.server.controller;


import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.sun.jersey.spi.container.servlet.ServletContainer;

@Singleton
public class AmbariServer {
 private static Log LOG = LogFactory.getLog(AmbariServer.class);
 public static int CLIENT_PORT = 4080;
 private Server server = null;
 public volatile boolean running = true; // true while controller runs

 public void run() {
   server = new Server(CLIENT_PORT);

   try {
     Context root = new Context(server, "/", Context.SESSIONS);
     ServletHolder rootServlet = root.addServlet(DefaultServlet.class, "/");
     rootServlet.setInitOrder(1);

     ServletHolder sh = new ServletHolder(ServletContainer.class);
     sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
       "com.sun.jersey.api.core.PackagesResourceConfig");
     sh.setInitParameter("com.sun.jersey.config.property.packages", 
       "org.apache.ambari.server.api.rest");
     root.addServlet(sh, "/api/*");
     sh.setInitOrder(2);

     ServletHolder agent = new ServletHolder(ServletContainer.class);
     agent.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
       "com.sun.jersey.api.core.PackagesResourceConfig");
     agent.setInitParameter("com.sun.jersey.config.property.packages", 
       "org.apache.ambari.server.agent.rest");
     root.addServlet(agent, "/agent/*");
     agent.setInitOrder(3);

     server.setStopAtShutdown(true);

     /*
      * Start the server after controller state is recovered.
      */
     server.start();
     LOG.info("Started Server");
     server.join();
     LOG.info("Joined the Server");
   } catch (Exception e) {
     LOG.error("Error in the server", e);

   }
 }

 public void stop() throws Exception {
   try {
     server.stop();
   } catch (Exception e) {
     LOG.error("Error stopping the server", e);
   }
 }

 public static void main(String[] args) throws IOException {
   Injector injector = Guice.createInjector(new ControllerModule());
   try {
     LOG.info("Getting the controller");
     AmbariServer server = injector.getInstance(AmbariServer.class);
     if (server != null) {
       server.run();
     }
   } catch(Throwable t) {
     LOG.error("Failed to run the Ambari Server", t);
   }
 }
}
