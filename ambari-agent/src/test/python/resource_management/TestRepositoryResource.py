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

import os
import tempfile
from unittest import TestCase
from mock.mock import patch, MagicMock

from resource_management import *


class TestRepositoryResource(TestCase):
    @patch.object(System, "os_family", new='redhat')
    @patch("resource_management.libraries.providers.repository.File")
    def test_create_repo_redhat(self, file_mock):
        with Environment('/') as env:
            Repository('hadoop',
                       base_url='http://download.base_url.org/rpm/',
                       mirror_list='https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                       repo_file_name='Repository')

            self.assertTrue('hadoop' in env.resources['Repository'])
            defined_arguments = env.resources['Repository']['hadoop'].arguments
            expected_arguments = {'base_url': 'http://download.base_url.org/rpm/',
                                  'mirror_list': 'https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                                  'repo_file_name': 'Repository'}

            self.assertEqual(defined_arguments, expected_arguments)
            self.assertEqual(file_mock.call_args[0][0], '/etc/yum.repos.d/Repository.repo')

            template_item = file_mock.call_args[1]['content']
            template = str(template_item.name)
            expected_arguments.update({'repo_id': 'hadoop'})

            self.assertEqual(expected_arguments, template_item.context._dict)
            self.assertEqual("""[{{repo_id}}]
name={{repo_file_name}}
{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}
path=/
enabled=1
gpgcheck=0""", template)


    @patch.object(System, "os_family", new='suse')
    @patch("resource_management.libraries.providers.repository.File")
    def test_create_repo_suse(self, file_mock):
        with Environment('/') as env:
            Repository('hadoop',
                       base_url='http://download.base_url.org/rpm/',
                       mirror_list='https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                       repo_file_name='Repository')

            self.assertTrue('hadoop' in env.resources['Repository'])
            defined_arguments = env.resources['Repository']['hadoop'].arguments
            expected_arguments = {'base_url': 'http://download.base_url.org/rpm/',
                                  'mirror_list': 'https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                                  'repo_file_name': 'Repository'}

            self.assertEqual(defined_arguments, expected_arguments)
            self.assertEqual(file_mock.call_args[0][0], '/etc/zypp/repos.d/Repository.repo')

            template_item = file_mock.call_args[1]['content']
            template = str(template_item.name)
            expected_arguments.update({'repo_id': 'hadoop'})

            self.assertEqual(expected_arguments, template_item.context._dict)
            self.assertEqual("""[{{repo_id}}]
name={{repo_file_name}}
{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}
path=/
enabled=1
gpgcheck=0""", template)   
    
    
    @patch.object(tempfile, "NamedTemporaryFile")
    @patch("resource_management.libraries.providers.repository.Execute")
    @patch("resource_management.libraries.providers.repository.File")
    @patch("os.path.isfile", new=MagicMock(return_value=True))
    @patch("filecmp.cmp", new=MagicMock(return_value=False))
    @patch.object(System, "os_release_name", new='precise')        
    @patch.object(System, "os_family", new='debian')
    def test_create_repo_debian_repo_exists(self, file_mock, execute_mock, tempfile_mock):
      tempfile_mock.return_value = MagicMock(spec=file)
      tempfile_mock.return_value.__enter__.return_value.name = "/tmp/1.txt"
      
      with Environment('/') as env:
          Repository('HDP',
                     base_url='http://download.base_url.org/rpm/',
                     repo_file_name='HDP',
                     components = ['a','b','c']
          )
      
      template_item = file_mock.call_args_list[0]
      template_name = template_item[0][0]
      template_content = template_item[1]['content'].get_content()
      
      self.assertEquals(template_name, '/tmp/1.txt')
      self.assertEquals(template_content, 'deb http://download.base_url.org/rpm/ precise a b c\n')
      
      copy_item = str(file_mock.call_args_list[1])
      self.assertEqual(copy_item, "call('/etc/apt/sources.list.d/HDP.list', content=StaticFile('/tmp/1.txt'))")
      
      execute_command_item = execute_mock.call_args_list[0][0][0]
      self.assertEqual(execute_command_item, 'apt-get update -o Dir::Etc::sourcelist="sources.list.d/HDP.list" -o APT::Get::List-Cleanup="0"')

    @patch.object(tempfile, "NamedTemporaryFile")
    @patch("resource_management.libraries.providers.repository.Execute")
    @patch("resource_management.libraries.providers.repository.File")
    @patch("os.path.isfile", new=MagicMock(return_value=True))
    @patch("filecmp.cmp", new=MagicMock(return_value=True))
    @patch.object(System, "os_release_name", new='precise')        
    @patch.object(System, "os_family", new='debian')
    def test_create_repo_debian_doesnt_repo_exist(self, file_mock, execute_mock, tempfile_mock):
      tempfile_mock.return_value = MagicMock(spec=file)
      tempfile_mock.return_value.__enter__.return_value.name = "/tmp/1.txt"
      
      with Environment('/') as env:
          Repository('HDP',
                     base_url='http://download.base_url.org/rpm/',
                     repo_file_name='HDP',
                     components = ['a','b','c']
          )
      
      template_item = file_mock.call_args_list[0]
      template_name = template_item[0][0]
      template_content = template_item[1]['content'].get_content()
      
      self.assertEquals(template_name, '/tmp/1.txt')
      self.assertEquals(template_content, 'deb http://download.base_url.org/rpm/ precise a b c\n')
      
      self.assertEqual(file_mock.call_count, 1)
      self.assertEqual(execute_mock.call_count, 0)
      
    
    @patch("os.path.isfile", new=MagicMock(return_value=True))
    @patch.object(System, "os_family", new='debian')
    @patch("resource_management.libraries.providers.repository.Execute")
    @patch("resource_management.libraries.providers.repository.File")
    def test_remove_repo_debian_repo_exist(self, file_mock, execute_mock):
      with Environment('/') as env:
          Repository('HDP',
                     action = "remove",
                     repo_file_name='HDP'
          )
          
      self.assertEqual(str(file_mock.call_args), "call('/etc/apt/sources.list.d/HDP.list', action='delete')")
      self.assertEqual(execute_mock.call_args[0][0], 'apt-get update -o Dir::Etc::sourcelist="sources.list.d/HDP.list" -o APT::Get::List-Cleanup="0"')

    @patch("os.path.isfile", new=MagicMock(return_value=False))
    @patch.object(System, "os_family", new='debian')
    @patch("resource_management.libraries.providers.repository.Execute")
    @patch("resource_management.libraries.providers.repository.File")
    def test_remove_repo_debian_repo_doenst_exist(self, file_mock, execute_mock):
      with Environment('/') as env:
          Repository('HDP',
                     action = "remove",
                     repo_file_name='HDP'
          )
          
      self.assertEqual(file_mock.call_count, 0)
      self.assertEqual(execute_mock.call_count, 0)

    @patch.object(System, "os_family", new='redhat')
    @patch("resource_management.libraries.providers.repository.File")
    def test_remove_repo_redhat(self, file_mock):
        with Environment('/') as env:
            Repository('hadoop',
                       action='remove',
                       base_url='http://download.base_url.org/rpm/',
                       mirror_list='https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                       repo_file_name='Repository')

            self.assertTrue('hadoop' in env.resources['Repository'])
            defined_arguments = env.resources['Repository']['hadoop'].arguments
            expected_arguments = {'action': ['remove'],
                                  'base_url': 'http://download.base_url.org/rpm/',
                                  'mirror_list': 'https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                                  'repo_file_name': 'Repository'}
            self.assertEqual(defined_arguments, expected_arguments)
            self.assertEqual(file_mock.call_args[1]['action'], 'delete')
            self.assertEqual(file_mock.call_args[0][0], '/etc/yum.repos.d/Repository.repo')


    @patch.object(System, "os_family", new='suse')
    @patch("resource_management.libraries.providers.repository.File")
    def test_remove_repo_suse(self, file_mock):
        with Environment('/') as env:
            Repository('hadoop',
                       action='remove',
                       base_url='http://download.base_url.org/rpm/',
                       mirror_list='https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                       repo_file_name='Repository')

            self.assertTrue('hadoop' in env.resources['Repository'])
            defined_arguments = env.resources['Repository']['hadoop'].arguments
            expected_arguments = {'action': ['remove'],
                                  'base_url': 'http://download.base_url.org/rpm/',
                                  'mirror_list': 'https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                                  'repo_file_name': 'Repository'}
            self.assertEqual(defined_arguments, expected_arguments)
            self.assertEqual(file_mock.call_args[1]['action'], 'delete')
            self.assertEqual(file_mock.call_args[0][0], '/etc/zypp/repos.d/Repository.repo')
