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
package org.apache.ambari.server.controller.dependencies.security;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.controller.dependencies.ManagedDependencyIntegrationException;
import org.apache.ambari.server.controller.dependencies.security
    .ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingInput;
import org.apache.ambari.server.controller.dependencies.security
    .ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingProof;
import org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalType;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;

/**
 * Applies a managed HBase Kerberos identity to a detached descriptor and seals
 * the corresponding calculated configuration values.
 */
public final class ManagedHBaseKerberosDescriptorOverlay {
  private static final String HBASE = "HBASE";
  private static final String HBASE_SITE = "hbase-site";
  private static final String HBASE_ENV = "hbase-env";
  private static final String CORE_SITE = "core-site";
  private static final String AUTH_TO_LOCAL = "hadoop.security.auth_to_local";

  private static final String HEADLESS_IDENTITY = "hbase";
  private static final String SMOKE_IDENTITY = "hbase_smokeuser";
  private static final String HDFS_ADMIN_IDENTITY = "hbase_hbase_master_hdfs";

  private static final String HEADLESS_PRINCIPAL_CONFIGURATION =
      "hbase-env/hbase_principal_name";
  private static final String HEADLESS_KEYTAB_CONFIGURATION = "hbase-env/hbase_user_keytab";
  private static final String HEADLESS_KEYTAB = "${keytab_dir}/hbase.headless.keytab";
  private static final String DAEMON_KEYTAB = "${keytab_dir}/hbase.service.keytab";

  private static final Map<String, RoleShape> ROLE_SHAPES = Map.of(
      "HBASE_MASTER", new RoleShape("hbase_master_hbase",
          "hbase-site/hbase.master.kerberos.principal",
          "hbase-site/hbase.master.keytab.file"),
      "HBASE_REGIONSERVER", new RoleShape("hbase_regionserver_hbase",
          "hbase-site/hbase.regionserver.kerberos.principal",
          "hbase-site/hbase.regionserver.keytab.file"),
      "HBASE_THRIFT", new RoleShape("hbase_thrift_hbase",
          "hbase-site/hbase.thrift.kerberos.principal",
          "hbase-site/hbase.thrift.keytab.file"));

  private final KerberosDescriptorFactory descriptorFactory;
  private final ManagedHdfsAuthToLocalVerifier authToLocalVerifier;

  public ManagedHBaseKerberosDescriptorOverlay() {
    this(new KerberosDescriptorFactory(), new ManagedHdfsAuthToLocalVerifier());
  }

  ManagedHBaseKerberosDescriptorOverlay(KerberosDescriptorFactory descriptorFactory,
      ManagedHdfsAuthToLocalVerifier authToLocalVerifier) {
    this.descriptorFactory = Objects.requireNonNull(descriptorFactory, "descriptorFactory");
    this.authToLocalVerifier = Objects.requireNonNull(authToLocalVerifier,
        "authToLocalVerifier");
  }

  public KerberosDescriptor applyCopy(KerberosDescriptor effectiveComposite,
      KerberosDescriptor userDescriptor, ManagedHBaseKerberosOverlaySpec spec) {
    Objects.requireNonNull(effectiveComposite, "effectiveComposite");
    Objects.requireNonNull(spec, "spec");
    rejectUserConflicts(userDescriptor, spec);

    KerberosDescriptor copy = descriptorFactory.createInstance(effectiveComposite.toMap());
    KerberosServiceDescriptor hbase = requireService(copy);
    overlayHeadlessIdentity(hbase, spec);
    requireSmokeReference(hbase);
    for (Map.Entry<String, RoleShape> role : ROLE_SHAPES.entrySet()) {
      overlayDaemonIdentity(hbase, role.getKey(), role.getValue(), spec);
    }

    KerberosComponentDescriptor master = requireComponent(hbase, "HBASE_MASTER");
    validateHdfsAdminReference(master, spec.hasManagedHdfs());
    if (spec.hasManagedHdfs()) {
      master.removeIdentity(HDFS_ADMIN_IDENTITY);
    }
    hbase.putAuthToLocalProperty(ManagedHBaseKerberosOverlaySpec.AUTH_TO_LOCAL_PROPERTY);
    putDescriptorConfiguration(hbase, HBASE_ENV, "hbase_user",
        spec.effectiveShortUser());
    putDescriptorConfiguration(hbase, HBASE_ENV, "hbase_principal_name",
        spec.headlessPrincipal());
    putDescriptorConfiguration(hbase, HBASE_SITE, "hbase.superuser",
        spec.hbaseSuperuser());
    if (spec.hasManagedZooKeeper()) {
      putDescriptorConfiguration(hbase, HBASE_SITE, "zookeeper.znode.parent",
          spec.znodeParent());
    }
    assertOverlay(hbase, spec);
    return copy;
  }

