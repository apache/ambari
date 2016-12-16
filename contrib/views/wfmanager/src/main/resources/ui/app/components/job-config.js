/*
*    Licensed to the Apache Software Foundation (ASF) under one or more
*    contributor license agreements.  See the NOTICE file distributed with
*    this work for additional information regarding copyright ownership.
*    The ASF licenses this file to You under the Apache License, Version 2.0
*    (the "License"); you may not use this file except in compliance with
*    the License.  You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/

import Ember from 'ember';
import Constants from '../utils/constants';
import { validator, buildValidations } from 'ember-cp-validations';

const Validations = buildValidations({
  'filePath': validator('presence', {
    presence : true
  }),
  'configMap': {
    validators: [
      validator('job-params-validator', {
        dependentKeys: ['configMap.@each.value', 'showErrorMessage']
      })
    ]
  }
});


export default Ember.Component.extend(Validations, {
  systemConfigs : Ember.A([]),
  showingFileBrowser : false,
  jobXml : "",
  overwritePath : false,
  configMap : Ember.A([]),
  configPropsExists : false,
  savingInProgress : false,
  isStackTraceVisible: false,
  isStackTraceAvailable: false,
  alertType : "",
  alertMessage : "",
  alertDetails : "",
  filePath : "",
  showErrorMessage: false,
  displayName : Ember.computed('type', function(){
    if(this.get('type') === 'wf'){
      return "Workflow";
    }else if(this.get('type') === 'coord'){
      return "Coordinator";
    }else{
      return "Bundle";
    }
  }),
  initialize :function(){
    this.set("configPropsExists", this.get("jobConfigs").props.size>0);
    var configProperties = [];
    configProperties.pushObjects(this.extractJobParams());
    configProperties.pushObjects(this.extractJobProperties());
    this.configureExecutionSettings();
    this.set("configMap", configProperties);
    this.set("jobXml", this.get("jobConfigs").xml);
    this.set('filePath', Ember.copy(this.get('jobFilePath')));
    Object.keys(this.get('validations.attrs')).forEach((attr)=>{
    var field = 'validations.attrs.'+attr+'.isDirty';
    this.set(field, false);
    }, this);
  }.on('init'),
  rendered : function(){
    this.$("#configureJob").on('hidden.bs.modal', function () {
      this.sendAction('closeJobConfigs');
    }.bind(this));
    this.$("#configureJob").modal("show");    
  }.on('didInsertElement'),
  extractJobParams(){
    var params = [];
    var jobParams = this.get("jobConfigs").params;
    if(jobParams && jobParams.configuration && jobParams.configuration.property){
      jobParams.configuration.property.forEach((param)=>{
        if(param && !param.value){
          var prop= Ember.Object.create({
            name: param.name,
            value: null,
            isRequired : true
          });
          params.push(prop);
        }
      });
    }
    return params;
  },

  extractJobProperties(){
    var jobProperties = [];
    var jobParams = this.get("jobConfigs").params;
    this.get("jobConfigs").props.forEach(function(value) {
      if (value!== Constants.defaultNameNodeValue && value!==Constants.rmDefaultValue){
        var propName = value.trim().substring(2, value.length-1);
        var isRequired = true;
        if(jobParams && jobParams.configuration && jobParams.configuration.property){
          var param = jobParams.configuration.property.findBy('name', propName);
          if(param && param.value){
            isRequired = false;
          }else {
            isRequired = true;
          }
        }
        var prop= Ember.Object.create({
          name: propName,
          value: null,
          isRequired : isRequired
        });
        jobProperties.push(prop);
      }
    });
    return jobProperties;
  },
  configureExecutionSettings (){
    this.set('systemConfigs', Ember.A([]));
    if(this.get('type') !== 'coord' && !this.get('isDryrun')){
      this.get('systemConfigs').pushObject({displayName: 'Run on submit', name : 'runOnSubmit', value: false});
    }
    this.get('systemConfigs').pushObjects([
      {displayName: 'Use system lib path', name :'useSystemLibPath', value:true},
      {displayName: 'Rerun on Failure', name : 'rerunOnFailure', value:true}
    ]);
  },
  showNotification(data){
    if (!data){
      return;
    }
    if (data.type === "success"){
      this.set("alertType", "success");
    }
    if (data.type === "error"){
      this.set("alertType", "danger");
    }
    this.set("alertDetails", data.details);
    this.set("alertMessage", data.message);
    if(data.stackTrace && data.stackTrace.length){
      this.set("stackTrace", data.stackTrace);
      this.set("isStackTraceAvailable", true);
    } else {
      this.set("isStackTraceAvailable", false);
    }
  },
  prepareJobForSubmission(isDryrun){
    if(this.get('validations.isInvalid')){
      return;
    };
    this.set('jobFilePath', Ember.copy(this.get('filePath')));
    var url = Ember.ENV.API_URL + "/submitJob?app.path=" + this.get("filePath") + "&overwrite=" + this.get("overwritePath");
    url = url + "&jobType=" + this.get('displayName').toUpperCase();
    var submitConfigs = this.get("configMap");
    submitConfigs.forEach(function(item) {
      url = url + "&config." + item.name + "=" + item.value;
    }, this); 
    this.get('systemConfigs').forEach((config)=>{
      if(config.name === 'runOnSubmit' && !isDryrun){
        url = url + "&oozieparam.action=start";
      }else if(config.name !== 'runOnSubmit'){
        url = url + "&oozieconfig." + config.name + "=" + config.value;
      }
    });
    if(isDryrun){
      url = url + "&oozieparam.action=dryrun";
    }
    if ( this.get("jobConfigs").props.has("${resourceManager}")){
      url= url + "&resourceManager=useDefault";
    }
    this.set("savingInProgress", true);
    this.submitJob(url);
  },
  submitJob(url){
    Ember.$.ajax({
      url: url,
      method: "POST",
      dataType: "text",
      contentType: "text/plain;charset=utf-8",
      beforeSend: function(request) {
        request.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        request.setRequestHeader("X-Requested-By", "workflow-designer");
      },
      data: this.get("jobXml"),
      success: function(response) {
        var result=JSON.parse(response);
        this.showNotification({
          "type": "success",
          "message": this.get('displayName') +" saved.",
          "details": "Job id :"+result.id
        });
        this.set("savingInProgress",false);
      }.bind(this),
      error: function(response) {
        console.log(response);
        this.set("savingInProgress",false);
        this.set("isStackTraceVisible",true);
        this.showNotification({
          "type": "error",
          "message": "Error occurred while saving "+ this.get('displayName').toLowerCase(),
          "details": this.getParsedErrorResponse(response),
          "stackTrace": this.getStackTrace(response.responseText)
        });
      }.bind(this)
    });
  },
  getStackTrace(data){
    if(data){
     try{
      var stackTraceMsg = JSON.parse(data).stackTrace;
      if(!stackTraceMsg){
        return "";
      }
     if(stackTraceMsg instanceof Array){
       return stackTraceMsg.join("").replace(/\tat /g, '<br/>&nbsp;&nbsp;&nbsp;&nbsp;at&nbsp;');
     } else {
       return stackTraceMsg.replace(/\tat /g, '<br/>&nbsp;&nbsp;&nbsp;&nbsp;at&nbsp;');
     }
     } catch(err){
       return "";
     }
    }
    return "";
  },
  startJob (jobId){
    this.set('startingInProgress', true);
    var url = [Ember.ENV.API_URL,
      "/v2/job/", jobId, "?action=", 'start','&user.name=oozie'
    ].join("");
    Ember.$.ajax({
      url: url,
      method: 'PUT',
      beforeSend: function (xhr) {
        xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        xhr.setRequestHeader("X-Requested-By", "Ambari");
      }
    }).done(function(){
      this.set('startingInProgress', false);
      this.showNotification({
        "type": "success",
        "message": this.get('displayName')+" Started",
        "details": jobId
      });
    }.bind(this)).fail(function(response){
      this.set('startingInProgress', false);
      this.showNotification({
        "type": "error",
        "message": "Error occurred while starting "+ this.get('displayName').toLowerCase(),
        "details": this.getParsedErrorResponse(response),
        "stackTrace": this.getStackTrace(response.responseText)
      });
    }.bind(this));
  },
  getParsedErrorResponse (response){
    var detail;
    if (response.responseText && response.responseText.charAt(0)==="{"){
      var jsonResp=JSON.parse(response.responseText);
      if (jsonResp.status==="workflow.oozie.error"){
        detail="Oozie error. Please check the workflow.";
      }else if(jsonResp.message && jsonResp.message.indexOf("<html>") > -1){
        detail= "";
      }else{
        detail=jsonResp.message;
      }
    }else{
      detail=response; 
    }
    return detail;
  },
  actions: {
    selectFile(){
      this.set("showingFileBrowser",true);
    },
    showStackTrace(){
      this.set("isStackTraceVisible", true);
    },
    hideStackTrace(){
      this.set("isStackTraceVisible", false);
    },
    closeFileBrowser(){
      this.set("showingFileBrowser",false);
    },
    dryrun(){
      this.set('showErrorMessage', true);
      this.prepareJobForSubmission(true);
    },
    save(){
      this.set('showErrorMessage', true);
      this.prepareJobForSubmission(false);
    },
    previewXml(){
      this.set("showingPreview",true);
    },
    closePreview(){
      this.set("showingPreview",false);
    }
  }
});
