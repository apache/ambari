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
import re


class RangeradminV2:
  sInstance = None

  def __init__(self, url='http://localhost:6080'):
    
    self.base_url = url
    self.url_login = self.base_url + '/login.jsp'
    self.url_login_post = self.base_url + '/j_spring_security_check'
    self.url_repos = self.base_url + '/service/assets/assets'
    self.url_repos_pub = self.base_url + '/service/public/v2/api/service'
    self.url_policies = self.base_url + '/service/public/v2/api/policy'
    self.url_groups = self.base_url + '/service/xusers/groups'
    self.url_users = self.base_url + '/service/xusers/users'
    self.url_sec_users = self.base_url + '/service/xusers/secure/users'

  def get_repository_by_name_urllib2(self, name, component, status, usernamepassword):
    """
    :param name: name of the component, from which, function will search in list of repositories
    :param component:, component for which repository has to be checked
    :param status: active or inactive
    :param usernamepassword: user credentials using which repository needs to be searched. 
    :return: Returns Ranger repository object if found otherwise None
    """
    try:
      search_repo_url = self.url_repos_pub + "?name=" + name + "&type=" + component + "&status=" + status
      request = urllib2.Request(search_repo_url)
      base_64_string = base64.encodestring(usernamepassword).replace('\n', '')
      request.add_header("Content-Type", "application/json")
      request.add_header("Accept", "application/json")
      request.add_header("Authorization", "Basic {0}".format(base_64_string))
      result = urllib2.urlopen(request)
      response_code = result.getcode()
      response = json.loads(result.read())
      if response_code == 200 and len(response) > 0:
        for repo in response:
          repo_dump = json.loads(json.JSONEncoder().encode(repo))
          if repo_dump['name'].lower() == name.lower():
            return repo_dump
        return None
      else:
        return None
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        Logger.error("Error getting {0} repository for component {1}. Http status code - {2}. \n {3}".format(name, component, e.code, e.read()))
      else:
        Logger.error("Error getting {0} repository for component {1}. Reason - {2}.".format(name, component, e.reason))
      return None
    except httplib.BadStatusLine:
      Logger.error("Ranger Admin service is not reachable, please restart the service and then try again")
      return None
      
    
  def create_ranger_repository(self, component, repo_name, repo_properties, 
                               ambari_ranger_admin, ambari_ranger_password,
                               admin_uname, admin_password, policy_user):
    response_code = self.check_ranger_login_urllib2(self.base_url)
    repo_data = json.dumps(repo_properties)
    ambari_ranger_password = unicode(ambari_ranger_password)
    admin_password = unicode(admin_password)
    ambari_username_password_for_ranger = format('{ambari_ranger_admin}:{ambari_ranger_password}')
    
    if response_code is not None and response_code == 200:
      user_resp_code = self.create_ambari_admin_user(ambari_ranger_admin, ambari_ranger_password, format("{admin_uname}:{admin_password}"))
      if user_resp_code is not None and user_resp_code == 200:
        repo = self.get_repository_by_name_urllib2(repo_name, component, 'true', ambari_username_password_for_ranger)
        if repo is not None:
          Logger.info('{0} Repository {1} exist'.format(component.title(), repo['name']))
        else:
          response = self.create_repository_urllib2(repo_data, ambari_username_password_for_ranger)
          if response is not None:
            Logger.info('{0} Repository created in Ranger admin'.format(component.title()))
          else:
            Logger.error('{0} Repository creation failed in Ranger admin'.format(component.title()))
      else:
        Logger.error('Ambari admin user creation failed')
          
  def create_repository_urllib2(self, data, usernamepassword):
    """
    :param data: json object to create repository
    :param usernamepassword: user credentials using which repository needs to be searched. 
    :return: Returns created Ranger repository object
    """
    try:
      search_repo_url = self.url_repos_pub
      base_64_string = base64.encodestring('{0}'.format(usernamepassword)).replace('\n', '')
      headers = {
        'Accept': 'application/json',
        "Content-Type": "application/json"
      }
      request = urllib2.Request(search_repo_url, data, headers)
      request.add_header("Authorization", "Basic {0}".format(base_64_string))
      result = urllib2.urlopen(request)
      response_code = result.getcode()
      response = json.loads(json.JSONEncoder().encode(result.read()))

      if response_code == 200:
        Logger.info('Repository created Successfully')
        return response
      else:
        Logger.error('Repository creation failed')
        return None
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        Logger.error("Error creating repository. Http status code - {0}. \n {1}".format(e.code, e.read()))
      else:
        Logger.error("Error creating repository. Reason - {0}.".format(e.reason))
      return None
    except httplib.BadStatusLine:
      Logger.error("Ranger Admin service is not reachable, please restart the service and then try again")
      return None

  def check_ranger_login_urllib2(self, url):
    """
    :param url: ranger admin host url
    :param usernamepassword: user credentials using which repository needs to be searched. 
    :return: Returns login check response 
    """
    try:
      response = urllib2.urlopen(url)
      response_code = response.getcode()
      return response_code
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        Logger.error("Connection failed to Ranger Admin. Http status code - {0}. \n {1}".format(e.code, e.read()))
      else:
        Logger.error("Connection failed to Ranger Admin. Reason - {0}.".format(e.reason))
      return None
    except httplib.BadStatusLine, e:
      Logger.error("Ranger Admin service is not reachable, please restart the service and then try again")
      return None

  def create_ambari_admin_user(self,ambari_admin_username, ambari_admin_password,usernamepassword):
    """
    :param ambari_admin_username: username of user to be created 
    :param ambari_admin_username: user password of user to be created 
    :return: Returns user credentials if user exist otherwise rerutns credentials of  created user.
    """
    flag_ambari_admin_present = False
    match = re.match('[a-zA-Z0-9_\S]+$', ambari_admin_password)
    if match is None:
      raise Fail('Invalid password given for Ranger Admin user for Ambari')
    try:
      url =  self.url_users + '?name=' + str(ambari_admin_username)
      request = urllib2.Request(url)
      base_64_string = base64.encodestring(usernamepassword).replace('\n', '')
      request.add_header("Content-Type", "application/json")
      request.add_header("Accept", "application/json")
      request.add_header("Authorization", "Basic {0}".format(base_64_string))
      result = urllib2.urlopen(request)
      response_code = result.getcode()
      response = json.loads(result.read())
      if response_code == 200 and len(response['vXUsers']) >= 0:
        for vxuser in response['vXUsers']:
          if vxuser['name'] == ambari_admin_username:
            flag_ambari_admin_present = True
            break
          else:
            flag_ambari_admin_present = False

        if flag_ambari_admin_present:
          Logger.info(ambari_admin_username + ' user already exists.')
          return response_code
        else:
          Logger.info(ambari_admin_username + ' user is not present, creating user using given configurations')
          url = self.url_sec_users
          admin_user = dict()
          admin_user['status'] = 1
          admin_user['userRoleList'] = ['ROLE_SYS_ADMIN']
          admin_user['name'] = ambari_admin_username
          admin_user['password'] = ambari_admin_password
          admin_user['description'] = ambari_admin_username
          admin_user['firstName'] = ambari_admin_username
          data =  json.dumps(admin_user)
          base_64_string = base64.encodestring('{0}'.format(usernamepassword)).replace('\n', '')
          headers = {
            'Accept': 'application/json',
            "Content-Type": "application/json"
          }
          request = urllib2.Request(url, data, headers)
          request.add_header("Authorization", "Basic {0}".format(base_64_string))
          result = urllib2.urlopen(request)
          response_code =  result.getcode()
          response = json.loads(json.JSONEncoder().encode(result.read()))
          if response_code == 200 and response is not None:
            Logger.info('Ambari admin user creation successful.')
            return response_code
          else:
            Logger.error('Ambari admin user creation failed.')
            return None
      else:
        return None
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        Logger.error("Error creating ambari admin user. Http status code - {0}. \n {1}".format(e.code, e.read()))
        return None
      else:
        Logger.error("Error creating ambari admin user. Reason - {0}.".format(e.reason))
        return None
    except httplib.BadStatusLine:
      Logger.error("Ranger Admin service is not reachable, please restart the service and then try again")
      return None
