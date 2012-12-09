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
  content: null,
  classNames: ['background-operations'],
  classNameBindings: ['isOpen'],
  isOpen: function () {
    return this.get('content.isOpen');
  }.property('content.isOpen'),
  iconClass: function(){
    return this.get('isOpen') ? 'icon-minus' : 'icon-plus';
  }.property('isOpen'),
  showOperationLog:function(){
    this.set('content.isOpen', !this.get('content.isOpen'));
    this.set('isTextArea', false);
  },
  hasProgressBar: function () {
    return this.get('content.command') == 'EXECUTE';
  }.property('content.command'),
  isInProgress: function () {
    var status = this.get('content.status');
    return status == 'IN_PROGRESS' || status == 'QUEUED' || status == 'PENDING';
  }.property('content.status'),
  barColor: function () {
    if (this.get('isInProgress')) {
      return 'progress-info';
    } else {
      if (this.get('content.status') == 'COMPLETED') return 'progress-success';
      return 'progress-danger';
    }
  }.property('isInProgress'),
  buttonLabel:function(){
    var button = $(this.get('element')).find('.textTrigger');
    if(this.get('isTextArea')){
      button.text('press CTRL+C');
    } else {
      button.text('click to highlight');
    }
  }.observes('isTextArea'),
  didInsertElement: function () {
    var self = this;
    var button = $(this.get('element')).find('.textTrigger');
    button.click(function () {
      self.set('isTextArea', !self.get('isTextArea'));
    });
    $(this.get('element')).find('.content-area').mouseenter(
      function () {
        var element = $(this);
        element.css('border', '1px solid #dcdcdc');
        button.css('visibility', 'visible');
      }).mouseleave(
      function () {
        var element = $(this);
        element.css('border', 'none');
        button.css('visibility', 'hidden');
      })
  },
  isTextArea: false,
  textArea: Em.TextArea.extend({
    didInsertElement: function(){
      var element = $(this.get('element'));
      element.width($(this.get('parentView').get('element')).width() - 10);
      element.height($(this.get('parentView').get('element')).height());
      element.select();
      element.css('resize', 'none');
    },
    readOnly: true,
    value: function(){
      var operation = this.get('content');
      var content = "";
      content += operation.command + " " + operation.role + " on " + operation.host_name + "\n";
      content += "exitcode: " + operation.exit_code + "\n";
      content += "stderr: " + operation.stderr + "\n";
      content += "stdout: " + operation.stdout + "\n";
      return content;
    }.property('content')
  })
});