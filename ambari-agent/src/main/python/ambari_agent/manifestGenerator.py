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

import json
import os.path
import logging
from uuid import getnode as get_mac
from shell import shellRunner
from datetime import datetime
from AmbariConfig import AmbariConfig


logger = logging.getLogger()

non_global_configuration_types = ["hdfs-site", "core-site", 
                          "mapred-queue-acls",
                             "hadoop-policy", "mapred-site", 
                             "capacity-scheduler", "hbase-site",
                             "hbase-policy", "hive-site", "oozie-site", 
                             "webhcat-site", "hdfs-exclude-file"]

#read static imports from file and write them to manifest
def writeImports(outputFile, modulesdir, inputFileName='imports.txt'):
  inputFile = open(inputFileName, 'r')
  logger.info("Modules dir is " + modulesdir)
  outputFile.write('#' + datetime.now().strftime('%d.%m.%Y %H:%M:%S') + os.linesep)
  for line in inputFile:
    modulename = line.rstrip()
    line = "import '" + modulesdir + os.sep + modulename + "'" + os.linesep
    outputFile.write(line)
    
  inputFile.close()

def generateManifest(parsedJson, fileName, modulesdir, ambariconfig):
  logger.info("JSON Received:")
  logger.info(json.dumps(parsedJson, sort_keys=True, indent=4))
#reading json
  hostname = parsedJson['hostname']
  clusterHostInfo = {} 
  if 'clusterHostInfo' in parsedJson:
    if parsedJson['clusterHostInfo']:
      clusterHostInfo = parsedJson['clusterHostInfo']
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
  #writing manifest
  manifest = open(fileName, 'w')
  #Check for Ambari Config and make sure you pick the right imports file
  importsfile = "imports.txt"
  if ambariconfig.has_option('puppet', 'imports_file') :
    importsfile = ambariconfig.get('puppet', 'imports_file')
    
  logger.info("Using imports file " + importsfile)   
  #writing imports from external static file
  writeImports(outputFile=manifest, modulesdir=modulesdir, inputFileName=importsfile)
  
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
     
  manifest.close()
    
  
  #read dictionary
def readDict(file, separator='='):
  result = dict()
  
  for line in file :
    dictTuple = line.partition(separator)
    result[dictTuple[0].strip()] = dictTuple[2].strip()
  
  return result
  

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
  for flatConfigName in flatConfigs.iterkeys():
    for flatConfig in flatConfigs[flatConfigName].iterkeys():
      flatDict[flatConfig] = flatConfigs[flatConfigName][flatConfig]
  for gconfigKey in flatDict.iterkeys():
    outputFile.write('$' + gconfigKey + ' = "' + flatDict[gconfigKey] + '"' + os.linesep)

#write xml configurations
def writeNonGlobalConfigurations(outputFile, xmlConfigs):
  outputFile.write('$configuration =  {\n')

  for configName in xmlConfigs.iterkeys():

    config = xmlConfigs[configName]
    
    outputFile.write(configName + '=> {\n')
    coma = ''
    for configParam in config.iterkeys():
      outputFile.write(coma + '"' + configParam + '" => \'' + config[configParam] + '\'')
      coma = ',\n'

    outputFile.write('\n},\n')
    
  outputFile.write('\n}\n')

#write node tasks
def writeTasks(outputFile, roles, ambariconfig, clusterHostInfo=None, 
               hostname="localhost"):
  #reading dictionaries
  rolestoclass = "rolesToClass.dict"
  if ambariconfig.has_option('puppet','roles_to_class'):
    rolestoclass = ambariconfig.get('puppet', 'roles_to_class')
                              
  rolesToClassFile = open(rolestoclass, 'r')
  rolesToClass = readDict(rolesToClassFile)
  rolesToClassFile.close()

  servicestates = "serviceStates.dict"
  if ambariconfig.has_option('puppet','service_states'):
    servicestates = ambariconfig.get('puppet', 'service_states')
                              
  serviceStatesFile =  open(servicestates, 'r')
  serviceStates = readDict(serviceStatesFile)
  serviceStatesFile.close()

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

