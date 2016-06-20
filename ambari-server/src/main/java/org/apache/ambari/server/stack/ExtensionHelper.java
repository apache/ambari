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

package org.apache.ambari.server.stack;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ExtensionInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.ExtensionMetainfoXml;
import org.apache.ambari.server.utils.VersionUtils;

/**
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
public class ExtensionHelper {

  public static void validateDeleteLink(Clusters clusters, StackInfo stack, ExtensionInfo extension) throws AmbariException {
    validateNotRequiredExtension(stack, extension);
    validateServicesNotInstalled(clusters, stack, extension);
  }

  private static void validateServicesNotInstalled(Clusters clusters, StackInfo stack, ExtensionInfo extension) throws AmbariException {
    for (Cluster cluster : clusters.getClusters().values()) {
      for (ServiceInfo service : extension.getServices()) {
        try {
          if (service != null && cluster.getService(service.getName()) != null) {
            String message = "Extension service is still installed"
                        + ", stackName=" + stack.getName()
                        + ", stackVersion=" + stack.getVersion()
                        + ", service=" + service.getName()
                        + ", extensionName=" + extension.getName()
                        + ", extensionVersion=" + extension.getVersion();

            throw new AmbariException(message);
          }
        }
        catch (ServiceNotFoundException e) {
          //Eat the exception
        }
      }
    }
  }

  public static void validateCreateLink(StackInfo stack, ExtensionInfo extension) throws AmbariException {
    validateSupportedStackVersion(stack, extension);
    validateServiceDuplication(stack, extension);
    validateRequiredExtensions(stack, extension);
  }

  private static void validateSupportedStackVersion(StackInfo stack, ExtensionInfo extension) throws AmbariException {
    for (ExtensionMetainfoXml.Stack validStack : extension.getStacks()) {
      if (validStack.getName().equals(stack.getName())) {
        String minStackVersion = validStack.getVersion();
        if (VersionUtils.compareVersions(stack.getVersion(), minStackVersion) >= 0) {
          //Found a supported stack version
          return;
        }
      }
    }

    String message = "Stack is not supported by extension"
		+ ", stackName=" + stack.getName()
		+ ", stackVersion=" + stack.getVersion()
		+ ", extensionName=" + extension.getName()
		+ ", extensionVersion=" + extension.getVersion();

    throw new AmbariException(message);
  }

  private static void validateServiceDuplication(StackInfo stack, ExtensionInfo extension) throws AmbariException {
    for (ServiceInfo service : extension.getServices()) {
      if (service != null) {
        ServiceInfo stackService = null;
        try {
          stackService = stack.getService(service.getName());
        }
        catch (Exception e) {
          //Eat the exception
        }
        if (stackService != null) {
          String message = "Existing service is included in extension"
                      + ", stackName=" + stack.getName()
                      + ", stackVersion=" + stack.getVersion()
                      + ", service=" + service.getName()
                      + ", extensionName=" + extension.getName()
                      + ", extensionVersion=" + extension.getVersion();

          throw new AmbariException(message);
        }
      }
    }
  }

  private static void validateRequiredExtensions(StackInfo stack, ExtensionInfo extension) throws AmbariException {
    for (ExtensionMetainfoXml.Extension requiredExtension : extension.getExtensions()) {
      if (requiredExtension != null) {
        String message = "Stack has not linked required extension"
                    + ", stackName=" + stack.getName()
                    + ", stackVersion=" + stack.getVersion()
                    + ", extensionName=" + extension.getName()
                    + ", extensionVersion=" + extension.getVersion()
                    + ", requiredExtensionName=" + requiredExtension.getName()
                    + ", requiredExtensionVersion=" + requiredExtension.getVersion();
        try {
          ExtensionInfo stackExtension = stack.getExtension(requiredExtension.getName());
          if (stackExtension != null) {
            String version = requiredExtension.getVersion();
            if (version.endsWith("*")) {
              version = version.substring(0, version.length() - 1);
              if (!stackExtension.getVersion().startsWith(version)) {
                throw new AmbariException(message);
              }
            }
            else if (!stackExtension.getVersion().equals(version)) {
              throw new AmbariException(message);
            }
          }
        }
        catch (Exception e) {
          throw new AmbariException(message, e);
        }
      }
    }
  }

  private static void validateNotRequiredExtension(StackInfo stack, ExtensionInfo extension) throws AmbariException {
    for (ExtensionInfo stackExtension : stack.getExtensions()) {
      if (stackExtension != null) {
        for (ExtensionMetainfoXml.Extension requiredExtension : stackExtension.getExtensions()) {
          if (requiredExtension != null && requiredExtension.getName().equals(extension.getName())) {
            String message = "Stack extension is required by extension"
                        + ", stackName=" + stack.getName()
                        + ", stackVersion=" + stack.getVersion()
                        + ", extensionName=" + extension.getName()
                        + ", extensionVersion=" + extension.getVersion()
                        + ", dependentExtensionName=" + stackExtension.getName()
                        + ", dependentExtensionVersion=" + stackExtension.getVersion();

            throw new AmbariException(message);
          }
        }
      }
    }
  }

}
