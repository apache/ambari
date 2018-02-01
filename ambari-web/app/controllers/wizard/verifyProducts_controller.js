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

App.WizardVerifyProductsController = App.WizardStepController.extend({

  VERIFYREPO_INPROGRESS: 0,
  VERIFYREPO_SUCCEEDED: 1,
  VERIFYREPO_FAILED: 2,

  name: 'wizardVerifyProductsController',

  stepName: 'verifyProducts',

  mpacks: [],

  repos: [],

  loadStep: function () {
    const selectedMpacks = this.get('content.selectedMpacks');

    const mpacks = [];
    selectedMpacks.forEach(mpack => {
      mpacks.pushObject(Em.Object.create({
        name: mpack.name,
        displayName: mpack.displayName,
        publicUrl: mpack.publicUrl,
        downloadUrl: mpack.downloadUrl,
        version: mpack.version,
        operatingSystems: mpack.operatingSystems.map(os =>
          Em.Object.create({
            type: os.type,
            selected: os.selected,
            isFirstSelected: os.isFirstSelected,
            isLastSelected: os.isLastSelected,
            repos: os.repos.map(repo =>
              Em.Object.create({
                id: repo.id, //this one is globally unique
                repoId: repo.repoId, //this is the one displayed in the UI
                downloadUrl: repo.downloadUrl,
                isFirst: repo.isFirst,
                isLast: repo.isLast,
                inProgress: true,
                succeeded: false,
                failed: false
              })
            )
          })
        )
      }));
    });
    this.set('mpacks', mpacks);

    const repos = this.get('mpacks').reduce(
      (repos, mpack) => repos.concat(
        mpack.get('operatingSystems').reduce(
          (repos, os) => repos.concat(
            os.get('repos')
          ),
          []
        )
      ),
      []
    );
    this.set('repos', repos);

    repos.forEach(repo => this.verifyRepo(repo).then(this.verifyRepoSucceeded.bind(this), this.verifyRepoFailed.bind(this)));
  },

  /**
   * Ensures that repo state flags remain in sync.
   * 
   * @param {any} repo to change state of
   * @param {any} state to change to
   */
  setRepoState: function (repo, state) {
    switch (state) {
      case this.get('VERIFYREPO_INPROGRESS'):
        repo.set('succeeded', false);
        repo.set('failed', false);
        repo.set('inProgress', true);
        break;
      case this.get('VERIFYREPO_SUCCEEDED'):
        repo.set('succeeded', true);
        repo.set('failed', false);
        repo.set('inProgress', false);  
        break;
      case this.get('VERIFYREPO_FAILED'):
        repo.set('succeeded', false);
        repo.set('failed', true);
        repo.set('inProgress', false);  
        break;  
    }
  },

  /**
   * This will be a no-op until the server actually has some way of verifying repos.
   * 
   * @param {any} repo 
   */
  verifyRepo: function (repo) {
    const dfd = $.Deferred();
    
    const self = this;
    setTimeout(function () {
      self.setRepoState(repo, self.get('VERIFYREPO_INPROGRESS'));
      dfd.resolve(repo);
    });

    return dfd.promise();
  },

  verifyRepoSucceeded: function (repo) {
    this.setRepoState(repo, this.get('VERIFYREPO_SUCCEEDED'));
  },

  verifyRepoFailed: function (repo) {
    this.setRepoState(repo, this.get('VERIFYREPO_FAILED'));
  },

  retryVerifyRepo: function (repo) {
    this.verifyRepo(repo).then(this.verifyRepoSucceeded.bind(this), this.verifyRepoFailed.bind(this));
  },

  isSubmitDisabled: function () {
    const repos = this.get('repos');
    return App.get('router.btnClickInProgress')
      || (this.get('wizardController.errors') && this.get('wizardController.errors').length > 0)
      || repos.filterProperty('succeeded', false).length > 0;
  }.property('repos.@each.succeeded', 'App.router.btnClickInProgress', 'wizardController.errors'),

  submit: function () {
    if (App.get('router.nextBtnClickInProgress')) {
      return;
    }

    App.router.send('next');  
  }
});
