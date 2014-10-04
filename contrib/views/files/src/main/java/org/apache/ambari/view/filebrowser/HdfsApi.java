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

package org.apache.ambari.view.filebrowser;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import org.apache.hadoop.security.UserGroupInformation;
import org.json.simple.JSONArray;

import java.util.LinkedHashMap;

/**
 * Hdfs Business Delegate
 */
public class HdfsApi {
  private final Configuration conf = new Configuration();

  private FileSystem fs;
  private UserGroupInformation ugi;

  /**
   * Constructor
   * @param defaultFs hdfs uri
   * @param username user.name
   * @throws IOException
   * @throws InterruptedException
   */
  public HdfsApi(String defaultFs, String username) throws IOException,
      InterruptedException {
    conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
    conf.set("fs.webhdfs.impl", "org.apache.hadoop.hdfs.web.WebHdfsFileSystem");
    conf.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");
    fs = FileSystem.get(URI.create(defaultFs), conf, username);
    ugi = UserGroupInformation.createProxyUser(username,
        UserGroupInformation.getLoginUser());
  }

  /**
   * List dir operation
   * @param path path
   * @return array of FileStatus objects
   * @throws FileNotFoundException
   * @throws IOException
   * @throws InterruptedException
   */
  public FileStatus[] listdir(final String path) throws FileNotFoundException,
      IOException, InterruptedException {
    return ugi.doAs(new PrivilegedExceptionAction<FileStatus[]>() {
      public FileStatus[] run() throws FileNotFoundException, Exception {
        return fs.listStatus(new Path(path));
      }
    });
  }

  /**
   * Get file status
   * @param path path
   * @return file status
   * @throws IOException
   * @throws FileNotFoundException
   * @throws InterruptedException
   */
  public FileStatus getFileStatus(final String path) throws IOException,
      FileNotFoundException, InterruptedException {
    return ugi.doAs(new PrivilegedExceptionAction<FileStatus>() {
      public FileStatus run() throws FileNotFoundException, IOException {
        return fs.getFileStatus(new Path(path));
      }
    });
  }

