package com.google.u2f.server.impl.androidattestation;

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
