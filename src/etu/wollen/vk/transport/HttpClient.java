package etu.wollen.vk.transport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpClient {
	
	// workaround for certificates (not for production!)
    private static class TrustAnyTrustManager implements X509TrustManager { 
    	  
        public void checkClientTrusted(X509Certificate[] chain, String authType) 
                throws CertificateException { 
        } 
  
        public void checkServerTrusted(X509Certificate[] chain, String authType) 
                throws CertificateException { 
        } 
  
        public X509Certificate[] getAcceptedIssuers() { 
            return new X509Certificate[] {}; 
        } 
    }
    
    private static class TrustAnyHostnameVerifier implements HostnameVerifier { 
        public boolean verify(String hostname, SSLSession session) { 
            return true; 
        } 
    }
    
    private SSLSocketFactory socketFactory;
    private HostnameVerifier hostnameVerifier;
    private boolean debugEnabled = false;
	private long lastCall = 0;

	private static volatile HttpClient instance;

	public static HttpClient getInstance() {
		HttpClient localInstance = instance;
		if (localInstance == null) {
			synchronized (HttpClient.class) {
				localInstance = instance;
				if (localInstance == null) {
					instance = localInstance = new HttpClient();
				}
			}
		}
		return localInstance;
	}
    
    private HttpClient(){
        SSLContext sc;
		try {
			sc = SSLContext.getInstance("SSL");
			sc.init(null, new TrustManager[] { new TrustAnyTrustManager() }, new java.security.SecureRandom()); 
			socketFactory = sc.getSocketFactory();
			
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			e.printStackTrace();
		}
		hostnameVerifier = new TrustAnyHostnameVerifier();
    }

	// send GET and return response in UTF-8
	private String sendGET(String urlToRead) throws Exception {
		
		URL url = new URL(urlToRead);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setSSLSocketFactory(socketFactory); 
		conn.setHostnameVerifier(hostnameVerifier); 
		conn.setRequestMethod("GET");
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		StringBuilder result = new StringBuilder(conn.getContentLength());
		char[] cbuf = new char[1024];
		int len;
		while ((len = rd.read(cbuf)) != -1) {
			result.append(cbuf, 0, len);
		}
		rd.close();
		return result.toString();
	}

	private String sendGET(String urlToRead, int delay) throws Exception {
		if(delay <= 0) return sendGET(urlToRead);

		synchronized (this) {
			long currentTime = System.currentTimeMillis();
			long diff = currentTime - lastCall;
			if (delay > diff) {
				Thread.sleep(delay - diff);
			}
			String response = sendGET(urlToRead);
			lastCall = System.currentTimeMillis();
			return response;
		}
	}

	public String sendGETtimeout(String request, int attempts) throws Exception {
		String response = null;
		for (int i = 0; i < attempts; ++i) {
			try {
				response = sendGET(request, 334);
				break; // exit cycle
			} catch (Exception e) {
				if (i < attempts - 1) {
					if(debugEnabled) {
						System.out.println("Request >>> " + request + System.lineSeparator());
					}
					System.out.println("Connection timed out... " + (attempts - i - 1) + " more attempts...");
				} else {
					throw e;
				}
			}
		}
		if(debugEnabled) {
			System.out.println("Request >>> " + request + System.lineSeparator() + "Response <<< " + response);
		}
		return response;
	}

	public void setDebugEnabled(boolean debugEnabled){
		this.debugEnabled = debugEnabled;
	}
}
