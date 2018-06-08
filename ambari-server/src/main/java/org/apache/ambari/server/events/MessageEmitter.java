/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.events;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotRegisteredException;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.agent.stomp.dto.AckReport;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Is used to define a strategy for emitting message to subscribers.
 */
public abstract class MessageEmitter {
  private final static Logger LOG = LoggerFactory.getLogger(MessageEmitter.class);

  protected final AgentSessionManager agentSessionManager;
  protected final SimpMessagingTemplate simpMessagingTemplate;
  private AmbariEventPublisher ambariEventPublisher;

  protected final ScheduledExecutorService emittersExecutor = Executors.newScheduledThreadPool(10,
      new ThreadFactoryBuilder().setNameFormat("agent-message-emitter-%d").build());
  protected final ExecutorService monitorsExecutor = Executors.newFixedThreadPool(10,
      new ThreadFactoryBuilder().setNameFormat("ambari-message-monitor-%d").build());

  protected static final AtomicLong MESSAGE_ID = new AtomicLong(0);
  protected ConcurrentHashMap<Long, ScheduledFuture> unconfirmedMessages = new ConcurrentHashMap<>();
  protected ConcurrentHashMap<Long, BlockingQueue<ExecutionCommandEvent>> messagesToEmit = new ConcurrentHashMap<>();

  // is used to cancel agent queue check on unregistering
  protected ConcurrentHashMap<Long, Future> monitors = new ConcurrentHashMap<>();

  public final int retryCount;
  public final int retryInterval;

  public MessageEmitter(AgentSessionManager agentSessionManager, SimpMessagingTemplate simpMessagingTemplate,
                        AmbariEventPublisher ambariEventPublisher, int retryCount, int retryInterval) {
    this.agentSessionManager = agentSessionManager;
    this.simpMessagingTemplate = simpMessagingTemplate;
    this.ambariEventPublisher = ambariEventPublisher;
    this.retryCount = retryCount;
    this.retryInterval = retryInterval;
    ambariEventPublisher.register(this);
  }

  /**
   * Determines destinations and emits message.
   * @param event message should to be emitted.
   * @throws AmbariException
   */
  abstract void emitMessage(STOMPEvent event) throws AmbariException, InterruptedException;

  public void emitMessageRetriable(ExecutionCommandEvent event) throws AmbariException, InterruptedException {
    // set message identifier used to recognize NACK/ACK agent response
    event.setMessageId(MESSAGE_ID.getAndIncrement());

    Long hostId = event.getHostId();
    if (!messagesToEmit.containsKey(hostId)) {
      LOG.error("Trying to emit message to unregistered host with id {}", hostId);
      return;
    }
    messagesToEmit.get(hostId).add(event);
  }

  private class MessagesToEmitMonitor implements Runnable {

    private final Long hostId;

    public MessagesToEmitMonitor(Long hostId) {
      this.hostId = hostId;
    }

    @Override
    public void run() {
      while (true) {
        try {
          ExecutionCommandEvent event = messagesToEmit.get(hostId).take();
          EmitMessageTask emitMessageTask = new EmitMessageTask(event);
          ScheduledFuture scheduledFuture =
              emittersExecutor.scheduleAtFixedRate(emitMessageTask,
                  0, retryInterval, TimeUnit.SECONDS);
          emitMessageTask.setScheduledFuture(scheduledFuture);
          unconfirmedMessages.put(event.getMessageId(), scheduledFuture);

          scheduledFuture.get();
        } catch (InterruptedException e) {
          // can be interrupted when no responses were received from agent and HEARTBEAT_LOST will be fired
          return;
        } catch (CancellationException e) {
          // scheduled tasks can be canceled
        } catch (ExecutionException e) {
          LOG.error("Error during preparing command to emit", e);
          // generate delivery failed event
          ambariEventPublisher.publish(new MessageNotDelivered(hostId));
          return;
        }
      }
    }
  }

  public void processReceiveReport(AckReport ackReport) {
    Long messageId = ackReport.getMessageId();
    if (AckReport.AckStatus.OK.equals(ackReport.getStatus())) {
      if (unconfirmedMessages.containsKey(messageId)) {
        unconfirmedMessages.get(messageId).cancel(true);
        unconfirmedMessages.remove(messageId);
      } else {
        LOG.warn("OK agent report was received again for already complete command with message id {}", messageId);
      }
    } else {
      LOG.error("Received {} agent report for execution command with messageId {} with following reason: {}",
          ackReport.getStatus(), messageId, ackReport.getReason());
    }
  }

