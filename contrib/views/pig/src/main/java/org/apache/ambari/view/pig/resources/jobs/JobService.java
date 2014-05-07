/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.pig.resources.jobs;

import com.google.inject.Inject;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.pig.persistence.utils.ItemNotFound;
import org.apache.ambari.view.pig.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.pig.resources.files.FileResource;
import org.apache.ambari.view.pig.resources.jobs.models.PigJob;
import org.apache.ambari.view.pig.services.BaseService;
import org.apache.ambari.view.pig.utils.FilePaginator;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Servlet for Pig Jobs
 * API:
 * GET /:id
 *      read job info
 * POST /
 *      create new job
 *      Required: scriptId
 *      Optional: params
 * GET /
 *      get all jobs of current user
 * GET /:id/notify
 *      callback from Templeton
 */
public class JobService extends BaseService {
  @Inject
  ViewResourceHandler handler;

  protected JobResourceManager resourceManager = null;

  /**
   * Get resource manager object
   * @return resource manager object
   */
  public synchronized JobResourceManager getResourceManager() {
    if (resourceManager == null) {
      resourceManager = new JobResourceManager(context);
    }
    return resourceManager;
  }

  /**
   * Set resource manager object
   * @param resourceManager resource manager object
   */
  public synchronized void setResourceManager(JobResourceManager resourceManager) {
    this.resourceManager = resourceManager;
  }

  /**
   * Get single item
   */
  @GET
  @Path("{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJob(@PathParam("jobId") String jobId) {
    PigJob job = null;
    try {
      job = getResourceManager().read(jobId);
    } catch (ItemNotFound itemNotFound) {
      return Response.status(404).build();
    }
    getResourceManager().retrieveJobStatus(job);
    JSONObject object = new JSONObject();
    object.put("job", job);
    return Response.ok(object).build();
  }

  /**
   * Get single item
   */
  @DELETE
  @Path("{jobId}")
  public Response killJob(@PathParam("jobId") String jobId) throws IOException {
    PigJob job = null;
    try {
      job = getResourceManager().read(jobId);
    } catch (ItemNotFound itemNotFound) {
      return Response.status(404).build();
    }
    getResourceManager().killJob(job);
    return Response.status(204).build();
  }

  /**
   * Callback from templeton
   */
  @GET
  @Path("{jobId}/notify")
  public Response jobCompletionNotification(@Context HttpHeaders headers,
                                            @Context UriInfo ui,
                                            @PathParam("jobId") final String jobId) {
    PigJob job = null;
    try {
      job = getResourceManager().ignorePermissions(new Callable<PigJob>() {
        public PigJob call() throws Exception {
          PigJob job = null;
          try {
            job = getResourceManager().read(jobId);
          } catch (ItemNotFound itemNotFound) {
            return null;
          }
          return job;
        }
      });
    } catch (Exception e) {
      return Response.status(500).build();
    }
    if (job == null)
      return Response.status(404).build();

    getResourceManager().retrieveJobStatus(job);
    return Response.ok().build();
  }

  @GET
  @Path("{jobId}/results/{fileName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response jobExitCode(@Context HttpHeaders headers,
                              @Context UriInfo ui,
                              @PathParam("jobId") String jobId,
                              @PathParam("fileName") String fileName,
                              @QueryParam("page") Long page) {
    PigJob job = null;
    try {
      job = getResourceManager().read(jobId);
    } catch (ItemNotFound itemNotFound) {
      return Response.ok("No such job").status(404).build();
    }
    try {
      String filePath = job.getStatusDir() + "/" + fileName;
      LOG.debug("Reading file " + filePath);
      FilePaginator paginator = new FilePaginator(filePath, context);

      if (page == null)
        page = 0L;

      FileResource file = new FileResource();
      file.setFilePath(filePath);
      file.setFileContent(paginator.readPage(page));
      file.setHasNext(paginator.pageCount() > page + 1);
      file.setPage(page);
      file.setPageCount(paginator.pageCount());

      JSONObject object = new JSONObject();
      object.put("file", file);
      return Response.ok(object).status(200).build();
    } catch (IOException e) {
      return Response.ok(e.getMessage()).status(404).build();
    } catch (InterruptedException e) {
      return Response.ok(e.getMessage()).status(404).build();
    }
  }

  /**
   * Get all jobs
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJobList(@Context HttpHeaders headers, @Context UriInfo ui) {
    List allJobs = getResourceManager().readAll(
        new OnlyOwnersFilteringStrategy(this.context.getUsername()));

    JSONObject object = new JSONObject();
    object.put("jobs", allJobs);
    return Response.ok(object).build();
  }

  /**
   * Create job
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response runJob(PigJobRequest request, @Context HttpServletResponse response,
                         @Context UriInfo ui) {
    if (!request.validatePOST()) {
      return badRequestResponse(request.explainPOST());
    }
    try {
      getResourceManager().create(request.job);
    } catch (IllegalArgumentException e) {
      return badRequestResponse(e.getMessage());
    } catch (WebServiceException e) {
      return serverErrorResponse(e.getMessage());
    }

    PigJob job = null;

    try {
      job = getResourceManager().read(request.job.getId());
    } catch (ItemNotFound itemNotFound) {
      return Response.status(404).build();
    }

    response.setHeader("Location",
        String.format("%s/%s", ui.getAbsolutePath().toString(), request.job.getId()));

    JSONObject object = new JSONObject();
    object.put("job", job);
    return Response.ok(object).status(201).build();
  }

  /**
   * Wrapper object for json mapping
   */
  public static class PigJobRequest {
    public PigJob job;

    public String explainPOST() {
      StringBuilder result = new StringBuilder();
      if ((job.getPigScript() == null || job.getPigScript().isEmpty()) &&
          (job.getForcedContent() == null || job.getForcedContent().isEmpty()))
        result.append("No pigScript file or forcedContent specifed;");
      if (job.getTitle() == null || job.getTitle().isEmpty())
        result.append("No title specifed;");
      if (job.getId() != null && !job.getTitle().isEmpty())
        result.append("ID should not exists in creation request;");
      return result.toString();
    }

    public boolean validatePOST() {
      return explainPOST().isEmpty();
    }
  }
}
