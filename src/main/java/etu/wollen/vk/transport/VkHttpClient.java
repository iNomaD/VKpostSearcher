package etu.wollen.vk.transport;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static etu.wollen.vk.conf.Config.HTTP_CONNECTION_TIMEOUT_SECONDS;
import static etu.wollen.vk.conf.Config.HTTP_MAX_ATTEMPTS;

public class VkHttpClient {
    private HttpClient httpClient;
    private boolean debugEnabled = false;
    private int delayMillis = 0;
	private long lastCall = 0;

	private static volatile VkHttpClient instance;

	public static VkHttpClient getInstance() {
		VkHttpClient localInstance = instance;
		if (localInstance == null) {
			synchronized (VkHttpClient.class) {
				localInstance = instance;
				if (localInstance == null) {
					instance = localInstance = new VkHttpClient();
				}
			}
		}
		return localInstance;
	}
    
    private VkHttpClient(){
		httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(HTTP_CONNECTION_TIMEOUT_SECONDS))
			.version(HttpClient.Version.HTTP_1_1)
			.build();
    }

	private String sendGet(String urlToRead) throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder()
			.GET()
			.uri(URI.create(urlToRead))
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		return response.body();
	}

	private String sendGet(String urlToRead, int delay) throws IOException, InterruptedException {
		if(delay <= 0) return sendGet(urlToRead);

		synchronized (this) {
			long currentTime = System.currentTimeMillis();
			long diff = currentTime - lastCall;
			if (delay > diff) {
				Thread.sleep(delay - diff);
			}
			String response = sendGet(urlToRead);
			lastCall = System.currentTimeMillis();
			return response;
		}
	}

	public String httpGet(String request) throws IOException, InterruptedException {
		String response = null;
		for (int i = 0; i < HTTP_MAX_ATTEMPTS; ++i) {
			try {
				response = sendGet(request, delayMillis);
				break;
			} catch (IOException e) {
				if (i < HTTP_MAX_ATTEMPTS - 1) {
					// TODO logging with log4j
					if(debugEnabled) {
						System.out.println("Request >>> " + request + System.lineSeparator());
					}
					System.out.println("Connection timed out... " + (HTTP_MAX_ATTEMPTS - i - 1) + " more attempts...");
				} else {
					throw e;
				}
			}
		}
		if (debugEnabled) {
			System.out.println("Request >>> " + request + System.lineSeparator() + "Response <<< " + response);
		}
		return response;
	}

	public void setDebugEnabled(boolean debugEnabled){
		this.debugEnabled = debugEnabled;
	}

	public void setDelay(int delayMillis){
		this.delayMillis = delayMillis;
	}
}
