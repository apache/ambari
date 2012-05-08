function generateServiceMasterOptions (Y, masterHost, allHosts) {

  var generatedOptions = "<option>" + masterHost + "</option>";

  /* The dropdown hosts are allHosts minus masterHost. */
  Y.Array.each( allHosts, function(host) {

      if( host != masterHost ) {
        generatedOptions += "<option>" + host + "</option>";
      }
    });

  return generatedOptions;
}

function renderDeploy (Y, deployInfo) {

  Y.one('#deploySubmitButtonId').on('click',function (e) {

      e.target.set('disabled', true);

      var deployRequestData = {};

      Y.io("../php/deploy.php?clusterName="+deployInfo.clusterName, {

          method: 'POST',
          data: Y.JSON.stringify(deployRequestData),
          timeout : 10000,
          on: {
            success: function (x,o) {
                  e.target.set('disabled', false);
                  Y.log("RAW JSON DATA: " + o.responseText);

                  // Process the JSON data returned from the server
                  try {
                    xxxJson = Y.JSON.parse(o.responseText);
                  }
                  catch (e) {
                    alert("JSON Parse failed!");
                    return;
                  }

                  Y.log("PARSED DATA: " + Y.Lang.dump(xxxJson));

//                  /* Done with this stage, hide it. */
//                  Y.one("#deployCoreDivId").setStyle('display','none');
//
//                  /* Render the next stage. */
//                  renderConfigureCluster(Y, xxxJson);
//
//                  /* Show off our rendering. */
//                  Y.one("#configureClusterCoreDivId").setStyle('display','block');
            },
            failure: function (x,o) {
                  e.target.set('disabled', false);
              alert("Async call failed!");
            }
          }
      });
  });
}
