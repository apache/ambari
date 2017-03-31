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
from resource_management import Logger

def __parse_component_mappings(component_mappings):
  components = list()
  component_mappings_list = component_mappings.split(';')
  if component_mappings_list and len(component_mappings_list) > 0:
    metadata_list = map(lambda x : x.split(':'), component_mappings_list)
    if metadata_list and len(metadata_list) > 0:
      for metadata in metadata_list:
        if (len(metadata) == 2):
          logids = metadata[1].split(',')
          components.extend(logids)
          Logger.info("Found logids for logsearch component %s - (%s) " % (metadata[0], metadata[1]))
  return components

def get_logsearch_meta_configs(configurations):
  logsearch_meta_configs = {}
  for key, value in configurations.iteritems():  # iter on both keys and values
    if str(key).endswith('logsearch-conf'):
      logsearch_meta_configs[key] = value
      Logger.info("Found logsearch config entry : " + key)
  return logsearch_meta_configs

def get_logfeeder_metadata(logsearch_meta_configs):
  """
  get logfeeder pattern metadata list, an element: (e.g.) :
  ['service_config_name' : 'pattern json content']
  """
  logfeeder_contents = {}
  for key, value in logsearch_meta_configs.iteritems():
    if 'content' in logsearch_meta_configs[key] and logsearch_meta_configs[key]['content'].strip():
      logfeeder_contents[key] = logsearch_meta_configs[key]['content']
      Logger.info("Found logfeeder pattern content in " + key)
  return logfeeder_contents

def get_logsearch_metadata(logsearch_meta_configs):
  """
  get logsearch metadata list, an element (e.g.) :
  ['service_name_key' : {component1 : [logid1, logid2]}, {component2 : [logid1, logid2]}]
  """
  logsearch_service_component_mappings = {}
  for key, value in logsearch_meta_configs.iteritems():
    if 'service_name' in logsearch_meta_configs[key] and 'component_mappings' in logsearch_meta_configs[key]:
      service_name = logsearch_meta_configs[key]['service_name']
      component_mappings = __parse_component_mappings(logsearch_meta_configs[key]['component_mappings'])
      if service_name.strip() and component_mappings:
        logsearch_service_component_mappings[service_name] = component_mappings
    if 'service_component_mappings' in logsearch_meta_configs[key]:
      service_component_mappings = logsearch_meta_configs[key]['service_component_mappings']
      if service_component_mappings.strip():
        for service_component_mapping in service_component_mappings.split('|'):
          tokens = service_component_mapping.split('=')
          service_name = tokens[0]
          component_mappings = __parse_component_mappings(tokens[1])
          if service_name.strip() and component_mappings:
            logsearch_service_component_mappings[service_name] = component_mappings

  return logsearch_service_component_mappings

