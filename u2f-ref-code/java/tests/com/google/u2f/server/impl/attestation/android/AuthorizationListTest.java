package com.google.u2f.server.impl.attestation.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.security.cert.CertificateParsingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Unit tests for {@link AuthorizationList}
 */
@RunWith(JUnit4.class)
public class AuthorizationListTest {
  private static final List<Purpose> EMPTY_PURPOSE = new ArrayList<Purpose>();
  private static final List<Purpose> ONE_PURPOSE = Arrays.asList(Purpose.KM_PURPOSE_SIGN);
  private static final List<Purpose> TWO_PURPOSES =
      Arrays.asList(Purpose.KM_PURPOSE_SIGN, Purpose.KM_PURPOSE_VERIFY);
  private static final Integer KEY_SIZE = 256;
  private static final List<BlockMode> EMPTY_BLOCKMODE = new ArrayList<BlockMode>();
  private static final List<BlockMode> ONE_BLOCKMODE = Arrays.asList(BlockMode.KM_MODE_CBC);
  private static final List<BlockMode> TWO_BLOCKMODE =
      Arrays.asList(BlockMode.KM_MODE_CBC, BlockMode.KM_MODE_CTR);

  @Test
  public void toJson_nullValues() throws Exception {
    JsonObject json = new AuthorizationList(null, null, null, null).toJson();

    assertFalse(json.has(AuthorizationList.JSON_ALGORITHM_KEY));
    assertFalse(json.has(AuthorizationList.JSON_PURPOSE_KEY));
    assertFalse(json.has(AuthorizationList.JSON_KEY_SIZE_KEY));
    assertFalse(json.has(AuthorizationList.JSON_BLOCK_MODE_KEY));
  }

  @Test
  public void toJson_emptyPurpose() throws Exception {
    JsonObject json = new AuthorizationList(EMPTY_PURPOSE, null, null, null).toJson();

    List<Purpose> extractedPurpose = extractListFromJsonArray(
        json.get(AuthorizationList.JSON_PURPOSE_KEY).getAsJsonArray(), Purpose.class);

    assertTrue(EMPTY_PURPOSE.containsAll(extractedPurpose));
    assertTrue(extractedPurpose.containsAll(EMPTY_PURPOSE));
  }

  @Test
  public void toJson_onePurpose() throws Exception {
    AuthorizationList authorizationList = new AuthorizationList(ONE_PURPOSE, null, null, null);
    JsonObject json = authorizationList.toJson();

    List<Purpose> extractedPurpose = extractListFromJsonArray(
        json.get(AuthorizationList.JSON_PURPOSE_KEY).getAsJsonArray(), Purpose.class);

    assertTrue(ONE_PURPOSE.containsAll(extractedPurpose));
    assertTrue(extractedPurpose.containsAll(ONE_PURPOSE));
  }

  @Test
  public void toJson_twoPurposes() throws Exception {
    JsonObject json = new AuthorizationList(TWO_PURPOSES, null, null, null).toJson();

    List<Purpose> extractedPurpose = extractListFromJsonArray(
        json.get(AuthorizationList.JSON_PURPOSE_KEY).getAsJsonArray(), Purpose.class);

    assertTrue(TWO_PURPOSES.containsAll(extractedPurpose));
    assertTrue(extractedPurpose.containsAll(TWO_PURPOSES));
  }

  @Test
  public void toJson_algorithm() throws Exception {
    JsonObject json = new AuthorizationList(null, Algorithm.KM_ALGORITHM_EC, null, null).toJson();

    assertEquals(
        Algorithm.KM_ALGORITHM_EC.toString(),
        json.get(AuthorizationList.JSON_ALGORITHM_KEY).getAsString());
  }

  @Test
  public void toJson_keysize() throws Exception {
    JsonObject json = new AuthorizationList(null, null, KEY_SIZE, null).toJson();

    assertEquals(KEY_SIZE.intValue(), json.get(AuthorizationList.JSON_KEY_SIZE_KEY).getAsInt());
  }

  @Test
  public void toJson_emptyBlockMode() throws Exception {
    JsonObject json = new AuthorizationList(null, null, null, EMPTY_BLOCKMODE).toJson();

    List<BlockMode> extractedBlockMode = extractListFromJsonArray(
        json.get(AuthorizationList.JSON_BLOCK_MODE_KEY).getAsJsonArray(), BlockMode.class);

    assertTrue(EMPTY_BLOCKMODE.containsAll(extractedBlockMode));
    assertTrue(extractedBlockMode.containsAll(EMPTY_BLOCKMODE));
  }

  @Test
  public void toJson_oneBlockMode() throws Exception {
    AuthorizationList authorizationList = new AuthorizationList(null, null, null, ONE_BLOCKMODE);
    JsonObject json = authorizationList.toJson();

    List<BlockMode> extractedBlockMode = extractListFromJsonArray(
        json.get(AuthorizationList.JSON_BLOCK_MODE_KEY).getAsJsonArray(), BlockMode.class);

    assertTrue(ONE_BLOCKMODE.containsAll(extractedBlockMode));
    assertTrue(extractedBlockMode.containsAll(ONE_BLOCKMODE));
  }

  @Test
  public void toJson_twoBlockMode() throws Exception {
    JsonObject json = new AuthorizationList(null, null, null, TWO_BLOCKMODE).toJson();

    List<BlockMode> extractedBlockMode = extractListFromJsonArray(
        json.get(AuthorizationList.JSON_BLOCK_MODE_KEY).getAsJsonArray(), BlockMode.class);

    assertTrue(TWO_BLOCKMODE.containsAll(extractedBlockMode));
    assertTrue(extractedBlockMode.containsAll(TWO_BLOCKMODE));
  }

  // TODO(aczeskis): There is a cleaner way of doing this in Java 8.  In Java 8, we can make Purpose
  // & BlockMode implement an interface (so the function could call .fromString() on the
  // parameterized type).  Unfortunately, Java 7 does not allow interfaces to have static methods!
  // This decision was fixed in Java 8.
  private <T> List<T> extractListFromJsonArray(JsonArray array, Class<T> type) throws Exception {
    Iterator<JsonElement> iterator = array.iterator();
    List<T> result = new ArrayList<T>();
    while (iterator.hasNext()) {
      result.add(buildTypeFromString(iterator.next().getAsString(), type));
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static <T> T buildTypeFromString(String string, Class<T> type)
      throws CertificateParsingException {
    if (type == Purpose.class) {
      return (T) Purpose.fromString(string);
    } else if (type == BlockMode.class) {
      return (T) BlockMode.fromString(string);
    } else {
      throw new CertificateParsingException("Cannot build type " + type.getSimpleName());
    }
  }
}