  public SealedConfigurations sealCalculatedConfigurations(
      Map<String, Map<String, String>> calculated,
      ManagedHBaseKerberosOverlaySpec spec) {
    Objects.requireNonNull(calculated, "calculated");
    Objects.requireNonNull(spec, "spec");
    Map<String, Map<String, String>> sealed = mutableDeepCopy(calculated);
    putCalculated(sealed, HBASE_ENV, "hbase_user", spec.effectiveShortUser());
    putCalculated(sealed, HBASE_ENV, "hbase_principal_name", spec.headlessPrincipal());
    putCalculated(sealed, HBASE_SITE, "hbase.master.kerberos.principal",
        spec.rolePrincipalPattern());
    putCalculated(sealed, HBASE_SITE, "hbase.regionserver.kerberos.principal",
        spec.rolePrincipalPattern());
    putCalculated(sealed, HBASE_SITE, "hbase.thrift.kerberos.principal",
        spec.rolePrincipalPattern());
    putCalculated(sealed, HBASE_SITE, "hbase.superuser", spec.hbaseSuperuser());
    if (spec.hasManagedZooKeeper()) {
      putCalculated(sealed, HBASE_SITE, "zookeeper.znode.parent", spec.znodeParent());
    }

    String rules = valueAt(sealed, CORE_SITE, AUTH_TO_LOCAL);
    if (rules == null || rules.isBlank()) {
      throw conflict("core-site/hadoop.security.auth_to_local",
          "The calculated consumer-local auth-to-local policy is missing.");
    }
    ConsumerLocalMappingProof mappingProof = authToLocalVerifier.proveConsumerLocalMappings(
        rules, new ConsumerLocalMappingInput(spec.realm(), spec.effectiveShortUser(),
            spec.rolePrincipalPattern(), spec.headlessPrincipal(), spec.smokePrincipal(),
            spec.smokeShortUser()));
    return new SealedConfigurations(immutableDeepCopy(sealed), mappingProof);
  }

  private void rejectUserConflicts(KerberosDescriptor userDescriptor,
      ManagedHBaseKerberosOverlaySpec spec) {
    if (userDescriptor == null) {
      return;
    }
    checkControlledConfigurations(userDescriptor, spec);
    KerberosServiceDescriptor hbase = userDescriptor.getService(HBASE);
    if (hbase == null) {
      return;
    }
    Set<String> userAuthProperties = hbase.getAuthToLocalProperties();
    if (userAuthProperties != null && !userAuthProperties.isEmpty()
        && !Set.of(ManagedHBaseKerberosOverlaySpec.AUTH_TO_LOCAL_PROPERTY)
            .equals(userAuthProperties)) {
      throw conflict("HBASE/auth_to_local_properties",
          "A user Kerberos descriptor changes the managed consumer-local rule target.");
    }

    checkIdentityIfPresent(hbase.getIdentity(HEADLESS_IDENTITY), spec.headlessPrincipal(),
        spec.effectiveShortUser(), "r", "HBASE/hbase");
    for (Map.Entry<String, RoleShape> role : ROLE_SHAPES.entrySet()) {
      KerberosComponentDescriptor component = hbase.getComponent(role.getKey());
      if (component != null) {
        checkIdentityIfPresent(component.getIdentity(role.getValue().identityName()),
            spec.rolePrincipalPattern(), spec.effectiveShortUser(), "",
            "HBASE/" + role.getKey() + "/" + role.getValue().identityName());
      }
    }
    KerberosComponentDescriptor master = hbase.getComponent("HBASE_MASTER");
    if (spec.hasManagedHdfs() && master != null
        && master.getIdentity(HDFS_ADMIN_IDENTITY) != null) {
      throw conflict("HBASE/HBASE_MASTER/" + HDFS_ADMIN_IDENTITY,
          "A user Kerberos descriptor retains the local HDFS administrator identity.");
    }
  }

