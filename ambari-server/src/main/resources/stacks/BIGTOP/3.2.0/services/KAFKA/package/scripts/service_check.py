#!/usr/bin/env python3
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

from contextlib import nullcontext
import uuid

from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script.script import Script


class ServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    topic = (
      f"ambari_kafka_service_check_{uuid.uuid4().hex}"
      if params.kafka_delete_topic_enable
      else "ambari_kafka_service_check"
    )
    cache_context = nullcontext(None)
    if params.kafka_service_check_uses_kerberos:
      if not params.kerberos_security_enabled:
        raise Fail("Kafka uses GSSAPI but cluster Kerberos is not enabled")
      if not params.kafka_keytab_path or not params.kafka_jaas_principal:
        raise Fail("Kafka service-check Kerberos credentials are not configured")
      cache_context = PrivateKerberosCache(
        params.kafka_user,
        params.user_group,
        prefix="ambari-kafka-service-check-",
      )

    with cache_context as kerberos_cache:
      command_environment = {
        "JAVA_HOME": params.java64_home,
        "LOG_DIR": params.kafka_log_dir,
      }
      if params.kafka_service_check_uses_sasl:
        command_environment["KAFKA_OPTS"] = (
          "-Djava.security.auth.login.config=" + params.kafka_client_jaas_file
        )

      if kerberos_cache is not None:
        command_environment = kerberos_cache.merge_environment(
          command_environment
        )
        kerberos_cache.kinit(
          params.kinit_path_local,
          params.kafka_keytab_path,
          params.kafka_jaas_principal,
          timeout=params.kafka_service_check_timeout,
        )

      base_command = (
        params.kafka_topics,
        "--bootstrap-server",
        params.kafka_bootstrap_servers,
        "--command-config",
        params.kafka_client_properties,
      )
      self._check_topic(params, base_command, command_environment, topic)

  @staticmethod
  def _check_topic(params, base_command, command_environment, topic):
    check_failed = False
    topic_created = False
    try:
      shell.checked_call(
        base_command
        + (
          "--create",
          "--if-not-exists",
          "--topic",
          topic,
          "--partitions",
          "1",
          "--replication-factor",
          "1",
        ),
        user=params.kafka_user,
        env=command_environment,
        timeout=params.kafka_service_check_timeout,
      )
      topic_created = True

      _, description = shell.checked_call(
        base_command + ("--describe", "--topic", topic),
        user=params.kafka_user,
        env=command_environment,
        timeout=params.kafka_service_check_timeout,
      )
      if f"Topic: {topic}" not in description:
        raise Fail(f"Kafka did not describe service-check topic {topic}")

      _, under_replicated = shell.checked_call(
        base_command
        + ("--describe", "--topic", topic, "--under-replicated-partitions"),
        user=params.kafka_user,
        env=command_environment,
        timeout=params.kafka_service_check_timeout,
      )
      if any(
        line.lstrip().startswith("Topic:") for line in under_replicated.splitlines()
      ):
        raise Fail(f"Kafka topic {topic} has under-replicated partitions")
    except Exception:
      check_failed = True
      raise
    finally:
      if topic_created and params.kafka_delete_topic_enable:
        try:
          shell.checked_call(
            base_command + ("--delete", "--topic", topic),
            user=params.kafka_user,
            env=command_environment,
            timeout=params.kafka_service_check_timeout,
          )
        except Exception as error:
          if check_failed:
            Logger.error(
              f"Could not clean up Kafka service-check topic {topic}: {error}"
            )
          else:
            raise


if __name__ == "__main__":
  ServiceCheck().execute()
