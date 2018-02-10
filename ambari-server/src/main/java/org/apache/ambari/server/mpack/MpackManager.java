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
package org.apache.ambari.server.mpack;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.ambari.server.controller.MpackRequest;
import org.apache.ambari.server.controller.MpackResponse;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.orm.dao.MpackDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Module;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.OsSpecific;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.StackMetainfoXml;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Manages all mpack related behavior including parsing of stacks and providing access to
 * mpack information.
 */
public class MpackManager {
  private static final String MPACK_METADATA = "mpack.json";
  private static final String METAINFO_FILE_NAME = "metainfo.xml";
  private static final String MPACK_TAR_LOCATION = "staging";
  private static final String MODULES_DIRECTORY = "services";
  private static final String MIN_JDK_PROPERTY = "min-jdk";
  private static final String MAX_JDK_PROPERTY = "max-jdk";
  private static final String DEFAULT_JDK_VALUE = "1.8";
  private final static Logger LOG = LoggerFactory.getLogger(MpackManager.class);
  protected Map<Long, Mpack> mpackMap = new ConcurrentHashMap<>();
  private File mpacksStaging;
  private MpackDAO mpackDAO;
  private StackDAO stackDAO;
  private File stackRoot;

  @AssistedInject
  public MpackManager(
    @Assisted("mpacksv2Staging") File mpacksStagingLocation,
    @Assisted("stackRoot") File stackRootDir,
    MpackDAO mpackDAOObj,
    StackDAO stackDAOObj) {
    mpacksStaging = mpacksStagingLocation;
    mpackDAO = mpackDAOObj;
    stackRoot = stackRootDir;
    stackDAO = stackDAOObj;

    parseMpackDirectories();

  }

  /**
   * Parses mpackdirectories during boostrap/ambari-server restart
   * Reads from /var/lib/ambari-server/mpacks-v2/
   *
   * @throws IOException
   */
  private void parseMpackDirectories() {

    try {
      for (final File dirEntry : mpacksStaging.listFiles()) {
        if (dirEntry.isDirectory()) {
          String mpackName = dirEntry.getName();
          LOG.info("Reading mpack :" + mpackName);
          if (!mpackName.equals(MPACK_TAR_LOCATION)) {
            for (final File file : dirEntry.listFiles()) {
              if (file.isDirectory()) {
                String mpackVersion = file.getName();
                List resultSet = mpackDAO.findByNameVersion(mpackName, mpackVersion);
                if (resultSet.size() > 0) {
                  MpackEntity mpackEntity = (MpackEntity) resultSet.get(0);

                  // Read the mpack.json file into Mpack Object for further use.
                  String mpackJsonContents = new String((Files.readAllBytes(Paths.get(file + "/" + MPACK_METADATA))),
                    "UTF-8");
                  Gson gson = new Gson();
                  Mpack existingMpack = gson.fromJson(mpackJsonContents, Mpack.class);
                  existingMpack.setMpackId(mpackEntity.getMpackId());
                  existingMpack.setMpackUri(mpackEntity.getMpackUri());
                  mpackMap.put(mpackEntity.getMpackId(), existingMpack);
                }
              }
            }
          }
        }
      }
    } catch (NullPointerException|IOException e) {
      e.printStackTrace();
    }
  }


  public Map<Long, Mpack> getMpackMap() {
    return mpackMap;
  }

  public void setMpackMap(Map<Long, Mpack> mpackMap) {
    this.mpackMap = mpackMap;
  }


