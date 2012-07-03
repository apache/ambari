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

function generateEligibleMountPoints () {

    /* The list we're about to build up. */
    var desiredMountPoints = [];

    var selections = globalYui.all("#configureClusterMountPointsDynamicRenderDivId input[type=checkbox]");
    selections.each( function(selection) {

      if( selection.get('checked') == true ) {
        desiredMountPoints.push( selection.get('value') );
      }
    });

    globalYui.log("desired mount points: "+globalYui.Lang.dump(desiredMountPoints));
    var customMountPointsString = globalYui.Lang.trim( globalYui.one("#customMountPointsId").get('value') );

    if( customMountPointsString.length != 0 ) {

      globalYui.log("custom string = " + customMountPointsString);

      /* Merge the split version of customMountPointsString into our final list
       * of mount points to send back to the server for committing...
       */
      desiredMountPoints.push.apply( desiredMountPoints, globalYui.Array.filter(customMountPointsString.split(','), function (elem) {
          if (globalYui.Lang.trim(elem).length > 0) {
            return true;
          } else {
            return false;
          }
      } ));
    
      globalYui.log(desiredMountPoints.join(','));
    }

    /* ...But not before performing a de-dupe, just to be safe. */
    return globalYui.Array.dedupe( desiredMountPoints );
}

function generateServiceDirs (servicesInfo) {

  var generatedServiceDirs = {};

  var eligibleMountPoints = generateEligibleMountPoints();

  for (items in servicesInfo) {
    for (serviceName in servicesInfo[items]) {
      for (component in servicesInfo[items][serviceName]) {

      var serviceInfo = servicesInfo[items][serviceName][component];

      var serviceDirs = [];

      for( currentDirNum = 0; currentDirNum < eligibleMountPoints.length; ++currentDirNum ) {

        /* serviceInfo.maxDirectoriesNeeded that we get from the server is a cap 
         * on how many directories need to be generated for that service - the
         * user can always pick fewer than that, so guard against going out of
         * bounds.
         */
         if ((serviceInfo.maxDirectoriesNeeded != -1) && 
             (currentDirNum >= serviceInfo.maxDirectoriesNeeded)) {
           break;
         }

          var currentDirName = eligibleMountPoints[currentDirNum];
          
          /* Add a trailing slash if it doesn't exist already. */
          if( currentDirName.substr(-1) != '/' ) {
            currentDirName += '/';
          }

          serviceDirs.push( currentDirName + serviceInfo.suffix );
      }

      var serviceDirValue = serviceDirs.join(',');

      generatedServiceDirs[component] = { 
        "serviceName" : serviceName,
        'value'       : serviceDirValue, 
        'displayName' : serviceInfo.displayName 
      };
    }
    }
  }

  globalYui.log("Generated Service Dirs: "+globalYui.Lang.dump(generatedServiceDirs));

  return generatedServiceDirs;
}

function renderEffectiveClusterConfig (generatedClusterConfig) {

  var clusterConfigDisplayMarkup = "";

  for (var configKey in generatedClusterConfig) {
    if (generatedClusterConfig.hasOwnProperty( configKey )) {

      var configElement = generatedClusterConfig[configKey];

      var configElementName = configKey;
      var configElementIdName = configElementName + 'Id';

      var dirsArray = configElement.value.split(',');

      /* Inefficient, with all the string concatenations, but clear to read. */
      clusterConfigDisplayMarkup += 
        '<div class="formElement">' + 
          '<label for=' + configElementIdName + '>' + configElement.displayName + '</label>' +
          '<ul style="list-style-type: none;float:left;" id=' + configElementIdName + '>'; 
        for (var dirs in dirsArray) {
          clusterConfigDisplayMarkup += 
          '<li>' + dirsArray[dirs] + '</li>';
        } 

        clusterConfigDisplayMarkup += '</ul>' + '<div style="clear:both"></div></div>';
        globalYui.log("HTML GENERATED: " + clusterConfigDisplayMarkup);
    }
  }

  /* Link the newly-generated clusterConfigInputMarkup into the DOM (making 
   * sure it comes before the existing #configureClusterSubmitButtonId), thus
   * rendering it.
   */
  globalYui.one("#configureClusterMountPointsDisplayDivId").setContent( clusterConfigDisplayMarkup );
}

/* Modify the working version of generatedClusterConfig to make it fit for
 * sending to our backend.
 */  
