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

package org.apache.ambari.view.phonelist;

import org.apache.ambari.view.ViewContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servlet for phone list view.
 */
public class PhoneListServlet extends HttpServlet {

  /**
   * The view context.
   */
  private ViewContext viewContext;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    ServletContext context = config.getServletContext();
    viewContext = (ViewContext) context.getAttribute(ViewContext.CONTEXT_ATTRIBUTE);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String name = request.getParameter("name");
    String phone = request.getParameter("phone");

    if (name != null && name.length() > 0 && phone != null && phone.length() > 0) {
      if (request.getParameter("add") != null) {
        if (viewContext.getInstanceData(name) != null) {
          throw new IllegalArgumentException("A number for " + name + " already exists.");
        }
        viewContext.putInstanceData(name, phone);
      } else if (request.getParameter("update") != null) {
        viewContext.putInstanceData(name, phone);
      } else if (request.getParameter("delete") != null) {
        viewContext.removeInstanceData(name);
      }
    }
    listAll(request, response);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);

    PrintWriter writer = response.getWriter();


    String name = request.getParameter("name");
    String phone = (name == null || name.length() == 0) ? null : viewContext.getInstanceData(name);

    if (phone != null) {
      editNumber(writer, name, phone);
    } else {
      listAll(request, response);
    }
  }

  private void enterNumber(PrintWriter writer) {
    writer.println("<form name=\"input\" method=\"POST\">");
    writer.println("<table>");
    writer.println("<tr>");
    writer.println("<td>Name:</td><td><input type=\"text\" name=\"name\"></td><br/>");
    writer.println("</tr>");
    writer.println("<tr>");
    writer.println("<td>Phone Number:</td><td><input type=\"text\" name=\"phone\"></td><br/><br/>");
    writer.println("</tr>");
    writer.println("</table>");
    writer.println("<input type=\"submit\" value=\"Add\" name=\"add\">");
    writer.println("</form>");
  }

  private void editNumber(PrintWriter writer, String name, String phone) {
    writer.println("<form name=\"input\" method=\"POST\">");
    writer.println("<table>");
    writer.println("<tr>");
    writer.println("<td>Name:</td><td><input type=\"text\" name=\"name\" value=\"" + name + "\" readonly></td><br/>");
    writer.println("</tr>");
    writer.println("<tr>");
    writer.println("<td>Phone Number:</td><td><input type=\"text\" name=\"phone\" value=\"" + phone + "\"></td><br/><br/>");
    writer.println("</tr>");
    writer.println("</table>");
    writer.println("<input type=\"submit\" value=\"Update\" name=\"update\">");
    writer.println("<input type=\"submit\" value=\"Delete\" name=\"delete\">");
    writer.println("</form>");
  }

  private void listAll(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String name;Map<String, String> data = new LinkedHashMap<String, String>(viewContext.getInstanceData());

    PrintWriter writer = response.getWriter();

    writer.println("<h1>Phone List :" + viewContext.getInstanceName() + "</h1>");

    writer.println("<table border=\"1\" style=\"width:300px\">");
    writer.println("<tr>");
    writer.println("<td>Name</td>");
    writer.println("<td>Phone Number</td>");
    writer.println("</tr>");

    for (Map.Entry<String,String> entry : data.entrySet()){
      name = entry.getKey();
      writer.println("<tr>");
      writer.println("<td><A href=" + request.getRequestURI() + "?name=" + name + ">" + name + "</A></td>");
      writer.println("<td>" + entry.getValue() + "</td>");
      writer.println("</tr>");
    }
    writer.println("</table><br/><hr/>");

    enterNumber(writer);
  }
}

