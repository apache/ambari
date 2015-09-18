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

package org.apache.ambari.server.security.encryption;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.ambari.server.AmbariException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * InMemoryCredentialStoreService is a CredentialStoreService implementation that creates and manages
 * a JCEKS (Java Cryptography Extension KeyStore) in memory.  The key store and its contents are
 * encrypted using the key from the supplied {@link MasterKeyService}.
 * <p/>
 * This class handles the details of the in-memory storage buffer and associated input and output
 * streams. Each credential is stored in its own KeyStore that may be be purged upon some
 * retention timeout - if specified.
 */
public class InMemoryCredentialStoreService extends CredentialStoreServiceImpl {
  private static final Logger LOG = LoggerFactory.getLogger(InMemoryCredentialStoreService.class);

  /**
   * A cache containing the KeyStore data
   */
  private final Cache<String, KeyStore> cache;

  /**
   * Constructs a new InMemoryCredentialStoreService where credentials have no retention timeout
   */
  public InMemoryCredentialStoreService() {
    this(0, TimeUnit.MINUTES, false);
  }

  /**
   * Constructs a new InMemoryCredentialStoreService with a specified credential timeout
   *
   * @param retentionDuration the time in some units to keep stored credentials, from the time they are added
   * @param units             the units for the retention duration (minutes, seconds, etc...)
   * @param activelyPurge     true to actively purge credentials after the retention time has expired;
   *                          otherwise false, to passively purge credentials after the retention time has expired
   */
  public InMemoryCredentialStoreService(final long retentionDuration, final TimeUnit units, boolean activelyPurge) {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();

    // If the retentionDuration is less the 1, then no retention policy is to be enforced
    if (retentionDuration > 0) {
      // If actively purging expired credentials, set up a timer to periodically clean the cache
      if (activelyPurge) {
        ThreadFactory threadFactory = new ThreadFactory() {
          @Override
          public Thread newThread(Runnable runnable) {
            Thread t = Executors.defaultThreadFactory().newThread(runnable);
            if (t != null) {
              t.setName(String.format("%s active cleanup timer", InMemoryCredentialStoreService.class.getSimpleName()));
              t.setDaemon(true);
            }
            return t;
          }
        };
        Runnable runnable = new Runnable() {
          @Override
          public void run() {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Cleaning up cache due to retention timeout of {} milliseconds",
                  units.toMillis(retentionDuration));
            }
            cache.cleanUp();
          }
        };

        Executors.newSingleThreadScheduledExecutor(threadFactory).schedule(runnable, 1, TimeUnit.MINUTES);
      }

      builder.expireAfterWrite(retentionDuration, units);
    }

    cache = builder.build();
  }

  @Override
  public void addCredential(String alias, char[] value) throws AmbariException {
    if ((alias == null) || alias.isEmpty()) {
      throw new IllegalArgumentException("Alias cannot be null or empty.");
    }

    KeyStore keyStore = loadKeyStore(null, DEFAULT_STORE_TYPE);
    addCredential(keyStore, alias, value);
    cache.put(alias, keyStore);
  }

  @Override
  public char[] getCredential(String alias) throws AmbariException {
    char[] credential = null;

    if ((alias != null) && !alias.isEmpty()) {
      KeyStore keyStore = cache.getIfPresent(alias);
      if (keyStore != null) {
        credential = getCredential(keyStore, alias);
      }
    }

    return credential;
  }

  @Override
  public void removeCredential(String alias) throws AmbariException {
    if (alias != null) {
      cache.invalidate(alias);
    }
  }

  @Override
  protected void persistCredentialStore(KeyStore keyStore) throws AmbariException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected KeyStore loadCredentialStore() throws AmbariException {
    throw new UnsupportedOperationException();
  }
}
