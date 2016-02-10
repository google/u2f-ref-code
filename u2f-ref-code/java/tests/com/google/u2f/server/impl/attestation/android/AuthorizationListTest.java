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

  @Test
  public void toJson_nullValues() throws Exception {
    JsonObject json = new AuthorizationList(null, null).toJson();

    assertFalse(json.has(AuthorizationList.JSON_ALGORITHM_KEY));
    assertFalse(json.has(AuthorizationList.JSON_PURPOSE_KEY));
  }

  @Test
  public void toJson_emptyPurpose() throws Exception {
    AuthorizationList authorizationList =
        new AuthorizationList(EMPTY_PURPOSE, Algorithm.KM_ALGORITHM_EC);
    JsonObject json = authorizationList.toJson();

    assertEquals(
        Algorithm.KM_ALGORITHM_EC.toString(),
        json.get(AuthorizationList.JSON_ALGORITHM_KEY).getAsString());
    List<Purpose> extractedPurpose = extractPurposeListFromJsonArray(
        json.get(AuthorizationList.JSON_PURPOSE_KEY).getAsJsonArray());

    assertTrue(EMPTY_PURPOSE.containsAll(extractedPurpose));
    assertTrue(extractedPurpose.containsAll(EMPTY_PURPOSE));
  }

  @Test
  public void toJson_onePurpose() throws Exception {
    AuthorizationList authorizationList =
        new AuthorizationList(ONE_PURPOSE, Algorithm.KM_ALGORITHM_HMAC);
    JsonObject json = authorizationList.toJson();

    assertEquals(
        Algorithm.KM_ALGORITHM_HMAC.toString(),
        json.get(AuthorizationList.JSON_ALGORITHM_KEY).getAsString());
    List<Purpose> extractedPurpose = extractPurposeListFromJsonArray(
        json.get(AuthorizationList.JSON_PURPOSE_KEY).getAsJsonArray());

    assertTrue(ONE_PURPOSE.containsAll(extractedPurpose));
    assertTrue(extractedPurpose.containsAll(ONE_PURPOSE));
  }

  @Test
  public void toJson_twoPurposes() throws Exception {
    JsonObject json = new AuthorizationList(TWO_PURPOSES, Algorithm.KM_ALGORITHM_RSA).toJson();

    assertEquals(
        Algorithm.KM_ALGORITHM_RSA.toString(),
        json.get(AuthorizationList.JSON_ALGORITHM_KEY).getAsString());
    List<Purpose> extractedPurpose = extractPurposeListFromJsonArray(
        json.get(AuthorizationList.JSON_PURPOSE_KEY).getAsJsonArray());

    assertTrue(TWO_PURPOSES.containsAll(extractedPurpose));
    assertTrue(extractedPurpose.containsAll(TWO_PURPOSES));
  }

  private List<Purpose> extractPurposeListFromJsonArray(JsonArray array) throws Exception {
    Iterator<JsonElement> iterator = array.iterator();
    List<Purpose> result = new ArrayList<Purpose>();
    while (iterator.hasNext()) {
      result.add(Purpose.fromString(iterator.next().getAsString()));
    }
    return result;
  }
}