function polishClusterConfig (generatedClusterConfig) {

  var polishedClusterConfig = {};

  for (var configKey in generatedClusterConfig) {
    if (generatedClusterConfig.hasOwnProperty( configKey )) {
      serviceName = generatedClusterConfig[configKey]["serviceName"];
      if (!polishedClusterConfig.hasOwnProperty(serviceName)) {
        polishedClusterConfig[serviceName] = {};
      }
      polishedClusterConfig[serviceName][configKey] = generatedClusterConfig[configKey].value;
    }
  }

  return polishedClusterConfig;
}

var globalServicesInfo = null;

function eventHandlerFunc (e)
{
  renderEffectiveClusterConfig(generateServiceDirs(globalServicesInfo));
}

var registeredConfigureClusterEventHandlers = false;

function renderConfigureCluster (clusterConfig) {

  globalServicesInfo = globalYui.Array( clusterConfig.servicesInfo );

  /* Clear out the contents of #customMountPointsId each time we render this
   * screen, to maintain our guarantee of invalidating all forward pages once
   * the user moves back.
   */
  globalYui.one("#customMountPointsId").set('value', '');

  if( !registeredConfigureClusterEventHandlers ) {

    globalYui.one('#configureClusterSubmitButtonId').on('click',function (e) {

        e.target.set('disabled', true);

        var itemsExist = false;
        var selections = globalYui.all("#configureClusterMountPointsDynamicRenderDivId input[type=checkbox]");
        selections.each( function(selection) {
          if( selection.get('checked') == true ) {
            itemsExist = true;
          }
        });
        if (globalYui.Lang.trim( globalYui.one("#customMountPointsId").get('value') ) != '') {
          itemsExist = true;
        }
        if (!itemsExist) {
          alert("Please select one mount point at the least");
          e.target.set('disabled', false);
          return;
        }

        /* For now, our cluster config consists solely of the generated service directories. */
        var generatedClusterConfig = generateServiceDirs(globalServicesInfo);

        var configureClusterRequestData = { 
            mountPoints : generateEligibleMountPoints(), 
            clusterConfig : polishClusterConfig(generatedClusterConfig) 
        };

        globalYui.log(globalYui.Lang.dump(configureClusterRequestData.clusterConfig));

        var url = "../php/frontend/configureCluster.php?clusterName="+clusterConfig.clusterName;
        var requestData = configureClusterRequestData;
        var submitButton = e.target;
        var thisScreenId = "#configureClusterCoreDivId";
        var nextScreenId = "#configureClusterAdvancedCoreDivId";
        var nextScreenRenderFunction = renderOptionsPage;
        submitDataAndProgressToNextScreen(url, requestData, submitButton, thisScreenId, nextScreenId, nextScreenRenderFunction);
     });
    
    globalYui.one('#previewLinkId').on('click', function(e) {
      previewPanel = createInformationalPanel('#informationalPanelContainerDivId', 'Preview Directories to be used by Hadoop');
      previewPanel.set('centered', true);
      previewPanel.set('bodyContent', globalYui.one('#configureClusterDisplayDivId').getContent());
      var okButton = {
          value: 'OK',
          action: function (e) {
            e.preventDefault();
            destroyInformationalPanel(previewPanel);
          },
          section: 'footer'
        };
      previewPanel.addButton(okButton);
      previewPanel.show();
    });

    /* event on mountPoints to be checked. */
    globalYui.one('#configureClusterMountPointsInputDivId').delegate(
        { 
        'click': eventHandlerFunc, 
        'keyup' : eventHandlerFunc
        }, 
        "input[type=checkbox],input[type=text]");

    registeredConfigureClusterEventHandlers = true;
  }

  /* Generate the key form elements into clusterConfigInputMarkup. */
  var mountPoints = globalYui.Array( clusterConfig.mountPoints.sort(globalYui.ArraySort.compare) );

  var clusterConfigInputMarkup = "";

  globalYui.Array.each(mountPoints, function(mountPoint) {

      /* Inefficient, with all the string concatenations, but clear to read. */
      clusterConfigInputMarkup += 
          '<label class="checkbox" for="">' + mountPoint + 
          '<input type=checkbox name=mountPoints id=mountPointsId checked=true value=' + mountPoint + '>' +
          '</label><br/>'; 
    });

  /* Link the newly-generated clusterConfigInputMarkup into the DOM. */
  globalYui.one("#configureClusterMountPointsDynamicRenderDivId").setContent( clusterConfigInputMarkup );

  hideLoadingImg();
  globalYui.one('#configureClusterCoreDivId').setStyle("display", "block");

  renderEffectiveClusterConfig(generateServiceDirs(globalServicesInfo));

}
