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
                Select Disk Mount Points
              </div>
            </li>
            <li id="configureClusterAdvancedStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  6
                </span>
                Advanced Configuration
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
        
        <!-- Used to render informational/status messages (error, success reports and such like) -->
        <div id="formStatusDivId" class="formStatusBar" style="visibility:hidden">Nothing
        </div>
       
        <div name="installationMainFormsDiv" id="installationMainFormsDivId">
         
          <div name ="createClusterCoreDiv" id="createClusterCoreDivId">
            <form id="createClusterFormId" >
              <fieldset>
                <!--<legend>Create Cluster</legend>-->
                <label for="clusterNameId">Enter a name for your cluster</label>
                <input type="text" name="clusterName" id="clusterNameId" value="">
              </fieldset>
            </form>
                <br/>
                <p>
                <input type="button" name="createClusterSubmitButton" id="createClusterSubmitButtonId" value="Create Cluster" class="submitButton">
                </p>
              </div>

              <div id="addNodesCoreDivId" style="display:none">

                <fieldset>
                  <form id="addNodesFilesFormId" enctype="multipart/form-data" method="post">
                    <div class="pageSummary">
                      We need information about the nodes you want to use for installing Hadoop, please provide them below:
                    </div>
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
                <div class="pageSummary">Select the list of services below that you wish to install on the cluster. Note that some of them may have dependencies which we will automatically select.</div>
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
               </fieldset>

                <br/>
                <p>
                  <input type="button" name="selectServicesSubmitButton" id="selectServicesSubmitButtonId" value="Select services" class="submitButton">
                </p>

              </div>
              <!-- End of selectServicesCoreDivId -->

              <div name="assignHostsCoreDiv" id="assignHostsCoreDivId" style="display:none">
                <div id="statusDivId">
                </div>

                <div class="pageSummary"> We have categorized the given nodes into Node-groups and also suggested the locations of various service-masters. Drap and drop service-masters that you wish to change. Click on the given links if you wish to change the node assignments</div>
                <div id="serviceMastersLinksDivId">
                </div>
                <div id="nodeGroupsCoreDivId">
                </div>
                <input type="button" name="selectServiceMastersSubmitButton" id="selectServiceMastersSubmitButtonId" value="Submit" class="submitButton">

              </div>

              <div name="configureClusterCoreDiv" id="configureClusterCoreDivId" style="display:none">

                <div class="pageSummary">We observed that the following mount points are available on your nodes. Please confirm/modify the list, these mount points will be used for storing data in various services as shown on the right side.</div>
                <form id="configureClusterFormId">
                  <div id="configureClusterInputContainerDivId">
                    <div name="configureClusterInputDiv" id="configureClusterInputDivId">
                        <fieldset id="configureClusterInputFieldSetId">
                          <!--<legend>Select mount points</legend>-->
                          <div name="configureClusterMountPointsInputDiv" id="configureClusterMountPointsInputDivId">
                            <div id="configureClusterMountPointsDynamicRenderDivId"></div>
                            <p>
                              <label for="customMountPoints">Custom mount points</label>
                              <input type="text" name="customMountPoints" id="customMountPointsId" value="" placeholder="Comma-Separated List">
                            </p>
                          </div>  
                          <!-- Additional <div>s for other categories of cluster configuration go here -->
                        </fieldset>
                    </div>

                    <br/>

                    <p>
                      <input type="button" name="configureClusterSubmitButton" id="configureClusterSubmitButtonId" value="Submit" class="submitButton">
                    </p>
                  </div>

                  <div name="configureClusterDisplayDiv" id="configureClusterDisplayDivId">
                    <fieldset>
                      <!--<legend>Effective mount points</legend>-->
                      <div name="configureClusterMountPointsDisplayDiv" id="configureClusterMountPointsDisplayDivId">
                      </div>
                    </fieldset>
                  </div>
                  </form>
                </div>

                <div id="configureClusterAdvancedCoreDivId" style="display:none">
                  <div class="pageSummary">Edit some/all of the following to affect per-service configuration. We have chimed in with reasonable defaults whereever possible.</div>
                  <form id="configureClusterAdvancedFormId">
                    <fieldset id="configureClusterAdvancedFieldSetId">
                      <!--<legend>Advanced configuration</legend>-->
                      <div id="configureClusterAdvancedDynamicRenderDivId"></div>
                    </fieldset>
                    <p>
                    <input type="button" name="configureClusterAdvancedSubmitButton" id="configureClusterAdvancedSubmitButtonId" value="Submit" class="submitButton">
                    </p>
                  </form>
                </div>

                <div name="deployCoreDiv" id="deployCoreDivId" style="display:none">
                  <div class="pageSummary">Please review your settings below, we are ready for a deploy! If you wish to make any changes, you can click on the installation stages above.</div>
                  <form id="deployFormId">
                    <fieldset id="deployFieldSetId">
                      <!--<legend>Review your settings</legend>-->
                      <div id="deployDynamicRenderDivId"></div>
                    </fieldset>
                    <p>
                    <input type="button" name="deploySubmitButton" id="deploySubmitButtonId" value="Deploy" class="submitButton">
                    </p>
                  </form>
                </div>

                <div id="txnProgressCoreDivId" style="display:none">

                  <!-- Used to render informational/status messages (error, success reports and such like) -->
                  <div id="txnProgressStatusDivId" class="formStatusBar" style="display:none">
                    <div id="txnProgressStatusMessageDivId"></div>
                    <div id="txnProgressStatusActionsDivId"></div>
                  </div>
 
                  <fieldset id="txnProgressFieldSetId">
               <!--     <legend>Display Progress</legend> -->
                    <div id="txnProgressDynamicRenderDivId"></div>
                  </fieldset>
                </div>

              </div>
            </div>
            <!-- End of installation Wizard -->

            <!-- The mechanism by which we black out the screen and affix the
                 the spotlight on a smaller portion of it. -->
            <div id="blackScreenDivId" style="display:none"></div>
            <!-- The image we use to let users know something is loading, and 
                 that they should wait. -->
            <div id="loadingDivId" style="display:none">
              <div id="loadingBlackScreenDivId"></div>
              <img id="loadingImgId" src="../images/loadingLarge.gif"/>
            </div>

            <!-- Placeholder for our informational YUI panel. -->
            <div id="informationalPanelContainerDivId">
            </div>

            <hr/>
            <?php require "./footer.html"; ?>

            <!-- Javascript Scaffolding -->
            <script type="text/javascript">

            var freshInstall = true;

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
