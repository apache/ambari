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

App.PigScriptEditController = App.EditController.extend({
  needs:['pig'],
  isScript:true,
  pigParams:Em.A(),

  /*
    Updates nav title when renaming script.
   */
  updateNavTitle:function (controller) {
    var pigC = controller.get('controllers.pig')
    if (!($.inArray(pigC.get('category'),['udfs','scripts','history'])>=0)){
      pigC.set('category',this.get('content.name'));
    }
  }.observes('content.name'),

  /*
    Observes params (%VAR%) in sript file 
    and updates pigParams object as they changes. 
   */
  pigParamsMatch:function (controller) {
    var editorContent = this.get('content.pigScript').then(function(data) {
      editorContent = data.get('fileContent');
      if (editorContent) {
            var match_var = editorContent.match(/\%\w+\%/g);
            if (match_var) {
              var oldParams = controller.pigParams;
              controller.set('pigParams',[]);
              match_var.forEach(function (param) {
                var suchParams = controller.pigParams.filterProperty('param',param);
                if (suchParams.length == 0){
                  var oldParam = oldParams.filterProperty('param',param);
                  var oldValue = oldParam.get('firstObject.value');
                  controller.pigParams.pushObject(Em.Object.create({param:param,value:oldValue,title:param.replace(/%/g,'')}));
                }
              });
            } else {
              controller.set('pigParams',[]);
            }
          };
    }, function(reason) {
      var trace = null;
      if (reason.responseJSON.trace)
        trace = reason.responseJSON.trace;
      controller.send('showAlert', {'message':Em.I18n.t('scripts.alert.file_not_found',{title: 'Error'}),status:'error',trace:trace});
    });
  }.observes('content.pigScript.fileContent'),

});
