/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var App = require('app');

App.stackMapper = App.QuickDataMapper.create({
  modelStack: App.Stack,
  modelOS: App.OperatingSystem,
  modelRepo: App.Repository,
  
  configStack: {
    id: 'id',
    stack_name: 'stack_name',
    stack_version: 'stack_version',
    active: 'active',
    parent_stack_version: 'parent_stack_version',
    min_upgrade_version: 'min_upgrade_version',
    min_jdk_version: 'min_jdk',
    max_jdk_version: 'max_jdk',
    is_selected: 'is_selected',
    config_types: 'config_types',
    operating_systems_key: 'operating_systems',
    operating_systems_type: 'array',
    operating_systems: {
      item: 'id'
    }
  },
  
  configOS: {
    id: 'id',
    os_type: 'os_type',
    stack_name: 'stack_name',
    stack_version: 'stack_version',
    stack_id: 'stack_id',
    repositories_key: 'repositories',
    repositories_type: 'array',
    repositories: {
      item: 'id'
    }
  },
  
  configRepository: {
    id: 'id',
    base_url: 'base_url',
    default_base_url: 'default_base_url',
    latest_base_url: 'latest_base_url',
    mirrors_list: 'mirrors_list',
    os_type: 'os_type',
    repo_id: 'repo_id',
    repo_name: 'repo_name',
    stack_name: 'stack_name',
    stack_version: 'stack_version',
    operating_system_id: 'os_id'
  },
  
  map: function(json) {
    var modelStack = this.get('modelStack');
    var modelOS = this.get('modelOS');
    var modelRepo = this.get('modelRepo');
    var resultStack = [];
    var resultOS = [];
    var resultRepo = [];

    var stackVersions = json.items.filterProperty('Versions.active');
    stackVersions.sortProperty('Versions.stack_version').reverse().forEach(function(item) {
      var stack = item.Versions;
      var operatingSystemsArray = [];

      stack.id = stack.stack_name + "-" + stack.stack_version;

      item.operating_systems.forEach(function(ops) {
        var operatingSystems = ops.OperatingSystems;

        var repositoriesArray = [];
        ops.repositories.forEach(function(repo) {
          repo.Repositories.id = [repo.Repositories.stack_name, repo.Repositories.stack_version, repo.Repositories.os_type, repo.Repositories.repo_id].join('-');
          repo.Repositories.os_id = [repo.Repositories.stack_name, repo.Repositories.stack_version, repo.Repositories.os_type].join('-');
          resultRepo.push(this.parseIt(repo.Repositories, this.get('configRepository')));
          repositoriesArray.pushObject(repo.Repositories);
        }, this);


        operatingSystems.id = operatingSystems.stack_name + "-" + operatingSystems.stack_version + "-" + operatingSystems.os_type;
        operatingSystems.stack_id = operatingSystems.stack_name + "-" + operatingSystems.stack_version;
        operatingSystems.repositories = repositoriesArray;
        resultOS.push(this.parseIt(operatingSystems, this.get('configOS')));
        operatingSystemsArray.pushObject(operatingSystems);
        
      }, this);
      

      stack.operating_systems = operatingSystemsArray;
      resultStack.push(this.parseIt(stack, this.get('configStack')));
      
    }, this);

    App.store.commit();
    App.store.loadMany(modelRepo, resultRepo);
    App.store.loadMany(modelOS, resultOS);
    App.store.loadMany(modelStack, resultStack);
  }
});
