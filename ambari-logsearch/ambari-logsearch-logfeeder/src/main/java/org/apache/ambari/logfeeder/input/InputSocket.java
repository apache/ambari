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
package org.apache.ambari.logfeeder.input;

import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.plugin.input.Input;
import org.apache.ambari.logsearch.appender.LogsearchConversion;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputSocketDescriptor;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.spi.LoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class InputSocket extends Input<LogFeederProps, InputSocketMarker, InputSocketDescriptor> {

  private static final Logger LOG = LoggerFactory.getLogger(InputSocket.class);

  private ServerSocket serverSocket;
  private Thread thread;
  private int port;
  private String protocol;
  private boolean secure;
  private boolean log4j;

  @Override
  public void init(LogFeederProps logFeederProperties) throws Exception {
    super.init(logFeederProperties);
    port = (int) ObjectUtils.defaultIfNull(getInputDescriptor().getPort(), 0);
    if (port == 0) {
      throw new IllegalArgumentException(String.format("Port needs to be set for socket input (type: %s)", getInputDescriptor().getType()));
    }

    protocol = (String) ObjectUtils.defaultIfNull(getInputDescriptor().getProtocol(), "tcp");
    secure = (boolean) ObjectUtils.defaultIfNull(getInputDescriptor().isSecure(), false);
    log4j = (boolean) ObjectUtils.defaultIfNull(getInputDescriptor().isLog4j(), false);
  }

  @Override
  public boolean monitor() {
    if (isReady()) {
      LOG.info("Start monitoring socket thread...");
      thread = new Thread(this, getNameForThread());
      thread.start();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void start() throws Exception {
    LOG.info("Starting socket server (port: {}, protocol: {}, secure: {})", port, protocol, secure);
    ServerSocketFactory socketFactory = secure ? SSLServerSocketFactory.getDefault() : ServerSocketFactory.getDefault();
    InputSocketMarker inputSocketMarker = new InputSocketMarker(this, port, protocol, secure, log4j);
    LogsearchConversion loggerConverter = new LogsearchConversion();

    try {
      serverSocket = socketFactory.createServerSocket(port);
      while (!isDrain()) {
        Socket socket = serverSocket.accept();
        if (log4j) {
          try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()))) {
            LoggingEvent loggingEvent = (LoggingEvent) ois.readObject();
            String jsonStr = loggerConverter.createOutput(loggingEvent);
            LOG.trace("Incoming socket logging event: " + jsonStr);
            outputLine(jsonStr, inputSocketMarker);
          }
        } else {
          try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
            String line = in.readLine();
            LOG.trace("Incoming socket message: " + line);
            outputLine(line, inputSocketMarker);
          }
        }
      }
    } catch (SocketException socketEx) {
      LOG.warn("{}", socketEx.getMessage());
    } finally {
      serverSocket.close();
    }
  }

  @Override
  public void setDrain(boolean drain) {
    super.setDrain(drain);
    LOG.info("Stopping socket input: {}", getShortDescription());
    try {
      serverSocket.close();
      setClosed(true);
    } catch (Exception e) {
      LOG.error("Error during closing socket input.", e);
    }
  }

  @Override
  public String getNameForThread() {
    return String.format("socket=%s-%s-%s", getLogType(), this.protocol, this.port);
  }

  @Override
  public String getShortDescription() {
    return String.format("%s - (port: %d, protocol: %s)", getLogType(), port, protocol);
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public InputSocketMarker getInputMarker() {
    return null;
  }

  @Override
  public void setReady(boolean isReady) {
  }

  @Override
  public void checkIn(InputSocketMarker inputMarker) {
  }

  @Override
  public void lastCheckIn() {
  }

  @Override
  public String getReadBytesMetricName() {
    return null;
  }

  @Override
  public String getStatMetricName() {
    return null;
  }

  @Override
  public boolean logConfigs() {
    return false;
  }
}
