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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.ambari.server.controller.MpackRequest;
import org.apache.ambari.server.controller.MpackResponse;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.orm.dao.MpackDAO;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.state.Mpacks;
import org.apache.ambari.server.state.Packlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


/**
 * Manages all mpack related behavior including parsing of stacks and providing access to
 * mpack information.
 */
public class MpackManager {
  protected Map<Long, Mpacks> mpackMap = new HashMap<>();
  private File mpackStaging;
  private MpackDAO mpackDAO;

  private final static Logger LOG = LoggerFactory.getLogger(MpackManager.class);

  @AssistedInject
  public MpackManager(@Assisted("mpackv2Staging") File mpackStagingLoc, MpackDAO mpackDAOObj) {
    mpackStaging = mpackStagingLoc;
    mpackDAO = mpackDAOObj;
    //Todo : Madhu Load all mpack.json into mpackMap during ambari-server startup
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
  public MpackResponse registerMpack(MpackRequest mpackRequest) throws IOException, IllegalArgumentException, ResourceAlreadyExistsException {
    //Todo : Madhu Expand the folders
    //Todo : Madhu Update StacksAPI
    Long mpackId;
    Mpacks mpacks = new Mpacks();
    String mpackName = "";
    String mpackVersion = "";
    boolean isValidMetadata = true;
    if (mpackRequest.getRegistryId() != null) {
      mpackName = mpackRequest.getMpackName();
      mpackVersion = mpackRequest.getMpackVersion();
      mpacks = parseMpacksJson(mpackName, mpackVersion);
      mpacks.setRegistryId(mpackRequest.getRegistryId());
      //Validate the Post request
      isValidMetadata = validateMpackInfo(mpackName, mpackVersion, mpacks.getName(), mpacks.getVersion());
    } else {
      //Download the mpack and return the path.
      String[] urlSplit = mpackRequest.getMpackUrl().split("/");
      String mpackNameVersion = urlSplit[urlSplit.length - 1];
      final String patternStrName = "([a-zA-Z]+)-([a-zA-Z]+)-([a-zA-Z]+)";
      final String patternStrVersion = "([0-9]).([0-9]).([0-9]).([0-9])-([0-9]+)";
      Pattern REGEX = Pattern.compile(patternStrName);
      Matcher patternMatchObj = REGEX.matcher(mpackNameVersion);
      if (patternMatchObj.find()) {
        mpackName = patternMatchObj.group();
      }
      REGEX = Pattern.compile(patternStrVersion);
      patternMatchObj = REGEX.matcher(mpackNameVersion);
      if (patternMatchObj.find()) {
        mpackVersion = patternMatchObj.group();
      }
      mpacks = parseMpacksJson(mpackName, mpackVersion);
      mpacks.setMpacksUrl(mpackRequest.getMpackUrl());
    }
    if (isValidMetadata) {
      mpackId = populateDB(mpacks);
      if (mpackId != null) {
        mpackMap.put(mpackId, mpacks);
        mpacks.setMpackId(mpackId);
        return new MpackResponse(mpacks);
      } else {
        String message = "Mpack :" + mpackRequest.getMpackName() + " version: " + mpackRequest.getMpackVersion() + " already exists in server";
        throw new ResourceAlreadyExistsException(message);
      }
    } else {
      String message = "Incorrect information : Mismatch in - (" + mpackName + "," + mpacks.getName() + ") or (" + mpackVersion + "," + mpacks.getVersion() + ")";
      throw new IllegalArgumentException(message); //Mismatch in information
    }
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
  protected boolean validateMpackInfo(String expectedMpackName, String expectedMpackVersion, String actualMpackName, String actualMpackVersion) {
    if (expectedMpackName.equalsIgnoreCase(actualMpackName) && expectedMpackVersion.equalsIgnoreCase(actualMpackVersion))
      return true;
    else
      LOG.info("Incorrect information : Mismatch in - (" + expectedMpackName + "," + actualMpackName + ") or (" + expectedMpackVersion + "," + actualMpackVersion + ")");
    return false;
  }

  /**
   * Parses the mpack.json file for mpack information and stores in memory for powering /mpacks/{mpack_id}
   *
   * @return Mpacks
   * @throws IOException
   * @param mpackName
   * @param mpackVersion
   */
  protected Mpacks parseMpacksJson(String mpackName, String mpackVersion) throws IOException {
    Type type = new TypeToken<Mpacks>() {
    }.getType();
    Gson gson = new Gson();
    String mpackJson = mpackName + "-" + mpackVersion + ".json";
    JsonReader jsonReader = new JsonReader(new FileReader(mpackStaging + "/" + mpackName +"/" + mpackVersion + "/" + mpackJson));
    Mpacks mpacks = gson.fromJson(jsonReader, type);
    return mpacks;
  }

  /**
   * Make an entry in the mpacks database for the newly registered mpack.
   *
   * @param mpacks
   * @return
   * @throws IOException
   */
  protected Long populateDB(Mpacks mpacks) throws IOException {
    String mpackName = mpacks.getName();
    String mpackVersion = mpacks.getVersion();
    List resultSet = mpackDAO.findByNameVersion(mpackName, mpackVersion);
    if (resultSet.size() == 0) {
      LOG.info("Adding mpack {}-{} to the database", mpackName, mpackVersion);
      MpackEntity mpackEntity = new MpackEntity();
      mpackEntity.setMpackName(mpackName);
      mpackEntity.setMpackVersion(mpackVersion);
      mpackEntity.setMpackUrl(mpacks.getMpacksUrl());

      Long mpackId = mpackDAO.create(mpackEntity);
      return mpackId;
    }
    //mpack already exists
    return null;
  }

  /**
   * Fetches the packlet info stored in the memory for mpacks/{mpack_id} call.
   */
  public ArrayList<Packlet> getPacklets(Long mpackId) {
    Mpacks mpack = mpackMap.get(mpackId);
    if(mpack.getPacklets()!=null)
      return mpack.getPacklets();
    return null;
  }
}
