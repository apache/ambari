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

  getRegisteredMpackInfo: function () {
    return App.ajax.send({
      name: 'mpack.get_all_registered',
      sender: this
    })
  },

  /**
   * Populates mpacks array, repos array, and operatingSystems array based on info about registered mpacks.
   */
  loadStep: function () {
    this.getRegisteredMpackInfo().then(registeredMpacks => {
      const selectedMpacks = this.get('content.selectedMpacks');
      const mpacks = [];
  
      registeredMpacks.items.forEach(mpack => {
        const selectedMpack = selectedMpacks.find(selectedMpack => selectedMpack.name === mpack.MpackInfo.mpack_name && selectedMpack.version === mpack.MpackInfo.mpack_version);
        
        mpacks.push(Em.Object.create({
          id: mpack.MpackInfo.id,
          name: mpack.MpackInfo.mpack_name,
          displayName: mpack.MpackInfo.mpack_display_name,
          publicUrl: selectedMpack.publicUrl,
          downloadUrl: selectedMpack.downloadUrl,
          version: mpack.MpackInfo.mpack_version,
          operatingSystems: mpack.default_operating_systems.map(os => {
            //determines if the OS was selected in the database (as when the mpack is initially registered)            
            let initiallySelected;
            if (mpack.operating_systems) {
              initiallySelected = mpack.operating_systems.find(mpackOs => mpackOs.OperatingSystems.os_type === os.OperatingSystems.os_type);
            }

            //checks if the OS was selected in the UI
            let selectedOs;
            if (selectedMpack && selectedMpack.operatingSystems) {
              selectedOs = selectedMpack.operatingSystems.find(mpackOs => mpackOs.type === os.OperatingSystems.os_type);
            }

            return Em.Object.create({
              postdata: os.OperatingSystems,
              type: os.OperatingSystems.os_type,
              initiallySelected: initiallySelected ? true : false,
              selected: selectedOs ? true : false,
              isFirstSelected: false,
              isLastSelected: false,
              repos: os.OperatingSystems.repositories.map((repo, index, repos) => {
                let downloadUrl;

                if (selectedOs && selectedOs.repos) {
                  const selectedRepo = selectedOs.repos.findProperty('repoId', repo.repo_id);
                  
                  if (selectedRepo) {
                    downloadUrl = selectedRepo.downloadUrl;
                  }
                }

                return Em.Object.create({
                  id: `${mpack.MpackInfo.mpack_name}-${mpack.MpackInfo.mpack_version}-${os.OperatingSystems.os_type}-${repo.repo_id}`, //this is a unique ID used in client logic
                  repoId: repo.repo_id, //this is the repo ID used by the server and displayed in the UI
                  name: repo.repo_name,
                  publicUrl: repo.base_url,
                  downloadUrl: downloadUrl || repo.base_url,
                  isFirst: index === 0,
                  isLast: index === repos.length - 1
                });
              })
            });
          })
        }))
      });
      this.set('mpacks', mpacks);

      const repos = mpacks.reduce(
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
      
    this.get('wizardController').addErrors);
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
      return App.get('router.btnClickInProgress')
        || (this.get('wizardController.errors') && this.get('wizardController.errors').length > 0)
        || repos.filterProperty('downloadUrl', '').length > 0;
    }
    
    return true;
  }.property('anySelectedOs', 'repos.@each.downloadUrl', 'App.router.btnClickInProgress', 'wizardController.errors'),

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
    
    const osRepoPromises = [];
    mpacks.forEach(mpack => {
      mpack.get('operatingSystems').forEach(os => {
        const osRepos = os.postdata;

        if (os.selected) {
          osRepos.is_ambari_managed = !useRedHatSatellite;
          osRepos.repositories.forEach(repository => {
            const repo = os.repos.findProperty('repoId', repository.repo_id);
            repository.base_url = repo.get('downloadUrl');
          });
          if (os.initiallySelected === true) { //OS was initially selected and is still selected, so we are doing an update
            osRepoPromises.push(this.updateOsRepos(mpack.get('id'), osRepos.os_type, { OperatingSystems: osRepos }));
          } else { //OS is newly selected, so we are doing a create
            osRepoPromises.push(this.createOsRepos(mpack.get('id'), osRepos.os_type, { OperatingSystems: osRepos }));
          }
        } else {
          if (os.initiallySelected === true) { //OS was initially selected and is no longer selected, so we are doing a delete
            osRepoPromises.push(this.deleteOsRepos(mpack.get('id'), osRepos.os_type));
          }
        }
      });
    });

    $.when(...osRepoPromises).then(() => {
      App.router.send('next');  
    }, () => this.get('wizardController').addError(Em.i18n.t('installer.error.mpackOsModifications')));
  },

  createOsRepos: function (mpack, os, data) {
    return App.ajax.send({
      name: 'mpack.create_os_repos',
      sender: this,
      data: {
        mpack: mpack,
        os: os,
        data: data
      }
    });
  },

  updateOsRepos: function (mpack, os, data) {
    return App.ajax.send({
      name: 'mpack.update_os_repos',
      sender: this,
      data: {
        mpack: mpack,
        os: os,
        data: data
      }
    });
  },

  deleteOsRepos: function (mpack, os) {
    return App.ajax.send({
      name: 'mpack.delete_os_repos',
      sender: this,
      data: {
        mpack: mpack,
        os: os
      }
    });
  }  
});