  private void checkControlledConfigurations(KerberosDescriptor descriptor,
      ManagedHBaseKerberosOverlaySpec spec) {
    checkControlledConfigurationsIn(descriptor, spec);
    if (descriptor.getServices() == null) {
      return;
    }
    for (KerberosServiceDescriptor service : descriptor.getServices().values()) {
      checkControlledConfigurationsIn(service, spec);
      if (service.getComponents() != null) {
        for (KerberosComponentDescriptor component : service.getComponents().values()) {
          checkControlledConfigurationsIn(component, spec);
        }
      }
    }
  }

  private void checkControlledConfigurationsIn(AbstractKerberosDescriptorContainer container,
      ManagedHBaseKerberosOverlaySpec spec) {
    checkConfigurationIfSet(container, HBASE_ENV, "hbase_user",
        spec.effectiveShortUser());
    checkConfigurationIfSet(container, HBASE_ENV, "hbase_principal_name",
        spec.headlessPrincipal());
    checkConfigurationIfSet(container, HBASE_SITE, "hbase.master.kerberos.principal",
        spec.rolePrincipalPattern());
    checkConfigurationIfSet(container, HBASE_SITE, "hbase.regionserver.kerberos.principal",
        spec.rolePrincipalPattern());
    checkConfigurationIfSet(container, HBASE_SITE, "hbase.thrift.kerberos.principal",
        spec.rolePrincipalPattern());
    checkConfigurationIfSet(container, HBASE_SITE, "hbase.superuser",
        spec.hbaseSuperuser());
    if (spec.hasManagedZooKeeper()) {
      checkConfigurationIfSet(container, HBASE_SITE, "zookeeper.znode.parent",
          spec.znodeParent());
    }
  }

  private void checkConfigurationIfSet(AbstractKerberosDescriptorContainer container,
      String type, String property, String expected) {
    KerberosConfigurationDescriptor configuration = container.getConfiguration(type);
    String actual = configuration == null ? null : configuration.getProperty(property);
    if (actual != null && !expected.equals(actual)) {
      String containerPath = container.getPath().isEmpty() ? "" : container.getPath();
      throw conflict(containerPath + "/" + type + "/" + property,
          "A user configuration conflicts with the approved managed HBase identity.");
    }
  }

  private void checkIdentityIfPresent(KerberosIdentityDescriptor identity,
      String expectedPrincipal, String expectedOwner, String expectedGroupAccess,
      String path) {
    if (identity == null) {
      return;
    }
    if (identity.getReference() != null) {
      throw conflict(path, "A controlled HBase identity cannot be replaced by a reference.");
    }
    KerberosPrincipalDescriptor principal = identity.getPrincipalDescriptor();
    if (principal != null) {
      checkIfSet(principal.getValue(), expectedPrincipal, path + "/principal/value");
      checkIfSet(principal.getLocalUsername(), expectedOwner,
          path + "/principal/local_username");
    }
    KerberosKeytabDescriptor keytab = identity.getKeytabDescriptor();
    if (keytab != null) {
      checkIfSet(keytab.getOwnerName(), expectedOwner, path + "/keytab/owner/name");
      checkIfSet(keytab.getOwnerAccess(), "r", path + "/keytab/owner/access");
      checkIfSet(keytab.getGroupName(), "${cluster-env/user_group}",
          path + "/keytab/group/name");
      checkIfSet(keytab.getGroupAccess(), expectedGroupAccess,
          path + "/keytab/group/access");
    }
  }

  private void checkIfSet(String actual, String expected, String path) {
    if (actual != null && !expected.equals(actual)) {
      throw conflict(path,
          "A user Kerberos descriptor conflicts with the approved managed HBase identity.");
    }
  }

