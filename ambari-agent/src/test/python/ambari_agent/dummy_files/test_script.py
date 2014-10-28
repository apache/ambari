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

def get_tokens():
  '''
  return a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  '''
  return ('{{foo-site/bar}}','{{foo-site/baz}}')
  

def execute(parameters=None, host_name=None):
  '''
  returns a tuple containing the result code and a pre-formatted result label
  '''
  if parameters is not None:
    if '{{foo-site/bar}}' in parameters:
      bar = parameters['{{foo-site/bar}}']
    
    if '{{foo-site/baz}}' in parameters:
      baz = parameters['{{foo-site/baz}}']
  
  label = "bar is {0}, baz is {1}".format(bar, baz)  
  return (('WARNING', [label]))
