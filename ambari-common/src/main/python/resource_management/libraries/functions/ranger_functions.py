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
import sys
from StringIO import StringIO as BytesIO
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
from resource_management.core.logger import Logger
import urllib2, base64, httplib
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.format import format

class Rangeradmin:
  sInstance = None

  def __init__(self, url='http://localhost:6080'):

    self.baseUrl = url
    self.urlLogin = self.baseUrl + '/login.jsp'
    self.urlLoginPost = self.baseUrl + '/j_spring_security_check'
    self.urlRepos = self.baseUrl + '/service/assets/assets'
    self.urlReposPub = self.baseUrl + '/service/public/api/repository'
    self.urlPolicies = self.baseUrl + '/service/public/api/policy'
    self.urlGroups = self.baseUrl + '/service/xusers/groups'
    self.urlUsers = self.baseUrl + '/service/xusers/users'
    self.urlSecUsers = self.baseUrl + '/service/xusers/secure/users'

    self.session = None
    self.isLoggedIn = False

  def get_repository_by_name_urllib2(self, name, component, status, usernamepassword):
    try:
      searchRepoURL = self.urlReposPub + "?name=" + name + "&type=" + component + "&status=" + status
      request = urllib2.Request(searchRepoURL)
      base64string = base64.encodestring(usernamepassword).replace('\n', '')
      request.add_header("Content-Type", "application/json")
      request.add_header("Accept", "application/json")
      request.add_header("Authorization", "Basic {0}".format(base64string))
      result = urllib2.urlopen(request)
      response_code = result.getcode()
      response = json.loads(result.read())

      if response_code == 200 and len(response['vXRepositories']) > 0:
        for repo in response['vXRepositories']:
          repoDump = json.loads(json.JSONEncoder().encode(repo))
          if repoDump['name'] == name:
            return repoDump
        return None
      else:
        return None
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        Logger.error("HTTP Code: {0}".format(e.code))
        Logger.error("HTTP Data: {0}".format(e.read()))
      else:
        Logger.error("Error : {0}".format(e.reason))
      return None
    except httplib.BadStatusLine:
      Logger.error("Ranger Admin service is not reachable, please restart the service and then try again")
      return None
    
    
    
  def create_ranger_repository(self, component, repo_name, repo_properties, 
                               ambari_ranger_admin, ambari_ranger_password,
                               admin_uname, admin_password, policy_user):
    response_code, response_recieved = self.check_ranger_login_urllib2(self.urlLogin, 'test:test')
    repo_data = json.dumps(repo_properties)
    
    if response_code is not None and response_code == 200:
      ambari_ranger_admin, ambari_ranger_password = self.create_ambari_admin_user(ambari_ranger_admin, ambari_ranger_password, format("{admin_uname}:{admin_password}"))
      ambari_username_password_for_ranger = ambari_ranger_admin + ':' + ambari_ranger_password
      if ambari_ranger_admin != '' and ambari_ranger_password != '':
        retryCount = 0
        while retryCount <= 5:
          repo = self.get_repository_by_name_urllib2(repo_name, component, 'true', ambari_username_password_for_ranger)
          if repo and repo['name'] == repo_name:
            Logger.info('{0} Repository exist'.format(component.title()))
            break
          else:
            response = self.create_repository_urllib2(repo_data, ambari_username_password_for_ranger, policy_user)
            if response is not None:
              Logger.info('{0} Repository created in Ranger admin'.format(component.title()))
              break
            else:
              if retryCount < 5:
                Logger.info("Retry Repository Creation is being called")
                retryCount += 1
              else:
                raise Fail('{0} Repository creation failed in Ranger admin'.format(component.title()))
      else:
        raise Fail('Ambari admin username and password are blank ')
          
  def create_repository_urllib2(self, data, usernamepassword, policy_user):
    try:
      searchRepoURL = self.urlReposPub
      base64string = base64.encodestring('{0}'.format(usernamepassword)).replace('\n', '')
      headers = {
        'Accept': 'application/json',
        "Content-Type": "application/json"
      }
      request = urllib2.Request(searchRepoURL, data, headers)
      request.add_header("Authorization", "Basic {0}".format(base64string))
      result = urllib2.urlopen(request)
      response_code = result.getcode()
      response = json.loads(json.JSONEncoder().encode(result.read()))
      if response_code == 200:
        Logger.info('Repository created Successfully')
        # Get Policies
        repoData = json.loads(data)
        repoName = repoData['name']
        typeOfPolicy = repoData['repositoryType']
        ##Get Policies by repo name
        policyList = self.get_policy_by_repo_name(name=repoName, component=typeOfPolicy, status="true",
                                                  usernamepassword=usernamepassword)
        if policyList is not None and (len(policyList)) > 0:
          policiesUpdateCount = 0
          for policy in policyList:
            updatedPolicyObj = self.get_policy_params(typeOfPolicy, policy, policy_user)
            policyResCode, policyResponse = self.update_ranger_policy(updatedPolicyObj['id'],
                                                                      json.dumps(updatedPolicyObj), usernamepassword)
            if policyResCode == 200:
              policiesUpdateCount = policiesUpdateCount + 1
            else:
              Logger.info('Policy Update failed')
              ##Check for count of updated policies
          if len(policyList) == policiesUpdateCount:
            Logger.info(
              "Ranger Repository created successfully and policies updated successfully providing ambari-qa user all permissions")
            return response
          else:
            return None
        else:
          Logger.info("Policies not found for the newly created Repository")
        return None
      else:
        Logger.info('Repository creation failed')
        return None
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        Logger.error("HTTP Code: {0}".format(e.code))
        Logger.error("HTTP Data: {0}".format(e.read()))
      else:
        Logger.error("Error: {0}".format(e.reason))
      return None
    except httplib.BadStatusLine:
      Logger.error("Ranger Admin service is not reachable, please restart the service and then try again")
      return None

  def check_ranger_login_urllib2(self, url, usernamepassword):
    try:
      request = urllib2.Request(url)
      base64string = base64.encodestring(usernamepassword).replace('\n', '')
      request.add_header("Content-Type", "application/json")
      request.add_header("Accept", "application/json")
      request.add_header("Authorization", "Basic {0}".format(base64string))
      result = urllib2.urlopen(request)
      response = result.read()
      response_code = result.getcode()
      return response_code, response
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        Logger.error("HTTP Code: {0}".format(e.code))
        Logger.error("HTTP Data: {0}".format(e.read()))
      else:
        Logger.error("Error : {0}".format(e.reason))
      return None, None
    except httplib.BadStatusLine, e:
      Logger.error("Ranger Admin service is not reachable, please restart the service and then try again")
      return None, None

  def get_policy_by_repo_name(self, name, component, status, usernamepassword):
    try:
      searchPolicyURL = self.urlPolicies + "?repositoryName=" + name + "&repositoryType=" + component + "&isEnabled=" + status
      request = urllib2.Request(searchPolicyURL)
      base64string = base64.encodestring(usernamepassword).replace('\n', '')
      request.add_header("Content-Type", "application/json")
      request.add_header("Accept", "application/json")
      request.add_header("Authorization", "Basic {0}".format(base64string))
      result = urllib2.urlopen(request)
      response_code = result.getcode()
      response = json.loads(result.read())
      if response_code == 200 and len(response['vXPolicies']) > 0:
        return response['vXPolicies']
      else:
        return None
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        Logger.error("HTTP Code: {0}".format(e.code))
        Logger.error("HTTP Data: {0}".format(e.read()))
      else:
        Logger.error("Error: {0}".format(e.reason))
      return None
    except httplib.BadStatusLine:
      Logger.error("Ranger Admin service is not reachable, please restart the service and then try again")
      return None

  def update_ranger_policy(self, policyId, data, usernamepassword):
    try:
      searchRepoURL = self.urlPolicies + "/" + str(policyId)
      base64string = base64.encodestring('{0}'.format(usernamepassword)).replace('\n', '')
      headers = {
        'Accept': 'application/json',
        "Content-Type": "application/json"
      }
      request = urllib2.Request(searchRepoURL, data, headers)
      request.add_header("Authorization", "Basic {0}".format(base64string))
      request.get_method = lambda: 'PUT'
      result = urllib2.urlopen(request)
      response_code = result.getcode()
      response = json.loads(json.JSONEncoder().encode(result.read()))
      if response_code == 200:
        Logger.info('Policy updated Successfully')
        return response_code, response
      else:
        Logger.error('Update Policy failed')
        return None, None
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        Logger.error("HTTP Code: {0}".format(e.code))
        Logger.error("HTTP Data: {0}".format(e.read()))
      else:
        Logger.error("Error: {0}".format(e.reason))
      return None, None
    except httplib.BadStatusLine:
      Logger.error("Ranger Admin service is not reachable, please restart the service and then try again")
      return None, None

  def get_policy_params(self, typeOfPolicy, policyObj, policy_user):

    typeOfPolicy = typeOfPolicy.lower()
    if typeOfPolicy == "hdfs":
      policyObj['permMapList'] = [{'userList': [policy_user], 'permList': ['read', 'write', 'execute', 'admin']}]
    elif typeOfPolicy == "hive":
      policyObj['permMapList'] = [{'userList': [policy_user],
                                   'permList': ['select', 'update', 'create', 'drop', 'alter', 'index', 'lock', 'all',
                                                'admin']}]
    elif typeOfPolicy == "hbase":
      policyObj['permMapList'] = [{'userList': [policy_user], 'permList': ['read', 'write', 'create', 'admin']}]
    elif typeOfPolicy == "knox":
      policyObj['permMapList'] = [{'userList': [policy_user], 'permList': ['allow', 'admin']}]
    elif typeOfPolicy == "storm":
      policyObj['permMapList'] = [{'userList': [policy_user],
                                   'permList': ['submitTopology', 'fileUpload', 'getNimbusConf', 'getClusterInfo',
                                                'fileDownload', 'killTopology', 'rebalance', 'activate', 'deactivate',
                                                'getTopologyConf', 'getTopology', 'getUserTopology',
                                                'getTopologyInfo', 'uploadNewCredential', 'admin']}]
    return policyObj


  def create_ambari_admin_user(self,ambari_admin_username, ambari_admin_password,usernamepassword):
    try:
      url =  self.urlUsers + '?name=' + str(ambari_admin_username)
      request = urllib2.Request(url)
      base64string = base64.encodestring(usernamepassword).replace('\n', '')
      request.add_header("Content-Type", "application/json")
      request.add_header("Accept", "application/json")
      request.add_header("Authorization", "Basic {0}".format(base64string))
      result = urllib2.urlopen(request)
      response_code =  result.getcode()
      response = json.loads(result.read())
      if response_code == 200 and len(response['vXUsers']) >= 0:
        ambari_admin_username = ambari_admin_username
        flag_ambari_admin_present = False
        for vxuser in response['vXUsers']:
          rangerlist_username = vxuser['name']
          if rangerlist_username == ambari_admin_username:
            flag_ambari_admin_present = True
            break
          else:
            flag_ambari_admin_present = False

        if flag_ambari_admin_present:
          Logger.info(ambari_admin_username + ' user already exists, using existing user from configurations.')
          return ambari_admin_username,ambari_admin_password
        else:
          Logger.info(ambari_admin_username + ' user is not present, creating user using given configurations')
          url = self.urlSecUsers
          admin_user = dict()
          admin_user['status'] = 1
          admin_user['userRoleList'] = ['ROLE_SYS_ADMIN']
          admin_user['name'] = ambari_admin_username
          admin_user['password'] = ambari_admin_password
          admin_user['description'] = ambari_admin_username
          admin_user['firstName'] = ambari_admin_username
          data =  json.dumps(admin_user)
          base64string = base64.encodestring('{0}'.format(usernamepassword)).replace('\n', '')
          headers = {
            'Accept': 'application/json',
            "Content-Type": "application/json"
          }
          request = urllib2.Request(url, data, headers)
          request.add_header("Authorization", "Basic {0}".format(base64string))
          result = urllib2.urlopen(request)
          response_code =  result.getcode()
          response = json.loads(json.JSONEncoder().encode(result.read()))
          if response_code == 200 and response is not None:
            Logger.info('Ambari admin user creation successful.')
          else:
            Logger.info('Ambari admin user creation failed,setting username and password as blank')
            ambari_admin_username = ''
            ambari_admin_password = ''
          return ambari_admin_username,ambari_admin_password
      else:
        return '',''

    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        Logger.error("HTTP Code: {0}".format(e.code))
        Logger.error("HTTP Data: {0}".format(e.read()))
        return '',''
      else:
        Logger.error("Error: {0}".format(e.reason))
        return '',''
    except httplib.BadStatusLine:
      Logger.error("Ranger Admin service is not reachable, please restart the service and then try again")
      return '',''
