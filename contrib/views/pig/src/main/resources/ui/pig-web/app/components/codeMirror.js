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

App.CodeMirrorComponent = Ember.Component.extend({
  tagName: "textarea",
  readOnly:false,
  codeMirror:null,
  updateCM:function () {
    var cm = this.get('codeMirror');
    if (this.get('readOnly')) {
      cm.setValue((this.get('content.fileContent')||''));
      return cm.setOption('readOnly',true);
    }
    var cmElement = $(cm.display.wrapper);
    if (this.get('content.isLoaded')) {
      cm.setOption('readOnly',false);
      cmElement.removeClass('inactive');
      cm.setValue((this.get('content.fileContent')||''));
      cm.markClean();
    } else {
      cm.setOption('readOnly',true);
      cmElement.addClass('inactive');
    }
  }.observes('codeMirror', 'content.didLoad'),
  didInsertElement: function() {
    var cm = CodeMirror.fromTextArea(this.get('element'),{
      lineNumbers: true,
      matchBrackets: true,
      indentUnit: 4,
      keyMap: "emacs"
    });

    $('.editor-container').resizable({
      stop:function () {
        cm.setSize(null, this.style.height);
      },
      resize: function() {
        this.getElementsByClassName('CodeMirror')[0].style.height = this.style.height;
        this.getElementsByClassName('CodeMirror-gutters')[0].style.height = this.style.height;
      },
      minHeight:215,
      handles: {'s': '#sgrip' }
    });

    this.set('codeMirror',cm);
    if (!this.get('readOnly')) {
      cm.focus();
      cm.on('change', Em.run.bind(this,this.editorDidChange));
    }
  },
  editorDidChange:function () {
    var pig_script = this.get('content');
    if (pig_script.get('isLoaded')){
      pig_script.set('fileContent',this.get('codeMirror').getValue());
      this.get('codeMirror').markClean();
    }
  },
  scriptDidChange:function () {
    if (this.get('codeMirror').isClean() && this.get('content.isDirty')) {
      this.get('codeMirror').setValue(this.get(('content.fileContent')||''));
    }
  }.observes('content.fileContent'),
  willClearRender:function () {
    this.get('codeMirror').toTextArea();
  }
});
