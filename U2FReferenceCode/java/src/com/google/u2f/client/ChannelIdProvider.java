package com.google.u2f.client;

import com.google.gson.JsonObject;

public interface ChannelIdProvider {
	JsonObject getJsonChannelId();
}
