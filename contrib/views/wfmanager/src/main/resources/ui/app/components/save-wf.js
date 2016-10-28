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
  })
});


export default Ember.Component.extend(Validations, {
  showingFileBrowser : false,
  jobXml : "",
  overwritePath : false,
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
    this.set("jobXml", this.get("jobConfigs").xml);
    this.set('filePath', Ember.copy(this.get('jobFilePath')));
  }.on('init'),
  rendered : function(){
    this.$("#configureJob").on('hidden.bs.modal', function () {
      this.sendAction('closeJobConfigs');
    }.bind(this));
    this.$("#configureJob").modal("show");    
  }.on('didInsertElement'),
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
    if(data.stackTrace.length){
      this.set("stackTrace", data.stackTrace);
      this.set("isStackTraceAvailable", true);
    } else {
      this.set("isStackTraceAvailable", false);
    }
  },
  saveJob(){
    var url = Ember.ENV.API_URL + "/saveWorkflow?app.path=" + this.get("filePath") + "&overwrite=" + this.get("overwritePath");
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
        //var result=JSON.parse(response);
        this.showNotification({
          "type": "success",
          "message": "Workflow have been saved"
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
    saveWorkflow(){
		if(!this.get("validations.isInvalid")){
	      this.sendAction("setFilePath", this.get("filePath"));
	      this.set('showErrorMessage', true);
	      this.saveJob();
		}
    },
    closePreview(){
      this.set("showingPreview",false);
    }
  }
});
