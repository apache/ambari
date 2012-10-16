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

logger = logging.getLogger()

  #read static imports from file and write them to manifest
def writeImports(outputFile, inputFileName='imports.txt'):
  inputFile = open(inputFileName, 'r')
  modulesdir = os.path.abspath(os.getcwd() + "../../../puppet/modules/")
  logger.info("Modules dir is " + modulesdir)
  for line in inputFile:
    modulename = line.rstrip('\n')
    line = "import '" + modulesdir + "/" + modulename + "'\n"
    outputFile.write(line)
    
  inputFile.close()

def generateManifest(inputJsonStr):
#reading json
  parsedJson = json.loads(inputJsonStr)
  hostname = parsedJson['hostname']
  clusterHostInfo = parsedJson['clusterHostInfo']
  params = parsedJson['params']
  configurations = parsedJson['configurations']
  #hostAttributes = parsedJson['hostAttributes']
  roles = parsedJson['roleCommands']
  
#writing manifest
  manifest = open('site.pp', 'w')

  #writing imports from external static file
  writeImports(manifest)
  
  #writing nodes
  writeNodes(manifest, clusterHostInfo)
  
  #writing params from map
  writeParams(manifest, params)
  
  #writing config maps
  writeConfigurations(manifest, configurations)

  #writing host attributes
  #writeHostAttributes(manifest, hostAttributes)

  #writing task definitions 
  writeTasks(manifest, roles)
     
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
  for node in clusterHostInfo.iterkeys():
    outputFile.write('$' + node + '= [')
    coma = ''
    
    for value in clusterHostInfo[node]:
      outputFile.write(coma + '\'' + value + '\'')
      coma = ', '

    outputFile.write(']\n')

#write params
def writeParams(outputFile, params):
  for param in params.iterkeys():
    outputFile.write('$' +  param + '="' + params[param] + '"\n')

#write host attributes
def writeHostAttributes(outputFile, hostAttributes):
  outputFile.write('$hostAttributes={\n')

  coma = ''
  for attribute in hostAttributes.iterkeys():
    outputFile.write(coma + '"' +  attribute + '" => "{' + hostAttributes[attribute] + '"}')
    coma = ',\n'

  outputFile.write('}\n')

#write configurations
def writeConfigurations(outputFile, configs):
  outputFile.write('$configuration =  {\n')

  for configName in configs.iterkeys():
    outputFile.write(configName + '=> {\n')
    config = configs[configName]
    
    coma = ''
    for configParam in config.iterkeys():
      outputFile.write(coma + '"' + configParam + '" => "' + config[configParam] + '"')
      coma = ',\n'

    outputFile.write('\n},\n')
    
  outputFile.write('\n}\n')

#write node tasks
def writeTasks(outputFile, roles):
  #reading dictionaries
  rolesToClassFile = open('rolesToClass.dict', 'r')
  rolesToClass = readDict(rolesToClassFile)
  rolesToClassFile.close()

  serviceStatesFile =  open('serviceStates.dict', 'r')
  serviceStates = readDict(serviceStatesFile)
  serviceStatesFile.close()

  outputFile.write('node /default/ {\n ')
  writeStages(outputFile, len(roles))
  stageNum = 1

  for role in roles :
    rolename = role['role']
    command = role['cmd']
    taskParams = role['roleParams']
    taskParamsNormalized = normalizeTaskParams(taskParams)
    taskParamsPostfix = ''
    
    if len(taskParamsNormalized) > 0 :
      taskParamsPostfix = ', ' + taskParamsNormalized
    
    className = rolesToClass[rolename]
    serviceState = serviceStates[command]
    
    outputFile.write('class {\'' + className + '\':' + ' stage => ' + str(stageNum) + 
                     ', service_state => ' + serviceState + taskParamsPostfix + '}\n')
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
    outputFile.write(arrow + 'stage{' + str(i) + ' :}')
    arrow = ' -> '
  
  outputFile.write('\n')
    
logging.basicConfig(level=logging.DEBUG)    
#test code
jsonFile = open('test.json', 'r')
jsonStr = jsonFile.read() 
generateManifest(jsonStr)
