function generateEligibleMountPoints (Y) {

    /* The list we're about to build up. */
    var desiredMountPoints = [];

    var selections = Y.all("#configureClusterMountPointsInputDivId input[type=checkbox]");

    selections.each( function(selection) {

      if( selection.get('checked') == true ) {
        desiredMountPoints.push( selection.get('value') );
      }
    });

    var customMountPointsString = Y.Lang.trim( Y.one("#customMountPointsId").get('value') );

    if( customMountPointsString.length != 0 ) {

      Y.log("custom string = " + customMountPointsString);

      /* Merge the split version of customMountPointsString into our final list
       * of mount points to send back to the server for committing...
       */
      desiredMountPoints.push.apply( desiredMountPoints, customMountPointsString.split(',') );
      Y.log(customMountPointsString.split(',').length);
      Y.log(desiredMountPoints.join(','));
    }

    /* ...But not before performing a de-dupe, just to be safe. */
    return Y.Array.dedupe( desiredMountPoints );
}

function generateServiceDirs (Y, servicesInfo) {

  var generatedServiceDirs = {};

  var eligibleMountPoints = generateEligibleMountPoints(Y);

  Y.Array.each( servicesInfo, function (serviceInfo) {

      var serviceDirs = [];

      for( currentDirNum = 0; currentDirNum < serviceInfo.maxDirectoriesNeeded; ++currentDirNum ) {

        /* serviceInfo.maxDirectoriesNeeded that we get from the server is a cap 
         * on how many directories need to be generated for that service - the
         * user can always pick fewer than that, so guard against going out of
         * bounds.
         */
        if( currentDirNum < eligibleMountPoints.length ) {

          var currentDirName = eligibleMountPoints[currentDirNum];
          
          /* Add a trailing slash if it doesn't exist already. */
          if( currentDirName.substr(-1) != '/' ) {
            currentDirName += '/';
          }

          serviceDirs.push( currentDirName + serviceInfo.suffix );
        }
      }

      var serviceDirValue = serviceDirs.join(',');

      generatedServiceDirs[serviceInfo.keyName] = { 
        'value' : serviceDirValue, 
        'displayName' : serviceInfo.displayName 
      };
    });

  return generatedServiceDirs;
}

function renderEffectiveClusterConfig (Y,generatedClusterConfig) {

  var clusterConfigDisplayMarkup = "";

  for (var configKey in generatedClusterConfig) {
    if (generatedClusterConfig.hasOwnProperty( configKey )) {

      var configElement = generatedClusterConfig[configKey];

      var configElementName = configKey;
      var configElementIdName = configElementName + 'Id';

      /* Inefficient, with all the string concatenations, but clear to read. */
      clusterConfigDisplayMarkup += 
        '<p>' + 
          '<label for=' + configElementIdName + '>' + configElement.displayName + '</label>' +
          '<input type=text name=' + configElementName + ' id=' + configElementIdName + ' readonly=readonly value=' + configElement.value + '>' + 
          '<br/>' +
        '</p>';
    }
  }

  /* Link the newly-generated clusterConfigInputMarkup into the DOM (making 
   * sure it comes before the existing #configureClusterSubmitButtonId), thus
   * rendering it.
   */
  Y.one("#configureClusterMountPointsDisplayDivId").setContent( clusterConfigDisplayMarkup );
}

/* Modify the working version of generatedClusterConfig to make it fit for
 * sending to our backend.
 */  
function polishClusterConfig (generatedClusterConfig) {

  var polishedClusterConfig = {};

  for (var configKey in generatedClusterConfig) {
    if (generatedClusterConfig.hasOwnProperty( configKey )) {
      polishedClusterConfig[ configKey ] = generatedClusterConfig[configKey].value;
    }
  }

  return polishedClusterConfig;
}

function renderConfigureCluster (Y, clusterConfig) {

  var servicesInfo = Y.Array( clusterConfig.servicesInfo );

  Y.one('#configureClusterSubmitButtonId').on('click',function (e) {

      /* For now, our cluster config consists solely of the generated service directories. */
      var generatedClusterConfig = generateServiceDirs(Y, servicesInfo);

      var configureClusterRequestData = { 
          mountPoints : generateEligibleMountPoints(Y), 
          clusterConfig : polishClusterConfig(generatedClusterConfig) 
        };

      Y.io("../php/configureCluster.php?clusterName="+clusterConfig.clusterName, {

          method: 'POST',
          data: Y.JSON.stringify(configureClusterRequestData),
          timeout : 10000,
          on: {
            success: function (x,o) {
                  Y.log("RAW JSON DATA: " + o.responseText);

                  // Process the JSON data returned from the server
                  try {
                    deployInfoJson = Y.JSON.parse(o.responseText);
                  }
                  catch (e) {
                    alert("JSON Parse failed!");
                    return;
                  }

                  Y.log("PARSED DATA: " + Y.Lang.dump(deployInfoJson));

                  /* Done with this stage, hide it. */
                 // Y.one("#configureClusterCoreDivId").setStyle('display','none');

                  /* Render the next stage. */
                  //renderConfigureCluster(Y, clusterConfigJson);

                  /* Show off our rendering. */
                 // Y.one("#configureClusterCoreDivId").setStyle('display','block');
            },
            failure: function (x,o) {
              alert("Async call failed!");
            }
          }
      });
  });

  /* Generate the key form elements into clusterConfigInputMarkup. */
  var mountPoints = Y.Array( clusterConfig.mountPoints.sort(Y.ArraySort.compare) );

  var clusterConfigInputMarkup = "";

  Y.Array.each(mountPoints, function(mountPoint) {

      /* Inefficient, with all the string concatenations, but clear to read. */
      clusterConfigInputMarkup += 
        '<p>' + 
          '<input type=checkbox name=mountPoints checked=true value=' + mountPoint + '>' +
            mountPoint + 
          '<br/>' +
        '</p>';
    });

  /* Link the newly-generated clusterConfigInputMarkup into the DOM (making 
   * sure it comes before the existing #configureClusterSubmitButtonId), thus
   * rendering it.
   */
  Y.one("#configureClusterMountPointsInputDivId").prepend( clusterConfigInputMarkup );

  renderEffectiveClusterConfig(Y, generateServiceDirs(Y, servicesInfo));
}
