globalYui.one('#createClusterSubmitButtonId').on('click',function (e) {

      var createClusterData = {
        "clusterName" : globalYui.Lang.trim(globalYui.one("#clusterNameId").get('value')),
      };

      globalYui.log("Cluster Name: "+globalYui.Lang.dump(createClusterData));

      if (createClusterData.clusterName == '') {
        globalYui.one("#clusterNameId").addClass('formInputError');
        setFormStatus("Cluster name cannot be empty", true);
        globalYui.one("#clusterNameId").focus();
        return;
      }

      clearFormStatus();
      globalYui.one("#clusterNameId").removeClass('formInputError');
      globalYui.io("../php/frontend/createCluster.php", {

          method: 'POST',
          data: globalYui.JSON.stringify(createClusterData),
          timeout : 10000,
          on: {

            start: function (x,o) {
              showLoadingImg();
            },
            success: function (x,o) {
              hideLoadingImg();
              globalYui.log("RAW JSON DATA: " + o.responseText);

              // Process the JSON data returned from the server
              try {
                createClusterResponseJson = globalYui.JSON.parse(o.responseText);
              }
              catch (e) {
                alert("JSON Parse failed!");
                return;
              }

              globalYui.log("PARSED DATA: " + globalYui.Lang.dump(createClusterResponseJson));

              if (createClusterResponseJson.result != 0) {
                // Error!
                alert("Got error!" + createClusterResponseJson.error); 
                return;
              }

              createClusterResponseJson = createClusterResponseJson.response;

              /* Done with this stage, transition to the next. */
              // Add freshInstall tag
              createClusterResponseJson.freshInstall = true;
              transitionToNextStage( "#createClusterCoreDivId", createClusterData, 
                  "#addNodesCoreDivId", createClusterResponseJson, InstallationWizard.AddNodes.render );
            },
            failure: function (x,o) {
              hideLoadingImg();
              alert("Async call failed!");
            }
          }
      });
});


/* At the end of the installation wizard, we hide 
 * #installationWizardProgressBarDivId, so make sure we explicitly show
 * it at the beginning, to ensure we work correctly when user flow 
 * (potentially) cycles back here.
 */
globalYui.one('#installationWizardProgressBarDivId').setStyle('display', 'block');
