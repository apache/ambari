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

App.EditController = Em.ObjectController.extend({
  pigArgumentsPull: Em.A(),
  isExec:false,
  isRenaming:false,
  actions:{
    rename:function (opt) {
      var changedAttributes = this.content.changedAttributes(),
          controller = this;
      if (opt==='ask') {
        this.set('isRenaming',true);
      };

      if (opt==='cancel') {
        this.content.rollback();
        if (Em.isArray(changedAttributes.templetonArguments)) {
          this.content.set('templetonArguments',changedAttributes.templetonArguments[1]);
        }
        this.set('isRenaming',false);
      };

      if (opt==='confirm') {
        if (Em.isArray(changedAttributes.title)) {
          this.content.save().then(function () {
            controller.send('showAlert', {message:Em.I18n.t('editor.title_updated'),status:'success'});
          });
        }
        this.set('isRenaming',false);
      };

    },
    addArgument:function (arg) {
      if (!arg) {
        return false;
      };
      var pull = this.get('pigArgumentsPull');
      if (Em.$.inArray(arg,pull)<0) {
        pull.pushObject(arg);
      } else {
        this.send('showAlert', {'message': Em.I18n.t('scripts.alert.arg_present'), status:'info'});
      }
    },
    removeArgument:function (arg) {
      var index = this.pigArgumentsPull.indexOf(arg.toString());
      this.pigArgumentsPull.removeAt(index);
    },
    execute: function (object, operation) {
      var controller = this,
          job,
          sendAlert = function (status) {
            var alerts = {success:Em.I18n.t('job.alert.job_started'), error:Em.I18n.t('job.alert.start_filed')};
            return function (argument) {
              controller.set('isExec',false);
              var trace = null;
              if (status=='error') {
                trace = argument.responseJSON.trace;
              }
              controller.send('showAlert', {message:alerts[status],status:status,trace:trace});
              if (status=='success') {
                controller.transitionToRoute('job',job);
              };
            };
          };

      controller.set('isExec',true);

      return Ember.RSVP.resolve(object.get('pigScript')).then(function (file) {
        var savePromises = [file.save()];

        job = controller.prepareJob(file, object, operation);

        if (object.get('constructor.typeKey') === "script") {
          savePromises.push(object.save());
        };

        return Ember.RSVP.all(savePromises).then(function () {
          return job.save().then(sendAlert('success'),sendAlert('error'));
        });
      });
    },
  },
  prepareJob:function (file, object, operation) {
    var controller = this,
        args = object.get('templetonArguments'),
        parameters = this.get('pigParams.length') > 0,
        pigParams = this.get('pigParams') || [],
        fileContent = file.get('fileContent'),
        config;

    pigParams.forEach(function (param) {
      var rgParam = new RegExp(param.param,'g');
      fileContent = fileContent.replace(rgParam,param.value);
    });
    
    if (operation === 'execute') {
      config = {
        templetonArguments: args,
        title: object.get('title'),
        forcedContent: (parameters)? fileContent:null,
        pigScript: (!parameters)?file:null
      };
    } else if (operation === 'explain') {
      config = {
        templetonArguments:  '',
        title: 'Explain: "' + object.get('title') + '"',
        jobType: 'explain',
        sourceFileContent: (parameters)? fileContent:null,
        sourceFile: (!parameters)?file.get('id'):null,
        forcedContent: 'explain -script ${sourceFile}'
      }
    } else {
      config = {
        templetonArguments: (!args.match(/-check/g))?args+(args?"\t":"")+'-check':args,
        title: 'Syntax check: "' + object.get('title') + '"',
        jobType:  'syntax_check',
        forcedContent: (parameters)? fileContent:null,
        pigScript: (!parameters)?file:null
      }
    };
    
    if (object.get('constructor.typeKey') === 'script'){
      config.scriptId = object.get('id');
    } else {
      config.scriptId = (operation != 'explain')?object.get('scriptId'):null;
    };

    return this.store.createRecord('job',config);
  },
  /*
   *Is script or scritp file is dirty.
   * @return {boolean}
   */
  scriptDirty:function () {
    return this.get('content.isDirty') || this.get('content.pigScript.isDirty');
  }.property('content.pigScript.isDirty','content.isDirty'),

  /**
   * Is script is in error state.
   * @return {boolean}
   */
  scriptError:function () {
    return this.get('content.isError');
  }.property('content.isError'),

  /*
    Gets script arguments array from pigArgumentsPull
    and write to model as string
  */
  pigArgToString:function (controller,observer) {
    var args = controller.get('pigArgumentsPull');
    var oldargs = (this.get('content.templetonArguments'))?this.get('content.templetonArguments').w():[];
    if (args.length != oldargs.length) {
      this.set('content.templetonArguments',args.join('\t'));
    };
  }.observes('pigArgumentsPull.@each'),

  /*
    get script arguments string from model
    and write to pigArgumentsPull as array
  */
  pigArgToArray:function (controller,observer) {
    var args =  controller.get(observer);
    if (args && args.length > 0){
      controller.set('pigArgumentsPull',args.w());
    }
  }.observes('content.templetonArguments'),

  /**
   * available UDFs
   * @return {App.Udf} promise
   */
  ufdsList:function () {
    return this.store.find('udf');
  }.property('udf'),

});
