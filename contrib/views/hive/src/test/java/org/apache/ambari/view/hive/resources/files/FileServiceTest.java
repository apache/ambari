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

package org.apache.ambari.view.hive.resources.files;

import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.hive.ServiceTestUtils;
import org.apache.ambari.view.hive.HDFSTest;
import org.apache.ambari.view.hive.utils.*;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.easymock.EasyMock;
import org.json.simple.JSONObject;
import org.junit.*;
import org.junit.rules.ExpectedException;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.expect;

public class FileServiceTest extends HDFSTest {
  private final static int PAGINATOR_PAGE_SIZE = 4;  //4 bytes
  private FileService fileService;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    fileService = getService(FileService.class, handler, context);
    FilePaginator.setPageSize(PAGINATOR_PAGE_SIZE);
  }

  @BeforeClass
  public static void startUp() throws Exception {
    HDFSTest.startUp(); // super
  }

  @AfterClass
  public static void shutDown() throws Exception {
    HDFSTest.shutDown(); // super
  }

  @Override
  @After
  public void tearDown() throws Exception {
    fileService.getSharedObjectsFactory().clear(HdfsApi.class);
  }

  @Test
  public void testCreateFile() throws IOException, InterruptedException {
    Response response = createFile("/tmp/testCreateFile", "testCreateFile content");

    ServiceTestUtils.assertHTTPResponseNoContent(response);
    assertHDFSFileContains("/tmp/testCreateFile", "testCreateFile content");
  }

  @Test
  public void testCreateExistingFileForbidden() throws IOException, InterruptedException {
    createFile("/tmp/testOverwriteFile", "original content");
    thrown.expect(ServiceFormattedException.class);
    createFile("/tmp/testOverwriteFile", "new content");
  }

  @Test
  public void testCreateFilePathNotExists() throws IOException, InterruptedException {
    Response response = createFile("/non/existent/path/Luke", null);
    ServiceTestUtils.assertHTTPResponseNoContent(response);

    Response response2 = createFile("/tmp/Leia", null);
    ServiceTestUtils.assertHTTPResponseNoContent(response2);

    thrown.expect(ServiceFormattedException.class);
    Response response3 = createFile("/tmp/Leia", null); // file already exists
    Assert.assertEquals(400, response3.getStatus());
  }

  @Test
  public void testUpdateFileContent() throws Exception {
    createFile("/tmp/testUpdateFileContent", "some content");

    FileService.FileResourceRequest updateRequest = new FileService.FileResourceRequest();
    updateRequest.file = new FileResource();
    updateRequest.file.setFileContent("new content");

    Response response = fileService.updateFile(updateRequest, "/tmp/testUpdateFileContent");

    ServiceTestUtils.assertHTTPResponseNoContent(response);
    assertHDFSFileContains("/tmp/testUpdateFileContent", "new content");
  }

  @Test
  public void testPagination() throws Exception {
    createFile("/tmp/testPagination", "1234567890");  // 10 bytes, 3 pages if 1 page is 4 bytes

    Response response = fileService.getFilePage("/tmp/testPagination", 0L);
    ServiceTestUtils.assertHTTPResponseOK(response);

    JSONObject obj = ((JSONObject) response.getEntity());
    assertFileJsonResponseSanity(obj);

    FileResource firstPage = (FileResource) obj.get("file");
    Assert.assertEquals("1234", firstPage.getFileContent());
    Assert.assertEquals(3, firstPage.getPageCount());
    Assert.assertEquals(0, firstPage.getPage());
    Assert.assertTrue(firstPage.isHasNext());
    Assert.assertEquals("/tmp/testPagination", firstPage.getFilePath());


    response = fileService.getFilePage("/tmp/testPagination", 1L);
    ServiceTestUtils.assertHTTPResponseOK(response);

    FileResource secondPage = (FileResource) ((JSONObject) response.getEntity()).get("file");
    Assert.assertEquals("5678", secondPage.getFileContent());
    Assert.assertEquals(1, secondPage.getPage());
    Assert.assertTrue(secondPage.isHasNext());


    response = fileService.getFilePage("/tmp/testPagination", 2L);
    ServiceTestUtils.assertHTTPResponseOK(response);

    FileResource thirdPage = (FileResource) ((JSONObject) response.getEntity()).get("file");
    Assert.assertEquals("90", thirdPage.getFileContent());
    Assert.assertEquals(2, thirdPage.getPage());
    Assert.assertFalse(thirdPage.isHasNext());


    thrown.expect(BadRequestFormattedException.class);
    fileService.getFilePage("/tmp/testPagination", 3L);
  }

  @Test
  public void testZeroLengthFile() throws Exception {
    createFile("/tmp/testZeroLengthFile", "");

    Response response = fileService.getFilePage("/tmp/testZeroLengthFile", 0L);

    ServiceTestUtils.assertHTTPResponseOK(response);
    JSONObject obj = ((JSONObject) response.getEntity());
    assertFileJsonResponseSanity(obj);

    FileResource fileResource = (FileResource) obj.get("file");
    Assert.assertEquals("", fileResource.getFileContent());
    Assert.assertEquals(0, fileResource.getPage());
    Assert.assertFalse(fileResource.isHasNext());
  }

  @Test
  public void testFileNotFound() throws IOException, InterruptedException {
    assertHDFSFileNotExists("/tmp/notExistentFile");

    thrown.expect(NotFoundFormattedException.class);
    fileService.getFilePage("/tmp/notExistentFile", 2L);
  }

  @Test
  public void testDeleteFile() throws IOException, InterruptedException {
    createFile("/tmp/testDeleteFile", "some content");

    assertHDFSFileExists("/tmp/testDeleteFile");

    Response response = fileService.deleteFile("/tmp/testDeleteFile");
    ServiceTestUtils.assertHTTPResponseNoContent(response);

    assertHDFSFileNotExists("/tmp/testDeleteFile");
  }

  @Test
  public void testFakeFile() throws IOException, InterruptedException {
    String content = "Fake file content";
    String encodedContent = Base64.encodeBase64String(content.getBytes());
    String filepath = "fakefile://"+encodedContent;
    Response response = fileService.getFilePage(filepath,0l);

    ServiceTestUtils.assertHTTPResponseOK(response);
    JSONObject obj = ((JSONObject) response.getEntity());
    assertFileJsonResponseSanity(obj);

    FileResource fileResource = (FileResource) obj.get("file");
    Assert.assertEquals(content, fileResource.getFileContent());
    Assert.assertEquals(0, fileResource.getPage());
    Assert.assertFalse(fileResource.isHasNext());
  }

  @Test
  public void testJsonFakeFile() throws IOException, InterruptedException,Exception {
    String content = "{\"queryText\":\"Query Content\"}";
    String url = "http://fileurl/content#queryText";
    String filepath = "jsonpath:"+url;

    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);
    InputStream inputStream = IOUtils.toInputStream(content);
    reset(context);
    expect(context.getProperties()).andReturn(properties).anyTimes();
    expect(context.getURLStreamProvider()).andReturn(urlStreamProvider);
    expect(urlStreamProvider.readFrom(eq(url),eq("GET"),anyString(), EasyMock.<Map<String, String>>anyObject())).andReturn(inputStream);

    fileService = getService(FileService.class, handler, context);
    replay(context,urlStreamProvider);

    Response response = fileService.getFilePage(filepath,0l);

    ServiceTestUtils.assertHTTPResponseOK(response);
    JSONObject obj = ((JSONObject) response.getEntity());
    assertFileJsonResponseSanity(obj);

    FileResource fileResource = (FileResource) obj.get("file");
    Assert.assertEquals("Query Content", fileResource.getFileContent());
    Assert.assertEquals(0, fileResource.getPage());
    Assert.assertFalse(fileResource.isHasNext());
  }


  private Response createFile(String filePath, String content) throws IOException, InterruptedException {
    FileService.FileResourceRequest request = new FileService.FileResourceRequest();
    request.file = new FileResource();
    request.file.setFilePath(filePath);
    request.file.setFileContent(content);

    return fileService.createFile(request,
        ServiceTestUtils.getResponseWithLocation(), ServiceTestUtils.getDefaultUriInfo());
  }


  private void assertFileJsonResponseSanity(JSONObject obj) {
    Assert.assertTrue(obj.containsKey("file"));
  }

  private void assertHDFSFileContains(String filePath, String expectedContent) throws IOException {
    FSDataInputStream fileInputStream = hdfsCluster.getFileSystem().open(new Path(filePath));
    byte[] buffer = new byte[256];
    int read = fileInputStream.read(buffer);

    byte[] readData = Arrays.copyOfRange(buffer, 0, read);
    String actualContent = new String(readData, Charset.forName("UTF-8"));

    Assert.assertEquals(expectedContent, actualContent);
  }

  private void assertHDFSFileExists(String filePath) throws IOException {
    Assert.assertTrue( hdfsCluster.getFileSystem().exists(new Path(filePath)) );
  }

  private void assertHDFSFileNotExists(String filePath) throws IOException {
    Assert.assertFalse(hdfsCluster.getFileSystem().exists(new Path(filePath)) );
  }

}
