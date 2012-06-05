<?php require_once "./head.inc"; ?>
<html>
  <head>
    <title id="pageTitleId"><?php echo $RES['page.title'] ?></title>

    <!-- CSS -->
    <link type="text/css" rel="stylesheet" href="../yui-3.5.1/build/cssreset/cssreset-min.css">
    <link type="text/css" rel="stylesheet" href="../css/bootstrap.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common3.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/selectHosts.css" media="screen"/>
    <link rel="shortcut icon" href="../images/logo-micro.gif">
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
            <li id="createClusterStageId" class="installationWizardFirstStage installationWizardCurrentStage">
              <div>
                <span class="installationWizardStageNumber">
                  1
                </span>
                Create Cluster
              </div>
            </li>
            <li id="addNodesStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  2
                </span>
                Add Nodes
              </div>
            </li>
            <li id="selectServicesStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  3
                </span>
                Select Services
              </div>
            </li>
            <li id="assignHostsStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  4
                </span>
                Assign Hosts
              </div>
            </li>
            <li id="configureClusterStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  5
                </span>
                Select Mount Points
              </div>
            </li>
            <li id="configureClusterAdvancedStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  6
                </span>
                Custom Config
              </div>
            </li>
            <li id="deployClusterStageId" class="installationWizardLastStage installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  7
                </span>
                Review &amp; Deploy
              </div>
            </li>
          </ol>
        </div>

        <?php require "./utils.htmli"; ?>

        <div id="installationMainFormsDivId">
              <div id="createClusterCoreDivId">
                <div class="pageSummary">
                  <h2><?php echo $RES['initWizard.createCluster.pageSummary.header'] ?></h2>
                  <p><?php echo $RES['initWizard.createCluster.pageSummary.body'] ?></p>
                </div>
                <div id="formStatusDivId" class="formStatusBar" style="display:none">					
                </div>
                <div class="pageContent">       
                  <form id="createClusterFormId">
                    <label for="clusterNameId"><?php echo $RES['initWizard.createCluster.clusterName.label'] ?></label>
                    <input type="text" name="clusterName" id="clusterNameId" placeholder="cluster name" value="">
                  </form>
				  <a href="javascript:void 0" class="btn btn-large" id="createClusterSubmitButtonId"><?php echo $RES['initWizard.createCluster.submit.label'] ?></a>
                </div>
              </div>
              <div id="addNodesCoreDivId" style="display:none">                  
                  <div class="pageSummary">
                    <h2><?php echo $RES['initWizard.addNodes.pageSummary.header'] ?></h2>
                    <?php echo $RES['initWizard.addNodes.pageSummary.body'] ?>
                  </div>
                  <div id="formStatusDivId" class="alert alert-error" style="display:none">					
                  </div>                  
                  <div class="pageContent">
                  <form id="addNodesFilesFormId" enctype="multipart/form-data" method="post">
                    <label for="clusterDeployUserId"><?php echo $RES['common.sshUsername.label'] ?></label>
                    <input type="text" name="ClusterDeployUser" id="clusterDeployUserId" value="root" placeholder="">
                    <div class="separator"></div>
                    <label for="clusterDeployUserIdentityFileId"><?php echo $RES['common.sshPrivateKeyFile.label'] ?></label>
                    <input type="file" name="clusterDeployUserIdentityFile" id="clusterDeployUserIdentityFileId" value="" placeholder="">
                    <div class="separator"></div>
                    <label for="clusterHostsFileId"><?php echo $RES['common.hostsFile.label'] ?></label>
                    <input type="file" name="clusterHostsFile" id="clusterHostsFileId" value="" placeholder="">
                    <div class="separator"></div>
                    <div id="yumMirrorSupportFormId">
                      <div id="yumMirrorSupportFormButtonWrapperId">
                        <label class="checkbox" for="yumMirrorSupportFormButtonId"><?php echo $RES['initWizard.addNodes.useLocalYum.label'] ?>
                          <input type="checkbox" name="YumMirrorSupportFormButton" id="yumMirrorSupportFormButtonId" value="" placeholder="">
                        </label>
                      </div>
                      <div id="yumMirrorSupportFormFieldsId" style="display:none">
                        <label for="yumRepoFilePathId"><?php echo $RES['initWizard.addNodes.yumRepoFilePath.label'] ?></label>
                        <input type="text" name="YumRepoFilePath" id="yumRepoFilePathId" value="" placeholder="">
                        <label for="hmcArtifactsDownloadUrlId"><?php echo $RES['initWizard.addNodes.apacheArtifactsDownloadUrl.label'] ?></label>
                        <input type="text" name="HmcArtifactsDownloadUrl" id="hmcArtifactsDownloadUrlId" value="" placeholder="">
                        <label for="hmcGplArtifactsDownloadUrlId"><?php echo $RES['initWizard.addNodes.gplArtifactsDownloadUrl.label'] ?></label>
                        <input type="text" name="HmcGplArtifactsDownloadUrl" id="hmcGplArtifactsDownloadUrlId" value="" placeholder="">
                      </div>
                    </div>
                    <div id="fileUploadWrapperDivId">
                      <iframe name="fileUploadTarget" id="fileUploadTargetId" src="about:blank" style="display:none"></iframe>
                    </div>
                  </form>
                  <div class="separator"></div>
                  <a href="javascript:void 0" class="btn btn-large" id="addNodesSubmitButtonId"><?php echo $RES['initWizard.addNodes.submit.label'] ?></a>
                  </div>
              </div>

              <div name="selectServicesCoreDiv" id="selectServicesCoreDivId" style="display:none">
                <fieldset>
                <div class="pageSummary">
                  <h2><?php echo $RES['initWizard.selectServices.pageSummary.header'] ?></h2>
                  <p><?php echo $RES['initWizard.selectServices.pageSummary.body'] ?></p>
               	</div>
               	<div style="width:100%;height:40px">
                  <div id="formStatusDivId" class="formStatusBar" style="display:none">					
                  </div>               
                </div>
               	<div class="pageContent" style="margin-top:14px">
                  <div id="selectCoreServicesListId">
                   <ul id="selectCoreServicesListUlId">
                      <div id="selectCoreServicesDynamicRenderDivId">
                      </div>
                    </ul>
                  </div>
                  <div id="selectOptionalServicesListId">
                    <ul id="selectOptionalServicesListUlId">
                      <div id="selectOptionalServicesDynamicRenderDivId">
                      </div>
                    </ul>
                  </div>
                  <div id="selectNonSelectableServicesListId">
                    <ul id="selectNonSelectableServicesListUlId">
                      <div id="selectNonSelectableServicesDynamicRenderDivId">
                      </div>
                    </ul>
                  </div>
                </div>
                </fieldset>
                <a href="javascript:void 0" class="btn btn-large" style="margin:20px 0 0 60px" id="selectServicesSubmitButtonId" class="submitButton"><?php echo $RES['initWizard.selectServices.submit.label'] ?></a>

              </div>
              <!-- End of selectServicesCoreDivId -->

              <div name="assignHostsCoreDiv" id="assignHostsCoreDivId" style="display:none">
                <div id="statusDivId">
                </div>
                <div class="pageSummary">
                  <h2><?php echo $RES['initWizard.assignMasters.pageSummary.header'] ?></h2>
                  <p><?php echo $RES['initWizard.assignMasters.pageSummary.body'] ?></p>             
                </div>
                <div id="formStatusDivId" class="formStatusBar" style="display:none">					
                </div>               
                <div id="masterServices">
                  <div id="masterServicesToHostsContainer">
                    <div id="masterServicesToHosts"></div>
                    <a href="javascript:void 0" class="btn btn-large" id="selectServiceMastersSubmitButtonId"><?php echo $RES['initWizard.assignMasters.submit.label'] ?></a>
                  </div>                  
                  <div id="hostsToMasterServices"></div>
                </div>
                <div style="clear:both"></div>
              </div>

              <div name="configureClusterCoreDiv" id="configureClusterCoreDivId" style="display:none">
                <div class="pageSummary">
                  <h2><?php echo $RES['initWizard.configureCluster.pageSummary.header'] ?></h2>
                  <p><?php echo $RES['initWizard.configureCluster.pageSummary.body'] ?></p>
                </div>
                <div id="formStatusDivId" class="formStatusBar" style="display:none">					
                </div>

                <div id="configureClusterInputContainerDivId">
                  <form id="configureClusterFormId">
                    <div name="configureClusterInputDiv" id="configureClusterInputDivId">
                        <fieldset id="configureClusterInputFieldSetId">
                          <!--<legend>Select mount points</legend>-->
                          <div name="configureClusterMountPointsInputDiv" id="configureClusterMountPointsInputDivId">
                            <div id="configureClusterMountPointsDynamicRenderDivId"></div>
                            <p>
                              <label for="customMountPoints"><?php echo $RES['initWizard.configureCluster.customMountPoints.label'] ?></label>
                              <input type="text" name="customMountPoints" id="customMountPointsId" value="" placeholder="Comma-Separated List">
                            </p>
                          </div>
                          <!-- Additional <div>s for other categories of cluster configuration go here -->
                        </fieldset>
                    </div>
                    <!-- <input type="button" id="configureClusterSubmitButtonId" value="Submit" class="submitButton"> -->
                  </form>
                  <a href="javascript:void 0" class="btn btn-large" id="configureClusterSubmitButtonId"><?php echo $RES['initWizard.configureCluster.submit.label'] ?></a>
                </div>
                <div id="configureClusterDisplayDivId" style="display:none">
                  <fieldset>
                    <!--<legend>Effective mount points</legend>-->
                    <div name="configureClusterMountPointsDisplayDiv" id="configureClusterMountPointsDisplayDivId">
                    </div>
                  </fieldset>
                </div>
              </div>

                <div id="configureClusterAdvancedCoreDivId" style="display:none">
                  <div class="pageSummary">
                    <h2><?php echo $RES['initWizard.configureClusterAdvanced.pageSummary.header'] ?></h2>
                    <p><?php echo $RES['initWizard.configureClusterAdvanced.pageSummary.body'] ?></p>                  
                  </div>
                  <div id="formStatusDivId" class="formStatusBar" style="display:none">					
                  </div>               
                  <form id="configureClusterAdvancedFormId">
                    <fieldset id="configureClusterAdvancedFieldSetId">
                      <!--<legend>Advanced configuration</legend>-->
                      <div id="configureClusterAdvancedDynamicRenderDivId"></div>
                    </fieldset>
                  </form>
                  <label></label>
                  <a href="javascript:void 0" class="btn btn-large" id="configureClusterAdvancedSubmitButtonId"><?php echo $RES['initWizard.configureClusterAdvanced.submit.label'] ?></a>
                </div>

                <div name="deployCoreDiv" id="deployCoreDivId" style="display:none">
                  <div class="pageSummary">
                    <h2><?php echo $RES['initWizard.reviewAndDeploy.pageSummary.header'] ?></h2>
                    <p><?php echo $RES['initWizard.reviewAndDeploy.pageSummary.body'] ?></p>     
                  </div>
                  <div id="formStatusDivId" class="formStatusBar" style="display:none">					
                  </div>               
                  <form id="deployFormId">
                    <fieldset id="deployFieldSetId">
                      <!--<legend>Review your settings</legend>-->
                      <div id="deployDynamicRenderDivId"></div>
                    </fieldset>
                  </form>
                  <label></label>
                  <a href="javascript:void 0" class="btn btn-large" id="deploySubmitButtonId" value="Deploy"><?php echo $RES['initWizard.reviewAndDeploy.submit.label'] ?></a>
                </div>

                <?php require "./txnUtils.htmli"; ?>

              </div>
            </div>
            <!-- End of installation Wizard -->
    </div>
            <?php require "./footer.htmli"; ?>

            <!-- Javascript Scaffolding -->
            <script type="text/javascript">

            var freshInstall = true;
            var nodesAction = "addNodes";

            var InstallationWizard = {

              CreateCluster:
              {},
              AddNodes:
              {},
              AddNodesProgress:
              {},
              SelectServices:
              {},
              AssignMasters:
              {},
              ConfigureCluster:
              {},
              ConfigureServices:
              {},
              ReviewAndDeploy:
              {},
              DeployProgress:
              {}
            };

            var jsFilesToLoad = [
                '../js/ext/jquery.min.js',
                '../js/ext/bootstrap.min.js', 
                '../js/utils.js', 
                '../js/txnUtils.js',
                '../js/installationWizard.js',
                '../js/createCluster.js',
                '../js/addNodes.js',
                '../js/addNodesProgress.js',
                '../js/selectServices.js',
                '../js/assignMasters.js',
                '../js/configureCluster.js',
                '../js/configureServicesUtils.js',
                '../js/configureServices.js',
                '../js/reviewAndDeploy.js',
                '../js/deployProgress.js'                
              ];
            </script>

            <?php require "./bootstrapJs.htmli"; ?>
            <!-- End of Javascript Scaffolding -->

          </body>
        </html>
