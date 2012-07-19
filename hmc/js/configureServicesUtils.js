/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

function ConfigureServicesUtil() {

  var passwordsArray = [];
  
  function generateDivForService (option, type, service, property, unit, displayAttributes, isReconfigure) {
  	
    var unitClass = (unit != null) ? 'unit' : '';
    var unitLabel = (unit != null && unit != 'int') ? unit : '';
    
    // default: if this is reconfiguration, default to NOT editable
    // if this is not reconfiguration (original config), default to editable
    var readOnlyFlag = isReconfigure;
    
    if (displayAttributes != null) {
      if (isReconfigure && displayAttributes.reconfigurable != null && displayAttributes.reconfigurable) {
        readOnlyFlag = false;
      } else if (!isReconfigure && displayAttributes.editable != null && !displayAttributes.editable) {
        readOnlyFlag = true;
      }
    }
    
    var checked = (type === 'checkbox' && option.value === 'true') ? 'checked' : '';
  
    var retString = '<div class="formElement">' +
      '<label for="' + service + '">' + option.displayName + '</label>' +
      '<input class="' + unitClass + '" type="' + type + '" id="' + property + '" name="' + service + '" value="' + option.value + '" ' + checked;
    if (readOnlyFlag) {
      retString += ' readonly="readonly" ';
    }
  
    retString += '><label class="unit">' + unitLabel + '</label>' + 
      '<div class="contextualHelp">' + option.description + '</div>' +
      '<div class="formInputErrorReason" id="' + property + 'ErrorReason' + '" style="display:none"></div>' +
      '</div>';
    if (type == "password" && !isReconfigure) {
      retString += '<div class="formElement">' +
        '<label for="' + service + '"> Retype ' + option.displayName + '</label>' +
        '<input type="' + type + '" id="' + property + 'SecretService" class="retypePassword" name="' + service + '" value="' + option.value + '">' +
        '<div class="contextualHelp">' + option.description + '</div>' +
        '<div class="formInputErrorReason" id="' + property + 'SecretServiceErrorReason' + '" style="display:none"></div>' +
        '</div>';
  
      /// Put it in the global passwd array
      passwordsArray[passwordsArray.length] = {
        "passwordDivId"     : property,
        "verificationDivId" : property + 'SecretService'
      };
      // Y.log("Global Passwords Array: " + Y.Lang.dump(passwordsArray));
  
    }
    return retString;
  }
  
  /**
   * isReconfigure: true if this is for reconfiguring parameters on services; false if this is on "Advanced Config" page
   */
  this.getOptionsSummaryMarkup = function(optionsInfo, isReconfigure) {
    /* Reset passwordsArray at the beginning of each render cycle to
     * avoid using stale data from the last run - this isn't a problem on the
     * Configure Services page, but it bites us on the Manage Services page
     * there is re-use of this module of code within the same JS memory.
     */
    passwordsArray = [];
    var tabs = '<ul id="configureServicesTabs" class="nav nav-tabs">';
    var optionsSummary = (isReconfigure) ? '<div>': '<div class="tab-content">';

    var setServiceMarkup = function(service) {
      if (service.isEnabled === '1') {
        var serviceNeedsRender = false;
        var propertiesMarkup = "";
        for (var property in service.properties) {
          // service has configs, so needs render
          var type = convertDisplayType(service.properties[property].type);
          // Y.log("TYPE: " + type + "Property: " + property);
          if (type === "NODISPLAY") {
              continue;
          }
          serviceNeedsRender = true;
          var unit = service.properties[property].unit;
          var displayAttributes = null;
          if (service.properties[property].displayAttributes) {
             displayAttributes = service.properties[property].displayAttributes;
          }
  
          propertiesMarkup += generateDivForService(service.properties[property], type, service.serviceName, property, unit, displayAttributes, isReconfigure);
        }
        if (serviceNeedsRender) {
          tabs += '<li><a data-toggle="tab" href="#' + service.serviceName + '">' + service.displayName + '<span id="' + service.serviceName + 'ErrorCount" class="serviceErrorCount"></span></a></li>';
          // optionsSummary += "<fieldset> <legend>" + service.serviceName + "</legend>";
          optionsSummary += '<div class="tab-pane" name=\"configureClusterAdvancedPerServiceDiv\" id=\"' + service.serviceName + '\">';
          optionsSummary += propertiesMarkup;
          // optionsSummary += '</fieldset></div>';
          optionsSummary += '</div>';
        }
      }
    };    

    if (optionsInfo.services.NAGIOS !== undefined) {
      setServiceMarkup(optionsInfo.services.NAGIOS);
    }
    if (optionsInfo.services.HIVE !== undefined) {
      setServiceMarkup(optionsInfo.services.HIVE);
    }

    for (var servicesKey in optionsInfo.services) {
      if (servicesKey !== 'NAGIOS' && servicesKey !== 'HIVE') {
        setServiceMarkup(optionsInfo.services[servicesKey]);  
      }      
    }
    
    tabs += '</ul>';
    optionsSummary += '</div>';
    return (isReconfigure) ? optionsSummary : tabs + optionsSummary;
  };
  
  
  /////////////////// End of rendering related functions /////////////////////////////
  
  /////////////////// Submit related functions /////////////////////////////
    
  this.clearPasswordErrors = function () {
    for (var count = 0; count < passwordsArray.length; count++) {
      divId = "#" + passwordsArray[count]['verificationDivId'];
      this.clearErrorReason(divId);
    }
  };
  
  this.clearErrorReasons = function (opts) {
    for(serviceName in opts) {
      for (propKey in opts[serviceName]['properties']) {
        this.clearErrorReason('#' + propKey);
      }
    }
    // clear the error count displayed in all service tabs
    Y.all('.serviceErrorCount').setContent('');
  };
  
  this.setErrorReason = function(fieldDivId, errorReason) {
    Y.one(fieldDivId).addClass('formInputError');
    errorDivId = fieldDivId + 'ErrorReason';
    Y.one(errorDivId).setContent(errorReason);
    Y.one(errorDivId).show();
  };
  
  this.clearErrorReason = function(fieldDivId) {
    Y.one(fieldDivId).removeClass('formInputError');
    errorDivId = fieldDivId + 'ErrorReason';
    Y.one(errorDivId).setContent('');
    Y.one(errorDivId).hide();
  };
  
  this.checkPasswordCorrectness = function () {
    var count = 0;
    var focusId = '';
    var passwdMatch = true;
    var errCount = 0;
    var errString = ''; //"<ul>";
  
    for (count = 0; count < passwordsArray.length; count++) {
      var divId = "#" + passwordsArray[count]['passwordDivId'];
      var passwd = Y.one(divId).get('value');
      divId = "#" + passwordsArray[count]['verificationDivId'];
      var verifyPasswd = Y.one(divId).get('value');
  
      if (passwd !== verifyPasswd) {
        errCount++;
        errString = "Password does not match";
        this.setErrorReason(divId, errString);
        if (focusId === '') {
          focusId = divId;
        }
        passwdMatch = false;
      } else {
        this.clearErrorReason(divId, '');
      }
    }
  
    errString += ''; //"</ul>";
  
    retArray = {
      "passwdMatched" : passwdMatch,
      "focusOn"       : focusId,
      "errorCount"    : errCount,
      "errorString"   : errString
    };
  
    return retArray;
  };
  
  this.generateUserOpts = function () {
   
    var desiredOptions = {};
  
    var temp = Y.all("#configureClusterAdvancedDynamicRenderDivId div[name=configureClusterAdvancedPerServiceDiv]");
    temp.each(function (selection) {
  
      var selectionStr = "#configureClusterAdvancedDynamicRenderDivId input[name=" + selection.get('id') + "]";
      var prop = Y.all(selectionStr);
      var properties = {};
      prop.each(function (proper) {
        for (var i = 0; i < passwordsArray.length; i++) {
          if (proper.get('id') == passwordsArray[i]['verificationDivId']) {
            return;
          }
        }
 
        var value = Y.Lang.trim(proper.get('value'));
        if ((proper.get('type') == "checkbox")) {
          value = proper.get('checked').toString();
        }
  
        var keyName = Y.Lang.trim(proper.get('id'));
        properties[keyName] = {
        "value" : value,
        };
  
     });
  
      desiredOptions[selection.get('id')] = {
      "properties" : properties,
      };
  
    });
  
    clearFormStatus();
    this.clearErrorReasons(desiredOptions);
  
    return desiredOptions;
  };
  
  // update the error count displayed in the tab for the specified service.
  // also toggle the disabled status of the submit button.
  this.updateServiceErrorCount = function (serviceName) {
    var errorCount = 0;
    var serviceDivId = '#' + serviceName;
    var serviceDiv = Y.one(serviceDivId);
    var errorFields = serviceDiv.all('.formInputErrorReason');
    errorFields.each(function (errorField) {
      if (errorField.getStyle('display') !== 'none') {
        errorCount++;
      }
    });
    
    var submitButton = Y.one('#configureClusterAdvancedSubmitButtonId');

    if (errorCount > 0) {
      Y.one(serviceDivId + 'ErrorCount').setContent('<span style="margin-left:4px" class="badge badge-important">' + errorCount + '</span>');
      // there is at least one error.  disable the submit button      
      submitButton.addClass('disabled');
    } else {
      Y.one(serviceDivId + 'ErrorCount').setContent('');
      // if no errors at all, enable the submit button and clear error message at the top
      if (this.getTotalErrorCount() === 0) { 
        submitButton.removeClass('disabled');
        clearFormStatus();
      }
    }
  };
  
  this.getTotalErrorCount = function () {
    var totalErrorCount = 0;
    var tabsDiv = Y.one('#configureServicesTabs');
    var errorCountBadges = tabsDiv.all('span.badge');
    errorCountBadges.each(function (errorCountBadge) {
      totalErrorCount += parseInt(errorCountBadge.getHTML(), 10);
    });
    return totalErrorCount;
  };
  
  this.handleConfigureServiceErrors = function (errorResponse) {
    var errorCounts = {};
    var message = errorResponse.error;
    var serviceName = '';
    setFormStatus(message, true, true);
    for (propKey in errorResponse.properties)  {
      var errorReason = errorResponse.properties[propKey].error;
      var propDom = Y.one('#' + propKey);
      serviceName = propDom.get('name');
      if (errorCounts[serviceName] == null) {
        errorCounts[serviceName] = 1;
      } else {
        errorCounts[serviceName] += 1;
      }
    
      this.setErrorReason('#' + propKey, errorReason);
    }

    // if this is being invoked from cluster install wizard, update tabs with error counts.
    // else this is being invoked from reconfigure services so there are no tabs to update.
    var tabs = $('#configureServicesTabs');
    if (tabs.length > 0) {
      var firstServiceName = null;
      // show error counts in the tab for each service that had errors
      for (serviceName in errorCounts) {
        if (firstServiceName === null) {
          firstServiceName = serviceName;
        }
        this.updateServiceErrorCount(serviceName, errorCounts[serviceName]);
      }
      // open the first tab that has an error
      tabs.find('a[href="#' + firstServiceName + '"]').tab('show');
    }
    Y.one('#formStatusDivId').scrollIntoView();
    
  }.bind(this);
};

var configureServicesUtil = new ConfigureServicesUtil();
