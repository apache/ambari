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

App.MainViewsDetailsView = Em.View.extend({

  name: "mainViewsDetailsView",

  tagName: "iframe",
  classNames: ["views_sizes"],
  attributeBindings: ['src','seamless'],
  seamless: "seamless",

  interval: null,

  /**
   * Drop autoHeight timer
   */
  willDestroyElement: function() {
    var interval = this.get('interval');
    if (interval) {
      clearInterval(interval);
    }
  },

  /**
   * For view's iframe do autoHeight with timer
   * Timer is dropped when user navigates away
   */
  didInsertElement: function() {
    var interval,
      self = this,
      timer = function (resizeFunction, iframe) {
        interval = setInterval(function() {
          resizeFunction(iframe);
        }, 100);
        self.set('interval', interval);
    };
    $('iframe').iframeAutoHeight({triggerFunctions: [timer]});
  },

  src: function() {
    return window.location.origin + this.get('controller.content.href');
  }.property('controller.content')

});
