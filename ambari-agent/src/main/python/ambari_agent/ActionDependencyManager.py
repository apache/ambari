#!/usr/bin/env python2.6

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import logging
import Queue
import threading
import pprint
import os
import json

logger = logging.getLogger()

class ActionDependencyManager():
  """
  Implemments a scheduler of role commands (like DATANODE-START) based on
  dependencies between them. Class does not execute actions, it only
  breaks them on groups that may be executed in parallel.
  """

  DEPS_FILE_NAME="role_command_order.json"
  COMMENT_STR="_comment"

  # Dictionary of dependencies. Format:
  # BlockedRole-Command : [BlockerRole1-Command1, BlockerRole2-Command2, ...]


  def __init__(self, config):
    self.deps = {}
    self.last_scheduled_group = []
    self.scheduled_action_groups = Queue.Queue()
    self.lock = threading.RLock()
    self.config = config
    #self.read_dependencies()


  def read_dependencies(self):
    """
    Load dependencies from file
    """
    prefix_dir = self.config.get('agent', 'prefix')
    action_order_file = os.path.join(prefix_dir, self.DEPS_FILE_NAME)
    with open(action_order_file) as f:
      action_order_data = json.load(f)
    for deps_group in action_order_data.keys():
      if deps_group != self.COMMENT_STR: # if entry is not a comment
        deps_group_list = action_order_data[deps_group]
        for blocked_str in deps_group_list:
          if blocked_str != self.COMMENT_STR: # if entry is not a comment
            blocker_list = deps_group_list[blocked_str]
            if blocked_str not in self.deps:
              self.deps[blocked_str]=[]
            for blocker_str in blocker_list:
              self.deps[blocked_str].append(blocker_str)
    pass


  def is_action_group_available(self):
    return not self.scheduled_action_groups.empty()


  def get_next_action_group(self):
    """
    Returns next group of scheduled actions that may be
    executed in parallel. If queue is empty, blocks until
    an item is available (until next put_action() call)
    """
    next_group = self.scheduled_action_groups.get(block=True)
    with self.lock: # Synchronized
      if next_group is self.last_scheduled_group:
        # Group is not eligible for appending, creating new one
        self.last_scheduled_group = []

      dump_str = pprint.pformat(next_group)
      logger.debug("Next action group: {0}".format(dump_str))
      return next_group


  def put_actions(self, actions):
    """
    Schedules actions to be executed in some time at future.
    Here we rely on serial command execution sequence received from server.
    Some of these commands may be executed in parallel with others, so we
    unite them into a group.
    """
    with self.lock: # Synchronized
      for action in actions:
        self.dump_info(action)
        was_empty = len(self.last_scheduled_group) == 0
        if self.can_be_executed_in_parallel(action, self.last_scheduled_group):
          self.last_scheduled_group.append(action)
        else: # create a new group
          self.last_scheduled_group = [action]
          was_empty = True
        if was_empty:
          # last_scheduled_group is not empty now, so we add it to the queue
          self.scheduled_action_groups.put(self.last_scheduled_group)


  def dump_info(self, action):
    """
    Prints info about command to log
    """
    logger.info("Adding " + action['commandType'] + " for service " + \
                action['serviceName'] + " of cluster " + \
                action['clusterName'] + " to the queue.")
    logger.debug(pprint.pformat(action))


  def can_be_executed_in_parallel(self, action, group):
    """
    Checks whether action may be executed in parallel with a given group
    """
    # Hack: parallel execution disabled
    return False

    # from ActionQueue import ActionQueue
    # # Empty group is compatible with any action
    # if not group:
    #   return True
    # # Status commands are placed into a separate group to avoid race conditions
    # if action['commandType'] == ActionQueue.STATUS_COMMAND:
    #   for scheduled_action in group:
    #     if scheduled_action['commandType'] != ActionQueue.STATUS_COMMAND:
    #       return False
    #   return True
    # # We avoid executing install/upgrade threads in parallel with anything
    # standalone_commands = ["INSTALL", ActionQueue.ROLE_COMMAND_UPGRADE]
    # if action['roleCommand'] in standalone_commands:
    #   return False
    # # We can not perform few actions (like STOP and START) for a component
    # # at the same time
    # for scheduled_action in group:
    #   if scheduled_action['role'] == action['role']:
    #     return False
    # # In other cases, check dependencies
    # pattern = "{0}-{1}"
    # new_action_str = pattern.format(action['role'], action['roleCommand'])
    # for scheduled_action in group:
    #   if new_action_str in self.deps:
    #     blockers = self.deps[new_action_str]
    #     scheduled_action_str = pattern.format(
    #       scheduled_action['role'], scheduled_action['roleCommand'])
    #     if scheduled_action_str in blockers:
    #       return False
    # # Everything seems to be ok
    # return True