  private void overlayHeadlessIdentity(KerberosServiceDescriptor service,
      ManagedHBaseKerberosOverlaySpec spec) {
    KerberosIdentityDescriptor identity = requireIdentity(service, HEADLESS_IDENTITY);
    requireStandalone(identity, "HBASE/hbase");
    KerberosPrincipalDescriptor principal = requirePrincipal(identity, KerberosPrincipalType.USER,
        HEADLESS_PRINCIPAL_CONFIGURATION, "HBASE/hbase");
    requireOriginalOrManaged(principal.getValue(),
        "${hbase-env/hbase_user}${principal_suffix}@${realm}", spec.headlessPrincipal(),
        "HBASE/hbase/principal/value");
    principal.setValue(spec.headlessPrincipal());
    principal.setLocalUsername(spec.effectiveShortUser());
    KerberosKeytabDescriptor keytab = requireKeytab(identity, HEADLESS_KEYTAB,
        HEADLESS_KEYTAB_CONFIGURATION, "r", "HBASE/hbase");
    requireOriginalOrManaged(keytab.getOwnerName(), "${hbase-env/hbase_user}",
        spec.effectiveShortUser(), "HBASE/hbase/keytab/owner/name");
    keytab.setOwnerName(spec.effectiveShortUser());
  }

  private void overlayDaemonIdentity(KerberosServiceDescriptor service,
      String componentName, RoleShape role, ManagedHBaseKerberosOverlaySpec spec) {
    KerberosComponentDescriptor component = requireComponent(service, componentName);
    KerberosIdentityDescriptor identity = requireIdentity(component, role.identityName());
    String path = "HBASE/" + componentName + "/" + role.identityName();
    requireStandalone(identity, path);
    KerberosPrincipalDescriptor principal = requirePrincipal(identity,
        KerberosPrincipalType.SERVICE, role.principalConfiguration(), path);
    requireOriginalOrManaged(principal.getValue(), "hbase/_HOST@${realm}",
        spec.rolePrincipalPattern(), path + "/principal/value");
    principal.setValue(spec.rolePrincipalPattern());
    principal.setLocalUsername(spec.effectiveShortUser());
    KerberosKeytabDescriptor keytab = requireKeytab(identity, DAEMON_KEYTAB,
        role.keytabConfiguration(), "", path);
    requireOriginalOrManaged(keytab.getOwnerName(), "${hbase-env/hbase_user}",
        spec.effectiveShortUser(), path + "/keytab/owner/name");
    keytab.setOwnerName(spec.effectiveShortUser());
  }

  private void requireSmokeReference(KerberosServiceDescriptor service) {
    KerberosIdentityDescriptor smoke = requireIdentity(service, SMOKE_IDENTITY);
    if (!"/smokeuser".equals(smoke.getReference())
        || smoke.getPrincipalDescriptor() != null || smoke.getKeytabDescriptor() != null) {
      throw unsupported("HBASE/hbase_smokeuser",
          "The active HBase smoke identity has an unsupported descriptor shape.");
    }
  }

  private void validateHdfsAdminReference(KerberosComponentDescriptor master,
      boolean removalAllowed) {
    KerberosIdentityDescriptor identity = master.getIdentity(HDFS_ADMIN_IDENTITY);
    if (identity == null) {
      if (!removalAllowed) {
        throw unsupported("HBASE/HBASE_MASTER/" + HDFS_ADMIN_IDENTITY,
            "The local HDFS administrator reference is missing for an unbound HDFS service.");
      }
      return;
    }
    if (!"/HDFS/NAMENODE/hdfs".equals(identity.getReference())
        || identity.getPrincipalDescriptor() != null || identity.getKeytabDescriptor() != null) {
      throw unsupported("HBASE/HBASE_MASTER/" + HDFS_ADMIN_IDENTITY,
          "The local HDFS administrator reference has an unsupported descriptor shape.");
    }
  }

