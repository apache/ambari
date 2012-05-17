<html>
  <head>
    <title id="pageTitleId">Hortonworks Data Platform Portal</title>

    <!-- CSS -->
    <link type="text/css" rel="stylesheet" href="../yui-3.4.1/build/cssreset/cssreset-min.css"> 
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common3.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/selectHosts.css" media="screen"/>
    <!-- End CSS -->
  </head>

  <body class="yui3-skin-sam">
    <?php require "./header.html"; ?>
    <hr/>

    <div name="ContentDiv" id="ContentDivId"> 

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
         
          <br/>
              <div id="addNodesCoreDivId" style="display:block">

                <fieldset>
                  <form id="addNodesFilesFormId" enctype="multipart/form-data" method="post">
                    <p>
                    <label for="clusterDeployUserId">Cluster Deploy User</label>
                    <input type="text" name="ClusterDeployUser" id="clusterDeployUserId" value="root" placeholder="">
                    </p>
                    <br/>
                    <p>
                    <label for="clusterDeployUserIdentityFileId">Cluster Deploy User Identity File</label>
                    <input type="file" name="clusterDeployUserIdentityFile" id="clusterDeployUserIdentityFileId" value="" placeholder="">
                    </p>
                    <br/>
                    <p>
                    <label for="clusterHostsFileId">Cluster Hosts File</label>
                    <input type="file" name="clusterHostsFile" id="clusterHostsFileId" value="" placeholder="">
                    </p>

                    <div id="fileUploadWrapperDivId">
                      <iframe name="fileUploadTarget" id="fileUploadTargetId" src="about:blank" style="display:none"></iframe>
                    </div>
                  </form>
                </fieldset>
                <br/>
                <p>
                  <input type="button" id="addNodesSubmitButtonId" value="Add nodes" class="submitButton">
                </p>
              </div>

              <div name="selectServicesCoreDiv" id="selectServicesCoreDivId" style="display:none">
                <fieldset>
                  <!--<legend>Select Services</legend>-->
                  <form id="addNodesDataFormId">
              <div name="selectComponentsDynamicRenderDiv" id="selectComponentsDynamicRenderDivId"></div>

                    <!-- <p>
                    <label for="installMapReduceId">MapReduce (TASKTRACKER)</label>
                    <input type="checkbox" name="installMR" id="installMRId" value="installMapReduceValue" disabled="disabled" checked="yes">
                    </p>
                    <p>
                    <label for="installHdfsId">HDFS (DATANODE)</label>
                    <input type="checkbox" name="installHDFS" id="installHDFSId" value="installHdfsValue" disabled="disabled" checked="yes">
                    </p> -->
                  </form>
                </fieldset>
                <br/>
                <p>
                <input type="button" name="deployAddedNodesSubmitButton" id="deployAddedNodesSubmitButtonId" value="Deploy Nodes" class="submitButton">
                </p>
              </div>

              <?php require "./txnUtils.htmli"; ?>

              </div>
            </div>
            <!-- End of installation Wizard -->

            <hr/>
            <?php require "./footer.html"; ?>

            <!-- Javascript Scaffolding -->
            <script type="text/javascript">

            var freshInstallation = false;
            var clusterName = "<?php echo $_GET['clusterName']; ?>";

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
