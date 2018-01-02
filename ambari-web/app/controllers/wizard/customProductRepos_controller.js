/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
require('./wizardStep_controller');

App.WizardCustomProductReposController = App.WizardStepController.extend({

  name: 'wizardCustomProductReposController',

  stepName: 'customProductRepos',

  stacks: [],

  mpacks: [],

  repos: [],

  operatingSystems: [],

  isOsSelected: function (type) {
    const operatingSystems = this.get('operatingSystems');
    
    if (operatingSystems && operatingSystems.length > 0) {
      const os = operatingSystems.findProperty('type', type);
      
      if (os && os.get('selected')) {
        return true;
      }
    }

    return false;
  },

  /**
   * Pulls os and repo info from App.Stack and matches it up with selected mpacks.
   * Adds the built up object to the mpacks array.
   * Populates repos array.
   * Populates operatingSystems array.
   */
  loadStep: function () {
    const selectedMpacks = this.get('content.selectedMpacks');
    const stacks = [];

    App.Stack.find().forEach(stack => {
      const mpack = selectedMpacks.find(mpack => mpack.name === stack.get('stackName') && mpack.version === stack.get('stackVersion'));

      if (mpack) {
        stacks.push(Em.Object.create({
          id: stack.get('id'),
          name: mpack.name,
          version: mpack.version,
          operatingSystems: stack.get('operatingSystems').get('content').map((item, index) => {
            const os = stack.get('operatingSystems').objectAtContent(index);
            let selectedOs;
            if (mpack.operatingSystems) {
              selectedOs = mpack.operatingSystems.find(mpackOs => mpackOs.type === os.get('osType'));
            }

            return Em.Object.create({
              type: os.get('osType'),
              selected: selectedOs ? true : false,
              isFirstSelected: false,
              isLastSelected: false,
              repos: os.get('repositories').get('content').map((item, index, repos) => {
                const repo = os.get('repositories').objectAtContent(index);
                let downloadUrl;

                if (selectedOs) {
                  const selectedRepo = selectedOs.repos.find(mpackRepo => mpackRepo.id === repo.get('repoId'));
                  if (selectedRepo) {
                    downloadUrl = selectedRepo.downloadUrl;
                  }  
                }

                return Em.Object.create({
                  id: `${mpack.name}-${mpack.version}-${os.get('osType')}-${repo.get('repoId')}`, //this is a unique ID used in client logic
                  repoId: repo.get('repoId'), //this is the repo ID used by the server and displayed in the UI
                  name: repo.get('repoName'),
                  publicUrl: repo.get('baseUrlInit'),
                  downloadUrl: downloadUrl || repo.get('baseUrl'),
                  unique: repo.get('unique'), //this is a value that is only used by the server, but we need to preserve it
                  isFirst: index === 0,
                  isLast: index === repos.length - 1
                });
              })
            });
          })
        }));
      }
    });
    this.set('stacks', stacks);

    const mpacks = [];
    selectedMpacks.forEach(mpack => {
      const stack = stacks.find(stack => stack.get('name') === mpack.name && stack.get('version') === mpack.version);
      mpacks.pushObject(Em.Object.create({
        id: stack.get('id'), //this is actually the stack id from App.Stack, which is actually the repository_version id in the database, which is an integer
        name: mpack.name,
        displayName: mpack.displayName,
        publicUrl: mpack.publicUrl,
        downloadUrl: mpack.downloadUrl,
        version: mpack.version,
        operatingSystems: stack ? stack.operatingSystems : []
      }));
    });
    this.set('mpacks', mpacks);

    const repos = this.get('mpacks').reduce(
      (repos, mpack) => repos.concat(
        mpack.get('operatingSystems').reduce(
          (repos, os) => repos.concat(
            os.get('repos')
          ),
          [])
        ),
      []
    );
    this.set('repos', repos);

    const uniqueOperatingSystems = {};
    mpacks.forEach(mpack => {
      mpack.get('operatingSystems').forEach(os => {
        const osType = os.get('type');
        uniqueOperatingSystems[osType]
          ? uniqueOperatingSystems[osType].mpacks.pushObject(mpack)
          : uniqueOperatingSystems[osType] = {
              selected: os.get('selected'),
              mpacks: [mpack]
            };
      })
    });
    
    const operatingSystems = [];
    for (let osType in uniqueOperatingSystems) {
      operatingSystems.pushObject(Em.Object.create({
        type: osType,
        selected: uniqueOperatingSystems[osType].selected,
        mpacks: uniqueOperatingSystems[osType].mpacks
      }))
    }
    operatingSystems.sort((a, b) => a.get('type').localeCompare(b.get('type')));
    this.set('operatingSystems', operatingSystems);
  },

  /**
   * Returns the repo matching the given id.
   * 
   * @param {string} repoId consisting of mpackName-mpackVersion-osType-repoId
   */
  findRepoById: function (repoId) {
    const mpacks = this.get('mpacks');
    
    for (let mpack of mpacks) {
      for (let os of mpack.operatingSystems) {
        for (let repo of os.get('repos')) {
          if (repo.get('id') === repoId) {
            return repo;
          }
        }
      }
    }
  },

  toggleOs: function (osType) {
    const os = this.get('operatingSystems').findProperty('type', osType);
    
    if (os) {
      const mpacks = os.get('mpacks');
      const selected = os.get('selected');
      mpacks.forEach(mpack => {
        const os = mpack.operatingSystems.findProperty('type', osType);
        if (os) {
          os.set('selected', selected);
        }
      });   
    }
  },

  isStepDisabled: function (stepIndex, currentIndex) {
    const normallyDisabled = this._super(stepIndex, currentIndex);
    const useCustomRepo = this.get('wizardController.content.downloadConfig.useCustomRepo');

    return normallyDisabled || !useCustomRepo;
  },

  anySelectedOs: function () {
    const selectedOperatingSystems = this.get('operatingSystems').filterProperty('selected');
    return selectedOperatingSystems.length > 0;
  }.property('operatingSystems.@each.selected'),

  isSubmitDisabled: function () {
    if (this.get('anySelectedOs')) {
      const repos = this.get('repos');
      return repos.filterProperty('downloadUrl', '').length > 0 || App.get('router.btnClickInProgress');
    }
    
    return true;
  }.property('anySelectedOs', 'repos.@each.downloadUrl', 'App.router.btnClickInProgress'),

  submit: function () {
    if (App.get('router.nextBtnClickInProgress')) {
      return;
    }

    const mpacks = this.get('mpacks');

    const selectedMpacks = mpacks.map(selectedMpack =>
      ({
        name: selectedMpack.name,
        displayName: selectedMpack.displayName,
        publicUrl: selectedMpack.publicUrl,
        downloadUrl: selectedMpack.downloadUrl,
        version: selectedMpack.version,
        operatingSystems: selectedMpack.get('operatingSystems').filterProperty('selected').map(os =>
          ({
            type: os.get('type'),
            selected: os.get('selected'),
            isFirstSelected: os.get('isFirstSelected'),
            isLastSelected: os.get('isLastSelected'),
            repos: os.get('repos').map(repo =>
              ({
                id: repo.get('id'),
                repoId: repo.get('repoId'),
                downloadUrl: repo.get('downloadUrl'),
                isFirst: repo.get('isFirst'),
                isLast: repo.get('isLast')
              })
            )
          })
        )
      })
    );
    this.set('content.selectedMpacks', selectedMpacks);

    const useRedHatSatellite = this.get('content.downloadConfig.useRedHatSatellite')
    const updateRepoPromises = mpacks.map(mpack => {
      const repoToUpdate = {
        id: mpack.id, //this is actually the stack id from App.Stack, which is actually the repository_version id in the database, which is an integer
        stackName: mpack.name,
        stackVersion: mpack.version
      }

      const repo = Em.Object.create({
        useRedhatSatellite: useRedHatSatellite,
        operatingSystems: mpack.get('operatingSystems').map(os =>
          Em.Object.create({
            osType: os.type,
            repositories: os.get('repos').map(repo =>
              Em.Object.create({
                baseUrlInit: repo.get('publicUrl'),
                baseUrl: repo.get('downloadUrl'),
                repoId: repo.get('repoId'),
                repoName: repo.get('name'),
                unique: repo.get('unique') //this is a value that is only used by the server, but we need to preserve it
              })
            )
          })
        )
      });

      this.get('wizardController').updateRepoOSInfo(repoToUpdate, repo)
    });

    $.when(...updateRepoPromises).then(() => {
      App.router.send('next');  
    });
  }
});
