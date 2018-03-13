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

App.WizardDownloadMpacksView = Em.View.extend({

  templateName: require('templates/wizard/downloadMpacks'),

  didInsertElement: function () {
    this._super();
    var controller = this.get('controller');
    controller.addMpacks();
    controller.registerMpacks();

    //enable initial tooltips
    $('[data-toggle="tooltip"]').tooltip();
    //enables tooltips added later
    const target = document.querySelector('#downloadMpacks');
    const observer = new MutationObserver(() => {
      $('[data-toggle="tooltip"]').tooltip();
    });
    observer.observe(target, { childList: true, subtree: true });
  },

  willDestroyElement: function () {
    this.get('controller.mpacks').clear();
  }

});