  private KerberosPrincipalDescriptor requirePrincipal(KerberosIdentityDescriptor identity,
      KerberosPrincipalType type, String configuration, String path) {
    KerberosPrincipalDescriptor principal = identity.getPrincipalDescriptor();
    if (principal == null || principal.getType() != type
        || !configuration.equals(principal.getConfiguration())) {
      throw unsupported(path + "/principal",
          "The active HBase principal has an unsupported descriptor shape.");
    }
    return principal;
  }

  private KerberosKeytabDescriptor requireKeytab(KerberosIdentityDescriptor identity,
      String file, String configuration, String groupAccess, String path) {
    KerberosKeytabDescriptor keytab = identity.getKeytabDescriptor();
    if (keytab == null || !file.equals(keytab.getFile())
        || !configuration.equals(keytab.getConfiguration())
        || !"r".equals(keytab.getOwnerAccess())
        || !"${cluster-env/user_group}".equals(keytab.getGroupName())
        || !groupAccess.equals(keytab.getGroupAccess())) {
      throw unsupported(path + "/keytab",
          "The active HBase keytab has an unsupported ownership or access shape.");
    }
    return keytab;
  }

  private void requireStandalone(KerberosIdentityDescriptor identity, String path) {
    if (identity.getReference() != null) {
      throw unsupported(path,
          "The active HBase identity must be a standalone descriptor.");
    }
  }

  private void requireOriginalOrManaged(String actual, String original, String managed,
      String path) {
    if (!original.equals(actual) && !managed.equals(actual)) {
      throw unsupported(path,
          "The active HBase identity has an unsupported descriptor value.");
    }
  }

  private void assertOverlay(KerberosServiceDescriptor service,
      ManagedHBaseKerberosOverlaySpec spec) {
    assertIdentity(service.getIdentity(HEADLESS_IDENTITY), spec.headlessPrincipal(),
        spec.effectiveShortUser(), "HBASE/hbase");
    for (Map.Entry<String, RoleShape> role : ROLE_SHAPES.entrySet()) {
      KerberosComponentDescriptor component = requireComponent(service, role.getKey());
      assertIdentity(component.getIdentity(role.getValue().identityName()),
          spec.rolePrincipalPattern(), spec.effectiveShortUser(),
          "HBASE/" + role.getKey() + "/" + role.getValue().identityName());
    }
    if (spec.hasManagedHdfs()
        && requireComponent(service, "HBASE_MASTER").getIdentity(HDFS_ADMIN_IDENTITY) != null) {
      throw unsupported("HBASE/HBASE_MASTER/" + HDFS_ADMIN_IDENTITY,
          "The local HDFS administrator identity was not removed.");
    }
    if (!service.getAuthToLocalProperties().contains(
        ManagedHBaseKerberosOverlaySpec.AUTH_TO_LOCAL_PROPERTY)) {
      throw unsupported("HBASE/auth_to_local_properties",
          "The consumer-local rule target was not applied.");
    }
    assertConfiguration(service, HBASE_ENV, "hbase_user", spec.effectiveShortUser());
    assertConfiguration(service, HBASE_ENV, "hbase_principal_name",
        spec.headlessPrincipal());
    assertConfiguration(service, HBASE_SITE, "hbase.superuser", spec.hbaseSuperuser());
    if (spec.hasManagedZooKeeper()) {
      assertConfiguration(service, HBASE_SITE, "zookeeper.znode.parent",
          spec.znodeParent());
    }
  }

  private void assertConfiguration(KerberosServiceDescriptor service, String type,
      String property, String expected) {
    KerberosConfigurationDescriptor configuration = service.getConfiguration(type);
    if (configuration == null || !expected.equals(configuration.getProperty(property))) {
      throw unsupported(type + "/" + property,
          "The managed HBase configuration overlay could not be verified.");
    }
  }

  private void assertIdentity(KerberosIdentityDescriptor identity, String principalValue,
      String localUser, String path) {
    if (identity == null || identity.getPrincipalDescriptor() == null
        || identity.getKeytabDescriptor() == null
        || !principalValue.equals(identity.getPrincipalDescriptor().getValue())
        || !localUser.equals(identity.getPrincipalDescriptor().getLocalUsername())
        || !localUser.equals(identity.getKeytabDescriptor().getOwnerName())
        || !"r".equals(identity.getKeytabDescriptor().getOwnerAccess())) {
      throw unsupported(path,
          "The managed HBase identity overlay could not be verified.");
    }
  }

