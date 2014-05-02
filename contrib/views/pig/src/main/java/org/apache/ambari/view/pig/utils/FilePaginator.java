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

package org.apache.ambari.view.pig.utils;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.pig.services.BaseService;
import org.apache.hadoop.fs.FSDataInputStream;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static java.lang.Math.ceil;

public class FilePaginator {
    private static int PAGE_SIZE = 1*1024*1024;  // 1MB

    private String filePath;
    private HdfsApi hdfsApi;

    public FilePaginator(String filePath, ViewContext context) {
        this.filePath = filePath;
        hdfsApi = BaseService.getHdfsApi(context);
    }

    public static void setPageSize(int PAGE_SIZE) {
        FilePaginator.PAGE_SIZE = PAGE_SIZE;
    }

    public long pageCount() throws IOException, InterruptedException {
        return (long)
                ceil( hdfsApi.getFileStatus(filePath).getLen() / ((double)PAGE_SIZE) );
    }

    public String readPage(long page) throws IOException, InterruptedException {
        FSDataInputStream stream = hdfsApi.open(filePath);
        try {
            stream.seek(page * PAGE_SIZE);
        } catch (IOException e) {
            throw new IllegalArgumentException("Page " + page + " does not exists");
        }

        byte[] buffer = new byte[PAGE_SIZE];
        int readCount = 0;
        int read = 0;
        while(read < PAGE_SIZE) {
            try {
                readCount = stream.read(buffer, read, PAGE_SIZE-read);
            } catch (IOException e) {
                stream.close();
                throw e;
            }
            if (readCount == -1)
                break;
            read += readCount;
        }
        if (read != 0) {
            byte[] readData = Arrays.copyOfRange(buffer, 0, read);
            return new String(readData, Charset.forName("UTF-8"));
        } else {
            if (page == 0) {
                return "";
            }
            throw new IllegalArgumentException("Page " + page + " does not exists");
        }
    }
}
