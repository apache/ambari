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

  didInsertElement: function () {
    // on 'Enter' pressed, trigger modal window primary button if primary button is enabled(green)
    // on 'Esc' pressed, close the modal
    $(document).keydown(function(event){
      if (event.which == 13 || event.keyCode == 13 ) {
        var primaryButton = $(document).find('#modal > .modal-footer > .btn-success').last();
        if ((!$("*:focus").is("textarea")) && primaryButton.length > 0 && primaryButton.attr('disabled') != 'disabled') {
          event.preventDefault();
          event.stopPropagation();
          primaryButton.click();
          return false;
        }
      }
      return true;
    });
    $(document).keyup(function(event){
      if (event.which == 27 || event.keyCode == 27) {
        var closeButton = $(document).find('#modal > .modal-header > .close').last();
        if (closeButton.length > 0) {
          event.preventDefault();
          event.stopPropagation();
          closeButton.click();
          return false;
        }
      }
      return true;
    });
  }
});