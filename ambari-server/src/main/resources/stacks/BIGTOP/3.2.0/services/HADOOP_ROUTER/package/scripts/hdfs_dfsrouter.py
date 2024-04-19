from resource_management import check_process_status
from resource_management.core.resources.system import Directory, Execute, File
from ambari_commons.os_family_impl import OsFamilyImpl, OsFamilyFuncImpl
from resource_management.libraries import XmlConfig
from resource_management.core.shell import as_sudo
from resource_management.libraries.functions import format
from resource_management.core.shell import as_user
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.core import shell
import subprocess

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def dfsrouter(action=None):
    if action == "configure":
        import params
        XmlConfig("hdfs-rbf-site.xml",
                  conf_dir = params.hadoop_conf_dir,
                  configurations=params.config['configurations']['hdfs-rbf-site'],
                  configuration_attributes = params.config['configurationAttributes']['hdfs-rbf-site'],
                  owner = params.hdfs_user,
                  group = params.user_group,
                  mode = 0o644)

        if params.mount_table_content:
            File(params.mount_table_xml_inclusion_file_full_path,
                 owner=params.hdfs_user,
                 group=params.user_group,
                 content=params.mount_table_content,
                 mode=0o644
                 )
    elif action == "start" or action == "stop":
        import params
        service(
            action=action, name="dfsrouter",
            user=params.hdfs_user,
            create_pid_dir=True,
            create_log_dir=True
        )
    elif action == "status":
        import status_params
        check_process_status(status_params.dfsrouter_pid_file)



def service(action=None, name=None, user=None, options="", create_pid_dir=False,
            create_log_dir=False):
    """
    :param action: Either "start" or "stop"
    :param name: Component name, e.g., "namenode", "datanode", "secondarynamenode", "zkfc"
    :param user: User to run the command as
    :param options: Additional options to pass to command as a string
    :param create_pid_dir: Create PID directory
    :param create_log_dir: Crate log file directory
    """
    import params
    import status_params
    ulimit_cmd = "ulimit -c unlimited ; "
    options = options if options else ""
    pid_dir = status_params.hadoop_pid_dir
    pid_file = status_params.dfsrouter_pid_file
    hadoop_env_exports = {
        'HADOOP_LIBEXEC_DIR': params.hadoop_libexec_dir
    }
    log_dir = f"{params.hdfs_log_dir_prefix}/{user}"
    process_id_exists_command = as_sudo(["test", "-f", pid_file]) + " && " + as_sudo(["pgrep", "-F", pid_file])

    # on STOP directories shouldn't be created
    # since during stop still old dirs are used (which were created during previous start)
    if action != "stop":
        Directory(params.hadoop_pid_dir_prefix,
                  mode=0o755,
                  owner=params.hdfs_user,
                  group=params.user_group
                  )
        if create_pid_dir:
            Directory(pid_dir,
                      owner=user,
                      group=params.user_group,
                      create_parents = True)
        if create_log_dir:
            Directory(log_dir,
                      owner=user,
                      group=params.user_group,
                      create_parents = True)


    hadoop_daemon = f"{params.hadoop_bin}/hadoop-daemon.sh"

    if user == "root":
        cmd = [hadoop_daemon, "--config", params.hadoop_conf_dir, action, name]
        if options:
            cmd += [options, ]
        daemon_cmd = as_sudo(cmd)
    else:
        cmd = f"{ulimit_cmd} {hadoop_daemon} --config {params.hadoop_conf_dir} {action} {name}"
        if options:
            cmd += " " + options
        daemon_cmd = as_user(cmd, user)

    if action == "start":

        # remove pid file from dead process
        File(pid_file, action="delete", not_if=process_id_exists_command)

        try:
            Execute(daemon_cmd, not_if=process_id_exists_command, environment=hadoop_env_exports)
        except:
            show_logs(log_dir, user)
            raise
    elif action == "stop":
        try:
            Execute(daemon_cmd, only_if=process_id_exists_command, environment=hadoop_env_exports)
        except:
            show_logs(log_dir, user)
            raise

        # Wait until stop actually happens
        process_id_does_not_exist_command = format("! ( {process_id_exists_command} )")
        code, out = shell.call(process_id_does_not_exist_command,
                               env=hadoop_env_exports,
                               tries = 6,
                               try_sleep = 10,
                               )

        # If stop didn't happen, kill it forcefully
        if code != 0:
            code, out, err = shell.checked_call(("cat", pid_file), sudo=True, env=hadoop_env_exports, stderr=subprocess.PIPE)
            pid = out
            Execute(("kill", "-9", pid), sudo=True)

        File(pid_file, action="delete")