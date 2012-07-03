<?php
/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
*/
?>
<?php require_once "./head.inc"; ?>
<html>
  <head>
    <?php require "./head.htmli" ?>
    <script src="../js/ext/jquery.min.js"></script>
    <script src="../js/ext/bootstrap.min.js"></script>
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
                    <input type="hidden" name="ClusterDeployUser" id="clusterDeployUserId" value="root" placeholder="">
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
               	<div style="width:400px;height:40px;float:right;">
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
                <a href="javascript:void 0" class="btn btn-large" style="margin:10px 0 0 60px" id="selectServicesSubmitButtonId" class="submitButton"><?php echo $RES['initWizard.selectServices.submit.label'] ?></a>

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
                    <a id="previewLinkId" href="javascript:void 0"><?php echo $RES['initWizard.configureCluster.preview.label'] ?></a>
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
                  <div id="buttonAreaDivId" class="clearfix">
                    <div id="buttonGroupDivId">
                      <?php /* 
                      <div id="backNextDivId" class="btn-group">
                        <a href="javascript:void 0" class="btn btn-large" id="configureClusterAdvancedBackButtonId"><?php echo $RES['initWizard.configureClusterAdvanced.back.label'] ?></a>
                        <a href="javascript:void 0" class="btn btn-large" id="configureClusterAdvancedNextButtonId"><?php echo $RES['initWizard.configureClusterAdvanced.next.label'] ?></a>
                      </div>
                      */ ?>
                      <a href="javascript:void 0" class="btn btn-large" id="configureClusterAdvancedSubmitButtonId"><?php echo $RES['initWizard.configureClusterAdvanced.submit.label'] ?></a>
                    </div>
                  </div>
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
                'js/utils.js', 
                'js/txnUtils.js',
                'js/installationWizard.js',
                'js/createCluster.js',
                'js/addNodes.js',
                'js/addNodesProgress.js',
                'js/selectServices.js',
                'js/assignMasters.js',
                'js/configureCluster.js',
                'js/configureServicesUtils.js',
                'js/configureServices.js',
                'js/reviewAndDeploy.js',
                'js/deployProgress.js'
              ];

            // uncomment to allow jumping to a specified stage for development
            // jsFilesToLoad.push('js/test/initializeClusterTest.js');
            </script>

            <?php require "./bootstrapJs.htmli"; ?>
            <!-- End of Javascript Scaffolding -->

          </body>
        </html>
