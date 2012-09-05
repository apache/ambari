package org.apache.ambari.controller.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * A simple POJO to do a health check on the server to see if its running
 * or not
 */

@Path("/check")
public class HealthCheck {
  private static final String status = "RUNNING";
  // This method is called if TEXT_PLAIN is request
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String plainTextCheck() {
    return status;
  }

  // This method is called if XML is request
  @GET
  @Produces(MediaType.TEXT_XML)
  public String xmlCheck() {
    return "<?xml version=\"1.0\"?>" + "<status> " + status + "</status>";
  }

  // This method is called if HTML is request
  @GET
  @Produces(MediaType.TEXT_HTML)
  public String  htmlCheck() {
    return "<html> " + "<title>" + "Status" + "</title>"
        + "<body><h1>" + status + "</body></h1>" + "</html> ";
  }
} 

