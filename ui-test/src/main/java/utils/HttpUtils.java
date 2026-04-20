package utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import api.InjiVerifyConfigManager;

public class HttpUtils { 	

	public static String getInjiWebBaseUrl() {
        try {
            URL injiWebUrl = new URL(InjiVerifyConfigManager.getInjiWebUi());
            return injiWebUrl.getProtocol() + "://" + injiWebUrl.getAuthority();
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive inji-web base url", e);
        }
    }

    public static String InjiLoginUrl() {
        return InjiVerifyConfigManager.getproperty("apiInternalEndPoint");
    }
    
    public static String get(String key) {
        return InjiVerifyConfigManager.getproperty(key);
    }

	private static String buildApiInternalUrl(String path) {
		String baseUrl = InjiVerifyConfigManager.getproperty("apiInternalEndPoint");
		if (baseUrl == null || baseUrl.trim().isEmpty()) {
			throw new RuntimeException("Missing required config value for key: apiInternalEndPoint");
		}
		baseUrl = baseUrl.trim();
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		return baseUrl + path;
	}

	public static String getIdToken() throws IOException {
		String clientId = InjiVerifyConfigManager.getproperty("INJIWEB_GOOGLE_CLIENT_ID");
		String clientSecret = InjiVerifyConfigManager.getproperty("INJIWEB_GOOGLE_CLIENT_SECRET");
		String refreshToken = InjiVerifyConfigManager.getproperty("INJIWEB_GOOGLE_REFRESH_TOKEN");

		URL url = new URL("https://oauth2.googleapis.com/token");
		String urlParameters = "client_id=" + URLEncoder.encode(clientId, "UTF-8") +
				"&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8") +
				"&refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8") +
				"&grant_type=refresh_token";

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		try (OutputStream os = conn.getOutputStream()) {
			os.write(urlParameters.getBytes("UTF-8"));
		}

		int responseCode = conn.getResponseCode();
		String response = readResponse(conn);

		if (responseCode != 200) {
			throw new RuntimeException("Failed to get ID token. HTTP error code: " + responseCode + ", response: " + response);
		}

		String idToken = extractValue(response, "id_token");
		if (idToken == null || idToken.isEmpty()) {
			throw new RuntimeException("id_token not found in response: " + response);
		}

		return idToken;
	}

	public static String getSessionCookieFromIdToken(String idToken) throws IOException {
		String tokenLoginUrl = buildApiInternalUrl("/v1/mimoto/auth/google/token-login");
		URL url = new URL(tokenLoginUrl);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Authorization", "Bearer " + idToken);
		conn.setDoOutput(true);

		String body = "{}";
		try (OutputStream os = conn.getOutputStream()) {
			os.write(body.getBytes("UTF-8"));
		}

		int responseCode = conn.getResponseCode();
		if (responseCode != 200) {
			String errorResponse = readErrorResponse(conn);
			throw new RuntimeException("Failed to get session cookie. HTTP code: " + responseCode + ", response: " + errorResponse);
		}

		String cookie = conn.getHeaderField("Set-Cookie");
		if (cookie == null || (!cookie.contains("SESSION") && !cookie.contains("connect.sid"))) {
			throw new RuntimeException("Session cookie not found in response headers");
		}

		return cookie.split(";")[0];
	}

	public static List<String> getWalletIds(String sessionCookie) throws IOException {
		String walletsUrl = buildApiInternalUrl("/v1/mimoto/wallets");
		URL url = new URL(walletsUrl);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Cookie", sessionCookie);

		int responseCode = conn.getResponseCode();
		String response = readResponse(conn);

		if (responseCode != 200) {
			throw new RuntimeException("Failed to fetch wallets. HTTP code: " + responseCode + ", response: " + response);
		}

		return extractAllValues(response, "walletId");
	}

	public static void deleteWallet(String walletId, String sessionCookie) throws IOException {
		String deleteUrl = buildApiInternalUrl("/v1/mimoto/wallets/" + walletId);
		HttpURLConnection conn = (HttpURLConnection) new URL(deleteUrl).openConnection();
		conn.setRequestMethod("DELETE");
		conn.setRequestProperty("Cookie", sessionCookie);

		int responseCode = conn.getResponseCode();
		if (responseCode != 200) {
			throw new RuntimeException("Failed to delete wallet " + walletId + ". HTTP code: " + responseCode);
		}
	}

	public static void cleanupWallets() {
		try {
			String idToken = getIdToken();
			String session = getSessionCookieFromIdToken(idToken);
			List<String> walletIds = getWalletIds(session);
			for (String walletId : walletIds) {
				deleteWallet(walletId, session);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String readResponse(HttpURLConnection conn) throws IOException {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				response.append(line);
			}
			return response.toString();
		}
	}

	private static String readErrorResponse(HttpURLConnection conn) throws IOException {
		InputStream errorStream = conn.getErrorStream();
		if (errorStream == null) return "";
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		}
	}

	private static String extractValue(String json, String key) {
		String normalized = json.replaceAll("[\\n\\r]", "").replaceAll("\\s+", "");
		String pattern = "\"" + key + "\":\"";
		int start = normalized.indexOf(pattern);
		if (start == -1) return null;
		int end = normalized.indexOf("\"", start + pattern.length());
		return normalized.substring(start + pattern.length(), end);
	}

	private static List<String> extractAllValues(String json, String key) {
		List<String> values = new ArrayList<>();
		String normalized = json.replaceAll("[\\n\\r]", "").replaceAll("\\s+", "");
		String pattern = "\"" + key + "\":\"";
		int index = 0;
		while ((index = normalized.indexOf(pattern, index)) != -1) {
			int start = index + pattern.length();
			int end = normalized.indexOf("\"", start);
			if (end != -1) {
				values.add(normalized.substring(start, end));
				index = end + 1;
			} else {
				break;
			}
		}
		return values;
	}
}