  private KerberosServiceDescriptor requireService(KerberosDescriptor descriptor) {
    KerberosServiceDescriptor service = descriptor.getService(HBASE);
    if (service == null) {
      throw unsupported(HBASE, "The active Kerberos descriptor has no HBASE service.");
    }
    return service;
  }

  private KerberosComponentDescriptor requireComponent(KerberosServiceDescriptor service,
      String name) {
    KerberosComponentDescriptor component = service.getComponent(name);
    if (component == null) {
      throw unsupported("HBASE/" + name,
          "The active HBase Kerberos descriptor is missing a required role.");
    }
    return component;
  }

  private KerberosIdentityDescriptor requireIdentity(
      AbstractKerberosDescriptorContainer container, String name) {
    KerberosIdentityDescriptor identity = container.getIdentity(name);
    if (identity == null) {
      throw unsupported(container.getPath() + "/" + name,
          "The active HBase Kerberos descriptor is missing a required identity.");
    }
    return identity;
  }

  private void putDescriptorConfiguration(KerberosServiceDescriptor service, String type,
      String property, String value) {
    KerberosConfigurationDescriptor configuration = service.getConfiguration(type);
    if (configuration == null) {
      configuration = new KerberosConfigurationDescriptor(Map.of(type, new TreeMap<>()));
      service.putConfiguration(configuration);
    }
    configuration.putProperty(property, value);
  }

  private static Map<String, Map<String, String>> mutableDeepCopy(
      Map<String, Map<String, String>> source) {
    Map<String, Map<String, String>> copy = new TreeMap<>();
    for (Map.Entry<String, Map<String, String>> entry : source.entrySet()) {
      if (entry.getKey() == null || entry.getValue() == null) {
        throw new IllegalArgumentException("calculated configurations must not contain nulls");
      }
      copy.put(entry.getKey(), new TreeMap<>(entry.getValue()));
    }
    return copy;
  }

  private static Map<String, Map<String, String>> immutableDeepCopy(
      Map<String, Map<String, String>> source) {
    Map<String, Map<String, String>> copy = new TreeMap<>();
    for (Map.Entry<String, Map<String, String>> entry : source.entrySet()) {
      copy.put(entry.getKey(), Collections.unmodifiableMap(new TreeMap<>(entry.getValue())));
    }
    return Collections.unmodifiableMap(copy);
  }

  private static void putCalculated(Map<String, Map<String, String>> configurations,
      String type, String property, String value) {
    configurations.computeIfAbsent(type, ignored -> new TreeMap<>()).put(property, value);
  }

  private static String valueAt(Map<String, Map<String, String>> configurations,
      String type, String property) {
    Map<String, String> values = configurations.get(type);
    return values == null ? null : values.get(property);
  }

  private static ManagedDependencyIntegrationException conflict(String path, String message) {
    return new ManagedDependencyIntegrationException(422,
        "DEPENDENCY_KERBEROS_OVERRIDE_CONFLICT", message + " Path: " + path);
  }

  private static ManagedDependencyIntegrationException unsupported(String path,
      String message) {
    return new ManagedDependencyIntegrationException(422,
        "DEPENDENCY_KERBEROS_DESCRIPTOR_UNSUPPORTED", message + " Path: " + path);
  }

  private record RoleShape(String identityName, String principalConfiguration,
      String keytabConfiguration) {
  }

  public record SealedConfigurations(
      Map<String, Map<String, String>> configurations,
      ConsumerLocalMappingProof mappingProof) {
    public SealedConfigurations {
      configurations = immutableDeepCopy(Objects.requireNonNull(configurations,
          "configurations"));
      mappingProof = Objects.requireNonNull(mappingProof, "mappingProof");
    }

    @Override
    public String toString() {
      return "SealedConfigurations[configurations=redacted, mappingProof="
          + mappingProof + "]";
    }
  }
}
