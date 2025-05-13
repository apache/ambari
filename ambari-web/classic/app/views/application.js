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

App.ApplicationView = Em.View.extend({
  templateName: require('templates/application'),

  views: function () {
    return App.router.get('loggedIn') ? App.router.get('mainViewsController.visibleAmbariViews') : [];
  }.property('App.router.mainViewsController.visibleAmbariViews.[]', 'App.router.loggedIn'),

  didInsertElement: function () {
    // on 'Enter' pressed, trigger modal window primary button if primary button is enabled(green)
    // on 'Esc' pressed, close the modal
    $(document).keydown(function (event) {
      if (event.which === 13 || event.keyCode === 13) {
        $('.modal:last').trigger('enter-key-pressed');
      }
      return true;
    });
    $(document).keyup(function (event) {
      if (event.which === 27 || event.keyCode === 27) {
        $('.modal:last').trigger('escape-key-pressed');
      }
      return true;
    });
  },

  /**
   * Navigation Bar should be initialized after cluster data is loaded
   */
  initNavigationBar: function () {
    if (App.get('router.mainController.isClusterDataLoaded')) {
      const observer = new MutationObserver(mutations => {
        var targetNode
        if (mutations.some((mutation) => mutation.type === 'childList' && (targetNode = $('.navigation-bar')).length)) {
          observer.disconnect();
          targetNode.navigationBar({
            fitHeight: true,
            collapseNavBarClass: 'icon-double-angle-left',
            expandNavBarClass: 'icon-double-angle-right',
          });
        }
      });

      setTimeout(() => {
        // remove observer if selected element is not found in 10secs.
        observer.disconnect();
      }, 10000)

      observer.observe(document.body, {
        childList: true,
        subtree: true
      });
    }
  }.observes('App.router.mainController.isClusterDataLoaded')

});
