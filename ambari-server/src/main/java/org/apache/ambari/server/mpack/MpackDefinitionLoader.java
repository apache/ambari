/*
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
package org.apache.ambari.server.mpack;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.dao.ExtensionLinkDAO;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.StackResolutionContext;
import org.apache.ambari.server.state.ExtensionInfo;
import org.apache.ambari.server.state.stack.OsFamily;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Uses the existing Java inheritance resolver with explicit immutable inputs. */
@Singleton
public class MpackDefinitionLoader {
  private final MpackSnapshots snapshots;
  private final StackManagerFactory factory;
  private final OsFamily osFamily;
  private final ExtensionLinkDAO links;

  @Inject
  public MpackDefinitionLoader(MpackSnapshots snapshots, StackManagerFactory factory,
      OsFamily osFamily, ExtensionLinkDAO links) {
    this.snapshots = snapshots;
    this.factory = factory;
    this.osFamily = osFamily;
    this.links = links;
  }

  public List<StackResolutionContext.Binding> existingBindings() {
    return links.findAll().stream().map(link -> new StackResolutionContext.Binding(
        link.getStack().getStackName(), link.getStack().getStackVersion(),
        link.getExtension().getExtensionName(), link.getExtension().getExtensionVersion())).toList();
  }

  public StackManager resolve(MpackSnapshots.Snapshot snapshot) {
    snapshots.verify(snapshot);
    Path root = snapshots.resourceRoot(snapshot.id());
    try {
      StackManager candidate = factory.createCandidate(root.resolve("stacks").toFile(),
          root.resolve("common-services").toFile(), root.resolve("extensions").toFile(),
          osFamily, new StackResolutionContext(snapshot.id(), snapshot.bindings()));
      for (StackResolutionContext.Binding binding : snapshot.bindings()) {
        ExtensionInfo extension = candidate.getExtension(binding.extensionName(), binding.extensionVersion());
        boolean compatible = extension.getStacks().stream().anyMatch(stack ->
            stack.getName().equals(binding.stackName())
                && MpackManifest.compareVersions(binding.stackVersion(), stack.getVersion()) >= 0);
        if (!compatible) {
          throw new MpackException(MpackException.Code.VERSION_INCOMPATIBLE,
              "Extension does not declare support for the selected stack",
              Map.of("stack", binding.stackName() + "/" + binding.stackVersion(),
                  "extension", binding.extensionName() + "/" + binding.extensionVersion()));
        }
      }
      return candidate;
    } catch (org.apache.ambari.server.stack.DefinitionConflictException e) {
      throw new MpackException(MpackException.Code.RESOURCE_CONFLICT,
          "The selected definition context has conflicting service providers",
          Map.of("resource", e.getResourceIdentity(), "providers", List.of(
              provider(snapshot, e.getPreviousDirectory()), provider(snapshot, e.getCandidateDirectory()))));
    } catch (AmbariException e) {
      throw new MpackException(MpackException.Code.INVALID_MANIFEST,
          "The candidate definition set failed Ambari metadata validation");
    }
  }

  private String provider(MpackSnapshots.Snapshot snapshot, String directory) {
    Path root = snapshots.resourceRoot(snapshot.id());
    Path source = Path.of(directory).resolve("metainfo.xml").toAbsolutePath().normalize();
    if (!source.startsWith(root)) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Definition provider leaves the verified snapshot");
    }
    String path = root.relativize(source).toString().replace(source.getFileSystem().getSeparator(), "/");
    MpackSnapshots.Resource resource = snapshot.resources().get(path);
    if (resource == null) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Definition provider has no resource owner");
    }
    return resource.provider();
  }
}
