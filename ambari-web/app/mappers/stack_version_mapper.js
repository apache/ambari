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

App.stackVersionMapper = App.QuickDataMapper.create({
  modelStackVerion: App.StackVersion,
  modelRepositories: App.Repositories,
  modelOperatingSystems: App.OS,

  modelStack: {
    id: 'id',
    name: 'name',
    version: 'version',
    operating_systems_key: 'operating_systems',
    operating_systems_type: 'array',
    operating_systems: {
      item: 'id'
    },
    host_stack_versions_key: 'host_stack_versions',
    host_stack_versions_type: 'array',
    host_stack_versions: {
      item: 'id'
    },
    installed_hosts: 'installed_hosts',
    current_hosts: 'current_hosts'
  },

  modelOS: {
    id: 'id',
    name: 'os',
    stack_version_id: 'stack_version_id',
    repositories_key: 'repositories',
    repositories_type: 'array',
    repositories: {
      item: 'id'
    }
  },

  modelRepository: {
    id: 'id',
    type: 'type',
    baseurl: 'baseurl',
    os_id: 'os_id'
  },

  map: function (json) {
    var modelStackVerion = this.get('modelStackVerion');
    var modelOperatingSystems = this.get('modelOperatingSystems');
    var modelRepositories = this.get('modelRepositories');

    var resultStack = [];
    var resultOS = [];
    var resultRepo = [];

    if (json && json.items) {
      json.items.forEach(function (item) {
        var stack = item.StackVersion;
        stack.id = item.StackVersion.name + item.StackVersion.version;
        var osArray = [];
        var hostStackVersions = [];

        if (Em.get(item, 'StackVersion.repositories')) {
          item.StackVersion.repositories.forEach(function (os) {
            os.id = item.StackVersion.version + os.os;
            os.stack_version_id = stack.id;
            os.name = os.os;
            var repoArray = [];

            if (Em.get(os, 'baseurls')) {
              os.baseurls.forEach(function (repo) {
                repo.os_id = os.id;
                resultRepo.push(this.parseIt(repo, this.get('modelRepository')));
                repoArray.pushObject(repo);
              }, this);
            }

            os.repositories = repoArray;
            resultOS.push(this.parseIt(os, this.get('modelOS')));
            osArray.pushObject(os);
          }, this);
        }
        //TODO change loading form current api
        App.HostStackVersion.find().filterProperty('version', item.StackVersion.version).forEach(function(hv) {
          hostStackVersions.push({id: hv.get('id')});
        });
        stack.host_stack_versions = hostStackVersions;
        stack.operating_systems = osArray;
        resultStack.push(this.parseIt(stack, this.get('modelStack')));
      }, this);
    }
    App.store.commit();
    App.store.loadMany(modelRepositories, resultRepo);
    App.store.loadMany(modelOperatingSystems, resultOS);
    App.store.loadMany(modelStackVerion, resultStack);
  }
});