  private class EmitMessageTask implements Runnable {

    private final ExecutionCommandEvent executionCommandEvent;
    private ScheduledFuture scheduledFuture;
    private int retry_counter = 0;

    public EmitMessageTask(ExecutionCommandEvent executionCommandEvent) {
      this.executionCommandEvent = executionCommandEvent;
    }

    public void setScheduledFuture(ScheduledFuture scheduledFuture) {
      this.scheduledFuture = scheduledFuture;
    }

    @Override
    public void run() {
      if (retry_counter >= retryCount) {
        // generate delivery failed event and cancel emitter
        ambariEventPublisher.publish(new MessageNotDelivered(executionCommandEvent.getHostId()));
        unconfirmedMessages.remove(executionCommandEvent.getMessageId()); //?

        // remove commands queue for host
        messagesToEmit.remove(executionCommandEvent.getHostId());

        // cancel retrying to emit command
        scheduledFuture.cancel(true);

        // cancel checking for new commands for host
        monitors.get(executionCommandEvent.getHostId()).cancel(true);
        return;
      }
      try {
        retry_counter++;
        emitExecutionCommandToHost(executionCommandEvent);
      } catch (AmbariException e) {
        LOG.error("Error during emitting execution command with message id {} on attempt {}",
            executionCommandEvent.getMessageId(), retry_counter, e);
      }
    }
  }

  protected abstract String getDestination(STOMPEvent stompEvent);

  /**
   * Creates STOMP message header.
   * @param sessionId
   * @return message header.
   */
  protected MessageHeaders createHeaders(String sessionId) {
    return createHeaders(sessionId, null);
  }

  /**
   * Creates STOMP message header.
   * @param sessionId
   * @return message header.
   */
  protected MessageHeaders createHeaders(String sessionId, Long messageId) {
    SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    headerAccessor.setSessionId(sessionId);
    headerAccessor.setLeaveMutable(true);
    if (messageId != null) {
      headerAccessor.setNativeHeader("messageId", Long.toString(messageId));
    }
    return headerAccessor.getMessageHeaders();
  }

  /**
   * Emits message to all subscribers.
   * @param event message should to be emitted.
   */
  protected void emitMessageToAll(STOMPEvent event) {
    LOG.debug("Received status update event {}", event);
    simpMessagingTemplate.convertAndSend(getDestination(event), event);
  }

  /**
   * Emit message to specified host only.
   * @param event message should to be emitted.
   * @throws HostNotRegisteredException in case host is not registered.
   */
  protected void emitMessageToHost(STOMPHostEvent event) throws HostNotRegisteredException {
    Long hostId = event.getHostId();
    String sessionId = agentSessionManager.getSessionId(hostId);
    LOG.debug("Received status update event {} for host {} registered with session ID {}", event, hostId, sessionId);
    MessageHeaders headers = createHeaders(sessionId);
    simpMessagingTemplate.convertAndSendToUser(sessionId, getDestination(event), event, headers);
  }

  /**
   * Emit execution command to specified host only.
   * @param event message should to be emitted.
   * @throws HostNotRegisteredException in case host is not registered.
   */
  protected void emitExecutionCommandToHost(ExecutionCommandEvent event) throws HostNotRegisteredException {
    Long hostId = event.getHostId();
    Long messageId = event.getMessageId();
    String sessionId = agentSessionManager.getSessionId(hostId);
    LOG.debug("Received status update event {} for host {} registered with session ID {}", event, hostId, sessionId);
    MessageHeaders headers = createHeaders(sessionId, messageId);
    simpMessagingTemplate.convertAndSendToUser(sessionId, getDestination(event), event, headers);
  }

  @Subscribe
  public void onHostRegister(HostRegisteredEvent hostRegisteredEvent) {
    Long hostId = hostRegisteredEvent.getHostId();
    if (!messagesToEmit.containsKey(hostId)) {
      messagesToEmit.put(hostId, new LinkedBlockingQueue<>());
      monitors.put(hostId, monitorsExecutor.submit(new MessagesToEmitMonitor(hostId)));
    }
  }
}
