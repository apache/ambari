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

package org.apache.ambari.view.weather;

import org.apache.ambari.view.ResourceProvider;
import org.apache.ambari.view.ViewContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;

/**
 * Servlet class for Weather view UI.
 */
public class WeatherServlet extends HttpServlet {
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
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
  {
    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);

    PrintWriter writer = response.getWriter();
    writer.println("<h1>" + viewContext.getInstanceName() + " Weather</h1>");

    String targetCity = request.getParameter("city");

    if (targetCity == null) {
      Map<String, String> properties = viewContext.getProperties();

      String   cityStr = properties.get("cities");
      String[] cities  = cityStr.split(";");

      writer.println("<ul>");
      for (String city : cities) {
        writer.println("<li><A href=" + request.getRequestURI() + "?city=" + URLEncoder.encode(city, "UTF-8") + ">" + city + "</A></li>");
      }
      writer.println("</ul>");
    } else {
      // Use the view's resource provider...
      ResourceProvider resourceProvider = viewContext.getResourceProvider("city");

      if (resourceProvider != null) {
        writer.println("<b>" + targetCity + "</b><br><br>");

        CityResource resource;
        try {
          resource = (CityResource) resourceProvider.getResource(targetCity, Collections.singleton("weather"));
        } catch (Exception e) {
          throw new IOException(e);
        }

        String icon_src = (String) resource.getWeather().get("icon_src");

        Map<String, Object> weather = (Map<String, Object>) resource.getWeather().get("weather");
        Map<String, Object> main    = (Map<String, Object>) resource.getWeather().get("main");

        writer.println("<b>" +
            weather.get("main") + ":" +
            weather.get("description") +
            "</b><br><IMG SRC=\"" +
            icon_src +
            "\" ALT=\"Weather\">");

        writer.println("<table border=\"1\">");

        writer.println("<tr><td>");
        writer.println("Temp");
        writer.println("</td><td>");
        writer.println(main.get("temp"));
        writer.println("</td></tr>");

        writer.println("<tr><td>");
        writer.println("High");
        writer.println("</td><td>");
        writer.println(main.get("temp_max"));
        writer.println("</td></tr>");

        writer.println("<tr><td>");
        writer.println("Low");
        writer.println("</td><td>");
        writer.println(main.get("temp_min"));
        writer.println("</td></tr>");

        writer.println("<tr><td>");
        writer.println("Humidity");
        writer.println("</td><td>");
        writer.println(main.get("humidity"));
        writer.println("</td></tr>");

        writer.println("</table>");
      }
    }
  }
}
