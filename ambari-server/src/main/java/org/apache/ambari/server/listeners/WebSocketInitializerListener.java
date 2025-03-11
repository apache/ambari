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
package org.apache.ambari.server.listeners;

import java.util.Collections;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketInitializerListener implements ServletContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketInitializerListener.class);
    private final JettyWebSocketServletContainerInitializer initializer;

    public WebSocketInitializerListener(JettyWebSocketServletContainerInitializer initializer) {
        this.initializer = initializer;
    }
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        try {
            initializer.onStartup(Collections.emptySet(), servletContext);
            LOG.info("WebSocket container initialized");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        try {
            JettyWebSocketServerContainer container = (JettyWebSocketServerContainer) servletContext.getAttribute(JettyWebSocketServerContainer.class.getName());

            if (container != null) {
                container.stop();
                LOG.info("WebSocket container stopped.");
            } else {
                LOG.info("No WebSocket container found during shutdown.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
