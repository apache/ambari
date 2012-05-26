<html>
  <head>
    <title id="pageTitleId">Hortonworks Management Center</title>

    <!-- CSS -->
    <link type="text/css" rel="stylesheet" href="../yui-3.4.1/build/cssreset/cssreset-min.css">
	<link type="text/css" rel="stylesheet" href="../css/bootstrap.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common3.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/selectHosts.css" media="screen"/>
    <!-- End CSS -->
  </head>

  <body class="yui3-skin-sam">
    <?php require "./topnav.htmli"; ?>

    <div id="ContentDivId"> 

      <!-- List of clusters
      <div name="clustersListDiv" id="clustersListDivId">
      </div>
      -->

      <!-- Installation Wizard -->
      <div name="installationWizardDiv" id="installationWizardDivId" style="display:block">
        <div name="installationWizardProgressBarDiv" id="installationWizardProgressBarDivId">
          <ol id="installationWizardProgressBarListId">
            <li id="addNodesStageId" class="installationWizardCurrentStage">
              <div>
                <span class="installationWizardStageNumber">
                  1   
                </span>
                Add nodes
              </div>
            </li>
            <li id="selectServicesStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  2   
                </span>
                Select services
              </div>
            </li>
          </ol>
        </div>

        <br/>

        <?php require "./utils.htmli"; ?>

        <div name="installationMainFormsDiv" id="installationMainFormsDivId">
            <div class="pageSummary">
              <h2>Which nodes are you adding to the cluster?</h2>
              We'll use the SSH private key and public key files to perform installation on your nodes.
              <span>The public key that is paired with the private key must already be installed on all the nodes.</span>
            </div>
            <div id="formStatusDivId" class="formStatusBar" style="display:none">
            </div>
            <div class="pageContent">
              <div id="addNodesCoreDivId" style="display:block">
                <fieldset>
                  <form id="addNodesFilesFormId" enctype="multipart/form-data" method="post">
                    <p>
                    <label for="clusterDeployUserId">SSH Username</label>
                    <input type="text" name="ClusterDeployUser" id="clusterDeployUserId" value="root" placeholder="">
                    </p>
                    <br/>
                    <p>
                    <label for="clusterDeployUserIdentityFileId">SSH Private Key File</label>
                    <input type="file" name="clusterDeployUserIdentityFile" id="clusterDeployUserIdentityFileId" value="" placeholder="">
                    </p>
                    <br/>
                    <p>
                    <label for="clusterHostsFileId">Hosts File (newline-delimited list of hostnames)</label>
                    <input type="file" name="clusterHostsFile" id="clusterHostsFileId" value="" placeholder="">
                    </p>

                    <div id="fileUploadWrapperDivId">
                      <iframe name="fileUploadTarget" id="fileUploadTargetId" src="about:blank" style="display:none"></iframe>
                    </div>
                  </form>
                </fieldset>
                <button class="btn btn-large" id="addNodesSubmitButtonId">Add Nodes</button>
              </div>
            </div>

              <div name="selectServicesCoreDiv" id="selectServicesCoreDivId" style="display:none">
                <div id="formStatusDivId" class="formStatusBar" style="display:none">
                </div>
                <fieldset>
                  <!--<legend>Select Services</legend>-->
                  <form id="addNodesDataFormId">
                    <div name="selectComponentsDynamicRenderDiv" id="selectComponentsDynamicRenderDivId"></div>
                  </form>
                </fieldset>
                <button class="btn btn-large" id="deployAddedNodesSubmitButtonId">Deploy Nodes</button>
              </div>

              <?php require "./txnUtils.htmli"; ?>

              </div>
            </div>
            <!-- End of installation Wizard -->

          </div>
          <?php require "./footer.htmli"; ?>

            <!-- Javascript Scaffolding -->
            <script type="text/javascript">

            var freshInstallation = false;
            var clusterName = "<?php echo $_GET['clusterName']; ?>";
            var nodesAction = "addNodes";

            var InstallationWizard = {

              AddNodes: 
              {},
              AddNodesProgress:
              {},
              SelectComponents: 
              {},
              DeployProgress:
              {}
            };

            var jsFilesToLoad = [ 
                '../js/utils.js', 
                '../js/txnUtils.js',
                '../js/addNodes.js', 
                '../js/addNodesProgress.js', 
                '../js/selectComponents.js', 
                '../js/deployProgress.js', 
                '../js/addNodesWizardInit.js'
              ];
            </script>

            <?php require "./bootstrapJs.htmli"; ?>
            <!-- End of Javascript Scaffolding -->

  </body>
</html> 
