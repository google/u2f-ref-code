package com.google.u2f.server.impl.attestation.android;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.Objects;

/**
 * Authorization List that describes a Keymaster key
 */
public class AuthorizationList {
  private final List<Purpose> purposeList;
  private final Algorithm algorithm;

  @VisibleForTesting
  public static final String JSON_ALGORITHM_KEY = "algorithm";

  @VisibleForTesting
  public static final String JSON_PURPOSE_KEY = "purpose";

  protected AuthorizationList(List<Purpose> purpose, Algorithm algorithm) {
    this.purposeList = purpose;
    this.algorithm = algorithm;
  }

  public List<Purpose> getPurpose() {
    return purposeList;
  }

  public Algorithm getAlgorithm() {
    return algorithm;
  }

  @Override
  public int hashCode() {
    return Objects.hash(purposeList, algorithm);
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
        && Objects.equals(purposeList, other.purposeList);
  }

  @Override
  public String toString() {
    StringBuilder stringRepresentation = new StringBuilder();
    stringRepresentation.append("[");

    if (purposeList != null) {
      stringRepresentation.append("\n  purpose: ");
      stringRepresentation.append(purposeList);
    }

    if (algorithm != null) {
      stringRepresentation.append("\n  algorithm: ");
      stringRepresentation.append(algorithm);
    }

    stringRepresentation.append("\n]");

    return stringRepresentation.toString();
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (purposeList != null) {
      JsonArray purposeJsonArray = new JsonArray();
      for (Purpose p : purposeList) {
        purposeJsonArray.add(new JsonPrimitive(p.toString()));
      }
      json.add(JSON_PURPOSE_KEY, purposeJsonArray);
    }
    if (algorithm != null) {
      json.addProperty(JSON_ALGORITHM_KEY, algorithm.toString());
    }
    return json;
  }

  public static class Builder {
    private List<Purpose> purpose;
    private Algorithm algorithm;

    public Builder() {
      this.purpose = null;
      this.algorithm = null;
    }

    public Builder setPurpose(List<Purpose> purpose) {
      this.purpose = purpose;
      return this;
    }

    public Builder setAlgorithm(Algorithm algorithm) {
      this.algorithm = algorithm;
      return this;
    }

    public AuthorizationList build() {
      return new AuthorizationList(this.purpose, this.algorithm);
    }
  }
}
