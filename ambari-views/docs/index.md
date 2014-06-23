<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Ambari Views
========
Description
-----
  
Ambari Views offer a systematic way to plug-in UI capabilities to surface custom visualization, management and monitoring features in Ambari Web.

A view is a way of extending Ambari that allows 3rd parties to plug in new resource types along with the APIs, providers and UI to support them.  In other words, a view is an application that is deployed into the Ambari container.

Each view has a name and version.  The combination of view name and version must be unique within the Ambari container.  A view may also define parameters to be specified for each instance of the view.

Views in Ambari are a top level resource and are not directly associated with a cluster instance.

Examples
-----
View examples can be found under ambari-views/examples of the Apache Ambari project.

Basics
-----
###View Instances
A view may have multiple instances.  Each instance of a view has a name that is unique for that view.  An instance may provide property values for the parameters of the view.


For example, a file browser view may define a file system URI parameter.  A view deployer may define multiple instances of the file browser view, each with a different file system URI property value.

###View Admin
TBD.

###View Context
The view context gives the view components access to the runtime context that the Ambari container provides.  The context remains associated with the view instance for the duration of its lifetime.

An instance of the view context may be injected by the container into the various view components, such as resources, resource providers and servlets.

The context interface provides access to information about the associated view instance like the instance name and properties … 

      /**
       * Get the current user name.
       *
       * @return the current user name
       */
      public String getUsername();
    
      /**
       * Get the view name.
       *
       * @return the view name
       */
      public String getViewName();
    
      /**
       * Get the view instance name.
       *
       * @return the view instance name
       */
      public String getInstanceName();
    
      /**
       * Get the property values specified to create the view instance.
       *
       * @return the view instance property values
       */
      public Map<String, String> getProperties();
    
      /**
       * Save an instance data value for the given key.
       *
       * @param key    the key
       * @param value  the value
       */
      public void putInstanceData(String key, String value);
    
      /**
       * Get the instance data value for the given key.
       *
       * @param key  the key
       *
       * @return the instance data value
       */
      public String getInstanceData(String key);
    
      /**
       * Get the instance data values.
       *
       * @return the view instance property values
       */
      public Map<String, String> getInstanceData();
    
      /**
       * Remove the instance data value for the given key.
       *
       * @param key  the key
       */
      public void removeInstanceData(String key);
    
      /**
       * Get a property for the given key from the ambari configuration.
       *
       * @param key  the property key
       *
       * @return the property value; null indicates that the configuration contains no mapping for the key
       */
      public String getAmbariProperty(String key);
    
      /**
       * Get the view resource provider for the given resource type.
       *
       * @param type  the resource type
       *
       * @return the resource provider
       */
      public ResourceProvider<?> getResourceProvider(String type);
    
      /**
       * Get a URL stream provider.
       *
       * @return a stream provider
       */
      public URLStreamProvider getURLStreamProvider();
    
      /**
       * Get a data store for view persistence entities.
       *
       * @return a data store
       */
      public DataStore getDataStore();
 
###UI Components
The view archive may contain components of a web application such as html or JavaSript. 

See [View UI](#View UI).

#####WEB-INF/web.xml
The web.xml is the deployment descriptor used to deploy the view as a web app.  The Java EE standards apply for the descriptor.  

For example …

      <servlet>
        <servlet-name>HelloServlet</servlet-name>
        <servlet-class>org.apache.ambari.view.hello.HelloServlet</servlet-class>
      </servlet>
      <servlet-mapping>
        <servlet-name>HelloServlet</servlet-name>
        <url-pattern>/ui</url-pattern>
      </servlet-mapping>


For this simple hello world example a single servlet named **HelloServlet** is mapped to the context path **'/ui'**.


#####Servlets

Servlets contained in a view archive will be deployed as part of the view and mapped as described in the web.xml.

The view context may be accessed in the servlet by obtaining it as a servlet context attribute in the init() method.

For example … 

      private ViewContext viewContext;

      @Override
      public void init(ServletConfig config) throws ServletException {
        super.init(config);

        ServletContext context = config.getServletContext();
        viewContext = (ViewContext) context.getAttribute(ViewContext.CONTEXT_ATTRIBUTE);
      }

The view context may be used by the servlet to access information about the context provided by the Ambari container.

For example, in the doGet() method of the servlet we can use the view context instance to access the current user name or the view instance properties.

      protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter writer = response.getWriter();

        Map<String, String> properties = viewContext.getProperties();

        String name = properties.get("name");
        if (name == null) {
          name = viewContext.getUsername();
        }
        writer.println("<h1>Hello " + name + "!</h1>");
      }


