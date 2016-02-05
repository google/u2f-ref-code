package com.google.u2f.server.impl.attestation.android;

import java.util.List;
import java.util.Objects;

/**
 * Authorization List that describes a Keymaster key
 */
public class AuthorizationList {
  private final List<Purpose> purpose;
  private final Algorithm algorithm;

  protected AuthorizationList(List<Purpose> purpose, Algorithm algorithm) {
    this.purpose = purpose;
    this.algorithm = algorithm;
  }

  public List<Purpose> getPurpose() {
    return purpose;
  }

  public Algorithm getAlgorithm() {
    return algorithm;
  }

  @Override
  public int hashCode() {
    return Objects.hash(purpose, algorithm);
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
    return Objects.equals(algorithm, other.algorithm) && Objects.equals(purpose, other.purpose);
  }

  @Override
  public String toString() {
    String stringRepresentation = "[";

    if (purpose != null) {
      stringRepresentation += "purpose: " + purpose;
    }

    if (algorithm != null) {
      stringRepresentation += ", algorithm: " + algorithm;
    }

    stringRepresentation += "]";

    return stringRepresentation;
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
