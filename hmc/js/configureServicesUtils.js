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
  
    var retString = '<div class="formElement">' +
      '<label for="' + service + '">' + option.displayName + '</label>' +
      '<input class="' + unitClass + '" type="' + type + '" id="' + property + '" name="' + service + '" value="' + option.value + '"';
    if (readOnlyFlag) {
      retString += ' readonly="readonly" ';
    }
  
    retString += '><label class="unit">' + unitLabel + '</label>' + 
      '<div class="contextualHelp">' + option.description + '</div>' +
      '<div class="formInputErrorReason" id="' + property + 'ErrorReason' + '"></div>' +
      '</div>';
    if (type == "password") {
      retString += '<div class="formElement">' +
        '<label for="' + service + '"> Retype ' + option.displayName + '</label>' +
        '<input type="' + type + '" id="' + property + 'SecretService" name="' + service + '" value="' + option.value + '">' +
        '<div class="contextualHelp">' + option.description + '</div>' +
        '<div class="formInputErrorReason" id="' + property + 'SecretServiceErrorReason' + '" ></div>' +
        '</div>';
  
      /// Put it in the global passwd array
      passwordsArray[passwordsArray.length] = {
        "passwordDivId"     : property,
        "verificationDivId" : property + 'SecretService'
      };
      // globalYui.log("Global Passwords Array: " + globalYui.Lang.dump(passwordsArray));
  
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
    var optionsSummary = "";
    for (servicesKey in optionsInfo['services']) {
      if (optionsInfo['services'][servicesKey]["isEnabled"] == true) {
        var serviceNeedsRender = false;
        var propertiesRendering = "";
        for (property in optionsInfo['services'][servicesKey]["properties"]) {
          // service has configs, so needs render
          var type = convertDisplayType(optionsInfo['services'][servicesKey]['properties'][property]['type']);
          // globalYui.log("TYPE: " + type + "Property: " + property);
          if (type == "NODISPLAY") {
              continue;
          }
          serviceNeedsRender = true;
          var unit = optionsInfo['services'][servicesKey]['properties'][property]['unit'];
          var displayAttributes = null;
          if (optionsInfo['services'][servicesKey]['properties'][property]['displayAttributes']) {
             displayAttributes = optionsInfo['services'][servicesKey]['properties'][property]['displayAttributes'];
          }
  
          propertiesRendering += generateDivForService(optionsInfo['services'][servicesKey]["properties"][property], type, servicesKey, property, unit, displayAttributes, isReconfigure);
        }
        if (serviceNeedsRender) {
          optionsSummary += "<fieldset> <legend>" + servicesKey + "</legend>";
          optionsSummary += '<div name=\"configureClusterAdvancedPerServiceDiv\" id=\"' + servicesKey + '\">';
          optionsSummary += propertiesRendering;
          optionsSummary += '</fieldset></div>';
        }
      }
    }
    return optionsSummary;
  };
  
  
  /////////////////// End of rendering related functions /////////////////////////////
  
  /////////////////// Submit related functions /////////////////////////////
  
  // use this function for cleaning up the formInputError class added
  // to the fields that failed to satisfy correctness
  function cleanupClassesForErrors (divId) {
    globalYui.one(divId).removeClass('formInputError');
    globalYui.one(divId + "ErrorReason").setContent('');
  }
  
  this.cleanupClassesForPasswordErrors = function () {
    for (var count = 0; count < passwordsArray.length; count++) {
      divId = "#" + passwordsArray[count]['verificationDivId'];
      cleanupClassesForErrors(divId);
    }
  };
  
  this.clearErrorReasons = function (opts) {
    for(serviceName in opts) {
      //globalYui.log('Clear errors for svc : ' +  serviceName);
      //globalYui.log(globalYui.Lang.dump(opts[serviceName]['properties']));
      for (propKey in opts[serviceName]['properties']) {
        //globalYui.log('Clear errors for prop : ' +  propKey);
        globalYui.one('#' + propKey).removeClass('formInputError');
        var elem = globalYui.one('#' + propKey + 'ErrorReason');
        elem.setContent('');
        //} else {
        //  globalYui.log('Found invalid div for error reason for prop key : ' + propKey);
        //}
      }
    }
  };
  
  function addErrorStringToHiddenDiv (divId, errorReason) {
    errorDivId = divId + 'ErrorReason';
    globalYui.one(errorDivId).setContent(errorReason);
  }
  
  this.checkPasswordCorrectness = function () {
    var count = 0;
    var focusId = "";
    var passwdMatch = true;
    var errCount = 0;
    var errString = "<ul>";
  
    for (count = 0; count < passwordsArray.length; count++) {
      var divId = "#" + passwordsArray[count]['passwordDivId'];
      var passwd = globalYui.one(divId).get('value');
      divId = "#" + passwordsArray[count]['verificationDivId'];
      var verifyPasswd = globalYui.one(divId).get('value');
  
      if (passwd != verifyPasswd) {
        errCount++;
        errString += "<li>Password does not match for " + globalYui.one(divId).get('name') + "</li>";
        globalYui.one(divId).addClass('formInputError');
        addErrorStringToHiddenDiv(divId, errString);
        if (focusId == '') {
          focusId = "formStatusDivId";
        }
        passwdMatch = false;
      } else {
        globalYui.one(divId).removeClass('formInputError');
        addErrorStringToHiddenDiv(divId, '');
      }
    }
  
    errString += "</ul>";
  
    retArray = {
      "passwdMatched" : passwdMatch,
      "focusOn"       : focusId,
      "errorCount"    : errCount,
      "errorString"   : errString
    };
  
    return retArray;
  };
  
  this.generateUserOpts = function () {
  
    var retval = this.checkPasswordCorrectness();
    if (retval.passwdMatched != true) {
      setFormStatus(retval.errorString, true, true);
      document.getElementById(retval.focusOn).scrollIntoView();
      return {};
    }
    this.cleanupClassesForPasswordErrors();
  
    var desiredOptions = {};
  
    var temp = globalYui.all("#configureClusterAdvancedDynamicRenderDivId div[name=configureClusterAdvancedPerServiceDiv]");
    temp.each(function (selection) {
  
      var selectionStr = "#configureClusterAdvancedDynamicRenderDivId input[name=" + selection.get('id') + "]";
      var prop = globalYui.all(selectionStr);
      var properties = {};
      prop.each(function (proper) {
        for (var i = 0; i < passwordsArray.length; i++) {
          if (proper.get('id') == passwordsArray[i]['verificationDivId']) {
            return;
          }
        }
  
        var value = globalYui.Lang.trim(proper.get('value'));
        if ((proper.get('type') == "checkbox")) {
          value = proper.get('checked').toString();
        }
  
        var keyName = globalYui.Lang.trim(proper.get('id'));
        properties[keyName] = {
        "value" : value,
        };
  
     });
  
      desiredOptions[selection.get('id')] = {
      "properties" : properties,
      };
  
    });
  
  //  globalYui.log("Final Options: " + globalYui.Lang.dump(desiredOptions));
  
    clearFormStatus();
    this.clearErrorReasons(desiredOptions);
  
    return desiredOptions;
  };
  
  this.handleConfigureServiceErrors = function (errorResponse) {
    var message = errorResponse.error;
    setFormStatus(message, true, true);
    for (propKey in errorResponse['properties'])  {
      errorReason = errorResponse['properties'][propKey]['error'];
      globalYui.one('#' + propKey).addClass('formInputError');
      var elemReason = propKey + 'ErrorReason';
      //globalYui.log('Setting content ' + errorReason + ' for div ' + elemReason);
      globalYui.one('#' + elemReason).setContent(errorReason);
    }
    document.getElementById('formStatusDivId').scrollIntoView();
  };
};

var configureServicesUtil = new ConfigureServicesUtil();