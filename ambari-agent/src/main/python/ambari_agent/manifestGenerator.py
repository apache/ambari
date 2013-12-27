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

import json
import os.path
import logging
from datetime import datetime
import pprint
import AmbariConfig
import hostname
from ambari_agent import AgentException

HOSTS_LIST_KEY = "all_hosts"
PING_PORTS_KEY = "all_ping_ports"

logger = logging.getLogger()

non_global_configuration_types = ["hdfs-site", "core-site", 
                             "mapred-queue-acls",
                             "hadoop-policy", "mapred-site", 
                             "capacity-scheduler", "hbase-site",
                             "hbase-policy", "hive-site", "oozie-site", 
                             "webhcat-site", "hdfs-exclude-file", "hue-site",
                             "yarn-site"]

# Converts from 1-3,5,6-8 to [1,2,3,5,6,7,8] 
def convertRangeToList(list):
  
  resultList = []

  for i in list:
      
    ranges = i.split(',')
    
    for r in ranges:
      rangeBounds = r.split('-')
      if len(rangeBounds) == 2:
        
        if not rangeBounds[0] or not rangeBounds[1]:
          raise AgentException.AgentException("Broken data in given range, expected - ""m-n"" or ""m"", got : " + str(r))
            
        
        resultList.extend(range(int(rangeBounds[0]), int(rangeBounds[1]) + 1))
      elif len(rangeBounds) == 1:
        resultList.append((int(rangeBounds[0])))
      else:
        raise AgentException.AgentException("Broken data in given range, expected - ""m-n"" or ""m"", got : " + str(r))
    
  return resultList

#Converts from ['1:0-2,4', '42:3,5-7'] to [1,1,1,42,1,42,42,42]
def convertMappedRangeToList(list):
    
  resultDict = {}
  
  for i in list:
    valueToRanges = i.split(":")
    if len(valueToRanges) <> 2:
      raise AgentException.AgentException("Broken data in given value to range, expected format - ""value:m-n"", got - " + str(i))
    value = valueToRanges[0]
    rangesToken = valueToRanges[1]
    
    for r in rangesToken.split(','):
        
      rangeIndexes = r.split('-')
    
      if len(rangeIndexes) == 2:
          
        if not rangeIndexes[0] or not rangeIndexes[1]:
          raise AgentException.AgentException("Broken data in given value to range, expected format - ""value:m-n"", got - " + str(r))

        start = int(rangeIndexes[0])
        end = int(rangeIndexes[1])
        
        for k in range(start, end + 1):
          resultDict[k] = int(value)
        
        
      elif len(rangeIndexes) == 1:
        index = int(rangeIndexes[0])
        
        resultDict[index] = int(value)
       

  resultList = dict(sorted(resultDict.items())).values()
      
  return resultList

def decompressClusterHostInfo(clusterHostInfo):
  info = clusterHostInfo.copy()
  #Pop info not related to host roles  
  hostsList = info.pop(HOSTS_LIST_KEY)
  pingPorts = info.pop(PING_PORTS_KEY)

  decompressedMap = {}

  for k,v in info.items():
    # Convert from 1-3,5,6-8 to [1,2,3,5,6,7,8] 
    indexes = convertRangeToList(v)
    # Convert from [1,2,3,5,6,7,8] to [host1,host2,host3...]
    decompressedMap[k] = [hostsList[i] for i in indexes]
  
  #Convert from ['1:0-2,4', '42:3,5-7'] to [1,1,1,42,1,42,42,42]
  pingPorts = convertMappedRangeToList(pingPorts)
  
  #Convert all elements to str
  pingPorts = map(str, pingPorts)

  #Add ping ports to result
  decompressedMap[PING_PORTS_KEY] = pingPorts
  #Add hosts list to result
  decompressedMap[HOSTS_LIST_KEY] = hostsList
  
  return decompressedMap


#read static imports from file and write them to manifest
def writeImports(outputFile, modulesdir, importsList):
  logger.info("Modules dir is " + modulesdir)
  outputFile.write('#' + datetime.now().strftime('%d.%m.%Y %H:%M:%S') + os.linesep)
  for line in importsList:
    modulename = line.rstrip()
    line = "import '" + modulesdir + os.sep + modulename + "'" + os.linesep
    outputFile.write(line)


