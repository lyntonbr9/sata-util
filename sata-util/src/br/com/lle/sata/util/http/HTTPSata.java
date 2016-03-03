package br.com.lle.sata.util.http;

import static br.com.lle.sata.util.StringUtil.concat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTPSata {
	
	/**
	 * The POST method.
	 * 
	 * @param targetURL
	 *            : The URL to POST to.
	 * @param contentHash
	 *            : The hashtable of the parameters to be posted.
	 * 
	 * @return The String returned as a result of POSTing.
	 */
	public static String POST(String targetURL, Hashtable<String, String> contentHash) {
		try {
			// recupera a conexao
			HttpURLConnection conn = (HttpURLConnection) getConnection(targetURL);
			// The data streams used to read from and write to the URL connection.
			DataOutputStream out;
			DataInputStream in;
			// String returned as the result of the POST.
			String resultado = "";
			// Set connection parameters. We need to perform input and output,
			// so set both as true.
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("charset", "UTF-8");
			// Set the content type we are POSTing. We impersonate it as
			// encoded form data
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			// Disable use of caches.
			conn.setUseCaches(false);
			// get the output stream to POST to.
			out = new DataOutputStream(conn.getOutputStream());
			String content = "";
			// Create a single String value to be POSTED from the parameters passed
			// to us. This is done by making "name"="value" pairs for all the keys
			// in the Hashtable passed to us.
			Enumeration<String> e = contentHash.keys();
			boolean first = true;
			while (e.hasMoreElements()) {
				// For each key and value pair in the hashtable
				Object key = e.nextElement();
				Object value = contentHash.get(key);
				
				// If this is not the first key-value pair in the hashtable,
				// concatenate an "&" sign to the constructed String
				if (!first) {
					content = concat(content, "&");
				}
					
				// append to a single string. Encode the value portion
				content = concat(content, key, "=", URLEncoder.encode((String) value, "UTF-8"));

				first = false;
			}
			
			// Write out the bytes of the content string to the stream.
			out.writeBytes(content);
			out.flush();
			out.close();

			// Read input from the input stream.
			in = new DataInputStream(conn.getInputStream());
			
			// Read the input stream
			Scanner sc = new Scanner(in);
			while(sc.hasNextLine()) {
				resultado = concat(resultado, sc.nextLine(), "\n");
			}

			in.close();
			sc.close();
			
			// faz a conversão de UTF-8 para ISO-8859-1 por causa da acentuacao
			String respostaUTF8 = new String(resultado.getBytes(), "UTF-8");
			resultado = new String(respostaUTF8.getBytes(), "ISO-8859-1");
			
			// return the string that was read.
			return resultado;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}
	
	/**
	 * The GET method.
	 * 
	 * @param targetURL
	 *            : The URL to GET.
	 * @param contentHash
	 *            : The hashtable of the parameters to be send.
	 * 
	 * @return The String returned as a result of GETing.
	 */
	public static String GET(String targetURL, Hashtable<String, String> contentHash) {
		// String returned as the result of the GET.
		String content = "";
		try {
			// The data streams used to read from and write to the URL connection.
			DataInputStream in;
			// recupera a conexao
			HttpURLConnection conn = (HttpURLConnection) getConnection(targetURL);
			// Set connection parameters. We need to perform input.
			conn.setDoInput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("charset", "UTF-8");
			
			if (contentHash != null) {
				for (Map.Entry<String, String> entry : contentHash.entrySet()) {
					conn.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}

			// Disable use of caches.
			conn.setUseCaches(false);
			
			// Read input from the input stream.
			in = new DataInputStream(conn.getInputStream());
			
			// Read the input stream
			Scanner sc = new Scanner(in);
			while(sc.hasNextLine()) {
				content = concat(content, sc.nextLine(), "\n");
			}
			// close the streams
			in.close();
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return content;
	}
	
	private static URLConnection getConnection(String targetURL) throws IOException  {
		URL url = new URL(targetURL);
		Proxy proxy = getProxy();
		if (proxy != null)
			return url.openConnection(proxy);
		else
			return url.openConnection();
	}
	
	public static Proxy getProxy() {
		//http://proxylist.hidemyass.com/search-1299520#listable
		//return new Proxy(Proxy.Type.HTTP, new InetSocketAddress("186.228.5.114", 80));
		return null;
	}
	
	public static void main(String[] args) {
		String ativo = "PETRC13";
		String urlCotacao = concat("http://br.advfn.com/bolsa-de-valores/bovespa/", ativo, "/cotacao");
		String html = HTTPSata.GET(urlCotacao, null);
		//log(html);
		Pattern p = Pattern.compile("<td align=\"center\">\\d");
		Matcher m = p.matcher(html);
		// recupera preco exercico
		m.find();
		//System.out.println("start: " + m.start() + " end: " + m.end());
		//System.out.println("sustr: " + html.substring(m.start(), m.end()));
		int tdIndex = html.indexOf("</td>", m.start());
		System.out.println("dados: " + html.substring(m.start() + "<td align=\"center\">".length(), tdIndex));
		// recupera data vencimento
		m.find();
		//System.out.println("start: " + m.start() + " end: " + m.end());
		//System.out.println("sustr: " + html.substring(m.start(), m.end()));
		tdIndex = html.indexOf("</td>", m.start());
		String dataVenc = html.substring(m.start() + "<td align=\"center\">".length(), tdIndex);
		System.out.println("data vencimento: " + dataVenc.substring(8,10) + "/" + dataVenc.substring(5,7) + "/" +  dataVenc.substring(0,4));
			
	}
	
	/*
	private static void setProxyProperties() {
		if (SATAUtil.isAmbienteDesenvolvimento()) {
			System.setProperty("http.proxyHost", "177.68.147.37");
			System.setProperty("http.proxyPort", "1080");
		}

	*/

}
