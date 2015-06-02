#!/usr/bin/env python
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

import sys
import getopt
import json
import os
import shutil
import xml.etree.ElementTree as ET
import re


class _named_dict(dict):
  """
  Allow to get dict items using attribute notation, eg dict.attr == dict['attr']
  """

  def __init__(self, _dict):

    def repl_list(_list):
      for i, e in enumerate(_list):
        if isinstance(e, list):
          _list[i] = repl_list(e)
        if isinstance(e, dict):
          _list[i] = _named_dict(e)
      return _list

    dict.__init__(self, _dict)
    for key, value in self.iteritems():
      if isinstance(value, dict):
        self[key] = _named_dict(value)
      if isinstance(value, list):
        self[key] = repl_list(value)

  def __getattr__(self, item):
    if item in self:
      return self[item]
    else:
      dict.__getattr__(self, item)


def copy_tree(src, dest, exclude=None, post_copy=None):
  """
  Copy files form src to dest.

  :param src: source folder
  :param dest: destination folder
  :param exclude: list for excluding, eg [".xml"] will exclude all xml files
  :param post_copy: callable that accepts source and target paths and will be called after copying
  """
  for item in os.listdir(src):
    if exclude and item in exclude:
      continue

    _src = os.path.join(src, item)
    _dest = os.path.join(dest, item)

    if os.path.isdir(_src):
      if not os.path.exists(_dest):
        os.makedirs(_dest)
      copy_tree(_src, _dest, exclude, post_copy)
    else:
      _dest_dirname = os.path.dirname(_dest)
      if not os.path.exists(_dest_dirname):
        os.makedirs(_dest_dirname)
      shutil.copy(_src, _dest)

    if post_copy:
      post_copy(_src, _dest)


def process_metainfo(file_path, config_data, stack_version_changes):
  tree = ET.parse(file_path)
  root = tree.getroot()

  if root.find('versions') is not None or root.find('services') is None:
    # process stack metainfo.xml
    extends_tag = root.find('extends')
    if extends_tag is not None:
      version = extends_tag.text
      if version in stack_version_changes:
        extends_tag.text = stack_version_changes[version]
        tree.write(file_path)

    current_version = file_path.split(os.sep)[-2]
    modify_active_tag = False
    active_tag_value = None
    for stack in config_data.versions:
      if stack.version == current_version and 'active' in stack:
        modify_active_tag = True
        active_tag_value = stack.active
        break

    if modify_active_tag:
      versions_tag = root.find('versions')
      if versions_tag is None:
        versions_tag = ET.SubElement(root, 'versions')
      active_tag = versions_tag.find('active')
      if active_tag is None:
        active_tag = ET.SubElement(versions_tag, 'active')
      active_tag.text = active_tag_value
      tree.write(file_path)
  else:
    # process service metainfo.xml
    services_tag = root.find('services')
    if services_tag is not None:
      for service_tag in services_tag.findall('service'):
        name = service_tag.find('name').text
        for stack in config_data.versions:
          for service in stack.services:
            if service.name == name:
              if 'version' in service:
                service_version_tag = service_tag.find('version')
                if service_version_tag is None:
                  service_version_tag = ET.SubElement(service_tag, 'version')
                service_version_tag.text = service.version
                tree.write(file_path)


def process_upgrade_xml(file_path, target_version, config_data, stack_version_changes):
  # change versions in xml
  tree = ET.parse(file_path)
  root = tree.getroot()

  for target_tag in root.findall('target'):
    version = '.'.join([el for el in target_tag.text.split('.') if el != '*'])
    if version in stack_version_changes:
      target_tag.text = target_tag.text.replace(version, stack_version_changes[version])
      tree.write(file_path)

  for target_tag in root.findall('target-stack'):
    base_stack_name, base_stack_version = target_tag.text.split('-')
    new_target_stack_text = target_tag.text.replace(base_stack_name, config_data.stackName)
    if base_stack_version in stack_version_changes:
      new_target_stack_text = new_target_stack_text.replace(base_stack_version,
                                                            stack_version_changes[base_stack_version])
    target_tag.text = new_target_stack_text
    tree.write(file_path)

  # rename upgrade files
  if target_version in stack_version_changes:
    new_name = os.path.join(os.path.dirname(file_path),
                            'upgrade-{0}.xml'.format(stack_version_changes[target_version]))
    os.rename(file_path, new_name)
  pass