def generateManifest(parsedJson, fileName, modulesdir, ambariconfig):
  logger.debug("JSON Received:")
  logger.debug(json.dumps(parsedJson, sort_keys=True, indent=4))
#reading json
  hostname = parsedJson['hostname']
  clusterHostInfo = {} 
  if 'clusterHostInfo' in parsedJson:
    if parsedJson['clusterHostInfo']:
      clusterHostInfo = decompressClusterHostInfo(parsedJson['clusterHostInfo'])
  params = {}
  if 'hostLevelParams' in parsedJson: 
    if parsedJson['hostLevelParams']:
      params = parsedJson['hostLevelParams']
  configurations = {}
  if 'configurations' in parsedJson:
    if parsedJson['configurations']:
      configurations = parsedJson['configurations']
  nonGlobalConfigurationsKeys = non_global_configuration_types
  #hostAttributes = parsedJson['hostAttributes']
  roleParams = {}
  if 'roleParams' in parsedJson:
    if parsedJson['roleParams']:
      roleParams = parsedJson['roleParams']
  roles = [{'role' : parsedJson['role'],
            'cmd' : parsedJson['roleCommand'],
            'roleParams' : roleParams}]
  errMsg = ''
  try:
    #writing manifest
    manifest = open(fileName, 'w')
    #Change mode to make site.pp files readable to owner and group only
    os.chmod(fileName, 0660)

    #Check for Ambari Config and make sure you pick the right imports file

    #writing imports from external static file
    writeImports(outputFile=manifest, modulesdir=modulesdir, importsList=AmbariConfig.imports)

    #writing hostname
    writeHostnames(manifest)

    #writing nodes
    writeNodes(manifest, clusterHostInfo)

    #writing params from map
    writeParams(manifest, params, modulesdir)

    nonGlobalConfigurations = {}
    flatConfigurations = {}

    if configurations:
      for configKey in configurations.iterkeys():
        if configKey in nonGlobalConfigurationsKeys:
          nonGlobalConfigurations[configKey] = configurations[configKey]
        else:
          flatConfigurations[configKey] = configurations[configKey]

    #writing config maps
    if (nonGlobalConfigurations):
      writeNonGlobalConfigurations(manifest, nonGlobalConfigurations)
    if (flatConfigurations):
      writeFlatConfigurations(manifest, flatConfigurations)

    #writing host attributes
    #writeHostAttributes(manifest, hostAttributes)

    #writing task definitions
    writeTasks(manifest, roles, ambariconfig, clusterHostInfo, hostname)


  except TypeError:
    errMsg = 'Manifest can\'t be generated from the JSON \n' + \
                    json.dumps(parsedJson, sort_keys=True, indent=4)
    logger.error(errMsg)
  finally:
    manifest.close()

  return errMsg

def writeHostnames(outputFile):
  fqdn = hostname.hostname()
  public_fqdn = hostname.public_hostname()
  outputFile.write('$myhostname' + " = '" + fqdn + "'" + os.linesep)
  outputFile.write('$public_hostname' + " = '" + public_fqdn + "'" + os.linesep)

  #write nodes
def writeNodes(outputFile, clusterHostInfo):
  if clusterHostInfo.has_key('zookeeper_hosts'):
    clusterHostInfo['zookeeper_hosts'] = sorted(clusterHostInfo['zookeeper_hosts'])
  
  for node in clusterHostInfo.iterkeys():
    outputFile.write('$' + node + '= [')
    coma = ''
    
    for value in clusterHostInfo[node]:
      outputFile.write(coma + '\'' + value + '\'')
      coma = ', '

    outputFile.write(']\n')

#write params
def writeParams(outputFile, params, modulesdir):

  for paramName in params.iterkeys():
    if paramName == 'repo_info':     
      continue
      

    param = params[paramName]
    if type(param) is dict:

      outputFile.write('$' + paramName + '= {\n')

      coma = ''

      for subParam in param.iterkeys():
        outputFile.write(coma + '"' + subParam + '" => "' + param[subParam] + '"')
        coma = ',\n'

      outputFile.write('\n}\n')
    else:
      outputFile.write('$' +  paramName + '="' + param + '"\n')


