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

import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * KerberosDescriptorTest tests the stack- and service-level descriptors for certain stacks
 * and services
 */
public class KerberosDescriptorTest {
  private static File stacksDirectory;
  private static File hdpStackDirectory;
  private static File hdp22StackDirectory;
  private static File hdp22ServicesDirectory;
  private static File commonServicesDirectory;


  @BeforeClass
  public static void beforeClass() {
    URL rootDirectoryURL = KerberosDescriptorTest.class.getResource("/");
    Assert.notNull(rootDirectoryURL);

    stacksDirectory = new File(new File(rootDirectoryURL.getFile()).getParent(), "classes/stacks");
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

    commonServicesDirectory = new File(new File(rootDirectoryURL.getFile()).getParent(), "classes/common-services");
    Assert.notNull(commonServicesDirectory);
    Assert.isTrue(commonServicesDirectory.canRead());

  }

  @Test
  public void testHDP22HBASEServiceDescriptor() throws IOException {
    File hbaseDirectory = new File(hdp22ServicesDirectory, "HBASE");
    KerberosDescriptor descriptor = KerberosDescriptor.fromFile(new File(hbaseDirectory, "kerberos.json"));
    Assert.notNull(descriptor);
    Assert.notNull(descriptor.getServices());
    Assert.notNull(descriptor.getService("HBASE"));
  }

  @Test
  public void testHDP22HDFSServiceDescriptor() throws IOException {
    File hdfsDirectory = new File(hdp22ServicesDirectory, "HDFS");
    KerberosDescriptor descriptor = KerberosDescriptor.fromFile(new File(hdfsDirectory, "kerberos.json"));
    Assert.notNull(descriptor);
    Assert.notNull(descriptor.getServices());
    Assert.notNull(descriptor.getService("HDFS"));
  }

  @Test
  public void testHDP22YarnServiceDescriptor() throws IOException {
    File yarnDirectory = new File(hdp22ServicesDirectory, "YARN");
    KerberosDescriptor descriptor = KerberosDescriptor.fromFile(new File(yarnDirectory, "kerberos.json"));
    Assert.notNull(descriptor);
    Assert.notNull(descriptor.getServices());
    Assert.notNull(descriptor.getService("YARN"));
    Assert.notNull(descriptor.getService("MAPREDUCE2"));
  }
}