def process_stack_advisor(file_path, config_data, stack_version_changes):
  CLASS_NAME_REGEXP = '([A-Za-z]+)(\d+)StackAdvisor'

  stack_advisor_content = open(file_path, 'r').read()

  for stack_name, stack_version in  re.findall(CLASS_NAME_REGEXP, stack_advisor_content):
    what = stack_name + stack_version + 'StackAdvisor'
    stack_version_dotted = '.'.join(list(stack_version))
    if stack_version_dotted in stack_version_changes:
      to = config_data.stackName + stack_version_changes[stack_version_dotted].replace('.','') + 'StackAdvisor'
    else:
      to = config_data.stackName + stack_version + 'StackAdvisor'
    stack_advisor_content = stack_advisor_content.replace(what, to)

  with open(file_path, 'w') as f:
    f.write(stack_advisor_content)

def process_repoinfo_xml(file_path, config_data, stack_version_changes):
  tree = ET.parse(file_path)
  root = tree.getroot()
  for baseurl_tag in root.iter('baseurl'):
    baseurl_tag.text = 'http://examplerepo.com'

  tree.write(file_path)

def process_py_files(file_path, config_data, stack_version_changes):
  file_content = open(file_path, 'r').read()
  # replace select tools
  file_content = file_content.replace('hdp-select', config_data.selectTool)
  file_content = file_content.replace('conf-select', config_data.confSelectTool)

  with open(file_path, 'w') as f:
    f.write(file_content)

def copy_stacks(resources_folder, output_folder, config_data):
  original_folder = os.path.join(resources_folder, 'stacks', config_data.baseStackName)
  target_folder = os.path.join(output_folder, 'stacks', config_data.stackName)
  stack_version_changes = {}

  for stack in config_data.versions:
    if stack.version != stack.baseVersion:
      stack_version_changes[stack.baseVersion] = stack.version

  for stack in config_data.versions:
    original_stack = os.path.join(original_folder, stack.baseVersion)
    target_stack = os.path.join(target_folder, stack.version)

    desired_services = [service.name for service in stack.services]
    desired_services.append('stack_advisor.py')  # stack_advisor.py placed in stacks folder
    base_stack_services = os.listdir(os.path.join(original_stack, 'services'))
    ignored_services = [service for service in base_stack_services if service not in desired_services]

    def post_copy(src, target):
      # process metainfo.xml
      if target.endswith('metainfo.xml'):
        process_metainfo(target, config_data, stack_version_changes)
        return
      # process upgrade-x.x.xml
      _upgrade_re = re.compile('upgrade-(.*)\.xml')
      result = re.search(_upgrade_re, target)
      if result:
        target_version = result.group(1)
        process_upgrade_xml(target, target_version, config_data, stack_version_changes)
        return
      # process stack_advisor.py
      if target.endswith('stack_advisor.py'):
        process_stack_advisor(target, config_data, stack_version_changes)
        return
      # process repoinfo.xml
      if target.endswith('repoinfo.xml'):
        process_repoinfo_xml(target, config_data, stack_version_changes)
        return
      # process python files
      if target.endswith('.py'):
        process_py_files(target, config_data, stack_version_changes)
        return

        # TODO add more processing here for *.py files and others

    copy_tree(original_stack, target_stack, ignored_services, post_copy=post_copy)


def main(argv):
  HELP_STRING = 'WhiteLabelStackDefinition.py -c <config> -r <resources_folder> -o <output_folder>'
  config = ''
  resources_folder = ''
  output_folder = ''
  try:
    opts, args = getopt.getopt(argv, "hc:o:r:", ["config=", "out=", "resources="])
  except getopt.GetoptError:
    print HELP_STRING
    sys.exit(2)
  for opt, arg in opts:
    if opt == '-h':
      print HELP_STRING
      sys.exit()
    elif opt in ("-c", "--config"):
      config = arg
    elif opt in ("-r", "--resources"):
      resources_folder = arg
    elif opt in ("-o", "--out"):
      output_folder = arg
  if not config or not resources_folder or not output_folder:
    print HELP_STRING
    sys.exit(2)
  copy_stacks(resources_folder, output_folder, _named_dict(json.load(open(config, "r"))))


if __name__ == "__main__":
  main(sys.argv[1:])