#write host attributes
def writeHostAttributes(outputFile, hostAttributes):
  outputFile.write('$hostAttributes={\n')

  coma = ''
  for attribute in hostAttributes.iterkeys():
    outputFile.write(coma + '"' +  attribute + '" => "{' + hostAttributes[attribute] + '"}')
    coma = ',\n'

  outputFile.write('}\n')

#write flat configurations
def writeFlatConfigurations(outputFile, flatConfigs):
  flatDict = {}
  logger.debug("Generating global configurations =>\n" + pprint.pformat(flatConfigs))
  for flatConfigName in flatConfigs.iterkeys():
    for flatConfig in flatConfigs[flatConfigName].iterkeys():
      flatDict[flatConfig] = flatConfigs[flatConfigName][flatConfig]
  for gconfigKey in flatDict.iterkeys():
    outputFile.write('$' + gconfigKey + " = '" + escape(flatDict[gconfigKey]) + "'" + os.linesep)

#write xml configurations
def writeNonGlobalConfigurations(outputFile, xmlConfigs):
  outputFile.write('$configuration =  {\n')

  for configName in xmlConfigs.iterkeys():
    config = xmlConfigs[configName]
    logger.debug("Generating " + configName + ", configurations =>\n" + pprint.pformat(config))
    outputFile.write(configName + '=> {\n')
    coma = ''
    for configParam in config.iterkeys():
      outputFile.write(coma + '"' + configParam + '" => \'' + escape(config[configParam]) + '\'')
      coma = ',\n'

    outputFile.write('\n},\n')
    
  outputFile.write('\n}\n')

#write node tasks
def writeTasks(outputFile, roles, ambariconfig, clusterHostInfo=None, 
               hostname="localhost"):
  #reading dictionaries
  rolesToClass = AmbariConfig.rolesToClass

  serviceStates = AmbariConfig.serviceStates

  outputFile.write('node /default/ {\n ')

  writeStages(outputFile, len(roles) + 1)
  stageNum = 1

  outputFile.write('class {\'hdp\': stage => ' + str(stageNum) + '}\n')
  stageNum = stageNum + 1
  # Need to hack for zookeeper since we need 
  zk_hosts = []
  for role in roles :
    rolename = role['role']
    command = role['cmd']
    taskParams = role['roleParams']
    if (rolename == 'ZOOKEEPER_SERVER'):
      zk_hosts = clusterHostInfo['zookeeper_hosts']
      # Sort the list in lexicographical order
      taskParams['myid'] = str(sorted(zk_hosts).index(hostname) + 1)
    
    taskParamsNormalized = normalizeTaskParams(taskParams)
    taskParamsPostfix = ''
    
    if len(taskParamsNormalized) > 0 :
      taskParamsPostfix = ', ' + taskParamsNormalized
    
    className = rolesToClass[rolename]
   
    if command in serviceStates:
      serviceState = serviceStates[command] 
      outputFile.write('class {\'' + className + '\':' +
                        ' stage => ' + str(stageNum) + 
                     ', service_state => ' + serviceState 
                     + taskParamsPostfix + '}\n')
    else:
      outputFile.write('class {\'' + className + '\':' + 
                       ' stage => ' + str(stageNum) + 
                       taskParamsPostfix + '}\n')

    stageNum = stageNum + 1
  outputFile.write('}\n')

def normalizeTaskParams(taskParams):
  result = ''
  coma = ''
  
  for paramName in taskParams.iterkeys():
    result = coma + result + paramName + ' => ' + taskParams[paramName]
    coma = ','
    
  return result
  
def writeStages(outputFile, numStages):
  arrow = ''
  
  for i in range(numStages):
    outputFile.write(arrow + 'stage{' + str(i + 1) + ' :}')
    arrow = ' -> '
  
  outputFile.write('\n')

#Escape special characters
def escape(param):
    return param.replace('\\', '\\\\').replace('\'', '\\\'')
  
def main():
  logging.basicConfig(level=logging.DEBUG)    
  #test code
  jsonFile = open('test.json', 'r')
  jsonStr = jsonFile.read() 
  modulesdir = os.path.abspath(os.getcwd() + ".." + os.sep + ".." + 
                               os.sep + ".." + os.sep + "puppet" + 
                               os.sep + "modules" + os.sep)
  inputJsonStr = jsonStr
  parsedJson = json.loads(inputJsonStr)
  generateManifest(parsedJson, 'site.pp', modulesdir)
  

if __name__ == '__main__':
  main()

