"""
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
"""

import os
import logging

from ambari_agent.models.commands import AgentCommand
from models.hooks import HookPrefix


class ResolvedHooks(object):
  def __init__(self, pre_hooks, post_hooks):
    """
    :type pre_hooks tuple|list
    :type post_hooks tuple|list
    """
    self.pre_hooks = pre_hooks
    self.post_hooks = post_hooks


def _per_command_resolver(prefix, command):
  return "{}-{}".format(prefix, command)


def _per_service_resolver(prefix, command, service):
  if not service:
    return None

  return "{}-{}-{}".format(prefix, service, command)


def _per_role_resolver(prefix, command, service, role):
  if not service or not role:
    return None

  return "{}-{}-{}-{}".format(prefix, service, role, command)


class HooksOrchestrator(object):

  def __init__(self, injector):
    """

    :type injector InitializerModule
    """
    from InitializerModule import InitializerModule

    if not isinstance(injector, InitializerModule):
      raise TypeError("Expecting {} type".format(InitializerModule.__name__))

    self._file_cache = injector.file_cache
    self._logger = logging.getLogger()

  def resolve_hooks(self, command, command_name):
    """
    :type command dict
    :type command_name str
    :rtype ResolvedHooks
    """
    command_type = command["commandType"]
    if command_type == AgentCommand.status or not command_name:
      return None

    hook_dir = self._file_cache.get_hook_base_dir(command)

    if not hook_dir:
      return ResolvedHooks([], [])

    service = command["serviceName"] if "serviceName" in command else None
    component = command["role"] if "role" in command else None

    pre_hooks_topology = [
      _per_command_resolver(HookPrefix.pre, command_name),
      _per_service_resolver(HookPrefix.pre, command_name, service),
      _per_role_resolver(HookPrefix.pre, command_name, service, component)
    ]

    post_hooks_topology = [
      _per_role_resolver(HookPrefix.post, command_name, service, component),
      _per_service_resolver(HookPrefix.post, command_name, service),
      _per_command_resolver(HookPrefix.post, command_name)
    ]

    pre_hooks = []
    post_hooks = []

    for hook in pre_hooks_topology:
      resolved = self._resolve_path(hook_dir, hook)
      if resolved:
        pre_hooks.append(resolved)

    for hook in post_hooks_topology:
      resolved = self._resolve_path(hook_dir, hook)
      if resolved:
        post_hooks.append(resolved)

    return ResolvedHooks(pre_hooks, post_hooks)

  def _resolve_path(self, stack_hooks_dir, resolver):
    """
    Returns a tuple(path to hook script, hook base dir) according to string prefix
    and command name. If script does not exist, returns None
    """
    if not resolver:
      return None

    hook_dir = resolver
    hook_base_dir = os.path.join(stack_hooks_dir, hook_dir)
    hook_script_path = os.path.join(hook_base_dir, "scripts", "hook.py")
    if not os.path.isfile(hook_script_path):
      self._logger.debug("Hook script {0} not found, skipping".format(hook_script_path))
      return None
    return hook_script_path, hook_base_dir
