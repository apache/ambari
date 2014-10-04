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

App.FileUploaderComponent = Ember.Component.extend({
  didInsertElement:function () {
    var _this = this;
    this.uploader.reopen({
      sendAlert:function (e) {
        _this.sendAction('alert',e);
      }
    });
    this.fileInput.reopen({
      filesDidChange: function() {
        var files = this.get('files');
        if (!files) {
          this.set('parentView.files',null);
          this.set('parentView.controlInput.value','');
          this.set('value','');
          return;
        }
        var numFiles = files ? files.length : 1;
        var label = this.get('value').replace(/\\/g, '/').replace(/.*\//, '');
        var log = numFiles > 1 ? numFiles + ' files selected' : label;

        this.set('parentView.controlInput.value',log)
        this.set('parentView.files',files)

      }.observes('files')
    });
  },
  actions:{
    upload:function (files) {
      var uploader = this.get('uploader');
      var uploadBtn = Ladda.create(this.uploadButton.get('element'));
      var reset = function () {
        uploadBtn.stop();
        this.send('clear');
      };
      if (!uploader.get('isUploading')) {
        var path = this.get('path');
        if (!Ember.isEmpty(this.get('files'))) {
          var file = this.get('files')[0];
          uploadBtn.start();
          uploader.on('progress',function (e) {
            uploadBtn.setProgress(e.percent/100);
          })
          uploader.upload(file,{path:path}).finally(Em.run.bind(this,reset));
        }
      };
    },
    clear:function () {
      this.set('fileInput.files',null);
    }
  },
  uploader: null,
  layoutName:'components/uploader',
  path:'',
  info:'',
  files:null,
  isFiles:function () {
    return !this.get('files.length');
  }.property('files'),
  uploadButton: Em.View.createWithMixins(Ember.TargetActionSupport, {
    tagName:'button',
    target: Ember.computed.alias('controller'),
    classNames:['btn btn-success ladda-button'],
    classNameBindings:['isFiles:hide'],
    attributeBindings: ["data-style","data-size"],
    action:'upload',
    click: function() {
      this.triggerAction();
    }
  }),
  fileInput : Ember.TextField.create({
    type: 'file',
    attributeBindings: ['multiple'],
    multiple: false,
    files:null,
    change: function(e) {
      var input = e.target;
      if (!Ember.isEmpty(input.files)) {
        this.set('files', input.files);
      }
    },
  }),
  controlInput:Ember.TextField.create({
    readonly:true,
    classNames:['form-control']
  })
});