package com.pchinta.punchclock.web.service;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;

import com.pchinta.punchclock.web.model.ChatUser;
import com.pchinta.punchclock.web.model.User;

public class ChatService {

	public static final long APPLICAION_ID = 20372;
	public static final String AUTH_KEY = "e6pdtztOJuYZYzQ";
	public static final String AUTH_SECRET = "QaRPUY4SXt6akZU";

	public static String hmacDigest(String msg, String keyString, String algo) {

		String digest = null;
		try {
			SecretKeySpec key = new SecretKeySpec((keyString).getBytes(), algo);
			Mac mac = Mac.getInstance(algo);
			mac.init(key);

			byte[] bytes = mac.doFinal(msg.getBytes());

			StringBuffer hash = new StringBuffer();
			for (int i = 0; i < bytes.length; i++) {
				String hex = Integer.toHexString(0xFF & bytes[i]);
				if (hex.length() == 1) {
					hash.append('0');
				}
				hash.append(hex);
			}
			digest = hash.toString();
		} catch (InvalidKeyException e) {
		} catch (NoSuchAlgorithmException e) {
		}
		return digest;
	}

	public static String getSignature(long timestamp, int nonce) {
		return hmacDigest("application_id=" + APPLICAION_ID + "&auth_key="
				+ AUTH_KEY + "&nonce=" + nonce + "&timestamp=" + timestamp,
				AUTH_SECRET, "HmacSHA1");
	}

	@SuppressWarnings("unchecked")
	public static String authenticate() throws ClientProtocolException,
			IOException {
		ObjectMapper mapper = new ObjectMapper();
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost("https://api.quickblox.com/session.json");
		post.addHeader("Content-Type", "application/json");
		post.addHeader("QuickBlox-REST-API-Version", "0.1.0");
		JSONObject json = new JSONObject();
		json.put("application_id", APPLICAION_ID);
		json.put("auth_key", AUTH_KEY);
		long timestamp = getTimestamp();
		json.put("timestamp", timestamp);
		int nonce = (int) Math.round(33432 * Math.random());
		json.put("nonce", nonce);
		json.put("signature", getSignature(timestamp, nonce));
		post.setEntity(new StringEntity(json.toJSONString()));
		HttpResponse response = client.execute(post);
		HttpEntity responseEntity = response.getEntity();
		JsonNode respJsonObj = mapper.readTree(responseEntity.getContent());
		System.out.println(respJsonObj);
		return respJsonObj.get("session").get("token").getTextValue();

	}

	public static long getTimestamp() {
		long ticks = Calendar.getInstance().getTimeInMillis();
		ticks /= 1000;
		return ticks;
	}

	public static String createUser(User user) throws ClientProtocolException,
			IOException {
		ObjectMapper mapper = new ObjectMapper();
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost("http://api.quickblox.com/users.json");
		post.addHeader("Content-Type", "application/json");
		post.addHeader("QuickBlox-REST-API-Version", "0.1.0");
		post.addHeader("QB-Token", authenticate());
		ChatUser cu = new ChatUser();
		cu.setFull_name(user.getName());
		cu.setLogin(user.getDeviceId());
		String data = "{\"user\":" + mapper.writeValueAsString(cu) + "}";
		HttpEntity entity = new StringEntity(data, ContentType.APPLICATION_JSON);
		post.setEntity(entity);
		HttpResponse response = client.execute(post);
		HttpEntity responseEntity = response.getEntity();
		JsonNode respJsonObj = mapper.readTree(responseEntity.getContent());
		return respJsonObj.get("user").get("id").asText();
	}

	public static String updateUser(User user) throws ClientProtocolException,
			IOException {
		ObjectMapper mapper = new ObjectMapper();
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost("http://api.quickblox.com/users/"+user.getChatId()+".json");
		post.addHeader("Content-Type", "application/json");
		post.addHeader("QuickBlox-REST-API-Version", "0.1.0");
		post.addHeader("QB-Token", authenticate());
		ChatUser cu = new ChatUser();
		cu.setFull_name(user.getName());
		cu.setLogin(user.getDeviceId());
		String data = "{\"user\":" + mapper.writeValueAsString(cu) + "}";
		HttpEntity entity = new StringEntity(data, ContentType.APPLICATION_JSON);
		post.setEntity(entity);
		HttpResponse response = client.execute(post);
		HttpEntity responseEntity = response.getEntity();
		JsonNode respJsonObj = mapper.readTree(responseEntity.getContent());
		return respJsonObj.get("user").get("id").asText();
	}

	public static void main(String args[]) throws ClientProtocolException,
			IOException {
		User user = new User();
		user.setName("testmain");
		user.setDeviceId("asdfasfsd");
		String chatId = createUser(user);
		System.out.println(chatId);
	}
}
