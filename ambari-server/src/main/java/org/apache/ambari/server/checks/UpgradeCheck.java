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
package org.apache.ambari.server.checks;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.ScopeAnnotation;
import com.google.inject.Singleton;

/**
 * The {@link UpgradeCheck} annotation is used to provide ordering and grouping
 * to any {@link AbstractCheckDescriptor} instance.
 * <p/>
 * Classes marked with this annotation should also be {@link Singleton}. They
 * will be discovered on the classpath and then registered with the
 * {@link UpgradeCheckRegistry}.
 */
@Target({ ElementType.TYPE })
@Retention(RUNTIME)
@ScopeAnnotation
public @interface UpgradeCheck {

  /**
   * The group that the pre-upgrade check belongs to.
   *
   * @return the group, or {@link UpgradeCheckGroup#DEFAULT} if not specified.
   */
  UpgradeCheckGroup group() default UpgradeCheckGroup.DEFAULT;

  /**
   * The order of the pre-upgrade check within its group.
   * <p/>
   * The order is determined by a {@code float} so that new checks can be added
   * in between others without the need to reorder all of the existing checks.
   *
   * @return the order, or {@code 1.0f} if not specified.
   */
  float order() default 1.0f;

  /**
   * Gets whether the pre-upgrade check is required.
   * By default, a pre-upgrade check needs to be declared in the upgrade pack. This flag will override that setting.
   *
   * @return  flag state, or {@code true} if not specified
   */
  boolean required() default false;
}
