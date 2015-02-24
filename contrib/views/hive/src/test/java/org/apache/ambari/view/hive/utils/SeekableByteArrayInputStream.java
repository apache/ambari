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

package org.apache.ambari.view.hive.utils;

import org.apache.hadoop.fs.PositionedReadable;
import org.apache.hadoop.fs.Seekable;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
* Created by roma on 2/6/15.
*/
public class SeekableByteArrayInputStream extends ByteArrayInputStream
    implements Seekable, PositionedReadable {
  public SeekableByteArrayInputStream(byte[] buf) {
    super(buf);
  }

  public SeekableByteArrayInputStream(byte buf[], int offset, int length) {
    super(buf, offset, length);
    throw new UnsupportedOperationException("Seek code assumes offset is zero");
  }

  public void seek(long position) {
    if (position < 0 || position >= buf.length)
      throw new IllegalArgumentException("pos = " + position + " buf.lenght = " + buf.length);
    this.pos = (int) position;
  }

  public long getPos() {
    return this.pos;
  }

  @Override
  public boolean seekToNewSource(long l) throws IOException {
    throw new UnsupportedOperationException("seekToNewSource is not supported");
  }

  @Override
  public int read(long l, byte[] buffer, int offset, int length) throws IOException {
    this.seek(l);
    return this.read(buffer, offset, length);
  }

  @Override
  public void readFully(long l, byte[] bytes, int i, int i1) throws IOException {
    throw new UnsupportedOperationException("readFully is not supported");
  }

  @Override
  public void readFully(long l, byte[] bytes) throws IOException {
    throw new UnsupportedOperationException("readFully is not supported");
  }
}