  /**
   * Parses mpack.json to fetch mpack and associated packlet information and
   * stores the mpack to the database and mpackMap
   *
   * @param mpackRequest
   * @return MpackResponse
   * @throws IOException
   * @throws IllegalArgumentException
   * @throws ResourceAlreadyExistsException
   */
  public MpackResponse registerMpack(MpackRequest mpackRequest)
    throws IOException, IllegalArgumentException, ResourceAlreadyExistsException {

    Long mpackId;
    String mpackName = "";
    String mpackVersion = "";
    Mpack mpack = new Mpack();
    boolean isValidMetadata;
    String mpackDirectory = "";
    Path mpackTarPath;

    //Mpack registration using a software registry
    if (mpackRequest.getRegistryId() != null) {
      mpackName = mpackRequest.getMpackName();
      mpackVersion = mpackRequest.getMpackVersion();
      mpack.setRegistryId(mpackRequest.getRegistryId());

      LOG.info("Mpack Registration via Registry :" + mpackName);

      mpack = downloadMpackMetadata(mpackRequest.getMpackUri());
      isValidMetadata = validateMpackInfo(mpackName, mpackVersion, mpack.getName(), mpack.getVersion());

      if (isValidMetadata) {
        mpackTarPath = downloadMpack(mpackRequest.getMpackUri(), mpack.getDefinition());
        createMpackDirectory(mpack);
        mpackDirectory = mpacksStaging + File.separator + mpack.getName() + File.separator + mpack.getVersion();
      }
      else {
          String message =
            "Incorrect information : Mismatch in - (" + mpackName + "," + mpack.getName() + ") or (" + mpackVersion
              + "," + mpack.getVersion() + ")";
          throw new IllegalArgumentException(message); //Mismatch in information
        }
      }
    //Mpack registration using direct download
    else {
      mpack = downloadMpackMetadata(mpackRequest.getMpackUri());
      mpackTarPath = downloadMpack(mpackRequest.getMpackUri(), mpack.getDefinition());

      LOG.info("Custom Mpack Registration :" + mpackRequest.getMpackUri());

      if (createMpackDirectory(mpack)) {
        mpackDirectory = mpacksStaging + File.separator + mpack.getName() + File.separator + mpack.getVersion();
      }
    }
    extractMpackTar(mpack, mpackTarPath, mpackDirectory);
    mpack.setMpackUri(mpackRequest.getMpackUri());
    mpackId = populateDB(mpack);

    if (mpackId != null) {
      mpackMap.put(mpackId, mpack);
      mpack.setMpackId(mpackId);
      populateStackDB(mpack);
      return new MpackResponse(mpack);
    } else {
      String message = "Mpack :" + mpackRequest.getMpackName() + " version: " + mpackRequest.getMpackVersion()
        + " already exists in server";
      throw new ResourceAlreadyExistsException(message);
    }
  }

  /***
   * Download the mpack.json as a primary step towards providing
   * meta information about mpack and associated services
   * @param mpackURI
   * @return
   * @throws IOException
   */
  private Mpack downloadMpackMetadata(String mpackURI) throws IOException  {
    URL url = new URL(mpackURI);
    File stagingDir = new File(mpacksStaging.toString() + File.separator + MPACK_TAR_LOCATION);
    Path targetPath = new File(stagingDir.getPath() + File.separator + MPACK_METADATA).toPath();

    LOG.debug("Download mpack.json and store in :" + targetPath);

    if (!stagingDir.exists()) {
      stagingDir.mkdir();
    }

    Files.copy(url.openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    //Read the mpack.json file into Mpack Object for further use.
    Gson gson = new Gson();
    Mpack mpack = gson.fromJson(new FileReader(targetPath.toString()), Mpack.class);
    return mpack;
  }

  /***
   * A generic method to extract tar files.
   *
   * @param tarPath
   * @throws IOException
   */
  private void extractTar(Path tarPath, File untarDirectory) throws IOException {
    TarArchiveInputStream tarFile = new TarArchiveInputStream(
      new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(new File(String.valueOf(tarPath))))));
    TarArchiveEntry entry = null;
    File outputFile = null;

    LOG.debug("Extracting tar file :" + tarFile);

