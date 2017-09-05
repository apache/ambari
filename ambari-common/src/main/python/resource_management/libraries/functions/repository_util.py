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

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.libraries.resources.repository import Repository
import ambari_simplejson as json


__all__ = ["create_repo_files", "CommandRepository"]

# components_lits = repoName + postfix
UBUNTU_REPO_COMPONENTS_POSTFIX = ["main"]


def create_repo_files(template, command_repository):
  """
  Creates repositories in a consistent manner for all types
  :param command_repository: a CommandRepository instance
  :return:
  """

  if command_repository.version_id is None:
    raise Fail("The command repository was not parsed correctly")

  if 0 == len(command_repository.repositories):
    Logger.warning(
      "Repository for {0}/{1} has no repositories.  Ambari may not be managing this version.".format(
        command_repository.stack_name, command_repository.version_string))
    return

  # add the stack name to the file name just to make it a little easier to debug
  # version_id is the primary id of the repo_version table in the database
  file_name = "ambari-{0}-{1}".format(command_repository.stack_name.lower(),
                                      command_repository.version_id)

  append_to_file = False  # initialize to False to create the file anew.

  for repository in command_repository.repositories:

    if repository.repo_id is None:
      raise Fail("Repository with url {0} has no id".format(repository.base_url))

    if not repository.ambari_managed:
      Logger.warning(
        "Repository for {0}/{1}/{2} is not managed by Ambari".format(
          command_repository.stack_name, command_repository.version_string, repository.repo_id))
    else:
      Repository(repository.repo_id,
                 action = "create",
                 base_url = repository.base_url,
                 mirror_list = repository.mirrors_list,
                 repo_file_name = file_name,
                 repo_template = template,
                 components = repository.ubuntu_components,
                 append_to_file = append_to_file)
      append_to_file = True


def _find_value(dictionary, key):
  """
  Helper to find a value in a dictionary
  """
  if key not in dictionary:
    return None

  return dictionary[key]


class CommandRepository(object):
  """
  Class that encapsulates the representation of repositories passed in a command.  This class
  should match the CommandRepository class.
  """

  def __init__(self, jsonvalue):

    if isinstance(jsonvalue, dict):
      json_dict = jsonvalue
    elif isinstance(jsonvalue, basestring):
      json_dict = json.loads(jsonvalue)

    if json_dict is None:
      raise Fail("Cannot deserialize command repository {0}".format(str(jsonvalue)))

    # version_id is the primary id of the repo_version table in the database
    self.version_id = _find_value(json_dict, 'repoVersionId')
    self.stack_name = _find_value(json_dict, 'stackName')
    self.version_string = _find_value(json_dict, 'repoVersion')
    self.repositories = []

    repos_def = _find_value(json_dict, 'repositories')
    if repos_def is not None:
       if not isinstance(repos_def, list):
         repos_def = [repos_def]

       for repo_def in repos_def:
         self.repositories.append(_CommandRepositoryEntry(repo_def))


class _CommandRepositoryEntry(object):
  """
  Class that represents the entries of a CommandRepository.  This isn't meant to be instantiated
  outside a CommandRepository
  """
  def __init__(self, json_dict):
    self.repo_id = _find_value(json_dict, 'repoId')  # this is the id within the repo file, not an Ambari artifact
    self.repo_name = _find_value(json_dict, 'repoName')
    self.base_url = _find_value(json_dict, 'baseUrl')
    self.mirrors_list = _find_value(json_dict, 'mirrorsList')
    self.ambari_managed = _find_value(json_dict, 'ambariManaged')

    if self.ambari_managed is None:
      self.ambari_managed = True

    # if repoName is changed on the java side, this will fail for ubuntu since we rely on the
    # name being the same as how the repository was built
    self.ubuntu_components = [self.repo_name] + UBUNTU_REPO_COMPONENTS_POSTFIX
