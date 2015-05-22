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

define(['require', 'utils/LangSupport', 'bootstrap.notify'], function(require, localization) {
  'use strict';

  var Utils = {};

  Utils.defaultErrorHandler = function(model, error) {
    if (error.status == 401) {
      console.log("ERROR 401 occured.");
    }
  };

  Utils.uploadFile = function(restURL, data, successCallback, errorCallback){
    $.ajax({
        url: restURL,
        data: data,
        cache: false,
        contentType: false,
        processData: false,
        type: 'POST',
        success: successCallback,
        error: errorCallback
      });
  };

  Utils.notifyError = function(message){
    $('.top-right').notify({
      message: { html: "<i class='fa fa-warning'></i> " + message},
      type: 'danger',
      closable: true,
      transition: 'fade',
      fadeOut: { enabled: true, delay: 3000 }
    }).show();
  };

  Utils.notifySuccess = function(message){
    $('.top-right').notify({
      message: { html: "<i class='fa fa-check'></i> " + message},
      type: 'success',
      closable: true,
      transition: 'fade',
      fadeOut: { enabled: true, delay: 3000 }
    }).show();
  };

  Utils.notifyInfo = function(message){
    $('.top-right').notify({
      message: { html: "<i class='fa fa-info'></i> " + message},
      type: 'warning',
      closable: true,
      transition: 'fade',
      fadeOut: { enabled: true, delay: 3000 }
    }).show();
  };

  Utils.getStormHostDetails = function(){
    var url = location.pathname+'proxy?url=';
    $.ajax({
      url: '/api/v1/clusters/',
      cache: false,
      type: 'GET',
      async: false,
      success: function(response){
        var result = JSON.parse(response);
        if(_.isArray(result.items) && result.items.length){
          var flag = false;
          _.each(result.items, function(object){
            if(!flag){
              $.ajax({
                url: object.href,
                type: 'GET',
                async: false,
                success: function(res){
                  var config = JSON.parse(res);
                  var hostname;
                  _.each(config.alerts, function(obj){
                    if(obj.Alert.service_name === "STORM" && obj.Alert.definition_name === "storm_webui"){
                      hostname = obj.Alert.host_name;
                    }
                  });
                  if(_.isUndefined(hostname) || hostname == ""){
                    Utils.notifyError(localization.tt('msg.stormNotRunning'));
                  } else {
                    var obj = _.findWhere(config.service_config_versions, {"service_name": "STORM"});
                    if(!_.isUndefined(obj)){
                      var stormConfig = _.findWhere(obj.configurations, {"type": "storm-site"});
                      if(! _.isUndefined(stormConfig)){
                        flag = true;
                        url += 'http://'+hostname+':'+stormConfig.properties['ui.port'];
                      } else {
                        Utils.notifyError(localization.tt('msg.stormNotRunning'));
                      }
                    } else {
                      Utils.notifyError(localization.tt('msg.stormNotRunning'));
                    }
                  }
                },
                error: function(res){
                  Utils.notifyError(localization.tt('msg.stormNotRunning'));
                }
              });
            }
          });
        } else {
          Utils.notifyError(localization.tt('msg.stormNotConfigured'));
        }
      },
      error: function(error){
        Utils.notifyError(localization.tt('msg.stormNotRunning'));
      }
    });
    return url;
  };

  return Utils;
});