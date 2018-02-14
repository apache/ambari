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

App.WizardCustomProductReposView = Em.View.extend({

  templateName: require('templates/wizard/customProductRepos'),

  didInsertElement: function () {
    this._super();
    this.get('controller').loadStep();
    
    const operatingSystems = this.get('controller.operatingSystems'); 
    operatingSystems.forEach(os => this.updateOsState(os.get('type')));
  },

  /**
   *  Change first and last selected flags now that we've changed the selection.
   *  This is used by the template to display the first and last items differently.
   */
  updateOsState: function (osType) {
    const os = this.get('controller.operatingSystems').findProperty('type', osType);
    
    if (os) {
      const mpacks = os.get('mpacks');
      mpacks.forEach(mpack => {
        const selectedOperatingSystems = mpack.get('operatingSystems').filterProperty('selected');
        selectedOperatingSystems.forEach((os, index, array) => {
          if (index === 0) {
            os.set('isFirstSelected', true);
          } else {
            os.set('isFirstSelected', false);
          }

          if (index === array.length - 1) {
            os.set('isLastSelected', true);
          } else {
            os.set('isLastSelected', false);
          }
        });
      });
    }
  },

  revertButtonTooltip: Em.I18n.t('common.revert'),

  revertUrl: function (event) {
    const repoId = event.currentTarget.value;
    const repo = this.get('controller').findRepoById(repoId);
    repo.set('downloadUrl', repo.get('publicUrl'));
  },

  selectedOsChanged: function (event) {
    const os = event.srcElement.name;
    this.get('controller').toggleOs(os);
    this.get('parentView').updateOsState(os);
  }
});