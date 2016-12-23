/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.ambari.view.assets;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.ambari.view.ViewContext;
import org.apache.oozie.ambari.view.*;
import org.apache.oozie.ambari.view.assets.model.ActionAsset;
import org.apache.oozie.ambari.view.assets.model.ActionAssetDefinition;
import org.apache.oozie.ambari.view.assets.model.AssetDefintion;
import org.apache.oozie.ambari.view.model.APIResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.*;

import static org.apache.oozie.ambari.view.Constants.*;

public class AssetResource {

  private final static Logger LOGGER = LoggerFactory
    .getLogger(AssetResource.class);
  private final AssetService assetService;
  private final ViewContext viewContext;
  private final HDFSFileUtils hdfsFileUtils;
  private OozieUtils oozieUtils = new OozieUtils();
  private final OozieDelegate oozieDelegate;


  public AssetResource(ViewContext viewContext) {
    this.viewContext = viewContext;
    this.assetService = new AssetService(viewContext);
    hdfsFileUtils = new HDFSFileUtils(viewContext);
    oozieDelegate = new OozieDelegate(viewContext);
  }

  @GET
  public Response getAssets() {
    try {
      Collection<ActionAsset> assets = assetService.getAssets();
      APIResult result = new APIResult();
      result.setStatus(APIResult.Status.SUCCESS);
      result.getPaging().setTotal(assets != null ? assets.size() : 0L);
      result.setData(assets);
      return Response.ok(result).build();
    } catch (Exception e) {
      throw new ServiceFormattedException(e);
    }
  }

  @GET
  @Path("/mine")
  public Response getMyAssets() {
    try {
      Collection<ActionAsset> assets = assetService.getMyAssets();
      APIResult result = new APIResult();
      result.setStatus(APIResult.Status.SUCCESS);
      result.getPaging().setTotal(assets != null ? assets.size() : 0L);
      result.setData(assets);
      return Response.ok(result).build();
    } catch (Exception e) {
      throw new ServiceFormattedException(e);
    }
  }
  @POST
  public Response saveAsset(@Context HttpHeaders headers,
                            @QueryParam("id") String id, @Context UriInfo ui, String body) {
    try {
      Gson gson = new Gson();
      AssetDefintion assetDefinition = gson.fromJson(body,
        AssetDefintion.class);
      Map<String, String> validateAsset = validateAsset(headers,
        assetDefinition.getDefinition(), ui.getQueryParameters());
      if (!STATUS_OK.equals(validateAsset.get(STATUS_KEY))) {
        return Response.status(Status.BAD_REQUEST).build();
      }
      assetService.saveAsset(id, viewContext.getUsername(), assetDefinition);
      APIResult result = new APIResult();
      result.setStatus(APIResult.Status.SUCCESS);
      return Response.ok(result).build();
    } catch (Exception e) {
      throw new ServiceFormattedException(e);
    }
  }

  private List<String> getAsList(String string) {
    ArrayList<String> li = new ArrayList<>(1);
    li.add(string);
    return li;
  }

  public Map<String, String> validateAsset(HttpHeaders headers,
                                           String postBody, MultivaluedMap<String, String> queryParams) {
    String workflowXml = oozieUtils.generateWorkflowXml(postBody);
    try {
      Map<String, String> result = new HashMap<>();
      String tempWfPath = "/tmp" + "/tmpooziewfs/tempwf.xml";
      hdfsFileUtils.writeToFile(tempWfPath, workflowXml, true);
      queryParams.put("oozieparam.action", getAsList("dryrun"));
      queryParams.put("oozieconfig.rerunOnFailure", getAsList("false"));
      queryParams.put("oozieconfig.useSystemLibPath", getAsList("true"));
      queryParams.put("resourceManager", getAsList("useDefault"));
      String dryRunResp = oozieDelegate.submitWorkflowJobToOozie(headers,
        tempWfPath, queryParams, JobType.WORKFLOW);
      LOGGER.info(String.format("resp from validating asset=[%s]",
        dryRunResp));
      if (dryRunResp != null && dryRunResp.trim().startsWith("{")) {
        JsonElement jsonElement = new JsonParser().parse(dryRunResp);
        JsonElement idElem = jsonElement.getAsJsonObject().get("id");
        if (idElem != null) {
          result.put(STATUS_KEY, STATUS_OK);
        } else {
          result.put(STATUS_KEY, STATUS_FAILED);
          result.put(MESSAGE_KEY, dryRunResp);
        }
      } else {
        result.put(STATUS_KEY, STATUS_FAILED);
        result.put(MESSAGE_KEY, dryRunResp);
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @GET
  @Path("/{id}")
  public Response getAssetDetail(@PathParam("id") String id) {
    try {
      AssetDefintion assetDefinition = assetService.getAssetDetail(id);
      APIResult result = new APIResult();
      result.setStatus(APIResult.Status.SUCCESS);
      result.setData(assetDefinition);
      return Response.ok(result).build();
    } catch (Exception e) {
      throw new ServiceFormattedException(e);
    }
  }

  @GET
  @Path("/definition/id}")
  public Response getAssetDefinition(@PathParam("defnitionId") String id) {
    try {
      ActionAssetDefinition assetDefinition = assetService
        .getAssetDefinition(id);
      APIResult result = new APIResult();
      result.setStatus(APIResult.Status.SUCCESS);
      result.setData(assetDefinition);
      return Response.ok(result).build();
    } catch (Exception e) {
      throw new ServiceFormattedException(e);
    }
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@PathParam("id") String id) {
    try {
      ActionAsset asset = assetService.getAsset(id);
      if (asset == null) {
        throw new RuntimeException("Asset doesnt exist");
      }
      if (!viewContext.getUsername().equals(asset.getOwner())){
        throw new RuntimeException(
          "Dont have permission to delete this asset");
      }
      assetService.deleteAsset(id);
      APIResult result = new APIResult();
      result.setStatus(APIResult.Status.SUCCESS);
      return Response.ok(result).build();
    } catch (Exception e) {
      throw new ServiceFormattedException(e);
    }
  }

}