###Resources
Resources may be exposed for a view.  Each resource is defined in the view.xml descriptor.  All resource descriptions specify a resource name and service class.  See [view.xml](#view.xml).

Each resource endpoint may then be accessed through the Ambari API.  See [API](#API).

#####Service Class

The service class uses JAX-RS annotations to define the view resource endpoint.  Note that the injected ViewContext.


      @Inject
      ViewContext context;

      /**
       * @see org.apache.ambari.view.filebrowser.DownloadService
       * @return service
       */
      @Path("/download")
      public DownloadService download() {
        return new DownloadService(context);
      }  
      
      /**
       * @see org.apache.ambari.view.filebrowser.FileOperationService
       * @return service
       */
      @Path("/fileops")
      public FileOperationService fileOps() {
        return new FileOperationService(context);
      }    



####Managed Resources
A managed resource is a resource that is managed through the Ambari API framework.  In addition to a name and service class, a managed resource definition includes plural name, resource class and provider class.  See [view.xml](#view.xml).
 
The advantage of using a managed resource over a simple resource is that any queries through the REST API for managed resource may take advantage of any of the features of the API framework.  These include partial response, query predicates, pagination, sub-resource queries, etc.  See [API](#API).

#####Service Class

This service class uses JAX-RS annotations to define the actions to get the resources.  
This example shows how a service class could handle a request by delegating to the API framework.  Note that the injected ViewResourceHandler is used to pass control to the API framework.

      @Inject
      ViewResourceHandler resourceHandler;

      …
      @GET
      @Path("{scriptId}")
      @Produces(MediaType.APPLICATION_JSON)
      public Response getScript(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("scriptId") String scriptId) {
        return resourceHandler.handleRequest(headers, ui, scriptId);
      }
      



#####Resource Class

A resource class is a JavaBean that exposes the attributes of the resource.

For example …

    public class PigScript {
      private String id;
      private String title;
      private String pigScript;
      private String pythonScript;
      private String templetonArguments;
      private Date dateCreated;
      private String owner;
      … 
      public String getId() {
        return id;
      }

      public void setId(String id) {
        this.id = id;
      }
      …  
    }
  

#####Resource Provider Class

The resource provider class is an implementation of the ResourceProvider interface.  It provides access to resource for the Ambari API framework.

For example … 

      public class ScriptResourceProvider implements ResourceProvider<PigScript> {
        @Inject
        ViewContext viewContext;


        @Override
        public FileResource getResource(String resourceId, Set<String> propertyIds) throws
            SystemException, NoSuchResourceException, UnsupportedPropertyException {

          Map<String, String> properties = viewContext.getProperties();

          String userScriptsPath = properties.get("dataworker.userScriptsPath");

          try {
            return getResource(resourceId, userScriptsPath, propertyIds);
          } catch (IOException e) {
            throw new SystemException("Can't get resource " + resourceId + ".", e);
          }
        }
        ... 
      }

###Persistence

The application data map is different than the view instance properties that are specified to instantiate the view.  The application data map may contain any arbitrary string values that the view needs to persist and may be updated or queried at any point during the life of the view instance by the application logic. 

The view context contains methods to put and get values from the application data map.

      /**
       * Save an instance data value for the given key.
       *
       * @param key    the key
       * @param value  the value
       */
      public void putInstanceData(String key, String value);
      
      /**
       * Get the instance data value for the given key.
       *
       * @param key  the key
       *
       * @return the instance data value
       */
      public String getInstanceData(String key);
      
      /**
       * Get the instance data values.
       *
       * @return the view instance property values
       */
      public Map<String, String> getInstanceData();
      
      /**
       * Remove the instance data value for the given key.
       *
       * @param key  the key
       */

#####DataStore
The data store allows view instances to save JavaBean entities in the Ambari database.

The view components may acquire a data store instance through the injected view context.  The view context exposes a data store object … 

      /**
       * Get a data store for view persistence entities.
       *
       * @return a data store
       */
      public DataStore getDataStore();

The DataStore object exposes a simple interface for persisting view entities.  The basic CRUD operations are supported …  

      /**
       * Save the given entity to persistent storage.  The entity must be declared as an
       * <entity> in the <persistence> element of the view.xml.
       *
       * @param entity  the entity to be persisted.
       *
       * @throws PersistenceException thrown if the given entity can not be persisted
       */
      public void store(Object entity) throws PersistenceException;
    
      /**
       * Remove the given entity from persistent storage.
       *
       * @param entity  the entity to be removed.
       *
       * @throws PersistenceException thrown if the given entity can not be removed
       */
      public void remove(Object entity) throws PersistenceException;
    
      /**
       * Find the entity of the given class type that is uniquely identified by the
       * given primary key.
       *
       * @param clazz       the entity class
       * @param primaryKey  the primary key
       * @param <T>         the entity type
       *
       * @return the entity; null if the entity can't be found
       *
       * @throws PersistenceException thrown if an error occurs trying to find the entity
       */
      public <T> T find(Class<T> clazz, Object primaryKey) throws PersistenceException;
    
      /**
       * Find all the entities for the given where clause.  Specifying null for the where
       * clause should return all entities of the given class type.
       *
       * @param clazz        the entity class
       * @param whereClause  the where clause; may be null
       * @param <T>          the entity type
       *
       * @return all of the entities for the given where clause; empty collection if no
       *         entities can be found
       *
       * @throws PersistenceException
       */
      public <T> Collection<T> findAll(Class<T> clazz, String whereClause) throws PersistenceException;
      
Each entity to be persisted by the view should be specified in the view.xml.  See [view.xml](#view.xml).   


Packaging
-----

All views are packaged as a view archive.  The view archive contains the configuration file and various optional components of the view like resources and resource providers.

The view resource also contains all of the view UI components such as html, JavaSript and servlets.

###Dependencies
The view may include dependency classes and jars directly into the archive.  Classes should be included under **WEB-INF/classes**.  Jars should be included under **WEB-INF/lib**.

###view.xml

The view.xml file is the only required file for a view archive.  The view.xml is the configuration that describes the view and view instances for Ambari.

Element | Description
---|---
name    |The unique view name (required)
label   |The user facing name
version |The view version (required)
parameter| Define a properties used to describe an instance 
resource | Describe a resources exposed by the view
persistence   | Describe a view entities for persistence 
instance | Define an instances of a view

#####parameter
Element | Description
---|---
name    |The parameter name
description    |The parameter description
required    |Determines whether or not the parameter is required

#####resource
Element | Description
---|---
name    |The resource name (i.e file)
plural-name    |The parameter description (i.e. files) *
id-property    |The id field of the managed resource class *
resource-class |The class of the JavaBean that contains the attributes of a managed resource *
provider-class |The class of the managed resource provider *
service-class  |The class of the JAX-RS annotated service resource

For example …

    <resource>
        <name>files</name>
        <service-class>org.apache.ambari.view.filebrowser.FileBrowserService</service-class>
    </resource>

\* only required for a managed resource.  For example ...

    <resource>
      <name>script</name>
      <plural-name>scripts</plural-name>
      <id-property>id</id-property>
      <resource-class>org.apache.ambari.view.pig.resources.scripts.models.PigScript</resource-class>
      <provider-class>org.apache.ambari.view.pig.resources.scripts.ScriptResourceProvider</provider-class>
      <service-class>org.apache.ambari.view.pig.resources.scripts.ScriptService</service-class>
    </resource>


#####persitence
Element | Description
---|---
entity | An entity that may be persisted through the DataStore

#####entity
Element | Description
---|---
class | The class ot the JavaBean that contains the attributes of an entity
id-property | The id field of the entity

For example …

    <persistence>
      <entity>
        <class>org.apache.ambari.view.employee.EmployeeEntity</class>
        <id-property>id</id-property>
      </entity>
      <entity>
        <class>org.apache.ambari.view.employee.AddressEntity</class>
        <id-property>id</id-property>
      </entity>
      <entity>
        <class>org.apache.ambari.view.employee.ProjectEntity</class>
        <id-property>id</id-property>
      </entity>
    </persistence> 

#####instance
Element | Description
---|---
name | The unique instance name (required)
property | An instance property

#####property
Element | Description
---|---
key | The property key (must match view parameter name)
value | The property value




A complete view.xml example …

    <view>
        <name>FILES</name>
        <label>Files</label>
        <version>0.1.0</version>

        <parameter>
            <name>dataworker.defaultFs</name>
            <description>FileSystem URI</description>
            <required>true</required>
        </parameter>
        <parameter>
            <name>dataworker.username</name>
            <description>The username (defaults to ViewContext username)</description>
            <required>false</required>
        </parameter>
    
        <resource>
            <name>files</name>
            <service-class>org.apache.ambari.view.filebrowser.FileBrowserService</service-class>
        </resource>
        
        <instance>
            <name>FILES_1</name>
            <property>
                <key>dataworker.defaultFs</key>
                <value>hdfs://<server>:8020</value>
            </property>
        </instance>
    </view>

The configuration in this example defines a view named FILES that has a single instance.  You can see that the view includes a required parameter called **'dataworker.defaultFs'** and an optional parameter called **'dataworker.username'**.  The view also contains an entry for a resource named **'files'**. 


Deploy
-----
To deploy a view, simply place the view archive in the views folder of the ambari-server machine.  By default the views folder is located at ...

    /var/lib/ambari-server/resources/views


###Exploded Archive
When a view is deployed, the contents of the archive are expanded under the views folder.  By default the exploded view archives are located at … 

    /var/lib/ambari-server/resources/views/work/:viewName{:viewVersion}
    
For example, the exploded view archive for the above Files view would be found at …

    /var/lib/ambari-server/resources/views/work/FILES{0.1.0}

Once deployed, the user may make changes directly to the view components.  Changes to some files, such as html and other UI components, may be noticed right away while others require a restart of Ambari.  

Use
-----
###API

#####Get Views
The user may query for all of the deployed views.  Note that views are a top level resources (they are not sub-resources of a cluster).

    GET http://<server>:8080/api/v1/views/

    {
      "href" : "http://<server>:8080/api/v1/views/",
      "items" : [
        {
          "href" : "http://<server>:8080/api/v1/views/FILES",
          "ViewInfo" : {
            "view_name" : "FILES"
          }
        },
        {
          "href" : "http://<server>:8080/api/v1/views/HELLO_WORLD",
          "ViewInfo" : {
            "view_name" : "HELLO_WORLD"
          }
        }
      ]
    }

#####Get View
To get a specific view, request it by name.  Note that the response shows all of the versions of the view as sub-resources.

    GET http://<server>:8080/api/v1/views/FILES/
    
    {
      "href" : "http://<server>:8080/api/v1/views/FILES/",
      "ViewInfo" : {
        "view_name" : "FILES"
      },
      "versions" : [
        {
          "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0",
          "ViewVersionInfo" : {
            "version" : "0.1.0",
            "view_name" : "FILES"
          }
        }
      ]
    }


#####Get Versions
The user may request all of the versions of a named view.

    GET http://<server>:8080/api/v1/views/FILES/versions/
    
    {
      "href" : "http://<server>:8080/api/v1/views/FILES/versions/",
      "items" : [
        {
          "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0",
          "ViewVersionInfo" : {
            "version" : "0.1.0",
            "view_name" : "FILES"
          }
        }
      ]
    }

#####Get Version
A specific version can be requested.  Note that all of the instances of the view version are included in the response.

    GET http://<server>:8080/api/v1/views/FILES/versions/0.1.0/
    
    {
      "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/",
      "ViewVersionInfo" : {
        "archive" : "/var/lib/ambari-server/resources/views/work/FILES{0.1.0}",
        "label" : "Files",
        "parameters" : [
          {
            "name" : "dataworker.defaultFs",
            "description" : "FileSystem URI",
            "required" : true
          },
          {
            "name" : "dataworker.username",
            "description" : "The username (defaults to ViewContext username)",
            "required" : false
          }
        ],
        "version" : "0.1.0",
        "view_name" : "FILES"
      },
      "instances" : [
        {
          "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_1",
          "ViewInstanceInfo" : {
            "instance_name" : "FILES_1",
            "version" : "0.1.0",
            "view_name" : "FILES"
          }
        }
      ]
    }

#####Get Instances
The user may request all of the instances of a view.

    GET http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/
    
    {
      "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/",
      "items" : [
        {
          "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_1",
          "ViewInstanceInfo" : {
            "instance_name" : "FILES_1",
            "version" : "0.1.0",
            "view_name" : "FILES"
          }
        }
      ]
    }


#####Get Instance
A specific view instance may be requested by specifying its name.  Note that the instance resources are listed as sub-resources.

    GET http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_1
    
    {
      "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_1",
      "ViewInstanceInfo" : {
        "context_path" : "/views/FILES/0.1.0/FILES_1",
        "instance_name" : "FILES_1",
        "version" : "0.1.0",
        "view_name" : "FILES",
        "instance_data" : { },
        "properties" : {
          "dataworker.defaultFs" : "hdfs://<server>:8020"
        }
      },
      "resources" : [
        {
          "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_1/resources/files",
          "instance_name" : "FILES_1",
          "name" : "files",
          "version" : "0.1.0",
          "view_name" : "FILES"
        }
      ]
    }
    
#####Create Instance
New view instances may be created through the API.

    POST http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_2
    
#####Update Instance
The properties of a view instance may be updated through the API.

    PUT http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_2
    [{
      "ViewInstanceInfo" : {
          "properties" : {
            "dataworker.defaultFs" : "hdfs://MyServer:8020"
          }
        }
    }]

#####Delete Instance
A view instances may be deleted through the API.

    DELETE http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_2
    

#####Resources
A view resource may be accessed through the REST API.  The href for each view resource can be found in a request for the parent view instance.  The resource endpoints and behavior depends on the implementation of the JAX-RS annotated service class specified for the resource element in the view.xml. 

    GET http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_1/resources/files/fileops/listdir?path=%2F
    
    [{"path":"/app-logs","replication":0,"isDirectory":true,"len":0,"owner":"yarn","group":"hadoop","permission":"-rwxrwxrwx","accessTime":0,"modificationTime":1400006792122,"blockSize":0},{"path":"/mapred","replication":0,"isDirectory":true,"len":0,"owner":"mapred","group":"hdfs","permission":"-rwxr-xr-x","accessTime":0,"modificationTime":1400006653817,"blockSize":0},{"path":"/mr-history","replication":0,"isDirectory":true,"len":0,"owner":"hdfs","group":"hdfs","permission":"-rwxr-xr-x","accessTime":0,"modificationTime":1400006653822,"blockSize":0},{"path":"/tmp","replication":0,"isDirectory":true,"len":0,"owner":"hdfs","group":"hdfs","permission":"-rwxrwxrwx","accessTime":0,"modificationTime":1400006720415,"blockSize":0},{"path":"/user","replication":0,"isDirectory":true,"len":0,"owner":"hdfs","group":"hdfs","permission":"-rwxr-xr-x","accessTime":0,"modificationTime":1400006610050,"blockSize":0}]

#####Managed Resources
Any managed resources for a view may also be accessed through the REST API.  Note that instances of a managed resource appear as sub-resources of an instance under the plural name specified for the resource in the view.xml.  In this example, a list of managed resources named **'scripts'** is included in the response for the view instance … 
 
    GET http://<server>:8080/api/v1/views/PIG/versions/0.1.0/instances/INSTANCE_1
    
    {
      "href" : "http://<server>:8080/api/v1/views/PIG/versions/0.1.0/instances/INSTANCE_1",
      "ViewInstanceInfo" : {
        "context_path" : "/views/PIG/0.1.0/INSTANCE_1",
        "instance_name" : "INSTANCE_1",
        "version" : "0.1.0",
        "view_name" : "PIG",
        "instance_data" : { },
        "properties" : {
          … 
        }
      },
      "resources" : [ ],
      "scripts" : [
        {
          "href" : "http://<server>:8080/api/v1/views/PIG/versions/0.1.0/instances/INSTANCE_1/scripts/script1",
          "id" : "script1",
          "instance_name" : "INSTANCE_1",
          "version" : "0.1.0",
          "view_name" : "PIG"
        },
        {
          "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/INSTANCE_1/scripts/script2",
          "id" : "script2",
          "instance_name" : "INSTANCE_1",
          "version" : "0.1.0",
          "view_name" : "PIG"
        },
        …
      ]
    }
    
 
To get a single managed resource …
    
    
    GET http://<server>:8080/api/v1/views/PIG/versions/0.1.0/instances/INSTANCE_1/scripts/script1
    
    
    {
      "href" : "http://<server>:8080/api/v1/views/PIG/versions/0.1.0/instances/INSTANCE_1/scripts/script1",
      "id" : "script1",      
      "pigScript" : "… ",
      "pythonScript" : "… ",
      "templetonArguments" : "… ",
      "dateCreated" : … ,
      "owner" : "… ",
      "instance_name" : "INSTANCE_1",
      "version" : "0.1.0",
      "view_name" : "PIG"
    }
    
Because the resource is managed through the Ambari API framework, it may be queried using partial response and query predicates.  For example …
 
    GET http://<server>:8080/api/v1/views/PIG/versions/0.1.0/instances/INSTANCE_1/scripts?fields=pythonScript&owner=jsmith
    

###View UI
The context root of a view instance is …
    
    /views/:viewName/:viewVersion/:viewInstance/

For example, the context root can be found in the response for a view instance … 
    
![image](instance_response.png)

So, to access the UI of the above Files view the user would browse to …

    http://<server>:8080/views/FILES/0.1.0/FILES_1/  



![image](ui.png)
