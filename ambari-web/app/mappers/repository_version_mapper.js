/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var App = require('app');

App.repoVersionMapper = App.QuickDataMapper.create({
  modelRepoVersions: App.RepositoryVersion,
  modelOperatingSystems: App.OS,
  modelRepositories: App.Repository,
  modelServices: App.ServiceSimple,

  modelRepoVersion: function (isCurrentStackOnly) {
    var repoVersionsKey = isCurrentStackOnly ? 'RepositoryVersions' : 'CompatibleRepositoryVersions';
    return {
      id: repoVersionsKey + '.id',
      stack_version_id: repoVersionsKey + '.stackVersionId',
      display_name: repoVersionsKey + '.display_name',
      type: repoVersionsKey + '.type',
      repository_version: repoVersionsKey + '.repository_version',
      upgrade_pack: repoVersionsKey + '.upgrade_pack',
      stack_version_type: repoVersionsKey + '.stack_name',
      stack_version_number: repoVersionsKey + '.stack_version',
      use_redhat_satellite: 'use_redhat_satellite',
      stack_services_key: 'stack_services',
      stack_services_type: 'array',
      stack_services: {
        item: 'id'
      },
      operating_systems_key: 'operating_systems',
      operating_systems_type: 'array',
      operating_systems: {
        item: 'id'
      }
    };
  },

  modelOS: {
    id: 'id',
    repository_version_id: 'repository_version_id',
    os_type: 'OperatingSystems.os_type',
    stack_name: 'OperatingSystems.stack_name',
    stack_version: 'OperatingSystems.stack_version',
    repositories_key: 'repositories',
    repositories_type: 'array',
    repositories: {
      item: 'id'
    }
  },

  modelService: {
    id: 'id',
    name: 'name',
    display_name: 'display_name',
    latest_version: 'latest_version'
  },

  modelRepository: {
    id: 'id',
    operating_system_id: 'Repositories.operating_system_id',
    base_url : 'Repositories.base_url',
    default_base_url : 'Repositories.default_base_url',
    latest_base_url : 'Repositories.latest_base_url',
    mirrors_list : 'Repositories.mirrors_list',
    os_type : 'Repositories.os_type',
    repo_id : 'Repositories.repo_id',
    repo_name : 'Repositories.repo_name',
    stack_name : 'Repositories.stack_name',
    stack_version : 'Repositories.stack_version'
  },

  map: function (json, loadAll, isCurrentStackOnly) {
    var modelRepoVersions = this.get('modelRepoVersions');
    var modelOperatingSystems = this.get('modelOperatingSystems');
    var modelRepositories = this.get('modelRepositories');
    var modelServices = this.get('modelServices');

    var resultRepoVersion = [];
    var resultOS = [];
    var resultRepo = [];
    var resultService = [];
    var repoVersionsKey = isCurrentStackOnly ? 'RepositoryVersions' : 'CompatibleRepositoryVersions';

    if (json && json.items) {
      json.items.forEach(function (item) {
        if (loadAll || (item[repoVersionsKey] && !App.StackVersion.find().someProperty('repositoryVersion.id', item[repoVersionsKey].id))) {
          var repo = item;
          var osArray = [];
          var serviceArray = [];
          if (item.operating_systems) {
            item.operating_systems.forEach(function (os) {
              os.id = item[repoVersionsKey].repository_version + os.OperatingSystems.os_type;
              os.repository_version_id = repo.id;
              var repoArray = [];
              if (Em.get(os, 'repositories')) {
                os.repositories.forEach(function (repo) {
                  repo.id = repo.Repositories.repo_id + os.id;
                  repo.operating_system_id = os.id;
                  repoArray.pushObject(repo);
                  resultRepo.push(this.parseIt(repo, this.get('modelRepository')));
                }, this);
              }
              os.repositories = repoArray;
              osArray.pushObject(os);
              resultOS.push(this.parseIt(os, this.get('modelOS')));
            }, this);
          }
          if (item[repoVersionsKey].stack_services) {
            item[repoVersionsKey].stack_services.forEach(function (service) {
              var serviceObj = {
                id: service.name,
                name: service.name,
                display_name: service.display_name,
                latest_version: service.versions[0] ? service.versions[0] : ''
              };
              serviceArray.pushObject(serviceObj);
              resultService.push(this.parseIt(serviceObj, this.get('modelService')));
            }, this);
          } else if (item[repoVersionsKey].services) {
            item[repoVersionsKey].services.forEach(function (service) {
              var serviceObj = {
                id: service.name,
                name: service.name,
                display_name: service.display_name,
                latest_version: service.versions[0] ? service.versions[0].version: ''
              };
              serviceArray.pushObject(serviceObj);
              resultService.push(this.parseIt(serviceObj, this.get('modelService')));
            }, this);
          }
          repo.use_redhat_satellite = item.operating_systems[0].OperatingSystems.ambari_managed_repositories === false;
          repo.operating_systems = osArray;
          repo.stack_services = serviceArray;
          resultRepoVersion.push(this.parseIt(repo, this.modelRepoVersion(isCurrentStackOnly)));
        }
      }, this);
    }
    App.store.commit();
    App.store.loadMany(modelRepositories, resultRepo);
    App.store.loadMany(modelOperatingSystems, resultOS);
    App.store.loadMany(modelServices, resultService);
    App.store.loadMany(modelRepoVersions, resultRepoVersion);
  }
});
