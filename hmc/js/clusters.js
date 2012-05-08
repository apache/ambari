function generateClusterHostRoleMappingMarkup( clusterServices ) {

  var clusterHostRoleMappingMarkup = 
  '<fieldset id=clustersHostRoleMappingFieldsetId>' +
    '<legend>' +
      'Locations Of Service Masters' + 
    '</legend>';

  for (var serviceName in clusterServices) {
    if (clusterServices.hasOwnProperty(serviceName)) {

      if (clusterServices[serviceName].isEnabled == "1" && 
          clusterServices[serviceName].attributes.runnable &&
          !clusterServices[serviceName].attributes.noDisplay) {

        globalYui.Array.each( clusterServices[serviceName].components, function (serviceComponent) {
          
          if (serviceComponent.isMaster) {
            clusterHostRoleMappingMarkup += 
              '<div>' + 
                '<label>' + serviceComponent.displayName + '</label>' + 
                ' : ' +
                serviceComponent.hostName +
              '</div>' + 
              '<br/>';
          }
        });
      }
    }
  }

  clusterHostRoleMappingMarkup += 
  '</fieldset>';

  return clusterHostRoleMappingMarkup;
}

/* Main() */

/* The clusterName variable is set in the Javascript scaffolding spit out by manageServices.php */
globalYui.io( "../php/frontend/fetchClusterServices.php?clusterName=" + clusterName + "&getConfigs=true&getComponents=true", {
  timeout: 10000,
  on: {
    success: function(x, o) {

      hideLoadingImg();

      globalYui.log("RAW JSON DATA: " + o.responseText);

      var clusterServicesResponseJson;

      try {
        clusterServicesResponseJson = globalYui.JSON.parse(o.responseText);
      }
      catch (e) {
        alert("JSON Parse failed!");
        return;
      }

      globalYui.log(globalYui.Lang.dump(clusterServicesResponseJson));

      /* Check that clusterServicesResponseJson actually indicates success. */
      if( clusterServicesResponseJson.result == 0 ) {
        
        var clusterServices = clusterServicesResponseJson.response.services;

        /* Link the newly-generated markup into the DOM. */
        globalYui.one("#clustersHostRoleMappingDynamicRenderDivId").setContent( generateClusterHostRoleMappingMarkup(clusterServices) );

      }
      else {
        alert("Zoinks! Fetching Cluster Services failed!");
      }
    },
    failure: function(x, o) {

      hideLoadingImg();

      alert("Async call failed!");
    }
  }
});

