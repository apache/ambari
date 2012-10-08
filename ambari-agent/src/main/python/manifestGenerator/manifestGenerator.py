import json

def generateManifest(inputJsonStr):
#reading json
  parsedJson = json.loads(inputJsonStr)
  hostname = parsedJson['hostname']
  clusterHostInfo = parsedJson['clusterHostInfo']
  params = parsedJson['params']
  configurations = parsedJson['configurations']
  hostAttributes = parsedJson['hostAttributes']
  roles = parsedJson['roles']
  
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

  #cycle here - writing host attributes
  writeHostAttributes(manifest, hostAttributes)

  #writing task definitions 
  writeTasks(manifest, roles)
     
  manifest.close()
    
  
  #read static imports from file and write them to manifest
  def writeImports(outputFile, inputFileName='imports.txt'):
    inputFile = open(inputFileName, 'r')
    
    for line in inputFile:
      outputFile.write(line)
      
    inputFile.close()

  #write nodes
  def writeNodes(outputFile, clusterHostInfo):
    for node in clusterHostInfo.iterkeys():
      outputFile.write('$' + node + '= ['
    
      coma = ''
      for host in node:
        outputFile.write(coma + '\'' + host + '\'')
        coma = ', '

      outputFile.write(']\n'

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
    outputFile.write('$configuration =  {\n'
  
    for configName in configs.iterkeys():
      outputFile.write('$' + configName + '=> {\n')
      config = configs[configName]
      
      coma = ''
      for configParam in config.iterkeys():
        outputFile.write(coma + '"' + configParam + '" => "' + config[configParam] + '"')
        coma = ',\n'

      outputFile.write('\n}\n')
      
    outputFile.write('\n}\n'

  #write node tasks
  def writeTasks(outputFile, tasks):
    for task in tasks :
      nodename = task['role']
      command = task['roleCommand']
      taskParams = task['params']
    #TODO: write node task to file
      