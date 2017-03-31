/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.resources.settings;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive20.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.hive20.persistence.utils.StorageFactory;
import org.apache.ambari.view.hive20.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.hive20.utils.UniqueConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

/**
 *
 */
public class SettingsResourceManager extends PersonalCRUDResourceManager<Setting> {

  protected final Logger LOG = LoggerFactory.getLogger(getClass());

  private final ViewContext context;

  @Inject
  public SettingsResourceManager(ViewContext context) {
    super(Setting.class, new StorageFactory(context), context);
    this.context = context;
  }

  public List<Setting> getSettings() {
    String username = context.getUsername();
    return super.readAll(new OnlyOwnersFilteringStrategy(username));
  }

  public void removeSetting(String id) throws ItemNotFound {
    super.delete(id);
  }

  public Setting create(Setting setting) {
    List<Setting> settings = getSettings();
    if (checkUniqueViolation(settings, setting)) {
      LOG.error("Setting key: {} already exist for the user.", setting.getKey());
      throw new UniqueConstraintViolationException("Setting key: " + setting.getKey() + " already exists for the user");
    }
    return super.create(setting);
  }

  public Setting update(String id, Setting setting) throws ItemNotFound {
    Setting current = read(id);
    if(!current.getKey().equalsIgnoreCase(setting.getKey())) {
      // If the settings key has changed
      List<Setting> settings = getSettings();
      if (checkUniqueViolation(settings, setting)) {
        LOG.error("Setting key: {} already exist for the user.", setting.getKey());
        throw new UniqueConstraintViolationException("Setting key: " + setting.getKey() + " already exists for the user");
      }
    }
    return super.update(setting, id);
  }

  /**
   * @param settings List of all settings for the user
   * @param setting  new settings to be created
   * @return true if the settings key is not unique
   */
  private boolean checkUniqueViolation(List<Setting> settings, final Setting setting) {
    Optional<Setting> settingOptional = FluentIterable.from(settings).filter(new Predicate<Setting>() {
      @Override
      public boolean apply(@Nullable Setting input) {
        return input.getKey().equalsIgnoreCase(setting.getKey());
      }
    }).first();
    return settingOptional.isPresent();
  }
}
