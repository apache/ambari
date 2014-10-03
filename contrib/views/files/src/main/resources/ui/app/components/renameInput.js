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

App.RenameInputComponent = Ember.Component.extend({
  tagName:'span',
  layoutName:'components/renameInput',
  actions:{
    rename:function (opt) {
      var tmpName;

      switch (opt) {
        case 'edit': this.set('isRenaming',true); break;
        case 'cancel': this.set('isRenaming',false); break;
        case 'confirm':
          tmpName = this.get('tmpName');
          if (tmpName.length==0) {
            break;
          };
          this.sendAction('confirm',this.get('filePath'),tmpName);
          this.set('isRenaming',false);
          break;

        default: this.toggleProperty('isRenaming');
      }
    },
  },

  /**
   * passed params
   */
  file:null,
  actionName:null,
  isRenaming:false,

  fileName:function () {
    var name, file = this.get('file');
    if (file instanceof DS.Model) {
      name = this.get('file.name');
    } else {
      name = file.substr(file.lastIndexOf('/')+1);
    };
    return name;
  }.property('file'),

  filePath:function () {
    var path, file = this.get('file');
    if (file instanceof DS.Model) {
      path = this.get('file.path');
    } else {
      path = file;
    };
    return path;
  }.property('file'),

  setTmpName:function () {
    if (this.get('isRenaming')) {
      this.set('tmpName',this.get('fileName'));
    } else {
      this.set('tmpName','');
    };
  }.observes('isRenaming'),

  onFileChange:function () {
    this.set('isRenaming',false);
  }.observes('file'),

  renameInputView: Em.TextField.extend({
    controller:null,
    didInsertElement:function () {
      var element = $(this.get('element'));
      element.focus().val(this.value)
    },
    keyUp: function(e) {
      var target = this.get('targetObject');
      if (e.keyCode==13) {
        return target.send('rename', 'confirm');
      };

      if (e.keyCode==27) {
        return target.send('rename', 'cancel');
      };
    }
  })
});
