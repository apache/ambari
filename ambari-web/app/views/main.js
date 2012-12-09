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

App.MainView = Em.View.extend({
  templateName:require('templates/main')
});

App.MainBackgroundOperation = Em.View.extend({
  content:null,
  classNames:['background-operations'],
  classNameBindings:['isOpen'],
  isOpen:false,
  iconClass:function () {
    return this.get('isOpen') ? 'icon-minus' : 'icon-plus';
  }.property('isOpen'),
  showOperationLog:function () {
    this.set('isOpen', !this.get('isOpen'))
    var operation = this.get('content');
    var self = this;
    if (!this.get('isOpenShowLog') && !this.get('logDetails')) {

      jQuery.getJSON('data/hosts/background_operations/logs/task' + operation.taskId + '.json',
        function (data) {
          var fields = ['stdout', 'stderror'];
          if (data) {
            fields.forEach(function (fieldName) {
              if (data.fieldName) {
                data.fieldName = data.fieldName.highlight(["fail", "err"]);
              }
            });
          }

          if (App.testMode) {
            var stdError = "Donec quis error tincidunt dolor. word Proin vel fail dignissim metus. In hac err habitasse platea dictumst. Err Mauris error tortor dui, commodo vitae failure placerat ut, venenatis nec err dolor. failure Lorem ipsum dolor sit amet, fail err consectetur fail adipiscing elit. Error Vivamus vel velit ipsum, id laoreet velit. Nullam vel err augue a tortor mattis semper fail, in nec neque.";
            stdError = stdError.highlight(["err", "fail"]);
            data.stderror = stdError;
          }

          self.set('logDetails', data);
        }
      );
    }

    if (App.testMode) {
      var stdError = "Donec quis error tincidunt dolor. word Proin vel fail dignissim metus. In hac err habitasse platea dictumst. Err Mauris error tortor dui, commodo vitae failure placerat ut, venenatis nec err dolor. failure Lorem ipsum dolor sit amet, fail err consectetur fail adipiscing elit. Error Vivamus vel velit ipsum, id laoreet velit. Nullam vel err augue a tortor mattis semper fail, in nec neque.";
      stdError = stdError.highlight(["err", "fail"]);
      var data = {stderror:stdError};
      self.set('logDetails', data);
    }

    this.set('isOpenShowLog', !this.get('isOpenShowLog'));
  }
});