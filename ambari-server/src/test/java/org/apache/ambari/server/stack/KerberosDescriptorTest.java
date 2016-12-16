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

package org.apache.ambari.server.stack;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.Assert;

/**
 * KerberosDescriptorTest tests the stack- and service-level descriptors for certain stacks
 * and services
 */
@Ignore
public class KerberosDescriptorTest {
  private static final KerberosDescriptorFactory KERBEROS_DESCRIPTOR_FACTORY = new KerberosDescriptorFactory();

  private static File stacksDirectory;
  private static File hdpStackDirectory;
  private static File hdp22StackDirectory;
  private static File hdp22ServicesDirectory;
  private static File commonServicesDirectory;

  @BeforeClass
  public static void beforeClass() {
    URL rootDirectoryURL = KerberosDescriptorTest.class.getResource("/");
    Assert.notNull(rootDirectoryURL);

    File resourcesDirectory = new File(new File(rootDirectoryURL.getFile()).getParentFile().getParentFile(), "src/main/resources");
    Assert.notNull(resourcesDirectory);
    Assert.isTrue(resourcesDirectory.canRead());

    stacksDirectory = new File(resourcesDirectory, "stacks");
    Assert.notNull(stacksDirectory);
    Assert.isTrue(stacksDirectory.canRead());

    hdpStackDirectory = new File(stacksDirectory, "HDP");
    Assert.notNull(hdpStackDirectory);
    Assert.isTrue(hdpStackDirectory.canRead());

    hdp22StackDirectory = new File(hdpStackDirectory, "2.2");
    Assert.notNull(hdp22StackDirectory);
    Assert.isTrue(hdp22StackDirectory.canRead());

    hdp22ServicesDirectory = new File(hdp22StackDirectory, "services");
    Assert.notNull(hdp22ServicesDirectory);
    Assert.isTrue(hdp22ServicesDirectory.canRead());

    commonServicesDirectory = new File(resourcesDirectory, "common-services");
    Assert.notNull(commonServicesDirectory);
    Assert.isTrue(commonServicesDirectory.canRead());

  }

  @Test
  public void testCommonHBASEServiceDescriptor() throws IOException {
    KerberosDescriptor descriptor = getKerberosDescriptor(commonServicesDirectory, "HBASE", "0.96.0.2.0");
    Assert.notNull(descriptor);
    Assert.notNull(descriptor.getServices());
    Assert.notNull(descriptor.getService("HBASE"));
  }

  @Test
  public void testCommonHDFSServiceDescriptor() throws IOException {
    KerberosDescriptor descriptor = getKerberosDescriptor(commonServicesDirectory, "HDFS", "2.1.0.2.0");
    Assert.notNull(descriptor);
    Assert.notNull(descriptor.getServices());
    Assert.notNull(descriptor.getService("HDFS"));
  }

  @Test
  public void testCommonYarnServiceDescriptor() throws IOException {
    KerberosDescriptor descriptor = getKerberosDescriptor(commonServicesDirectory, "YARN", "2.1.0.2.0");
    Assert.notNull(descriptor);
    Assert.notNull(descriptor.getServices());
    Assert.notNull(descriptor.getService("YARN"));
    Assert.notNull(descriptor.getService("MAPREDUCE2"));
  }

  @Test
  public void testCommonFalconServiceDescriptor() throws IOException {
    KerberosDescriptor descriptor = getKerberosDescriptor(commonServicesDirectory, "FALCON", "0.5.0.2.1");
    Assert.notNull(descriptor);
    Assert.notNull(descriptor.getServices());
    Assert.notNull(descriptor.getService("FALCON"));
  }

  @Test
  public void testCommonHiveServiceDescriptor() throws IOException {
    KerberosDescriptor descriptor = getKerberosDescriptor(commonServicesDirectory, "HIVE", "0.12.0.2.0");
    Assert.notNull(descriptor);
    Assert.notNull(descriptor.getServices());
    Assert.notNull(descriptor.getService("HIVE"));
  }

  @Test
  public void testCommonKnoxServiceDescriptor() throws IOException {
    KerberosDescriptor descriptor = getKerberosDescriptor(commonServicesDirectory, "KNOX", "0.5.0.2.2");
    Assert.notNull(descriptor);
    Assert.notNull(descriptor.getServices());
    Assert.notNull(descriptor.getService("KNOX"));
  }

  @Test
  public void testCommonOozieServiceDescriptor() throws IOException {
    KerberosDescriptor descriptor;

    descriptor = getKerberosDescriptor(commonServicesDirectory, "OOZIE", "4.0.0.2.0");
    Assert.notNull(descriptor);
    Assert.notNull(descriptor.getServices());
    Assert.notNull(descriptor.getService("OOZIE"));
  }

  @Test
  public void testCommonStormServiceDescriptor() throws IOException {
    KerberosDescriptor descriptor = getKerberosDescriptor(commonServicesDirectory, "STORM", "0.9.1");
    Assert.notNull(descriptor);
    Assert.notNull(descriptor.getServices());
    Assert.notNull(descriptor.getService("STORM"));
  }

  @Test
  public void testCommonZookeepeerServiceDescriptor() throws IOException {
    KerberosDescriptor descriptor = getKerberosDescriptor(commonServicesDirectory, "ZOOKEEPER", "3.4.5");
    Assert.notNull(descriptor);
    Assert.notNull(descriptor.getServices());
    Assert.notNull(descriptor.getService("ZOOKEEPER"));
  }

  @Test
  public void testCommonSparkServiceDescriptor() throws IOException {
    KerberosDescriptor descriptor = getKerberosDescriptor(commonServicesDirectory, "SPARK", "1.2.1");
    Assert.notNull(descriptor);
    Assert.notNull(descriptor.getServices());
    Assert.notNull(descriptor.getService("SPARK"));
  }

  private KerberosDescriptor getKerberosDescriptor(File baseDirectory, String service, String version) throws IOException {
    File serviceDirectory = new File(baseDirectory, service);
    File serviceVersionDirectory = new File(serviceDirectory, version);
    return KERBEROS_DESCRIPTOR_FACTORY.createInstance(new File(serviceVersionDirectory, "kerberos.json"));
  }
}
