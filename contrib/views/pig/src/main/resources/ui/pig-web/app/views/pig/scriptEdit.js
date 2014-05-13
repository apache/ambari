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

App.PigScriptEditView = Em.View.extend({
  didInsertElement:function () {
    $('.file-path').tooltip();
    if (this.get('controller.isJob')) {
      this.set('isEditConfirmed',false);
    };
  },
  actions:{
    insertUdf:function (udf) {
      var cm = this.get('codeMirror');
      cm.setValue('REGISTER ' + udf.get('path') + '\n'+ cm.getValue());
    },
    confirmEdit:function () {
      this.set('isEditConfirmed', true);
    }
  },
  argumentInput: Ember.TextField.extend({
    viewName:"argumentInput",
    actions:{
      sendArgument:function(){
        this.get('parentView.controller').send('addArgument',this.get('value'));
        this.set('value',null);
      }
    },
    classNames:["form-control argadd"],
    keyPress:function  (event) {
      if (event.keyCode=="13") {
        this.send('sendArgument');
      }
    }
  }),
  showEditor:function () {
    return this.get('controller.category')!='results';
  }.property('controller.category','codeMirror'),
  codeMirror:null,
  codeMirrorView: Ember.TextArea.extend({
    valueBinding:"content.fileContent",
    updateCM:function (view,trigger) {
      var cm = this.get('parentView.codeMirror');
      var cmElement = $(cm.display.wrapper);
      if (this.get('content.isLoaded')) {
        if (this.get('parentView.controller.isJob') && !this.get('parentView.isEditConfirmed')) {
          cm.setOption('readOnly',true);
          cmElement.addClass('inactive');
        } else {
          cm.setOption('readOnly',false);
          cmElement.removeClass('inactive')
        }
        this.get('parentView.codeMirror').setValue(this.get('content.fileContent')||'');
      } else {
        cm.setOption('readOnly',true);
        cmElement.addClass('inactive');
      };

    }.observes('parentView.codeMirror', 'content.didLoad', 'parentView.isEditConfirmed'),
    didInsertElement: function() {
      var self = this;
      var cm = CodeMirror.fromTextArea(this.get('element'),{
        lineNumbers: true,
        matchBrackets: true,
        indentUnit: 4,
        keyMap: "emacs"
      });
      this.set('parentView.codeMirror',cm);
      cm.on('change',function (cm) {
        var pig_script = self.get('content');
        if (pig_script.get('isLoaded')){
          pig_script.set('fileContent',cm.getValue());
        }
      });                  
    }
  }),
  isEditConfirmed: true,
  pigHelperView: Em.View.extend({
    actions:{
      putToEditor:function (helper) {
        helper = helper.toString();
        
        var editor = this.get('parentView.codeMirror');
        var cursor = editor.getCursor();
        var pos = this.findPosition(helper);
        
        editor.replaceRange(helper, cursor, cursor);
        editor.focus();

        if (pos.length>1) {
          editor.setSelection(
            {line:cursor.line, ch:cursor.ch + pos[0]},
            {line:cursor.line, ch:cursor.ch + pos[1]+1}
          );
        }

        return false;
      }
    },
    findPosition: function (curLine){
      var pos= curLine.indexOf("%");
      var posArr=[];
      while(pos > -1) {
        posArr.push(pos);
        pos = curLine.indexOf("%", pos+1);
      }
      return posArr;
    },
    templateName:'pig/util/pigHelper',
    helpers:[
      {
        'title':'Eval Functions',
        'helpers':[
          'AVG(%VAR%)',
          'CONCAT(%VAR1%, %VAR2%)',
          'COUNT(%VAR%)',
          'COUNT_START(%VAR%)',
          'IsEmpty(%VAR%)',
          'DIFF(%VAR1%, %VAR2%)',
          'MAX(%VAR%)',
          'MIN(%VAR%)',
          'SIZE(%VAR%)',
          'SUM(%VAR%)',
          'TOKENIZE(%VAR%, %DELIM%)',
        ]
      },
      {
        'title':'Relational Operators',
        'helpers':[
          'COGROUP %VAR% BY %VAR%',
          'CROSS %VAR1%, %VAR2%;',
          'DISTINCT %VAR%;',
          'FILTER %VAR% BY %COND%',
          'FLATTEN(%VAR%)',
          'FOREACH %DATA% GENERATE %NEW_DATA%',
          'FOREACH %DATA% {%NESTED_BLOCK%}',
          'GROUP %VAR% BY %VAR%',
          'GROUP %VAR% ALL',
          'JOIN %VAR% BY ',
          'LIMIT %VAR% %N%',
          'ORDER %VAR% BY %FIELD%',
          'SAMPLE %VAR% %SIZE%',
          'SPLIT %VAR1% INTO %VAR2% IF %EXPRESSIONS%',
          'UNION %VAR1%, %VAR2%',
        ]
      },
      {
        'title':'I/0',
        'helpers':[
          "LOAD '%FILE%';",
          'DUMP %VAR%;',
          'STORE %VAR% INTO %PATH%;',
        ]
      },
      {
        'title':'Debug',
        'helpers':[
          'EXPLAIN %VAR%;',
          'ILLUSTRATE %VAR%;',
          'DESCRIBE %VAR%;',
        ]
      },
      {
        'title':'HCatalog',
        'helpers':[
          "LOAD '%TABLE%' USING org.apache.hcatalog.pig.HCatLoader();",
        ]
      },
      {
        'title':'Math',
        'helpers':[
          'ABS(%VAR%)',
          'ACOS(%VAR%)',
          'ASIN(%VAR%)',
          'ATAN(%VAR%)',
          'CBRT(%VAR%)',
          'CEIL(%VAR%)',
          'COS(%VAR%)',
          'COSH(%VAR%)',
          'EXP(%VAR%)',
          'FLOOR(%VAR%)',
          'LOG(%VAR%)',
          'LOG10(%VAR%)',
          'RANDOM(%VAR%)',
          'ROUND(%VAR%)',
          'SIN(%VAR%)',
          'SINH(%VAR%)',
          'SQRT(%VAR%)',
          'TAN(%VAR%)',
          'TANH(%VAR%)',
        ]
      },
      {
        'title':'Tuple, Bag, Map Functions',
        'helpers':[
          'TOTUPLE(%VAR%)',
          'TOBAG(%VAR%)',
          'TOMAP(%KEY%, %VALUE%)',
          'TOP(%topN%, %COLUMN%, %RELATION%)',
        ]
      },
      {
        'title':'String Functions',
        'helpers':[
          "INDEXOF(%STRING%, '%CHARACTER%', %STARTINDEX%)",
          "LAST_INDEX_OF(%STRING%, '%CHARACTER%', %STARTINDEX%)",
          "LOWER(%STRING%)",
          "REGEX_EXTRACT(%STRING%, %REGEX%, %INDEX%)",
          "REGEX_EXTRACT_ALL(%STRING%, %REGEX%)",
          "REPLACE(%STRING%, '%oldChar%', '%newChar%')",
          "STRSPLIT(%STRING%, %REGEX%, %LIMIT%)",
          "SUBSTRING(%STRING%, %STARTINDEX%, %STOPINDEX%)",
          "TRIM(%STRING%)",
          "UCFIRST(%STRING%)",
          "UPPER(%STRING%)",
        ]
      },
      {
        'title':'Macros',
        'helpers':[
          "IMPORT '%PATH_TO_MACRO%';",
        ]
      },
      {
        'title':'HBase',
        'helpers':[
          "LOAD 'hbase://%TABLE%' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('%columnList%')",
          "STORE %VAR% INTO 'hbase://%TABLE%' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('%columnList%')",
        ]
      },
      {
        'title':'Python UDF',
        'helpers':[
          "REGISTER 'python_udf.py' USING jython AS myfuncs;",
        ]
      }
    ]
  }),
  pigParamView:Em.TextField.extend({})
});
