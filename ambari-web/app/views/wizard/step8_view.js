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

App.WizardStep8View = Em.View.extend({

  templateName: require('templates/wizard/step8'),

  didInsertElement: function () {
    this.get('controller').loadStep();
  },

  /**
   * Print review-report
   * @method printReview
   */
  printReview: function () {
    var o = $("#step8-info");
    o.jqprint();
  },

  repoInfo: function() {
    var repoInfo = this.get('controller.clusterInfo.repoInfo');
    if (!repoInfo) {
      return [];
    }
    return repoInfo.map(function (item) {
      var link = item.get('base_url');
      try {
        var urlObject = new URL(link);
        if (urlObject.username && urlObject.password) {
          urlObject.username = urlObject.username.replace(/./g, "*");
          urlObject.password = urlObject.password.replace(/./g, "*");
          link = urlObject.toString();
        }
      } catch (e) {
      }

      return {
        os_type: item.get('os_type'),
        repo_id: item.get('repo_id'),
        base_url: link
      };
    });
  }.property('controller.clusterInfo.repoInfo')
});

