package com.google.u2f.server.impl.attestation.android;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Authorization List that describes a Keymaster key
 */
public class AuthorizationList {
  private final List<Purpose> purposeList;
  private final Algorithm algorithm;
  private final Integer keySize;
  private final List<BlockMode> blockModeList;

  @VisibleForTesting
  public static final String JSON_ALGORITHM_KEY = "algorithm";
  @VisibleForTesting
  public static final String JSON_PURPOSE_KEY = "purpose";
  @VisibleForTesting
  public static final String JSON_KEY_SIZE_KEY = "keysize";
  @VisibleForTesting
  public static final String JSON_BLOCK_MODE_KEY = "blockmode";

  protected AuthorizationList(List<Purpose> purposeList, Algorithm algorithm, Integer keySize,
      List<BlockMode> blockModeList) {
    this.purposeList = purposeList;
    this.algorithm = algorithm;
    this.keySize = keySize;
    this.blockModeList = blockModeList;
  }

  public List<Purpose> getPurposeList() {
    return purposeList;
  }

  public Algorithm getAlgorithm() {
    return algorithm;
  }

  public Integer getKeySize() {
    return keySize;
  }

  public List<BlockMode> getBlockModeList() {
    return blockModeList;
  }

  @Override
  public int hashCode() {
    return Objects.hash(purposeList, algorithm, keySize, blockModeList);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    AuthorizationList other = (AuthorizationList) obj;
    return Objects.equals(algorithm, other.algorithm)
        && areEqualIgnoringOrder(purposeList, other.purposeList)
        && Objects.equals(keySize, other.keySize)
        && areEqualIgnoringOrder(blockModeList, other.blockModeList);
  }

  @Override
  public String toString() {
    StringBuilder stringRepresentation = new StringBuilder();
    stringRepresentation.append("[");

    if (purposeList != null) {
      stringRepresentation.append("\n  purpose list: ");
      stringRepresentation.append(purposeList);
    }

    if (algorithm != null) {
      stringRepresentation.append("\n  algorithm: ");
      stringRepresentation.append(algorithm);
    }

    if (keySize != null) {
      stringRepresentation.append("\n  key size: ");
      stringRepresentation.append(keySize);
    }

    if (blockModeList != null) {
      stringRepresentation.append("\n  block mode list: ");
      stringRepresentation.append(blockModeList);
    }

    stringRepresentation.append("\n]");

    return stringRepresentation.toString();
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (purposeList != null) {
      JsonArray purposeJsonArray = new JsonArray();
      for (Purpose purpose : purposeList) {
        purposeJsonArray.add(new JsonPrimitive(purpose.toString()));
      }
      json.add(JSON_PURPOSE_KEY, purposeJsonArray);
    }
    if (algorithm != null) {
      json.addProperty(JSON_ALGORITHM_KEY, algorithm.toString());
    }
    if (keySize != null) {
      json.addProperty(JSON_KEY_SIZE_KEY, keySize);
    }
    if (blockModeList != null) {
      JsonArray blockModeJsonArray = new JsonArray();
      for (BlockMode blockMode : blockModeList) {
        blockModeJsonArray.add(new JsonPrimitive(blockMode.toString()));
      }
      json.add(JSON_BLOCK_MODE_KEY, blockModeJsonArray);
    }
    return json;
  }

  public static class Builder {
    private List<Purpose> purpose;
    private Algorithm algorithm;
    private Integer keySize;
    private List<BlockMode> blockMode;

    public Builder() {
      this.purpose = null;
      this.algorithm = null;
      this.keySize = null;
      this.blockMode = null;
    }

    public Builder setPurpose(List<Purpose> purpose) {
      if (purpose != null) {
        this.purpose = new ArrayList<Purpose>(purpose);
      }
      return this;
    }

    public Builder setAlgorithm(Algorithm algorithm) {
      this.algorithm = algorithm;
      return this;
    }

    public Builder setKeySize(Integer keySize) {
      this.keySize = keySize;
      return this;
    }

    public Builder setBlockMode(List<BlockMode> blockMode) {
      if (blockMode != null) {
        this.blockMode = new ArrayList<BlockMode>(blockMode);
      }
      return this;
    }

    public AuthorizationList build() {
      return new AuthorizationList(this.purpose, this.algorithm, this.keySize, this.blockMode);
    }
  }

  private <T> boolean areEqualIgnoringOrder(List<T> list1, List<T> list2) {
    if (list1 != null && list2 != null) {
      return list1.containsAll(list2) && list2.containsAll(list1);
    }

    return list1 == null && list2 == null;
  }
}
