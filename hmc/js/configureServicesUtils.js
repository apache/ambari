
/////////////////// Render related functions /////////////////////////////

var globalPasswordsArray = [];

function generateDivForService (option, type, service, property, unit) { 
	
  var unitString = (unit != null) ? unit : '';
  var retString = '<div class="formElement">' +
    '<label for="' + service + '">' + option['displayName'] + '</label>' +
    //((unitString != '') ? '<div class="input-append">' : '') +
    '<input class="unit-' + unit + '" type="' + type + '" id="' + property + '" name="' + service + '" value="' + option['value'] + '"> ' + unitString + 
    '<div class="contextualHelp">' + option['description'] + '</div>' +
    //((unitString != '') ? '</div>' : '') + 
    '<div class="formInputErrorReason" id="' + property + 'ErrorReason' + '"></div>' +
    '</div>';
  if (type == "password") {
    retString += '<div class="formElement">' +
      '<label for="' + service + '"> Retype ' + option['displayName'] + '</label>' +
      '<input type="' + type + '" id="' + property + 'SecretService" name="' + service + '" value="' + option['value'] + '">' +
      '<div class="contextualHelp">' + option['description'] + '</div>' +
      '<div class="formInputErrorReason" id="' + property + 'SecretServiceErrorReason' + '" ></div>' +
      '</div>';

    /// Put it in the global passwd array
    globalPasswordsArray[globalPasswordsArray.length] = {
      "passwordDivId"     : property, 
      "verificationDivId" : property + 'SecretService'
    };
    globalYui.log("Global Passwords Array: " + globalYui.Lang.dump(globalPasswordsArray));

  }
  return retString;
}

function constructDOM(optionsInfo) {
  /* Reset globalPasswordsArray at the beginning of each render cycle to 
   * avoid using stale data from the last run - this isn't a problem on the
   * Configure Services page, but it bites us on the Manage Services page 
   * there is re-use of this module of code within the same JS memory. 
   */
  globalPasswordsArray = [];
  var optionsSummary = "";
  for (servicesKey in optionsInfo['services']) {
    if (optionsInfo['services'][servicesKey]["isEnabled"] == true) {
      var serviceNeedsRender = false;
      var propertiesRendering = "";
      for (property in optionsInfo['services'][servicesKey]["properties"]) {
        // service has configs, so needs render
        var type = convertDisplayType(optionsInfo['services'][servicesKey]['properties'][property]['type']);
//      globalYui.log("TYPE: " + type + "Property: " + property);
        if (type == "NODISPLAY") {
            continue;
        }
        serviceNeedsRender = true;
        var unit = optionsInfo['services'][servicesKey]['properties'][property]['unit'];
        propertiesRendering += generateDivForService(optionsInfo['services'][servicesKey]["properties"][property], type, servicesKey, property, unit);
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
}


/////////////////// End of rendering related functions /////////////////////////////

/////////////////// Submit related functions /////////////////////////////

// use this function for cleaning up the formInputError class added
// to the fields that failed to satisfy correctness
function cleanupClassesForErrors (divId) {
  globalYui.one(divId).removeClass('formInputError');
  globalYui.one(divId + "ErrorReason").setContent('');
}

function cleanupClassesForPasswordErrors () {
  for (count = 0; count < globalPasswordsArray.length; count++) {
    divId = "#" + globalPasswordsArray[count]['verificationDivId'];
    cleanupClassesForErrors(divId);
  }
}

function clearErrorReasons(opts) {
  for(serviceName in opts) {
    globalYui.log('Clear errors for svc : ' +  serviceName);
    globalYui.log(globalYui.Lang.dump(opts[serviceName]['properties']));
    for (propKey in opts[serviceName]['properties']) {
      globalYui.log('Clear errors for prop : ' +  propKey);
      globalYui.one('#' + propKey).removeClass('formInputError');
      var elem = globalYui.one('#' + propKey + 'ErrorReason');
      elem.setContent('');
      //} else {
      //  globalYui.log('Found invalid div for error reason for prop key : ' + propKey);
      //}
    }
  }
}

function addErrorStringToHiddenDiv (divId, errorReason) {
  errorDivId = divId + 'ErrorReason';
  globalYui.one(errorDivId).setContent(errorReason);
}

function checkPasswordCorrectness () {
  var count = 0;
  var focusId = "";
  var passwdMatch = true;
  var errCount = 0;
  var errString = "<ul>";

  for (count = 0; count < globalPasswordsArray.length; count++) {
    var divId = "#" + globalPasswordsArray[count]['passwordDivId'];
    var passwd = globalYui.one(divId).get('value');
    divId = "#" + globalPasswordsArray[count]['verificationDivId'];
    var verifyPasswd = globalYui.one(divId).get('value');

    if (passwd !== verifyPasswd) {
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
}

function generateUserOpts () {

  var retval = checkPasswordCorrectness();
  if (retval.passwdMatched !== true) {
    setFormStatus(retval.errorString, true, true);
    document.getElementById(retval.focusOn).scrollIntoView();
    return {};
  }
  cleanupClassesForPasswordErrors();
 
  var desiredOptions = {};

  var temp = globalYui.all("#configureClusterAdvancedDynamicRenderDivId div[name=configureClusterAdvancedPerServiceDiv]");
  temp.each(function (selection) {

    var selectionStr = "#configureClusterAdvancedDynamicRenderDivId input[name=" + selection.get('id') + "]";
    var prop = globalYui.all(selectionStr);
    var properties = {};
    prop.each(function (proper) {
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
  clearErrorReasons(desiredOptions); 

  return desiredOptions;
}

function handleConfigureServiceErrors(errorResponse) {
  var message = errorResponse.error;
  setFormStatus(message, true, true);
  for (propKey in errorResponse['properties'])  {
    errorReason = errorResponse['properties'][propKey]['error'];
    globalYui.one('#' + propKey).addClass('formInputError');
    var elemReason = propKey + 'ErrorReason';
    globalYui.log('Setting content ' + errorReason + ' for div ' + elemReason);
    globalYui.one('#' + elemReason).setContent(errorReason);
  }
  document.getElementById('formStatusDivId').scrollIntoView()    
}

/////////////////// End of submitting related functions /////////////////////////////
