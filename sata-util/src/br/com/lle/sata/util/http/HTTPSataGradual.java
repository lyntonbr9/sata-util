package br.com.lle.sata.util.http;

import static br.com.lle.sata.util.StringUtil.concat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.lle.sata.util.LogUtil;

public class HTTPSataGradual {
	
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
	
	private static Map<Long, String> ofertaPapeis = new LinkedHashMap<>(); 
	
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
	
	public static String MONITORAR(String aspnetSessionID) {
		// String returned as the result of the GET.
		String content = "";
		try {			
			// recupera a conexao
			HttpURLConnection conn = (HttpURLConnection) getConnectionBuscaRespostas();
			
			conn.setUseCaches(false);
			conn.setRequestProperty("Cookie", aspnetSessionID);
			
			// Read input from the input stream.
			DataInputStream in = new DataInputStream(conn.getInputStream());
			// Read the input stream
			Scanner sc = new Scanner(in);
			Long tempoLeitura = System.currentTimeMillis();
			// le a resposta
			String response = sc.nextLine(); 
			ofertaPapeis.put(tempoLeitura, response);
			LogUtil.log(tempoLeitura + ": " + response);
			
			// close the streams
			in.close();
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return content;
	}
	
	private static URLConnection getConnection(String targetURL) throws IOException  {
		//URL url = new URL(targetURL);
		//Proxy proxy = getProxy();
		//if (proxy != null)
		//	return url.openConnection(proxy);
		//else
		return new URL(targetURL).openConnection();
	}
	
	private static URLConnection getConnectionBuscaRespostas() throws IOException  {
		return urlBuscarRespostas.openConnection();
	}
	
	public static Proxy getProxy() {
		//http://proxylist.hidemyass.com/search-1299520#listable
		//return new Proxy(Proxy.Type.HTTP, new InetSocketAddress("186.228.5.114", 80));
		return null;
	}
	
	private static String URL_BUSCAR_RESPOSTAS = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=BuscarRespostas";
	
	private static String URL_ASSINAR_PAPEL = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=AssinarPapel&Papel={0}&Finalidade=LivroDeOferta30";
	
	private static String URL_LIBERAR_PAPEL = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=LiberarPapel&Papel={0}&Finalidade=livrodeoferta30";
	
	//private static String[] PAPEIS_MONITORADOS = new String[] {"PETRD70","PETRP70","PETRD73","PETRP73"};
	private static String[] PAPEIS_MONITORADOS = new String[] {"PETRD70","PETRP70"};
	 	
	private static URL urlBuscarRespostas = null;
	
	static {
		exec();
	}
	
	private static void exec() {
		try {
			urlBuscarRespostas = new URL(URL_BUSCAR_RESPOSTAS);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		//String urlCotacao = "https://www.gradualinvestimentos.com.br/Async/Login.aspx";
		
		//String url = "https://www.gradualinvestimentos.com.br";
		
		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=BuscarRespostas";
		
		Hashtable<String, String> ht = new Hashtable<>();
		//ht.put("Cookie", "ASP.NET_SessionId=onfsl5hhbkhculkggfucgzra");
		ht.put("Cookie", "ASP.NET_SessionId=gofn44gbx1q3doiqks2rjeh2");
		
		// assina os papeis para monitoracao
		for (String papel : PAPEIS_MONITORADOS) {
			String assinarPapel = MessageFormat.format(URL_ASSINAR_PAPEL, papel);
			String response = HTTPSataGradual.GET(assinarPapel, ht);
			LogUtil.log(response);
		}
		
		for (int i = 0; i < 100; i++) {
			// consulta dos dados
			MONITORAR("ASP.NET_SessionId=gofn44gbx1q3doiqks2rjeh2");
		}
		
		// libera os papeis da monitoracao
		for (String papel : PAPEIS_MONITORADOS) {
			String liberarPapel = MessageFormat.format(URL_LIBERAR_PAPEL, papel);
			String response = HTTPSataGradual.GET(liberarPapel, ht);
			LogUtil.log(response);
		}
		
		
		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=AssinarPapel&Papel=PETRD70&Finalidade=LivroDeOferta30";
		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=AssinarPapel&Papel=PETRP70&Finalidade=LivroDeOferta30";
		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=AssinarPapel&Papel=PETRD73&Finalidade=LivroDeOferta30";
		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=AssinarPapel&Papel=PETRP73&Finalidade=LivroDeOferta30";
		
		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=LiberarPapel&Papel=PETRP73&Finalidade=livrodeoferta30";
		
		/*
		Hashtable<String, String> ht = new Hashtable<>();
		//ht.put("Cookie", "ASP.NET_SessionId=onfsl5hhbkhculkggfucgzra");
		ht.put("Cookie", "ASP.NET_SessionId=gofn44gbx1q3doiqks2rjeh2");
		String html = HTTPSataGradual.GET(url, ht);
		System.out.println(html);
		*/
	}

}
