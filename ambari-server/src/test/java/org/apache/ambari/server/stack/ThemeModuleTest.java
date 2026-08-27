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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;

import org.apache.ambari.server.state.ThemeInfo;
import org.apache.ambari.server.state.theme.Theme;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableSet;

public class ThemeModuleTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testResolve() throws Exception {
    File parentThemeFile = new File(this.getClass().getClassLoader().getResource("parent-theme.json").getFile());
    File childThemeFile = new File(this.getClass().getClassLoader().getResource("child-theme.json").getFile());

    ThemeModule parentModule = new ThemeModule(parentThemeFile);
    ThemeModule childModule = new ThemeModule(childThemeFile);

    childModule.resolve(parentModule, null, null, null);

    Theme childTheme = childModule.getModuleInfo().getThemeMap().get(ThemeModule.THEME_KEY);
    Theme parentTheme = parentModule.getModuleInfo().getThemeMap().get(ThemeModule.THEME_KEY);

    assertNotNull(childTheme.getThemeConfiguration().getLayouts()); // not defined in child should be inherited

    assertEquals(10, parentTheme.getThemeConfiguration().getPlacement().getConfigs().size());
    assertEquals(12, childTheme.getThemeConfiguration().getPlacement().getConfigs().size()); //two more inherited

    assertEquals(10, parentTheme.getThemeConfiguration().getWidgets().size());
    assertEquals(12, childTheme.getThemeConfiguration().getWidgets().size());
  }

  @Test
  public void testAddErrors() {
    Set<String> errors = ImmutableSet.of("one error", "two errors");
    ThemeModule module = new ThemeModule((File) null);
    module.addErrors(errors);
    assertEquals(errors, ImmutableSet.copyOf(module.getErrors()));
  }

  @Test
  public void testResolveCopiesInheritedObjects() throws Exception {
    File parentThemeFile = new File(this.getClass().getClassLoader().getResource("parent-theme.json").getFile());
    File childThemeFile = writeTheme("inheriting-theme.json", "{\"name\":\"child\"}");
    ThemeModule parentModule = new ThemeModule(parentThemeFile);
    ThemeModule childModule = new ThemeModule(childThemeFile);

    childModule.resolve(parentModule, null, null, null);

    Theme parentTheme = parentModule.getModuleInfo().getThemeMap().get(ThemeModule.THEME_KEY);
    Theme childTheme = childModule.getModuleInfo().getThemeMap().get(ThemeModule.THEME_KEY);
    assertNotSame(parentTheme.getThemeConfiguration(), childTheme.getThemeConfiguration());
    assertNotSame(parentTheme.getThemeConfiguration().getLayouts(), childTheme.getThemeConfiguration().getLayouts());
    assertNotSame(parentTheme.getThemeConfiguration().getLayouts().get(0),
      childTheme.getThemeConfiguration().getLayouts().get(0));

    childTheme.getThemeConfiguration().getLayouts().get(0).setName("child-layout");
    assertEquals("default", parentTheme.getThemeConfiguration().getLayouts().get(0).getName());
  }

  @Test
  public void testMissingAndInvalidThemeFilesAreRejected() throws Exception {
    ThemeModule missing = new ThemeModule(new File(temporaryFolder.getRoot(), "missing-theme.json"));
    ThemeModule malformed = new ThemeModule(writeTheme("malformed-theme.json", "{not-json"));
    ThemeModule bindingFailure = new ThemeModule(writeTheme("binding-theme.json",
      "{\"configuration\":{\"layouts\":{}}}"));

    assertFalse(missing.isValid());
    assertFalse(missing.getErrors().isEmpty());
    assertFalse(malformed.isValid());
    assertFalse(malformed.getErrors().isEmpty());
    assertFalse(bindingFailure.isValid());
    assertFalse(bindingFailure.getErrors().isEmpty());
  }

  @Test
  public void testDeletedDescriptorSkipsLoadingAndInheritance() throws Exception {
    ThemeInfo deletedInfo = new ThemeInfo();
    deletedInfo.setFileName("deleted-theme.json");
    deletedInfo.setDeleted(true);
    ThemeModule deletedModule = new ThemeModule(
      new File(temporaryFolder.getRoot(), "deleted-theme.json"), deletedInfo);
    File parentThemeFile = new File(this.getClass().getClassLoader().getResource("parent-theme.json").getFile());
    ThemeModule parentModule = new ThemeModule(parentThemeFile);

    deletedModule.resolve(parentModule, null, null, null);

    assertTrue(deletedModule.isValid());
    assertTrue(deletedModule.isDeleted());
    assertNull(deletedModule.getModuleInfo().getThemeMap());
  }

  private File writeTheme(String name, String contents) throws Exception {
    File file = temporaryFolder.newFile(name);
    Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    return file;
  }
}
