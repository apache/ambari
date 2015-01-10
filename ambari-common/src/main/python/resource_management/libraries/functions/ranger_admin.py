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
import time
import pycurl
import sys
from StringIO import StringIO as BytesIO
import json
from resource_management.core.logger import Logger

class Rangeradmin:
  sInstance = None
  def __init__(self, url= 'http://localhost:6080'):
    
    self.baseUrl      =  url 
    self.urlLogin     = self.baseUrl + '/login.jsp'
    self.urlLoginPost = self.baseUrl + '/j_spring_security_check'
    self.urlRepos     = self.baseUrl + '/service/assets/assets'
    self.urlReposPub  = self.baseUrl + '/service/public/api/repository'
    self.urlPolicies  = self.baseUrl + '/service/assets/resources'
    self.urlGroups    = self.baseUrl + '/service/xusers/groups'
    self.urlUsers     = self.baseUrl + '/service/xusers/users'   
    self.urlSecUsers  = self.baseUrl + '/service/xusers/secure/users'   

    self.session    = None
    self.isLoggedIn = False

  def get_repository_by_name_pycurl(self, name, component, status, usernamepassword):
    searchRepoURL = self.urlReposPub + "?name=" + name + "&type=" + component + "&status=" + status
    responseCode, response = self.call_pycurl_request(url = searchRepoURL,data='',method='get',usernamepassword=usernamepassword)

    if response is None:
      return None
    elif responseCode == 200: 
      repos = json.loads(response)
      if repos is not None and len(repos['vXRepositories']) > 0:
        for repo in repos['vXRepositories']:
          repoDump = json.loads(json.JSONEncoder().encode(repo))
          if repoDump['name'] == name:
            return repoDump
        return None            
    else:
      Logger.error('Error occurred while creating repository')
      return None

  def create_repository_pycurl(self, data, usernamepassword):
    searchRepoURL = self.urlReposPub
    responseCode, response = self.call_pycurl_request(url =searchRepoURL, data=data, method='post', usernamepassword=usernamepassword)

    if response is None:
      return None
    elif responseCode != 200:
      Logger.info('Request for repository is not saved ,response is : %s', response)
    elif responseCode == 200:
      Logger.info('Repository created Successfully')
      return response
    else:
      return None  

  def call_pycurl_request(self, url, data, method, usernamepassword):
    buffer = BytesIO()
    header = BytesIO()
    url = str(url)
    # Creating PyCurl Requests
    c = pycurl.Curl()
    c.setopt(pycurl.URL,url)
    c.setopt(pycurl.HTTPHEADER, ['Content-Type: application/json','Accept: application/json'])
    c.setopt(pycurl.USERPWD, usernamepassword)
    c.setopt(pycurl.VERBOSE, 0)
    c.setopt(pycurl.WRITEFUNCTION ,buffer.write )
    c.setopt(pycurl.HEADERFUNCTION,header.write)
    c.setopt(pycurl.CONNECTTIMEOUT, 60)
    # setting proper method and parameters
    if method == 'get':
      c.setopt(pycurl.HTTPGET, 1)
    elif method == 'post':
      c.setopt(pycurl.POST, 1)
      c.setopt(pycurl.POSTFIELDS, data)
    elif method == 'put':
      c.setopt(pycurl.CUSTOMREQUEST, "PUT")
      c.setopt(pycurl.POSTFIELDS, str(data))
    elif method == 'delete':
      c.setopt(pycurl.CUSTOMREQUEST, "DELETE")
      c.setopt(pycurl.POSTFIELDS, str(data))
    else:
      Logger.error('Invalid option given for curl request')
    
    try:
      # making request
      c.perform()
      # getting response
      responseCode = c.getinfo(pycurl.HTTP_CODE)
      response = buffer.getvalue()
      headerResponse = header.getvalue()
      c.close()
      buffer.close()
      header.close()
      return responseCode, response
    except Exception, e:
        Logger.error(str(e))
        if c is not None:
          c.close()		 
    return None, None
