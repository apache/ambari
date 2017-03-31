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

import Ember from 'ember';

export default Ember.Component.extend({

  tagName: "query-editor",
  readOnly: false,

  _initializeEditor: function() {

    var editor,
      updateSize,
      self = this;

    updateSize = function () {
      editor.setSize(self.$(this).width(), self.$(this).height());
      editor.refresh();
    };

    this.set('editor', CodeMirror.fromTextArea(document.getElementById('code-mirror'), {
      mode: 'text/x-hive',
      indentWithTabs: true,
      smartIndent: true,
      lineNumbers: true,
      matchBrackets : true,
      autofocus: true,
      readOnly: self.get('readOnly'),
      extraKeys: {'Ctrl-Space': 'autocomplete'}
    }));


    CodeMirror.commands.autocomplete = function (cm) {
      var lastWord = cm.getValue().split(' ').pop();

      //if user wants to fill in a column
      if (lastWord.indexOf('.') > -1) {
        lastWord = lastWord.split('.')[0];

        self.getColumnsHint(cm, lastWord);
      } else {
        CodeMirror.showHint(cm);
      }
    };

    editor = this.get('editor');

    editor.on('cursorActivity', function () {
      self.set('highlightedText', editor.getSelections());
    });

    editor.setValue(this.get('query') || '');

    editor.on('change', function (instance) {
      Ember.run(function () {
        self.set('query', instance.getValue());
      });
    });

    this.$('.CodeMirror').resizable({
      handles: 's',

      resize: function () {
        Ember.run.debounce(this, updateSize, 150);
      }
    }).find('.ui-resizable-s').addClass('grip fa fa-reorder');


  }.on('didInsertElement'),


  updateValue: function () {
    var query = this.get('query');
    var editor = this.get('editor');

    var isFinalExplainQuery = (query.toUpperCase().trim().indexOf('EXPLAIN') > -1);
    var editorQuery = editor.getValue();

    if (editor.getValue() !== query) {
      if(isFinalExplainQuery){
        editor.setValue(editorQuery || '');
      }else {
        editor.setValue(query || '');
      }
    }

    this.sendAction('updateQuery', query);


  }.observes('query'),


  actions:{
  }

});
