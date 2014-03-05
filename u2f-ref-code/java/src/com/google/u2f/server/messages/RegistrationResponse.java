package com.google.u2f.server.messages;

public class RegistrationResponse {
  /** websafe-base64(raw registration response message) */
  private final String registrationData;

  /** websafe-base64(UTF8(stringified(client data))) */
  private final String bd;

  /** session id originally passed */
  private final String sessionId;

  public RegistrationResponse(String registrationData, String bd, String sessionId) {
    this.registrationData = registrationData;
    this.bd = bd;
    this.sessionId = sessionId;
  }

  public String getRegistrationData() {
    return registrationData;
  }

  public String getBd() {
    return bd;
  }

  public String getSessionId() {
    return sessionId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((bd == null) ? 0 : bd.hashCode());
    result = prime * result + ((registrationData == null) ? 0 : registrationData.hashCode());
    result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    RegistrationResponse other = (RegistrationResponse) obj;
    if (bd == null) {
      if (other.bd != null)
        return false;
    } else if (!bd.equals(other.bd))
      return false;
    if (registrationData == null) {
      if (other.registrationData != null)
        return false;
    } else if (!registrationData.equals(other.registrationData))
      return false;
    if (sessionId == null) {
      if (other.sessionId != null)
        return false;
    } else if (!sessionId.equals(other.sessionId))
      return false;
    return true;
  }
}