    //Create a loop to read every single entry in TAR file
    while ((entry = tarFile.getNextTarEntry()) != null) {
      outputFile = new File(untarDirectory, entry.getName());
      if (entry.isDirectory()) {
        if (!outputFile.exists()) {
          LOG.debug("Creating output directory " + outputFile.getAbsolutePath());
          if (!outputFile.mkdirs()) {
            throw new IllegalStateException("Couldn't create directory " + outputFile.getAbsolutePath());
          }
        }
      } else {
        File parentDir = outputFile.getParentFile();
        if (!parentDir.exists()) {
          LOG.debug("Attempting to create output directory " + parentDir.getAbsolutePath());
          if (!parentDir.mkdirs()) {
            throw new IllegalStateException("Couldn't create directory " + parentDir.getAbsolutePath());
          }
        }
        LOG.debug("Creating output file " + outputFile.getAbsolutePath());
        final OutputStream outputFileStream = new FileOutputStream(outputFile);
        IOUtils.copy(tarFile, outputFileStream);
        outputFileStream.close();
      }
    }
    tarFile.close();
  }

  /**
   * Mpack is downloaded as a tar.gz file.
   * It is extracted into /var/lib/ambari-server/resources/mpack-v2/{mpack-name}/{mpack-version} directory
   *
   * @param mpack          Mpack to process
   * @param mpackTarPath   Path to mpack tarball
   * @param mpackDirectory Mpack directory
   * @throws IOException
   */
  private void extractMpackTar(Mpack mpack, Path mpackTarPath, String mpackDirectory) throws IOException {

    extractTar(mpackTarPath, mpacksStaging);

    String mpackTarDirectory = mpackTarPath.toString();
    Path extractedMpackDirectory = Files.move
      (Paths.get(mpacksStaging + File.separator + mpackTarDirectory
          .substring(mpackTarDirectory.lastIndexOf('/') + 1, mpackTarDirectory.indexOf(".tar")) + File.separator),
        Paths.get(mpackDirectory), StandardCopyOption.REPLACE_EXISTING);

    LOG.debug("Extracting Mpack definitions into :" + extractedMpackDirectory);

    createServicesDirectory(extractedMpackDirectory, mpack);

    File metainfoFile = new File(extractedMpackDirectory + File.separator + METAINFO_FILE_NAME);
    // if metainfo.xml doesn't exist in mpack generate it
    if (!metainfoFile.exists()) {
      generateMetainfo(metainfoFile, mpack);
    }

    createSymLinks(mpack);
  }

  /**
   * Generate metainfo.xml based on prerequisites of mpack and write it to the mpack directory.
   * If required properties are missing in prerequisites fill them with default values.
   *
   * @param metainfoFile
   * @param mpack
   * @throws IOException
   */
  private void generateMetainfo(File metainfoFile, Mpack mpack) throws IOException {
    LOG.info("Generating {} for mpack {}", metainfoFile, mpack.getName());
    StackMetainfoXml generatedMetainfo = new StackMetainfoXml();
    StackMetainfoXml.Version version = new StackMetainfoXml.Version();
    version.setActive(true);
    generatedMetainfo.setVersion(version);

    //Add osSpecifics to the metainfo.xml
    OsSpecific osSpecific = new OsSpecific("any");
    OsSpecific.Package pkg = new OsSpecific.Package();
    pkg.setName(mpack.getName().toLowerCase());
    ArrayList<OsSpecific.Package> packageArrayList = new ArrayList<>();
    packageArrayList.add(pkg);
    ArrayList<OsSpecific> osSpecificArrayList = new ArrayList<>();
    osSpecificArrayList.add(osSpecific);
    osSpecific.addPackages(packageArrayList);
    generatedMetainfo.setOsSpecifics(osSpecificArrayList);

    Map<String, String> prerequisites = mpack.getPrerequisites();
    if (prerequisites != null && prerequisites.containsKey(MIN_JDK_PROPERTY)) {
      generatedMetainfo.setMinJdk(mpack.getPrerequisites().get(MIN_JDK_PROPERTY));
    } else {
      //this should not happen if mpack was configured correctly
      LOG.warn("Couldn't detect {} for mpack {}. Using default value {}", MIN_JDK_PROPERTY, mpack.getName(), DEFAULT_JDK_VALUE);
      generatedMetainfo.setMinJdk(DEFAULT_JDK_VALUE);
    }
    if (prerequisites != null && prerequisites.containsKey(MAX_JDK_PROPERTY)) {
      generatedMetainfo.setMaxJdk(mpack.getPrerequisites().get(MAX_JDK_PROPERTY));
    } else {
      //this should not happen if mpack was configured correctly
      LOG.warn("Couldn't detect {} for mpack {}. Using default value {}", MAX_JDK_PROPERTY, mpack.getName(), DEFAULT_JDK_VALUE);
      generatedMetainfo.setMaxJdk(DEFAULT_JDK_VALUE);
    }

    // Export generatedMetainfo to metainfo.xml
    try {
      JAXBContext ctx = JAXBContext.newInstance(StackMetainfoXml.class);
      Marshaller marshaller = ctx.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      FileOutputStream stackMetainfoFileStream = new FileOutputStream(metainfoFile);
      marshaller.marshal(generatedMetainfo, stackMetainfoFileStream);
      stackMetainfoFileStream.flush();
      stackMetainfoFileStream.close();
    } catch (JAXBException e) {
      e.printStackTrace();
    }
  }

  /***
   * Create a services directory and extract all the services tar file inside it. This readies it for cluster deployment
   *
   * @param extractedMpackDirectory
   * @param mpack
   * @throws IOException
   */
  private void createServicesDirectory(Path extractedMpackDirectory, Mpack mpack) throws IOException {
    File servicesDir = new File(extractedMpackDirectory.toAbsolutePath() + File.separator + MODULES_DIRECTORY);
    if (!servicesDir.exists()) {
      servicesDir.mkdir();
    }
    List<Module> modules = mpack.getModules();

    LOG.info("Creating services directory for mpack :" + mpack.getName());

    for (Module module : modules) {
      //if (module.getType() == Packlet.PackletType.SERVICE_PACKLET) { //Add back if there is going to be a view packlet
        String moduleDefinitionLocation = module.getDefinition();
        File serviceTargetDir = new File(servicesDir + File.separator + module.getName());
        extractTar(Paths.get(extractedMpackDirectory + File.separator + "modules" + File.separator + moduleDefinitionLocation), servicesDir);
        Path extractedServiceDirectory = Files.move(Paths.get(servicesDir + File.separator + moduleDefinitionLocation
            .substring(moduleDefinitionLocation.indexOf("/") + 1, moduleDefinitionLocation.indexOf(".tar.gz"))),
          serviceTargetDir.toPath(), StandardCopyOption.REPLACE_EXISTING);

    }
  }

  /**
   * Create the new mpack directory to hold the mpack files.
   *
   * @param mpack        Mpack to process
   * @return boolean
   * @throws IOException
   */
  private Boolean createMpackDirectory(Mpack mpack)
    throws IOException, ResourceAlreadyExistsException {
        //Check if the mpack already exists
        List<MpackEntity> mpackEntities = mpackDAO.findByNameVersion(mpack.getName(), mpack.getVersion());
        if (mpackEntities.size() == 0) {
          File mpackDirectory = new File(mpacksStaging + File.separator + mpack.getName());

          if (!mpackDirectory.exists()) {
            mpackDirectory.mkdir();
          }
          return true;
        } else {
          String message =
            "Mpack: " + mpack.getName() + " version: " + mpack.getVersion() + " already exists in server";
          throw new ResourceAlreadyExistsException(message);
        }
  }

  /***
   * Create a linkage between the staging directory and the working directory i.e from mpacks-v2 to stackRoot.
   * This will enable StackManager to parse the newly registered mpack as part of the stacks.
   *
   * @param mpack Mpack to process
   * @throws IOException
   */
  private void createSymLinks(Mpack mpack) throws IOException {

    String stackId = mpack.getStackId();
    String stackName = "";
    String stackVersion = "";
    if (stackId == null) {
      stackName = mpack.getName();
      stackVersion = mpack.getVersion();
    } else {
      StackId id = new StackId(stackId);
      stackName = id.getStackName();
      stackVersion = id.getStackVersion();
    }
    File stack = new File(stackRoot + "/" + stackName);
    Path stackPath = Paths.get(stackRoot + "/" + stackName + "/" + stackVersion);
    Path mpackPath = Paths.get(mpacksStaging + "/" + mpack.getName() + "/" + mpack.getVersion());

    if (!stack.exists()) {
      stack.mkdir();
    }
    if (Files.isSymbolicLink(stackPath)) {
      Files.delete(stackPath);
    }
    Files.createSymbolicLink(stackPath, mpackPath);
  }

  /***
   * Download the mpack from the given uri
   *
   * @param mpackURI
   * @param mpackDefinitionLocation
   * @return
   */
  public Path downloadMpack(String mpackURI, String mpackDefinitionLocation) throws IOException {

    File stagingDir = new File(mpacksStaging.toString() + File.separator + MPACK_TAR_LOCATION);
    Path targetPath = new File(stagingDir.getPath() + File.separator + mpackDefinitionLocation).toPath();
    String mpackTarURI = mpackURI.substring(0, mpackURI.lastIndexOf('/')) + File.separator + mpackDefinitionLocation;
    URL url = new URL(mpackTarURI);

    if (!stagingDir.exists()) {
      stagingDir.mkdir();
    }

    Files.copy(url.openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    return targetPath;
  }

  /**
   * Compares if the user's mpack information matches the downloaded mpack information.
   *
   * @param expectedMpackName
   * @param expectedMpackVersion
   * @param actualMpackName
   * @param actualMpackVersion
   * @return boolean
   */
  protected boolean validateMpackInfo(
    String expectedMpackName,
    String expectedMpackVersion,
    String actualMpackName,
    String actualMpackVersion) {

    String strippedActualMpackVersion = actualMpackVersion.substring(0, actualMpackVersion.lastIndexOf('.'));
    if (expectedMpackName.equalsIgnoreCase(actualMpackName) && expectedMpackVersion
      .equalsIgnoreCase(strippedActualMpackVersion)) {
      return true;
    } else {
      LOG.info("Incorrect information : Mismatch in - (" + expectedMpackName + "," + actualMpackName + ") or ("
        + expectedMpackVersion + "," + actualMpackVersion + ")");
      return false;
    }
  }

  /**
   * Make an entry in the mpacks database for the newly registered mpack.
   *
   * @param mpack
   * @return
   * @throws IOException
   */
  protected Long populateDB(Mpack mpack) throws IOException {

    String mpackName = mpack.getName();
    String mpackVersion = mpack.getVersion();
    List resultSet = mpackDAO.findByNameVersion(mpackName, mpackVersion);

    if (resultSet.size() == 0) {
      LOG.info("Adding mpack {}-{} to the database", mpackName, mpackVersion);

      MpackEntity mpackEntity = new MpackEntity();
      mpackEntity.setMpackName(mpackName);
      mpackEntity.setMpackVersion(mpackVersion);
      mpackEntity.setMpackUri(mpack.getMpackUri());
      mpackEntity.setRegistryId(mpack.getRegistryId());
      Long mpackId = mpackDAO.create(mpackEntity);
      return mpackId;
    }
    //mpack already exists
    return null;
  }

  /***
   * Makes an entry or updates the entry in the stack table to establish a link between the mpack and the
   * associated stack
   *
   * @param mpack
   * @throws IOException
   */
  protected void populateStackDB(Mpack mpack) throws IOException {

    String stackId = mpack.getStackId();
    String stackName = "";
    String stackVersion = "";
    if (stackId == null) {
      stackName = mpack.getName();
      stackVersion = mpack.getVersion();
    } else {
      StackId id = new StackId(stackId);
      stackName = id.getStackName();
      stackVersion = id.getStackVersion();
    }

    StackEntity stackEntity = stackDAO.find(stackName, stackVersion);
    if (stackEntity == null) {
      LOG.info("Adding stack {}-{} to the database", stackName, stackVersion);
      stackEntity = new StackEntity();

      stackEntity.setStackName(stackName);
      stackEntity.setStackVersion(stackVersion);
      stackEntity.setCurrentMpackId(mpack.getMpackId());
      stackDAO.create(stackEntity);
    } else {
      LOG.info("Updating stack {}-{} to the database", stackName, stackVersion);

      stackEntity.setCurrentMpackId(mpack.getMpackId());
      stackDAO.merge(stackEntity);
    }
  }

  /**
   * Fetches the mpack info stored in the memory for mpacks/{mpack_id} call.
   *
   * @param mpackId
   * @return list of {@link Module}
   */
  public List<Module> getModules(Long mpackId) {
    Mpack mpack = mpackMap.get(mpackId);
    return mpack.getModules();
  }

  /***
   * Remove the mpack and stack directories when a request comes in to delete a particular mpack.
   *
   * @param mpackEntity
   * @throws IOException
   */
  public boolean removeMpack(MpackEntity mpackEntity, StackEntity stackEntity) throws IOException {

    boolean stackDelete = false;
    File mpackDirToDelete = new File(
      mpacksStaging + File.separator + mpackEntity.getMpackName() + File.separator + mpackEntity.getMpackVersion());
    File mpackDirectory = new File(mpacksStaging + "/" + mpackEntity.getMpackName());
    String mpackName = mpackEntity.getMpackName() + "-" + mpackEntity.getMpackVersion() + ".tar.gz";
    Path mpackTarFile = Paths.get(mpacksStaging + File.separator + MPACK_TAR_LOCATION + File.separator + mpackName);

    LOG.info("Removing mpack :" + mpackName);

    mpackMap.remove(mpackEntity.getMpackId());
    FileUtils.deleteDirectory(mpackDirToDelete);

    if (mpackDirectory.isDirectory()) {
      if (mpackDirectory.list().length == 0) {
        Files.delete(mpackDirectory.toPath());
      }
    }
    if (stackEntity != null) {
      Path stackPath = Paths.get(stackRoot + "/" + stackEntity.getStackName() + "/" + stackEntity.getStackVersion());
      File stackDirectory = new File(stackRoot + "/" + stackEntity.getStackName());
      if (!Files.exists(stackPath))
        Files.delete(stackPath);
      if (stackDirectory.isDirectory()) {
        if (stackDirectory.list().length == 0) {
          Files.delete(stackDirectory.toPath());
        }
      }
      stackDelete = true;
    }

    if (Files.exists(mpackTarFile)) {
      Files.delete(mpackTarFile);
    }
    return stackDelete;
  }
}
