package com.google.u2f.server.messages;

public class SignResponse {

	/** websafe-base64(client data) */
	private final String bd;

	/** websafe-base64(raw response from U2F device) */
	private final String sign;

	/** challenge originally passed */
	private final String challenge;

	/** session id originally passed */
	private final String sessionId;

	/** application id originally passed */
	private final String appId;

	public SignResponse(String bd, String sign, String challenge, String sessionId, String appId) {
		this.bd = bd;
		this.sign = sign;
		this.challenge = challenge;
		this.sessionId = sessionId;
		this.appId = appId;
	}

	public String getBd() {
		return bd;
	}

	public String getSign() {
		return sign;
	}

	public String getChallenge() {
		return challenge;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getAppId() {
		return appId;
	}

	@Override
  public int hashCode() {
	  final int prime = 31;
	  int result = 1;
	  result = prime * result + ((appId == null) ? 0 : appId.hashCode());
	  result = prime * result + ((bd == null) ? 0 : bd.hashCode());
	  result = prime * result + ((challenge == null) ? 0 : challenge.hashCode());
	  result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
	  result = prime * result + ((sign == null) ? 0 : sign.hashCode());
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
	  SignResponse other = (SignResponse) obj;
	  if (appId == null) {
		  if (other.appId != null)
			  return false;
	  } else if (!appId.equals(other.appId))
		  return false;
	  if (bd == null) {
		  if (other.bd != null)
			  return false;
	  } else if (!bd.equals(other.bd))
		  return false;
	  if (challenge == null) {
		  if (other.challenge != null)
			  return false;
	  } else if (!challenge.equals(other.challenge))
		  return false;
	  if (sessionId == null) {
		  if (other.sessionId != null)
			  return false;
	  } else if (!sessionId.equals(other.sessionId))
		  return false;
	  if (sign == null) {
		  if (other.sign != null)
			  return false;
	  } else if (!sign.equals(other.sign))
		  return false;
	  return true;
  }
}
