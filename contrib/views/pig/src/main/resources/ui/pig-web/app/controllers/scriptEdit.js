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

App.ScriptEditController = Em.ObjectController.extend({
  needs:['script'],
  pigParams: Em.A(),
  isExec:false,
  isRenaming:false,
  titleWarn:false,
  tmpArgument:'',
  editor:null,

  handleRenaming:function () {
    if (this.get('content.title')) {
      this.set('titleWarn',false);
    }
  }.observes('content.title','isRenaming'),

  pigParamsMatch:function (controller) {
    editorContent = this.get('content.pigScript.fileContent');
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
    } else {
      controller.set('pigParams',[]);
    };
  }.observes('content.pigScript.fileContent','content.id'),


  oldTitle:'',
  actions: {
    rename:function (opt) {
      var changedAttributes = this.get('content').changedAttributes()

      if (opt==='ask') {
        this.set('oldTitle',this.get('content.title'));
        this.set('isRenaming',true);
      };

      if (opt==='cancel') {
        this.set('content.title',this.get('oldTitle'));
        this.set('oldTitle','');
        this.set('isRenaming',false);
      };

      if (opt==='confirm') {
        if (Em.isArray(changedAttributes.title) && this.get('content.title')) {
          this.get('content').save().then(function () {
            this.send('showAlert', {message:Em.I18n.t('editor.title_updated'),status:'success'});
          }.bind(this));
        }
        this.set('oldTitle','');
        this.set('isRenaming',false);
      }
    },
    addArgument:function () {
      var arg = this.get('tmpArgument');
      if (!arg) {
        return false;
      }
      var pull = this.get('content.argumentsArray');
      if (Em.$.inArray(arg,pull)<0) {
        pull.pushObject(arg);
        this.set('content.argumentsArray',pull);
        this.set('tmpArgument','');
      } else {
        this.send('showAlert', {'message': Em.I18n.t('scripts.alert.arg_present'), status:'info'});
      }
    },
    removeArgument:function (arg) {
      var removed = this.get('content.argumentsArray').removeObject(arg);
      this.set('content.argumentsArray',removed);
    },
    execute: function (script, operation) {
      var executeMethod = {
        'execute':this.prepareExecute,
        'explain':this.prepareExplain,
        'syntax_check':this.prepareSyntaxCheck
      };

      this.set('isExec',true);

      return Ember.RSVP.resolve(script.get('pigScript'))
        .then(function (file) {
          return Ember.RSVP.all([file.save(),script.save()]);
        })
        .then(executeMethod[operation].bind(this))
        .then(this.executeSuccess.bind(this), this.executeError.bind(this))
        .finally(Em.run.bind(this,this.set,'isExec',false));
    }
  },

  executeSuccess:function (job) {
    this.send('showAlert', {message:Em.I18n.t('job.alert.job_started'),status:'success'});
    if (this.target.isActive('script.edit')) {
      Em.run.next(this,this.transitionToRoute,'script.job',job);
    }
  },

  executeError:function (error) {
    var trace = (error.responseJSON)?error.responseJSON.trace:null;
    this.send('showAlert', {message:Em.I18n.t('job.alert.start_filed'),status:'error',trace:trace});
  },

  prepareExecute:function (data) {
    var file = data[0], script = data[1], pigParams = this.get('pigParams') || [], fileContent = file.get('fileContent');

    pigParams.forEach(function (param) {
      var rgParam = new RegExp(param.param,'g');
      fileContent = fileContent.replace(rgParam,param.value);
    });

    var job = this.store.createRecord('job',{
      scriptId: script.get('id'),
      templetonArguments: script.get('templetonArguments'),
      title: script.get('title'),
      forcedContent: (pigParams.length > 0)? fileContent:null,
      pigScript: (pigParams.length == 0)?file:null
    });

    return job.save();
  },
  prepareExplain:function (data) {
    var file = data[0], script = data[1], pigParams = this.get('pigParams') || [], fileContent = file.get('fileContent');

    pigParams.forEach(function (param) {
      var rgParam = new RegExp(param.param,'g');
      fileContent = fileContent.replace(rgParam,param.value);
    });

    var job = this.store.createRecord('job',{
      scriptId: script.get('id'),
      templetonArguments: '',
      title: 'Explain: "' + script.get('title') + '"',
      jobType: 'explain',
      sourceFileContent: (pigParams.length > 0)? fileContent:null,
      sourceFile: (pigParams.length == 0)?file.get('id'):null,
      forcedContent: 'explain -script ${sourceFile}'
    });

    return job.save();
  },
  prepareSyntaxCheck:function (data) {
    var file = data[0], script = data[1], pigParams = this.get('pigParams') || [], fileContent = file.get('fileContent'), args = script.get('templetonArguments');

    pigParams.forEach(function (param) {
      var rgParam = new RegExp(param.param,'g');
      fileContent = fileContent.replace(rgParam,param.value);
    });

    var job = this.store.createRecord('job',{
      scriptId: script.get('id'),
      templetonArguments: (!args.match(/-check/g))?args+(args?"\t":"")+'-check':args,
      title: 'Syntax check: "' + script.get('title') + '"',
      jobType:  'syntax_check',
      forcedContent: (pigParams.length > 0)? fileContent:null,
      pigScript: (pigParams.length == 0)?file:null
    });

    return job.save();
  },

  /**
   * Is script is in error state.
   * @return {boolean}
   */
  scriptError:function () {
    return this.get('content.isError');
  }.property('content.isError'),

  /**
   * available UDFs
   * @return {App.Udf} promise
   */
  ufdsList:function () {
    return this.store.find('udf');
  }.property('udf')
});