  /**
   * Make directory
   * @param path path
   * @return success
   * @throws IOException
   * @throws InterruptedException
   */
  public boolean mkdir(final String path) throws IOException,
      InterruptedException {
    return ugi.doAs(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws Exception {
        return fs.mkdirs(new Path(path));
      }
    });
  }

  /**
   * Rename
   * @param src source path
   * @param dst destination path
   * @return success
   * @throws IOException
   * @throws InterruptedException
   */
  public boolean rename(final String src, final String dst) throws IOException,
      InterruptedException {
    return ugi.doAs(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws Exception {
        return fs.rename(new Path(src), new Path(dst));
      }
    });
  }

  /**
   * Check is trash enabled
   * @return true if trash is enabled
   * @throws Exception
   */
  public boolean trashEnabled() throws Exception {
    return ugi.doAs(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws IOException {
        Trash tr = new Trash(fs, conf);
        return tr.isEnabled();
      }
    });
  }

  /**
   * Home directory
   * @return home directory
   * @throws Exception
   */
  public Path getHomeDir() throws Exception {
    return ugi.doAs(new PrivilegedExceptionAction<Path>() {
      public Path run() throws IOException {
        return fs.getHomeDirectory();
      }
    });
  }

  /**
   * Trash directory
   * @return trash directory
   * @throws Exception
   */
  public Path getTrashDir() throws Exception {
    return ugi.doAs(new PrivilegedExceptionAction<Path>() {
      public Path run() throws IOException {
        TrashPolicy trashPolicy = TrashPolicy.getInstance(conf, fs,
            fs.getHomeDirectory());
        return trashPolicy.getCurrentTrashDir().getParent();
      }
    });
  }

  /**
   * Empty trash
   * @return
   * @throws Exception
   */
  public Void emptyTrash() throws Exception {
    return ugi.doAs(new PrivilegedExceptionAction<Void>() {
      public Void run() throws IOException {
        Trash tr = new Trash(fs, conf);
        tr.expunge();
        return null;
      }
    });
  }

  /**
   * Move to trash
   * @param path path
   * @return success
   * @throws IOException
   * @throws InterruptedException
   */
  public boolean moveToTrash(final String path) throws IOException,
      InterruptedException {
    return ugi.doAs(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws Exception {
        return Trash.moveToAppropriateTrash(fs, new Path(path), conf);
      }
    });
  }

  /**
   * Delete
   * @param path path
   * @param recursive delete recursive
   * @return success
   * @throws IOException
   * @throws InterruptedException
   */
  public boolean delete(final String path, final boolean recursive)
      throws IOException, InterruptedException {
    return ugi.doAs(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws Exception {
        return fs.delete(new Path(path), recursive);
      }
    });
  }

  /**
   * Create file
   * @param path path
   * @param overwrite overwrite existent file
   * @return output stream
   * @throws IOException
   * @throws InterruptedException
   */
  public FSDataOutputStream create(final String path, final boolean overwrite)
      throws IOException, InterruptedException {
    return ugi.doAs(new PrivilegedExceptionAction<FSDataOutputStream>() {
      public FSDataOutputStream run() throws Exception {
        return fs.create(new Path(path), overwrite);
      }
    });
  }

  /**
   * Open file
   * @param path path
   * @return input stream
   * @throws IOException
   * @throws InterruptedException
   */
  public FSDataInputStream open(final String path) throws IOException,
      InterruptedException {
    return ugi.doAs(new PrivilegedExceptionAction<FSDataInputStream>() {
      public FSDataInputStream run() throws Exception {
        return fs.open(new Path(path));
      }
    });
  }

  /**
   * Change permissions
   * @param path path
   * @param permissions permissions in format rwxrwxrwx
   * @throws IOException
   * @throws InterruptedException
   */
  public boolean chmod(final String path, final String permissions) throws IOException,
      InterruptedException {
    return ugi.doAs(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws Exception {
        try {
          fs.setPermission(new Path(path), new FsPermission(permissions));
        } catch (Exception ex) {
          return false;
        }
        return true;
      }
    });
  }

  /**
   * Copy file
   * @param src source path
   * @param dest destination path
   * @return success
   * @throws IOException
   * @throws InterruptedException
   */
  public boolean copy(final String src, final String dest) throws IOException,
      InterruptedException {
    return ugi.doAs(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws Exception {
        return FileUtil
            .copy(fs, new Path(src), fs, new Path(dest), false, conf);
      }
    });
  }

  /**
   * Converts a Hadoop permission into a Unix permission symbolic representation
   * (i.e. -rwxr--r--) or default if the permission is NULL.
   *
   * @param p
   *          Hadoop permission.
   * @return the Unix permission symbolic representation or default if the
   *         permission is NULL.
   */
  private static String permissionToString(FsPermission p) {
    return (p == null) ? "default" : "-" + p.getUserAction().SYMBOL
        + p.getGroupAction().SYMBOL + p.getOtherAction().SYMBOL;
  }

  /**
   * Converts a Hadoop <code>FileStatus</code> object into a JSON array object.
   * It replaces the <code>SCHEME://HOST:PORT</code> of the path with the
   * specified URL.
   * <p/>
   *
   * @param status
   *          Hadoop file status.
   * @return The JSON representation of the file status.
   */

  public static Map<String, Object> fileStatusToJSON(FileStatus status) {
    Map<String, Object> json = new LinkedHashMap<String, Object>();
    json.put("path", Path.getPathWithoutSchemeAndAuthority(status.getPath())
        .toString());
    json.put("replication", status.getReplication());
    json.put("isDirectory", status.isDirectory());
    json.put("len", status.getLen());
    json.put("owner", status.getOwner());
    json.put("group", status.getGroup());
    json.put("permission", permissionToString(status.getPermission()));
    json.put("accessTime", status.getAccessTime());
    json.put("modificationTime", status.getModificationTime());
    json.put("blockSize", status.getBlockSize());
    json.put("replication", status.getReplication());
    return json;
  }

  /**
   * Converts a Hadoop <code>FileStatus</code> array into a JSON array object.
   * It replaces the <code>SCHEME://HOST:PORT</code> of the path with the
   * specified URL.
   * <p/>
   *
   * @param status
   *          Hadoop file status array.
   * @return The JSON representation of the file status array.
   */
  @SuppressWarnings("unchecked")
  public static JSONArray fileStatusToJSON(FileStatus[] status) {
    JSONArray json = new JSONArray();
    if (status != null) {
      for (FileStatus s : status) {
        json.add(fileStatusToJSON(s));
      }
    }
    return json;
  }

}
