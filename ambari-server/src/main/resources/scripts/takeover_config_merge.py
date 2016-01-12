#!/usr/bin/env python
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

import optparse
import sys
import os
import logging
import tempfile
import json
import base64
import time
import xml
import xml.etree.ElementTree as ET

logger = logging.getLogger('AmbariTakeoverConfigMerge')


class ConfigMerge:
  INPUT_DIR = '/etc/hadoop'
  OUTPUT_DIR = '/tmp'
  OUT_FILENAME = 'ambari_takeover_config_merge.out'
  JSON_FILENAME = 'ambari_takeover_config_merge.json'
  SUPPORTED_EXTENSIONS = ['.xml']
  SUPPORTED_FILENAME_ENDINGS = ['-site']

  config_files_map = {}

  def __init__(self, config_files_map):
    self.config_files_map = config_files_map

  @staticmethod
  def get_all_supported_files_grouped_by_name(extensions=SUPPORTED_EXTENSIONS):
    filePaths = {}
    for dirName, subdirList, fileList in os.walk(ConfigMerge.INPUT_DIR, followlinks=True):
      for file in fileList:
        root, ext = os.path.splitext(file)
        for filename_ending in ConfigMerge.SUPPORTED_FILENAME_ENDINGS:
          if root.endswith(filename_ending) and ext in extensions:
            if not file in filePaths:
              filePaths[file] = []
            filePaths[file].append(os.path.join(dirName, file))

    return filePaths

  # Used DOM parser to read data into a map
  def read_xml_data_to_map(self, path):
    configurations = {}
    tree = ET.parse(path)
    root = tree.getroot()
    for properties in root.getiterator('property'):
      name = properties.find('name')
      value = properties.find('value')

      if name != None:
        name_text = name.text if name.text else ""
      else:
        logger.warn("No name is found for one of the properties in {0}, ignoring it".format(path))
        continue

      if value != None:
        value_text = value.text if value.text else ""
      else:
        logger.warn("No value is found for \"{0}\" in {1}, using empty string for it".format(name_text, path))
        value_text = ""

      configurations[name_text] = value_text
    logger.debug("Following configurations found in {0}:\n{1}".format(path, configurations))
    return configurations

  @staticmethod
  def merge_configurations(filepath_to_configurations):
    configuration_information_dict = {}
    property_name_to_value_to_filepaths = {}
    merged_configurations = {}

    for path, configurations in filepath_to_configurations.iteritems():
      for configuration_name, value in configurations.iteritems():
        if not configuration_name in property_name_to_value_to_filepaths:
          property_name_to_value_to_filepaths[configuration_name] = {}
        if not value in property_name_to_value_to_filepaths[configuration_name]:
          property_name_to_value_to_filepaths[configuration_name][value] = []

        logger.debug("Iterating over '{0}' with value '{1}' in file '{2}'".format(configuration_name, value, path))
        property_name_to_value_to_filepaths[configuration_name][value].append(path)
        merged_configurations[configuration_name] = value

    return merged_configurations, property_name_to_value_to_filepaths

  @staticmethod
  def format_for_blueprint(configurations):
    all_configs = []
    for configuration_type, configuration_properties in configurations.iteritems():
      all_configs.append({})
      all_configs[-1][configuration_type] = configuration_properties

    return {
      "configurations": all_configs,
      "host_groups": [],
      "Blueprints": {}
    }

  @staticmethod
  def format_conflicts_output(property_name_to_value_to_filepaths):
    output = ""
    for property_name, value_to_filepaths in property_name_to_value_to_filepaths.iteritems():
      if len(value_to_filepaths) == 1:
        continue

      for value, filepaths in value_to_filepaths.iteritems():
        for filepath in filepaths:
          output += "| {0} | {1} | {2} |\n".format(property_name, filepath, value)

    return output

  def perform_merge(self):
    result_configurations = {}
    has_conflicts = False
    for filename, paths in self.config_files_map.iteritems():
      filepath_to_configurations = {}
      configuration_type = os.path.splitext(filename)[0]
      for path in paths:
        logger.debug("Read xml data from {0}".format(path))
        filepath_to_configurations[path] = self.read_xml_data_to_map(path)
      merged_configurations, property_name_to_value_to_filepaths = ConfigMerge.merge_configurations(
        filepath_to_configurations)

      conflicts_output = ConfigMerge.format_conflicts_output(property_name_to_value_to_filepaths)
      if conflicts_output:
        has_conflicts = True
        conflict_filename = os.path.join(self.OUTPUT_DIR, configuration_type + "-conflicts.txt")
        logger.warn(
          "You have configurations conflicts for {0}. Please check {1}".format(configuration_type, conflict_filename))
        with open(conflict_filename, "w") as fp:
          fp.write(conflicts_output)

      result_configurations[configuration_type] = merged_configurations

    result_json_file = os.path.join(self.OUTPUT_DIR, "blueprint.json")
    logger.info("Using '{0}' file as output for blueprint template".format(result_json_file))

    with open(result_json_file, 'w') as outfile:
      outfile.write(json.dumps(ConfigMerge.format_for_blueprint(result_configurations), sort_keys=True, indent=4,
                               separators=(',', ': ')))
    if has_conflicts:
      logger.info("Script finished with configurations conflicts, please resolve them before using the blueprint")
      return 1
    else:
      logger.info("Script successfully finished")
      return 0


def main():
  tempDir = tempfile.gettempdir()
  outputDir = os.path.join(tempDir)

  parser = optparse.OptionParser(usage="usage: %prog [options]")
  parser.set_description('This python program is an Ambari thin client and '
                         'supports Ambari cluster takeover by generating a '
                         'configuration json that can be used with a '
                         'blueprint.\n\nIt reads actual hadoop configs '
                         'from a target directory and produces an out file '
                         'with problems found that need to be addressed and '
                         'the json file which can be used to create the '
                         'blueprint.\n\nThis script only works with *-site.xml '
                         'files for now.'
                         )

  parser.add_option("-v", "--verbose", dest="verbose", action="store_true",
                    default=False, help="output verbosity.")
  parser.add_option("-o", "--outputdir", dest="outputDir", default=outputDir,
                    metavar="FILE", help="Output directory. [default: /tmp]")
  parser.add_option("-i", "--inputdir", dest="inputDir", help="Input directory.")

  (options, args) = parser.parse_args()

  # set verbose
  if options.verbose:
    logger.setLevel(logging.DEBUG)
  else:
    logger.setLevel(logging.INFO)

  ConfigMerge.INPUT_DIR = options.inputDir
  ConfigMerge.OUTPUT_DIR = options.outputDir

  logegr_file_name = os.path.join(ConfigMerge.OUTPUT_DIR, "takeover_config_merge.log")

  file_handler = logging.FileHandler(logegr_file_name, mode="w")
  formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
  file_handler.setFormatter(formatter)
  logger.addHandler(file_handler)
  stdout_handler = logging.StreamHandler(sys.stdout)
  stdout_handler.setLevel(logging.INFO)
  stdout_handler.setFormatter(formatter)
  logger.addHandler(stdout_handler)

  filePaths = ConfigMerge.get_all_supported_files_grouped_by_name()
  logger.info("Writing logs into '{0}' file".format(logegr_file_name))
  logger.debug("Following configuration files found:\n{0}".format(filePaths.items()))
  configMerge = ConfigMerge(filePaths)

  return configMerge.perform_merge()

if __name__ == "__main__":
  try:
    sys.exit(main())
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
