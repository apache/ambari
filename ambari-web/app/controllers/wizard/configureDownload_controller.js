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
var arrayUtils = require('utils/array_utils');

/**
 * @typedef {Em.Object} StackType
 * @property {string} stackName
 * @property {App.Stack[]} stacks
 * @property {boolean} isSelected
 */

/**
 * @type {Em.Object}
 */
var StackType = Em.Object.extend({
  stackName: '',
  stacks: [],
  isSelected: Em.computed.someBy('stacks', 'isSelected', true)
});

App.WizardConfigureDownloadController = Em.Controller.extend({

  name: 'wizardConfigureDownloadController',

  /**
   * @type {App.Stack}
   */
  selectedStack: Em.computed.findBy('content.stacks', 'isSelected', true),

  /**
   * @type {App.ServiceSimple[]}
   */
  servicesForSelectedStack: Em.computed.filterBy('selectedStack.stackServices', 'isHidden', false),

  optionsToSelect: {
    'usePublicRepo': {
      index: 0,
      isSelected: true
    },
    'useLocalRepo': {
      index: 1,
      isSelected: false,
      'uploadFile': {
        index: 0,
        name: 'uploadFile',
        file: '',
        hasError: false,
        isSelected: true
      },
      'enterUrl': {
        index: 1,
        name: 'enterUrl',
        url: '',
        placeholder: Em.I18n.t('installer.step1.useLocalRepo.enterUrl.placeholder'),
        hasError: false,
        isSelected: false
      }
    }
  },

  /**
   * Restore base urls for selected stack when user select to use public repository
   */
  usePublicRepo: function () {
    var selectedStack = this.get('selectedStack');
    if (selectedStack) {
      selectedStack.setProperties({
        useRedhatSatellite: false,
        usePublicRepo: true,
        useLocalRepo: false
      });
      selectedStack.restoreReposBaseUrls();
    }
  },

  /**
   * Clean base urls for selected stack when user select to use local repository
   */
  useLocalRepo: function () {
    var selectedStack = this.get('selectedStack');
    if (selectedStack) {
      selectedStack.setProperties({
        usePublicRepo: false,
        useLocalRepo: true
      });
      selectedStack.cleanReposBaseUrls();
    }
  },

    /**
     * List of stacks grouped by <code>stackNameVersion</code>
     *
     * @type {StackType[]}
     */
    availableStackTypes: function () {
      var stacks = this.get('content.stacks');
      return stacks ? stacks.mapProperty('stackNameVersion').uniq().sort().reverse().map(function (stackName) {
        return StackType.create({
          stackName: stackName,
          stacks: stacks.filterProperty('stackNameVersion', stackName).sort(arrayUtils.sortByIdAsVersion).reverse()
        })
      }) : [];
    }.property('content.stacks.@each.stackNameVersion'),

    /**
     * @type {StackType}
     */
    selectedStackType: Em.computed.findBy('availableStackTypes', 'isSelected', true),

    isLoadingComplete: Em.computed.equal('wizardController.loadStacksRequestsCounter', 0)

});
