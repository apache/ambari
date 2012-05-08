<html>
  <head>
    <title id="pageTitleId">Hortonworks Data Platform Portal</title>

    <?php require "./css_includes.html"; ?>
  </head>

  <body class="yui3-skin-sam">
    <?php require "./header.html"; ?>
    <hr/>

    <div name="ContentDiv" id="ContentDivId"> 

      <!-- Navigation bar -->
      <div name="navigationBarDiv" id="navigationBarDivId">
        <div name="navigationBarContentDiv" id="navigationBarContentDivId">
        <a href="index.php">Clusters</a>
        </div>
      <hr/>
      </div>
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
            <li id="initializeClusterStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  2   
                </span>
                Initialize Cluster
              </div>
            </li>
            <li id="assignHostsStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  3   
                </span>
                Assign Hosts
              </div>
            </li>
            <li id="configureClusterStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  4   
                </span>
                Configure Cluster
              </div>
            </li>
            <li id="configureClusterAdvancedStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  5   
                </span>
                Configure Cluster (Advanced)
              </div>
            </li>
            <li id="deployClusterStageId" class="installationWizardLastStage installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  6   
                </span>
                Deploy Cluster
              </div>
            </li>
          </ol>
        </div>
        
        <br/>
        
        <div name="installationMainFormsDiv" id="installationMainFormsDivId">

          <!-- Used to render informational/status messages (error, success reports and such like) -->
          <div id="displayProgressStatusDivId" style="display:none"></div>
          
          <br/>

          <div name ="createClusterCoreDiv" id="createClusterCoreDivId" style="display:none">
            <form id="createClusterFormId" >
              <fieldset>
                <legend>Create Cluster</legend>
                <label for="clusterNameId">Enter a name for your cluster</label>
                <input type="text" name="clusterName" id="clusterNameId" value="">
              </fieldset>
            </form>
                <br/>
                <p>
                <input type="button" name="createClusterSubmitButton" id="createClusterSubmitButtonId" value="Create Cluster" class="submitButton">
                </p>
              </div>

              <div name="initializeClusterCoreDiv" id="initializeClusterCoreDivId" style="display:none">

                <fieldset>
                  <legend>Hosts file and credentials</legend>
                  <form id="initializeClusterFilesFormId" enctype="multipart/form-data" method="post">
                    <p>
                    <label for="clusterDeployUserId">Cluster Deploy User</label>
                    <input type="text" name="clusterDeployUser" id="clusterDeployUserId" value="root" placeholder="">
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

                    <iframe name="fileUploadTarget" id="fileUploadTargetId" src="" style="display:none"></iframe>

                  </form>
                </fieldset>

                <br/>

                <fieldset>
                  <legend>Select Services</legend>
                  <form id="initializeClusterDataFormId">
                    <p>
                    <label for="installMapReduce">MapReduce</label>
                    <input type="checkbox" name="installMR" id="installMRId" value="installMapReduceValue" disabled="disabled" checked="yes">
                    </p>
                    <p>
                    <label for="installHdfs">HDFS</label>
                    <input type="checkbox" name="installHDFS" id="installHDFSId" value="installHdfsValue" disabled="disabled" checked="yes">
                    </p>
                    <p>
                    <label for="installHBaseId">HBase</label>
                    <input type="checkbox" name="installHBase" id="installHBaseId" value="installHBaseValue">
                    </p>
                    <p>
                    <label for="installHCatalogId">HCatalog</label>
                    <input type="checkbox" name="installHCatalog" id="installHCatalogId" value="installHCatalogValue">
                    </p>
                    <p>
                    <label for="installTempletonId">Templeton</label>
                    <input type="checkbox" name="installTempleton" id="installTempletonId" value="installTempletonValue">
                    </p>
                    <p>
                    <label for="installOozieId">Oozie</label>
                    <input type="checkbox" name="installOozie" id="installOozieId" value="installOozieValue">
                    </p>
                    <p>
                    <label for="installPigId">Pig</label>
                    <input type="checkbox" name="installPig" id="installPigId" value="installPigValue">
                    </p>
                    <p>
                    <label for="installSqoopId">Sqoop</label>
                    <input type="checkbox" name="installSqoop" id="installSqoopId" value="installSqoopValue">
                    <p>
                  </form>
                </fieldset>
                <br/>
                <p>
                <input type="button" name="initializeClusterSubmitButton" id="initializeClusterSubmitButtonId" value="Initialize" class="submitButton">
                </p>
              </div>

              <div name="assignHostsCoreDiv" id="assignHostsCoreDivId" style="display:none">
                <form id="assignHostsFormId">
                  <fieldset id="assignHostsFieldSetId">
                    <legend>Select master hosts</legend>
                    <div id="assignHostsDynamicRenderDivId"></div>
                  </fieldset>
                  <p>
                  <input type="button" name="assignHostsSubmitButton" id="assignHostsSubmitButtonId" value="submit" class="submitButton">
                  </p>
                </form>
              </div>

              <div name="configureClusterCoreDiv" id="configureClusterCoreDivId" style="display:none">

                <div name="configureClusterInputDiv" id="configureClusterInputDivId">
                  <form id="configureClusterFormId">
                    <fieldset id="configureClusterInputFieldSetId">
                      <legend>Select mount points</legend>
                      <div name="configureClusterMountPointsInputDiv" id="configureClusterMountPointsInputDivId">
                        <div id="configureClusterMountPointsDynamicRenderDivId"></div>
                        <p>
                          <label for="customMountPoints">Custom mount points</label>
                          <input type="text" name="customMountPoints" id="customMountPointsId" value="" placeholder="Comma-Separated List">
                        </p>
                      </div>  
                      <!-- Additional <div>s for other categories of cluster configuration go here -->
                    </fieldset>
                    <p>
                    <input type="button" name="configureClusterSubmitButton" id="configureClusterSubmitButtonId" value="Submit" class="submitButton">
                    </p>
                  </form>
                </div>

                  <br/>

                  <div name="configureClusterDisplayDiv" id="configureClusterDisplayDivId">
                    <fieldset>
                      <legend>Effective mount points</legend>
                      <div name="configureClusterMountPointsDisplayDiv" id="configureClusterMountPointsDisplayDivId">
                      </div>
                    </fieldset>
                  </div>
                </div>

                <div name="configureClusterAdvancedCoreDiv" id="configureClusterAdvancedCoreDivId" style="display:none">
                  <form id="configureClusterAdvancedFormId">
                    <fieldset id="configureClusterAdvancedFieldSetId">
                      <legend>Advanced configuration</legend>
                      <div id="configureClusterAdvancedDynamicRenderDivId"></div>
                    </fieldset>
                    <p>
                    <input type="button" name="configureClusterAdvancedSubmitButton" id="configureClusterAdvancedSubmitButtonId" value="Submit" class="submitButton">
                    </p>
                  </form>
                </div>

                <div name="deployCoreDiv" id="deployCoreDivId" style="display:block">
                  <form id="deployFormId">
                    <fieldset id="deployFieldSetId">
                      <legend>Review your settings</legend>
                      <div id="deployDynamicRenderDivId"></div>
                    </fieldset>
                    <p>
                    <input type="button" name="deploySubmitButton" id="deploySubmitButtonId" value="Deploy" class="submitButton">
                    </p>
                  </form>
                </div>

                <div name="displayProgressCoreDiv" id="displayProgressCoreDivId" style="display:none">
                  <fieldset id="displayProgressFieldSetId">
                    <legend>Deploy Progress</legend>
                    <div id="displayProgressDynamicRenderDivId"></div>
                  </fieldset>
                </div>

              </div>
            </div>
            <!-- End of installation Wizard -->

            <div id="loadingDivId">
              <img id="loadingImgId" src="../images/loadingLarge.gif" style="display:none"/>
            </div>

            <!-- Placeholder for our informational YUI Panel. -->
            <div id="informationalPanelContainerDivId">
              <div class="yui3-widget-hd"></div>
              <div class="yui3-widget-bd"></div>
              <div class="yui3-widget-ft"></div>
            </div>

            <hr/>
            <?php require "./footer.html"; ?>

            <?php require "./js_includes.html"; ?>

            <script type="text/javascript">

              YUI().use("node", "io", "dump", "json", "arraysort", "panel", "event", function (Y) {
                Y.log("starting up");
                renderDeploy(Y, {clusterName: 'rez3'});
              });

            </script>
          </body>
        </html> 
