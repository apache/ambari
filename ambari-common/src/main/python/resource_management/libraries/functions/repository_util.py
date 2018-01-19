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
UBUNTU_REPO_COMPONENTS_POSTFIX = "main"


def create_repo_files(template, command_repository):
  """
  Creates repositories in a consistent manner for all types
  :param command_repository: a CommandRepository instance
  :type command_repository CommandRepository
  :return: a dictionary with repo ID => repo file name mapping
  """

  if command_repository.version_id is None:
    raise Fail("The command repository was not parsed correctly")

  if 0 == len(command_repository.items):
    Logger.warning(
      "Repository for {0}/{1} has no repositories.  Ambari may not be managing this version.".format(
        command_repository.stack_name, command_repository.version_string))
    return {}

  append_to_file = False  # initialize to False to create the file anew.
  repo_files = {}

  for repository in command_repository.items:

    if repository.repo_id is None:
      raise Fail("Repository with url {0} has no id".format(repository.base_url))

    if not repository.ambari_managed:
      Logger.warning(
        "Repository for {0}/{1}/{2} is not managed by Ambari".format(
          command_repository.stack_name, command_repository.version_string, repository.repo_id))
    else:
      Repository(repository.repo_id,
                 action="create",
                 base_url=repository.base_url,
                 mirror_list=repository.mirrors_list,
                 repo_file_name=command_repository.repo_filename,
                 repo_template=template,
                 components=repository.ubuntu_components,
                 append_to_file=append_to_file)
      append_to_file = True
      repo_files[repository.repo_id] = command_repository.repo_filename

  return repo_files


def _find_value(dictionary, key, default=None):
  """
  Helper to find a value in a dictionary
  """
  if key not in dictionary:
    return default

  return dictionary[key]


class CommandRepositoryFeature(object):
  def __init__(self, feat_dict):
    """
    :type feat_dict dict
    """
    self.pre_installed = _find_value(feat_dict, "preInstalled", default=False)
    self.scoped = _find_value(feat_dict, "scoped", default=True)


class CommandRepository(object):
  """
  Class that encapsulates the representation of repositories passed in a command.  This class
  should match the CommandRepository class.
  """

  def __init__(self, repo_object):
    """
    :type repo_object dict|basestring
    """

    if isinstance(repo_object, dict):
      json_dict = dict(repo_object)   # strict dict(from ConfigDict) to avoid hidden type conversions
    elif isinstance(repo_object, basestring):
      json_dict = json.loads(repo_object)
    else:
      raise Fail("Cannot deserialize command repository {0}".format(str(repo_object)))

    # version_id is the primary id of the repo_version table in the database
    self.version_id = _find_value(json_dict, 'repoVersionId')
    self.stack_name = _find_value(json_dict, 'stackName')
    self.version_string = _find_value(json_dict, 'repoVersion')
    self.repo_filename = _find_value(json_dict, 'repoFileName')
    self.feat = CommandRepositoryFeature(_find_value(json_dict, "feature", default={}))
    self.items = []

    repos_def = _find_value(json_dict, 'repositories')
    if repos_def is not None:
       if not isinstance(repos_def, list):
         repos_def = [repos_def]

       for repo_def in repos_def:
         self.items.append(CommandRepositoryItem(self, repo_def))


class CommandRepositoryItem(object):
  """
  Class that represents the entries of a CommandRepository.  This isn't meant to be instantiated
  outside a CommandRepository
  """

  def __init__(self, repo, json_dict):
    """
    :type repo CommandRepository
    :type json_dict dict
    """
    self._repo = repo

    self.repo_id = _find_value(json_dict, 'repoId')  # this is the id within the repo file, not an Ambari artifact
    self.repo_name = _find_value(json_dict, 'repoName')
    self.distribution = _find_value(json_dict, 'distribution')
    self.components = _find_value(json_dict, 'components')
    self.base_url = _find_value(json_dict, 'baseUrl')
    self.mirrors_list = _find_value(json_dict, 'mirrorsList')
    self.ambari_managed = _find_value(json_dict, 'ambariManaged', default=True)

    self.ubuntu_components = [self.distribution if self.distribution else self.repo_name] + \
                             [self.components.replace(",", " ") if self.components else UBUNTU_REPO_COMPONENTS_POSTFIX]






