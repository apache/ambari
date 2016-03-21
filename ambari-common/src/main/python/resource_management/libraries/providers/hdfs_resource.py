# !/usr/bin/env python
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

Ambari Agent

"""
import re
import os
import time
from resource_management.core.environment import Environment
from resource_management.core.base import Fail
from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import File
from resource_management.core.providers import Provider
from resource_management.core.logger import Logger
from resource_management.core import shell
from resource_management.libraries.script import Script
from resource_management.libraries.functions import format
from resource_management.libraries.functions.get_user_call_output import get_user_call_output
from resource_management.libraries.functions import is_empty
from resource_management.libraries.functions import namenode_ha_utils

import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import subprocess

JSON_PATH = '/var/lib/ambari-agent/tmp/hdfs_resources_{timestamp}.json'
JAR_PATH = '/var/lib/ambari-agent/lib/fast-hdfs-resource.jar'

RESOURCE_TO_JSON_FIELDS = {
  'target': 'target',
  'type': 'type',
  'action': 'action',
  'source': 'source',
  'owner': 'owner',
  'group': 'group',
  'mode': 'mode',
  'recursive_chown': 'recursiveChown',
  'recursive_chmod': 'recursiveChmod',
  'change_permissions_for_parents': 'changePermissionforParents',
  'manage_if_exists': 'manageIfExists',
  'dfs_type': 'dfs_type'
}

class HdfsResourceJar:
  """
  This is slower than HdfsResourceWebHDFS implementation of HdfsResouce, but it works in any cases on any DFS types.

  The idea is to put all the files/directories/copyFromLocals we have to create/delete into a json file.
  And then create in it with ONLY ONE expensive hadoop call to our custom jar fast-hdfs-resource.jar which grabs this json.

  'create_and_execute' and 'delete_on_execute' does nothing but add files/directories to this json,
  while execute does all the expensive creating/deleting work executing the jar with the json as parameter.
  """
  def action_delayed(self, action_name, main_resource):
    resource = {}
    env = Environment.get_instance()
    if not 'hdfs_files' in env.config:
      env.config['hdfs_files'] = []

    # Put values in dictionary-resource
    for field_name, json_field_name in RESOURCE_TO_JSON_FIELDS.iteritems():
      if field_name == 'action':
        resource[json_field_name] = action_name
      elif field_name == 'mode' and main_resource.resource.mode:
        resource[json_field_name] = oct(main_resource.resource.mode)[1:]
      elif field_name == 'manage_if_exists':
        resource[json_field_name] = main_resource.manage_if_exists
      elif getattr(main_resource.resource, field_name):
        resource[json_field_name] = getattr(main_resource.resource, field_name)

    # Add resource to create
    env.config['hdfs_files'].append(resource)

  def action_execute(self, main_resource):
    env = Environment.get_instance()

    # Check required parameters
    main_resource.assert_parameter_is_set('user')

    if not 'hdfs_files' in env.config or not env.config['hdfs_files']:
      Logger.info("No resources to create. 'create_on_execute' or 'delete_on_execute' wasn't triggered before this 'execute' action.")
      return

    hadoop_bin_dir = main_resource.resource.hadoop_bin_dir
    hadoop_conf_dir = main_resource.resource.hadoop_conf_dir
    user = main_resource.resource.user
    security_enabled = main_resource.resource.security_enabled
    keytab_file = main_resource.resource.keytab
    kinit_path = main_resource.resource.kinit_path_local
    logoutput = main_resource.resource.logoutput
    principal_name = main_resource.resource.principal_name
    jar_path=JAR_PATH
    timestamp = time.time()
    json_path=format(JSON_PATH)

    if security_enabled:
      main_resource.kinit()

    # Write json file to disk
    File(json_path,
         owner = user,
         content = json.dumps(env.config['hdfs_files'])
    )

    # Execute jar to create/delete resources in hadoop
    Execute(format("hadoop --config {hadoop_conf_dir} jar {jar_path} {json_path}"),
            user=user,
            path=[hadoop_bin_dir],
            logoutput=logoutput,
    )

    # Clean
    env.config['hdfs_files'] = []

class WebHDFSUtil:
  def __init__(self, hdfs_site, run_user, security_enabled, logoutput=None):
    https_nn_address = namenode_ha_utils.get_property_for_active_namenode(hdfs_site, 'dfs.namenode.https-address',
                                                                          security_enabled, run_user)
    http_nn_address = namenode_ha_utils.get_property_for_active_namenode(hdfs_site, 'dfs.namenode.http-address',
                                                                         security_enabled, run_user)

    # check for dfs.http.policy and after that for deprecated(for newer stacks) dfs.https.enable
    self.is_https_enabled = False
    if not is_empty(hdfs_site['dfs.http.policy']):
      self.is_https_enabled = hdfs_site['dfs.http.policy'].lower() == "https_only"
    elif not is_empty(hdfs_site['dfs.https.enable']):
      self.is_https_enabled = hdfs_site['dfs.https.enable']

    address = https_nn_address if self.is_https_enabled else http_nn_address
    protocol = "https" if self.is_https_enabled else "http"

    self.address = format("{protocol}://{address}")
    self.run_user = run_user
    self.security_enabled = security_enabled
    self.logoutput = logoutput

  @staticmethod
  def is_webhdfs_available(is_webhdfs_enabled, default_fs):
    # only hdfs seems to support webHDFS
    return (is_webhdfs_enabled and default_fs.startswith("hdfs"))

  valid_status_codes = ["200", "201"]
  def run_command(self, target, operation, method='POST', assertable_result=True, file_to_put=None, ignore_status_codes=[], **kwargs):
    """
    assertable_result - some POST requests return '{"boolean":false}' or '{"boolean":true}'
    depending on if query was successful or not, we can assert this for them
    """
    target = HdfsResourceProvider.parse_path(target)

    url = format("{address}/webhdfs/v1{target}?op={operation}&user.name={run_user}", address=self.address, run_user=self.run_user)
    for k,v in kwargs.iteritems():
      url = format("{url}&{k}={v}")

    if file_to_put and not os.path.exists(file_to_put):
      raise Fail(format("File {file_to_put} is not found."))

    cmd = ["curl", "-sS","-L", "-w", "%{http_code}", "-X", method]

    if file_to_put:
      cmd += ["-T", file_to_put]
    if self.security_enabled:
      cmd += ["--negotiate", "-u", ":"]
    if self.is_https_enabled:
      cmd += ["-k"]

    cmd.append(url)
    _, out, err = get_user_call_output(cmd, user=self.run_user, logoutput=self.logoutput, quiet=False)
    status_code = out[-3:]
    out = out[:-3] # remove last line from output which is status code

    try:
      result_dict = json.loads(out)
    except ValueError:
      result_dict = out

    if status_code not in WebHDFSUtil.valid_status_codes+ignore_status_codes or assertable_result and result_dict and not result_dict['boolean']:
      formatted_output = json.dumps(result_dict, indent=2) if isinstance(result_dict, dict) else result_dict
      formatted_output = err + "\n" + formatted_output
      err_msg = "Execution of '%s' returned status_code=%s. %s" % (shell.string_cmd_from_args_list(cmd), status_code, formatted_output)
      raise Fail(err_msg)

    return result_dict

class HdfsResourceWebHDFS:
  """
  This is the fastest implementation of HdfsResource using WebHDFS.
  Since it's not available on non-hdfs FS and also can be disabled in scope of HDFS.
  We should still have the other implementations for such a cases.
  """

  """
  If we have more than this count of files to recursively chmod/chown
  webhdfs won't be used, but 'hadoop fs -chmod (or chown) -R ..' As it can really slow.
  (in one second ~17 files can be chmoded)
  """
  MAX_FILES_FOR_RECURSIVE_ACTION_VIA_WEBHDFS = 1000
  """
  This is used to avoid a lot of liststatus commands, which can take some time if directory
  contains a lot of files. LISTSTATUS of directory with 1000 files takes ~0.5 seconds.
  """
  MAX_DIRECTORIES_FOR_RECURSIVE_ACTION_VIA_WEBHDFS = 250

  def action_execute(self, main_resource):
    pass

  def _assert_valid(self):
    source = self.main_resource.resource.source
    type = self.main_resource.resource.type
    target = self.main_resource.resource.target

    if source:
      if not os.path.exists(source):
        raise Fail(format("Source {source} doesn't exist"))
      if type == "directory" and os.path.isfile(source):
        raise Fail(format("Source {source} is file but type is {type}"))
      elif type == "file" and os.path.isdir(source):
        raise Fail(format("Source {source} is directory but type is {type}"))

    self.target_status = self._get_file_status(target)

    if self.target_status and self.target_status['type'].lower() != type:
      raise Fail(format("Trying to create file/directory but directory/file exists in the DFS on {target}"))

  def action_delayed(self, action_name, main_resource):
    main_resource.assert_parameter_is_set('user')

    if main_resource.resource.security_enabled:
      main_resource.kinit()

    self.util = WebHDFSUtil(main_resource.resource.hdfs_site, main_resource.resource.user,
                            main_resource.resource.security_enabled, main_resource.resource.logoutput)
    self.mode = oct(main_resource.resource.mode)[1:] if main_resource.resource.mode else main_resource.resource.mode
    self.mode_set = False
    self.main_resource = main_resource
    self._assert_valid()
    
    if self.main_resource.manage_if_exists == False and self.target_status:
      Logger.info("Skipping the operation for not managed DFS directory " + str(self.main_resource.resource.target) +
                  " since immutable_paths contains it.")
      return        
    
    if action_name == "create":
      self._create_resource()
      self._set_mode(self.target_status)
      self._set_owner(self.target_status)
    else:
      self._delete_resource()

  def _create_resource(self):
    is_create = (self.main_resource.resource.source == None)

    if is_create and self.main_resource.resource.type == "directory":
      self._create_directory(self.main_resource.resource.target)
    elif is_create and self.main_resource.resource.type == "file":
      self._create_file(self.main_resource.target, mode=self.mode)
    elif not is_create and self.main_resource.resource.type == "file":
      self._create_file(self.main_resource.resource.target, source=self.main_resource.resource.source, mode=self.mode)
    elif not is_create and self.main_resource.resource.type == "directory":
      self._create_directory(self.main_resource.resource.target)
      self._copy_from_local_directory(self.main_resource.resource.target, self.main_resource.resource.source)

  def _copy_from_local_directory(self, target, source):
    for next_path_part in os.listdir(source):
      new_source = os.path.join(source, next_path_part)
      new_target = format("{target}/{next_path_part}")
      if os.path.isdir(new_source):
        Logger.info(format("Creating DFS directory {new_target}"))
        self._create_directory(new_target)
        self._copy_from_local_directory(new_target, new_source)
      else:
        self._create_file(new_target, new_source)

  def _create_directory(self, target):
    if target == self.main_resource.resource.target and self.target_status:
      return

    self.util.run_command(target, 'MKDIRS', method='PUT')

  def _get_file_status(self, target):
    list_status = self.util.run_command(target, 'GETFILESTATUS', method='GET', ignore_status_codes=['404'], assertable_result=False)
    return list_status['FileStatus'] if 'FileStatus' in list_status else None

  def _create_file(self, target, source=None, mode=""):
    """
    PUT file command in slow, however _get_file_status is pretty fast,
    so we should check if the file really should be put before doing it.
    """
    file_status = self._get_file_status(target) if target!=self.main_resource.resource.target else self.target_status
    mode = "" if not mode else mode

    if file_status:
      if source:
        length = file_status['length']
        local_file_size = os.stat(source).st_size # TODO: os -> sudo

        # TODO: re-implement this using checksums
        if local_file_size == length:
          Logger.info(format("DFS file {target} is identical to {source}, skipping the copying"))
          return
        elif not self.main_resource.resource.replace_existing_files:
          Logger.info(format("Not replacing existing DFS file {target} which is different from {source}, due to replace_existing_files=False"))
          return
      else:
        Logger.info(format("File {target} already exists in DFS, skipping the creation"))
        return

    Logger.info(format("Creating new file {target} in DFS"))
    kwargs = {'permission': mode} if mode else {}

    self.util.run_command(target, 'CREATE', method='PUT', overwrite=True, assertable_result=False, file_to_put=source, **kwargs)

    if mode and file_status:
      file_status['permission'] = mode


  def _delete_resource(self):
    if not self.target_status:
          return
    self.util.run_command(self.main_resource.resource.target, 'DELETE', method='DELETE', recursive=True)

  def _set_owner(self, file_status=None):
    owner = "" if not self.main_resource.resource.owner else self.main_resource.resource.owner
    group = "" if not self.main_resource.resource.group else self.main_resource.resource.group

    if (not owner or file_status and file_status['owner'] == owner) and (not group or file_status and file_status['group'] == group):
      return

    self.util.run_command(self.main_resource.resource.target, 'SETOWNER', method='PUT', owner=owner, group=group, assertable_result=False)

    results = []

    if self.main_resource.resource.recursive_chown:
      content_summary = self.util.run_command(self.main_resource.resource.target, 'GETCONTENTSUMMARY', method='GET', assertable_result=False)

      if content_summary['ContentSummary']['fileCount'] <= HdfsResourceWebHDFS.MAX_FILES_FOR_RECURSIVE_ACTION_VIA_WEBHDFS and content_summary['ContentSummary']['directoryCount'] <= HdfsResourceWebHDFS.MAX_DIRECTORIES_FOR_RECURSIVE_ACTION_VIA_WEBHDFS:
        self._fill_directories_list(self.main_resource.resource.target, results)
      else: # avoid chmowning a lot of files and listing a lot dirs via webhdfs which can take a lot of time.
        shell.checked_call(["hadoop", "fs", "-chown", "-R", format("{owner}:{group}"), self.main_resource.resource.target], user=self.main_resource.resource.user)

    if self.main_resource.resource.change_permissions_for_parents:
      self._fill_in_parent_directories(self.main_resource.resource.target, results)

    for path in results:
      self.util.run_command(path, 'SETOWNER', method='PUT', owner=owner, group=group, assertable_result=False)

  def _set_mode(self, file_status=None):
    if not self.mode or file_status and file_status['permission'] == self.mode:
      return

    if not self.mode_set:
      self.util.run_command(self.main_resource.resource.target, 'SETPERMISSION', method='PUT', permission=self.mode, assertable_result=False)

    results = []

    if self.main_resource.resource.recursive_chmod:
      content_summary = self.util.run_command(self.main_resource.resource.target, 'GETCONTENTSUMMARY', method='GET', assertable_result=False)

      if content_summary['ContentSummary']['fileCount'] <= HdfsResourceWebHDFS.MAX_FILES_FOR_RECURSIVE_ACTION_VIA_WEBHDFS and content_summary['ContentSummary']['directoryCount'] <= HdfsResourceWebHDFS.MAX_DIRECTORIES_FOR_RECURSIVE_ACTION_VIA_WEBHDFS:
        self._fill_directories_list(self.main_resource.resource.target, results)
      else: # avoid chmoding a lot of files and listing a lot dirs via webhdfs which can take a lot of time.
        shell.checked_call(["hadoop", "fs", "-chmod", "-R", self.mode, self.main_resource.resource.target], user=self.main_resource.resource.user)

    if self.main_resource.resource.change_permissions_for_parents:
      self._fill_in_parent_directories(self.main_resource.resource.target, results)

    for path in results:
      self.util.run_command(path, 'SETPERMISSION', method='PUT', permission=self.mode, assertable_result=False)


  def _fill_in_parent_directories(self, target, results):
    path_parts = HdfsResourceProvider.parse_path(target).split("/")[1:]# [1:] remove '' from parts
    path = "/"

    for path_part in path_parts:
      path += path_part + "/"
      results.append(path)

  def _fill_directories_list(self, target, results):
    list_status = self.util.run_command(target, 'LISTSTATUS', method='GET', assertable_result=False)['FileStatuses']['FileStatus']

    for file in list_status:
      if file['pathSuffix']:
        new_path = target + "/" + file['pathSuffix']
        results.append(new_path)

        if file['type'] == 'DIRECTORY':
          self._fill_directories_list(new_path, results)

class HdfsResourceProvider(Provider):
  def __init__(self, resource):
    super(HdfsResourceProvider,self).__init__(resource)
    self.ignored_resources_list = HdfsResourceProvider.get_ignored_resources_list(self.resource.hdfs_resource_ignore_file)
    self.fsType = getattr(resource, 'dfs_type')
    if self.fsType != 'HCFS':
      self.assert_parameter_is_set('hdfs_site')
      self.webhdfs_enabled = self.resource.hdfs_site['dfs.webhdfs.enabled']
          
  @staticmethod
  def parse_path(path):
    """
    hdfs://nn_url:1234/a/b/c -> /a/b/c
    hdfs://nn_ha_name/a/b/c -> /a/b/c
    hdfs:///a/b/c -> /a/b/c
    /a/b/c -> /a/b/c
    """
    math_with_protocol_and_nn_url = re.match("[a-zA-Z]+://[^/]+(/.+)", path)
    math_with_protocol = re.match("[a-zA-Z]+://(/.+)", path)

    if math_with_protocol_and_nn_url:
      path = math_with_protocol_and_nn_url.group(1)
    elif math_with_protocol:
      path = math_with_protocol.group(1)
    else:
      path = path

    return re.sub("[/]+", "/", path)

  @staticmethod
  def get_ignored_resources_list(hdfs_resource_ignore_file):
    if not hdfs_resource_ignore_file or not os.path.exists(hdfs_resource_ignore_file):
      return []

    with open(hdfs_resource_ignore_file, "rb") as fp:
      content = fp.read()

    hdfs_resources_to_ignore = []
    for hdfs_resource_to_ignore in content.split("\n"):
      hdfs_resources_to_ignore.append(HdfsResourceProvider.parse_path(hdfs_resource_to_ignore))

    return hdfs_resources_to_ignore


  def action_delayed(self, action_name):
    self.assert_parameter_is_set('type')

    parsed_path = HdfsResourceProvider.parse_path(self.resource.target)

    parsed_not_managed_paths = [HdfsResourceProvider.parse_path(path) for path in self.resource.immutable_paths]
    self.manage_if_exists = not parsed_path in parsed_not_managed_paths

    if parsed_path in self.ignored_resources_list:
      Logger.info("Skipping '{0}' because it is in ignore file {1}.".format(self.resource, self.resource.hdfs_resource_ignore_file))
      return

    self.get_hdfs_resource_executor().action_delayed(action_name, self)

  def action_create_on_execute(self):
    self.action_delayed("create")

  def action_delete_on_execute(self):
    self.action_delayed("delete")

  def action_execute(self):
    self.get_hdfs_resource_executor().action_execute(self)

  def get_hdfs_resource_executor(self):
    if self.fsType == 'HCFS':
      return HdfsResourceJar()
    elif WebHDFSUtil.is_webhdfs_available(self.webhdfs_enabled, self.resource.default_fs):
      return HdfsResourceWebHDFS()
    else:
      return HdfsResourceJar()

  def assert_parameter_is_set(self, parameter_name):
    if not getattr(self.resource, parameter_name):
      raise Fail("Resource parameter '{0}' is not set.".format(parameter_name))
    return True

  def kinit(self):
    keytab_file = self.resource.keytab
    kinit_path = self.resource.kinit_path_local
    principal_name = self.resource.principal_name
    user = self.resource.user

    Execute(format("{kinit_path} -kt {keytab_file} {principal_name}"),
            user=user
    )
