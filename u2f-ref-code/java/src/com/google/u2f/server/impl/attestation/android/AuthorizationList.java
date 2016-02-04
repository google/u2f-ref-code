package com.google.u2f.server.impl.attestation.android;

import com.google.common.base.Objects;

import java.util.List;

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
    return Objects.hashCode(purpose, algorithm);
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
    return Objects.equal(algorithm, other.algorithm) && Objects.equal(purpose, other.purpose);
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
