#!/usr/bin/env python

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os, errno
import logging
import logging.handlers
import sys

logger = logging.getLogger()

class ConfigWriter:

  def shell(self, category, options):
    content = ""
    for key in options:
      content+="export "+key+"=\""+options[key]+"\"\n"
    return self.write("config/"+category+".sh", content)

  def xml(self, category, options):
    content = """<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
"""
    for key in options:
      content+="  <property>\n"
      content+="    <name>"+key+"</name>\n"
      content+="    <value>"+options[key]+"</value>\n"
      content+="  </property>\n"
    content+= "</configuration>\n"
    return self.write("config/"+category+".xml", content)

  def plist(self, category, options):
    content = ""
    for key in options:
      content+=key+"="+options[key]+"\n"
    return self.write("config/"+category+".properties", content)

  def write(self, path, content):
    try:
      f = open(path, 'w')
      f.write(content)
      f.close()
      result = { 'exitCode' : 0 }
    except Exception:
      result = { 'exitCode' : 1 }
    return result

def main():
  logger.setLevel(logging.DEBUG)
  formatter = logging.Formatter("%(asctime)s %(filename)s:%(lineno)d - %(message)s")
  stream_handler = logging.StreamHandler()
  stream_handler.setFormatter(formatter)
  logger.addHandler(stream_handler)
  try:
    print "Ambari Component Library"
  except Exception, err:
    logger.exception(str(err))
    
if __name__ == "__main__":
  main()
