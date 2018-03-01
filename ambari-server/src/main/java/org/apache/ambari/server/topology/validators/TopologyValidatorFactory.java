/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology.validators;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;

import com.google.common.collect.ImmutableList;

public class TopologyValidatorFactory {

  private final List<TopologyValidator> validators;

  @Inject
  public TopologyValidatorFactory(Provider<AmbariMetaInfo> metaInfo, Configuration config) {
    validators = ImmutableList.<TopologyValidator>builder()
      .add(new RejectUnknownStacks(metaInfo))
      .add(new RejectUnknownComponents())
      .add(new DependencyAndCardinalityValidator())
      .add(new StackConfigTypeValidator())
      .add(new GplPropertiesValidator(config))
      .add(new SecretReferenceValidator())
      .add(new RequiredConfigPropertiesValidator())
      .add(new RequiredPasswordValidator())
      .add(new HiveServiceValidator())
      .add(new NameNodeHighAvailabilityValidator())
      .add(new UnitValidator(UnitValidatedProperty.ALL))
      .build();
  }

  public TopologyValidator createConfigurationValidatorChain() {
    return new ChainedTopologyValidator(validators);
  }

}
