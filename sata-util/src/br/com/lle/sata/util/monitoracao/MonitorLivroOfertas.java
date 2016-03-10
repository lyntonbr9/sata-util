package br.com.lle.sata.util.monitoracao;

import static br.com.lle.sata.util.StringUtil.concat;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.Gson;

import br.com.lle.sata.mobile.core.util.BlackScholes;
import br.com.lle.sata.util.LogUtil;

public class MonitorLivroOfertas {
	
	private static Map<Long, String> ofertaPapeis = new LinkedHashMap<>(); 
	
	private static String URL_BUSCAR_RESPOSTAS = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=BuscarRespostas";
	
	private static String URL_ASSINAR_PAPEL = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=AssinarPapel&Papel={0}&Finalidade=LivroDeOferta30";
	
	private static String URL_LIBERAR_PAPEL = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=LiberarPapel&Papel={0}&Finalidade=livrodeoferta30";
	
	private static String[] PAPEIS_MONITORADOS = new String[] {"PETR4","PETRD70","PETRP70"};
	 	
	private static URL urlBuscarRespostas = null;
	
	private static long MIN_TIMEOUT_LEITURA = 2000;
	
	private static long TIMEOUT_MONITORACAO = 10000;
	
	private static String conteudoRecente;
	
	static {
		criarURLBuscarRespostas();
	}
	
	private static void criarURLBuscarRespostas() {
		try {
			urlBuscarRespostas = new URL(URL_BUSCAR_RESPOSTAS);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public static String GET(String targetURL, Hashtable<String, String> contentHash) {
		// String returned as the result of the GET.
		String content = "";
		try {
			//System.setProperty("http.proxyHost", "proxyad.br-petrobras.com.br");
			//System.setProperty("http.proxyPort", "9090");
			
			// The data streams used to read from and write to the URL connection.
			DataInputStream in;
			// recupera a conexao
			HttpURLConnection conn = (HttpURLConnection) getConnection(targetURL);
			
			// Set connection parameters. We need to perform input.
			conn.setDoInput(true);
			// DEFAULT
			//conn.setRequestMethod("GET");
			//conn.setRequestProperty("charset", "UTF-8");
			// Disable use of caches.
			conn.setUseCaches(false);
			
			if (contentHash != null) {
				for (Map.Entry<String, String> entry : contentHash.entrySet()) {
					conn.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}

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
		return new URL(targetURL).openConnection();
	}
	
	private static URLConnection getConnectionBuscaRespostas() throws IOException  {
		return urlBuscarRespostas.openConnection();
	}
	
	public synchronized static String getConteudoRecente(){
		return conteudoRecente;
	}
	
	public synchronized static void setConteudoRecente(String conteudo){
		conteudoRecente = conteudo;
	}
	
	/*
	public static void main(String[] args) throws InterruptedException {
		

		//String urlCotacao = "https://www.gradualinvestimentos.com.br/Async/Login.aspx";
		
		//String url = "https://www.gradualinvestimentos.com.br";
		
		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=BuscarRespostas";
		
		String aspnetSessionID = "ASP.NET_SessionId=pegu4sddwp1dydvqmjuwrib2;";
		
		Hashtable<String, String> ht = new Hashtable<>();
		//ht.put("Cookie", "ASP.NET_SessionId=onfsl5hhbkhculkggfucgzra");
		ht.put("Cookie", aspnetSessionID);
		
		// assina os papeis para monitoracao
		for (String papel : PAPEIS_MONITORADOS) {
			String assinarPapel = MessageFormat.format(URL_ASSINAR_PAPEL, papel);
			String response = MonitorLivroOfertas.GET(assinarPapel, ht);
			LogUtil.log(response);
		}
		
		// cria uma thread para processar o livro de ofertas
		Thread t = new Thread(new MonitorLivroOfertas().new ProcessarLivroOfertas());
		// inicia o processamento
		t.start();
		// cria um timer para fazer a leitura
		Timer timer = new Timer(true);
		// instancia a task que ira fazer a leitura
		TimerTask task = new MonitorLivroOfertas().new LivroOfertasTask(aspnetSessionID);
		// vai rodar a tarefa de leitura a cada tempo definido
		timer.scheduleAtFixedRate(task, 0, MIN_TIMEOUT_LEITURA);
		// para a thread principal por um tempo limitado
		Thread.sleep(TIMEOUT_MONITORACAO);
		
		System.out.println("FIM PROCESSAMENTO!");
		
		// TODO: salva em arquivo a leitura do livro de ofertas
		
		// libera os papeis da monitoracao
		for (String papel : PAPEIS_MONITORADOS) {
			String liberarPapel = MessageFormat.format(URL_LIBERAR_PAPEL, papel);
			String response = MonitorLivroOfertas.GET(liberarPapel, ht);
			LogUtil.log(response);
		}
		
		// sai da aplicacao
		System.exit(0);

		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=AssinarPapel&Papel=PETRD70&Finalidade=LivroDeOferta30";
		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=AssinarPapel&Papel=PETRP70&Finalidade=LivroDeOferta30";
		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=AssinarPapel&Papel=PETRD73&Finalidade=LivroDeOferta30";
		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=AssinarPapel&Papel=PETRP73&Finalidade=LivroDeOferta30";
		
		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=LiberarPapel&Papel=PETRP73&Finalidade=livrodeoferta30";

	}
	*/
	
	public class LivroOfertasTask extends TimerTask {

		private String aspnetSessionID;
		
		LivroOfertasTask(String aspnetSessionID) {
			this.aspnetSessionID = aspnetSessionID;
		}
		
		@Override
		public void run() {
			lerLivroOfertas();
		}
		
		// TODO: Ler o livro de ofertas 
		public void lerLivroOfertas() {
			//setConteudoRecente("teste " + aspnetSessionID + " " + String.valueOf(System.currentTimeMillis()));
			MonitorLivroOfertas.getLivroOfertas(aspnetSessionID);
		}
		
	}
	
	public static String getLivroOfertas(String aspnetSessionID) {
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
			//String response = sc.nextLine(); 
			setConteudoRecente(sc.nextLine());
			ofertaPapeis.put(tempoLeitura, getConteudoRecente());
			//LogUtil.log(tempoLeitura + ": " + getConteudoRecente());
			
			// close the streams
			in.close();
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}
	
	
	private class ProcessarLivroOfertas implements Runnable {

		private long MAX_ITERACOES = 10;
		@Override
		public void run() {
			
			for (int i = 0; i < MAX_ITERACOES; i++) {
				// marca o inicio
				long inicio = System.currentTimeMillis();
				// Vai processar os dados
				processarLivroOfertas();
				// marca o fim do processamento
				long fim = System.currentTimeMillis();
				try {
					// se tiver terminado antes do tempo para leitura dos dados
					if ((fim - inicio) < MIN_TIMEOUT_LEITURA)
						// para a thread pelo tempo restante
						Thread.sleep(MonitorLivroOfertas.MIN_TIMEOUT_LEITURA - (fim - inicio));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		public void processarLivroOfertas() {
			long inicio = System.currentTimeMillis();
			//String leituraAtual = getConteudoRecente();
			String leituraAtual = "";
			//LogUtil.log(leituraAtual);
			//leituraAtual = "{\"Papel\":\"PETRD70\"}";
			//leituraAtual = "{\"Papel\":\"PETRD70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":21700.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,14\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"1,05\",\"jQTO\":\"-\"},{\"jNUC\":37,\"jNOC\":\"UM\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"1,01\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"0,95\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2900.0,\"jQA\":null,\"jPC\":\"0,80\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,52\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,30\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,26\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,24\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,21\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":4700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,21\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,24\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":3500.0,\"jQA\":null,\"jPC\":\"1,37\",\"jQTO\":\"-\"},{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"2,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"4,00\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"30,00\",\"jQTO\":\"-\"}]}";
			//leituraAtual = "[{\"Papel\":\"PETRD70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":21700.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,14\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"1,05\",\"jQTO\":\"-\"},{\"jNUC\":37,\"jNOC\":\"UM\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"1,01\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"0,95\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2900.0,\"jQA\":null,\"jPC\":\"0,80\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,52\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,30\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,26\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,24\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,21\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":4700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,21\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,24\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":3500.0,\"jQA\":null,\"jPC\":\"1,37\",\"jQTO\":\"-\"},{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"2,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"4,00\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"30,00\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRP70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":37600.0,\"jQA\":null,\"jPC\":\"0,58\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":38400.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":37600.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46500.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46500.0,\"jQA\":null,\"jPC\":\"0,56\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":27600.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":7600.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46400.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":30800.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":10,\"jNOC\":\"SPINELLI\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"0,70\",\"jQTO\":\"-\"}]}]";
			//leituraAtual = "{\"IdDaRequisicao\":null,\"LivrosDeOferta\":[],\"LivrosDeOferta30\":[{\"Papel\":\"PETRD70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":21700.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,14\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"1,05\",\"jQTO\":\"-\"},{\"jNUC\":37,\"jNOC\":\"UM\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"1,01\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"0,95\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2900.0,\"jQA\":null,\"jPC\":\"0,80\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,52\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,30\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,26\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,24\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,21\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":4700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,21\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,24\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":3500.0,\"jQA\":null,\"jPC\":\"1,37\",\"jQTO\":\"-\"},{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"2,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"4,00\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"30,00\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRP70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":37600.0,\"jQA\":null,\"jPC\":\"0,58\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":38400.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":37600.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46500.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46500.0,\"jQA\":null,\"jPC\":\"0,56\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":27600.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":7600.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46400.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":30800.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":10,\"jNOC\":\"SPINELLI\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"0,70\",\"jQTO\":\"-\"}]}],\"LivrosAgregados\":[],\"LivrosDeNegocios\":[],\"MensagensParaCotacao\":[],\"MensagensParaCotacaoOrdem\":[],\"MensagensParaCotacaoRapida\":[],\"MensagensParaAcompanhamentoDeOrdem\":[],\"MensagensParaAcompanhamentoDeStartStop\":[],\"Avisos\":null,\"Alertas\":[]}";
			leituraAtual = "{\"IdDaRequisicao\":\"1457609495334\",\"LivrosDeOferta\":[],\"LivrosDeOferta30\":[{\"Papel\":\"PETR4\",\"OfertasDeCompra\":[{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":90,\"jNOC\":\"EASYINVEST\",\"jQT\":600.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":800.0,\"jQA\":null,\"jPC\":\"7,50\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":12500.0,\"jQA\":null,\"jPC\":\"7,50\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,50\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":5300.0,\"jQA\":null,\"jPC\":\"7,49\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":262,\"jNOC\":\"MIRAE ASSET\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,45\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,45\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"7,45\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,45\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,45\",\"jQTO\":\"-\"},{\"jNUC\":262,\"jNOC\":\"MIRAE ASSET\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,44\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":1300.0,\"jQA\":null,\"jPC\":\"7,44\",\"jQTO\":\"-\"},{\"jNUC\":23,\"jNOC\":\"CONCORDIA\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,44\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,42\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,41\",\"jQTO\":\"-\"},{\"jNUC\":10,\"jNOC\":\"SPINELLI\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"7,41\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,41\",\"jQTO\":\"-\"},{\"jNUC\":131,\"jNOC\":\"FATOR\",\"jQT\":22300.0,\"jQA\":null,\"jPC\":\"7,40\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"7,40\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"7,40\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,40\",\"jQTO\":\"-\"},{\"jNUC\":129,\"jNOC\":\"PLANNER\",\"jQT\":11500.0,\"jQA\":null,\"jPC\":\"7,40\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"7,40\",\"jQTO\":\"-\"},{\"jNUC\":23,\"jNOC\":\"CONCORDIA\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,39\",\"jQTO\":\"-\"},{\"jNUC\":23,\"jNOC\":\"CONCORDIA\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,37\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":1800.0,\"jQA\":null,\"jPC\":\"7,37\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,36\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":147,\"jNOC\":\"ATIVA\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,65\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,66\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":3100.0,\"jQA\":null,\"jPC\":\"7,68\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,68\",\"jQTO\":\"-\"},{\"jNUC\":59,\"jNOC\":\"J SAFRA\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":4300.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":70,\"jNOC\":\"HSBC\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":5500.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":23,\"jNOC\":\"CONCORDIA\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":4000.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,72\",\"jQTO\":\"-\"},{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,72\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":2700.0,\"jQA\":null,\"jPC\":\"7,72\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":6700.0,\"jQA\":null,\"jPC\":\"7,72\",\"jQTO\":\"-\"},{\"jNUC\":308,\"jNOC\":\"CLEAR\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,73\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,75\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":600.0,\"jQA\":null,\"jPC\":\"7,75\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":3300.0,\"jQA\":null,\"jPC\":\"7,75\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,75\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,75\",\"jQTO\":\"-\"},{\"jNUC\":308,\"jNOC\":\"CLEAR\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,75\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":8600.0,\"jQA\":null,\"jPC\":\"7,77\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"},{\"jNUC\":308,\"jNOC\":\"CLEAR\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":2300.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRD70\",\"OfertasDeCompra\":[{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,51\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"0,49\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,30\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,26\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,24\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,21\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,14\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,14\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,14\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,13\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,13\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,13\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"2,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"3,00\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"4,00\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRP70\",\"OfertasDeCompra\":[],\"OfertasDeVenda\":[]},{\"Papel\":\"PETRD73\",\"OfertasDeCompra\":[],\"OfertasDeVenda\":[]},{\"Papel\":\"PETRP73\",\"OfertasDeCompra\":[],\"OfertasDeVenda\":[]}],\"LivrosAgregados\":[],\"LivrosDeNegocios\":[],\"MensagensParaCotacao\":[],\"MensagensParaCotacaoOrdem\":[],\"MensagensParaCotacaoRapida\":[{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"050000870\",\"jCN\":\"PETR4\",\"jDN\":\"20160309\",\"jHN\":\"18:08:06\",\"jCC\":\"58\",\"jCV\":\"147\",\"jCCN\":\"SOCOPA\",\"jCVN\":\"ATIVA\",\"jMPC\":\"7,55\",\"jMPV\":\"7,65\",\"jQMPC\":\"200\",\"jQMPV\":\"300\",\"jQAMC\":\"900\",\"jQAMV\":\"300\",\"jPC\":\"7,60\",\"jPTA\":\"0,00\",\"jVTO\":\"0,00\",\"jDTO\":\"10101000000\",\"jPM\":\"7,61\",\"jQT\":\"\",\"jXD\":\"7,780\",\"jND\":\"7,360\",\"jLA\":\"646121853\",\"jNN\":\"57560\",\"jIV\":\"\",\"jVR\":\"+1,74\",\"jEP\":\"0\",\"jVA\":\"7,70\",\"jVF\":\"7,60\",\"jIO\":\"X\",\"jPE\":\"0,00\",\"jDE\":\"00000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"050006688\",\"jCN\":\"PETRD70\",\"jDN\":\"20160309\",\"jHN\":\"18:15:00\",\"jCC\":\"39\",\"jCV\":\"58\",\"jCCN\":\"AGORA\",\"jCVN\":\"SOCOPA\",\"jMPC\":\"0,90\",\"jMPV\":\"2,70\",\"jQMPC\":\"1200\",\"jQMPV\":\"500\",\"jQAMC\":\"1200\",\"jQAMV\":\"500\",\"jPC\":\"1,15\",\"jPTA\":\"0,00\",\"jVTO\":\"-,00\",\"jDTO\":\"10101000000\",\"jPM\":\"1,26\",\"jQT\":\"\",\"jXD\":\"1,360\",\"jND\":\"1,090\",\"jLA\":\"1021574\",\"jNN\":\"348\",\"jIV\":\"-\",\"jVR\":\"-0,86\",\"jEP\":\"0\",\"jVA\":\"1,19\",\"jVF\":\"1,15\",\"jIO\":\"C\",\"jPE\":\"7,00\",\"jDE\":\"00000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"050006688\",\"jCN\":\"PETRP70\",\"jDN\":\"20160309\",\"jHN\":\"16:13:51\",\"jCC\":\"\",\"jCV\":\"\",\"jCCN\":null,\"jCVN\":null,\"jMPC\":\"0,00\",\"jMPV\":\"0,00\",\"jQMPC\":\"\",\"jQMPV\":\"\",\"jQAMC\":\"n/d\",\"jQAMV\":\"n/d\",\"jPC\":\"0,56\",\"jPTA\":\"0,00\",\"jVTO\":\"-,00\",\"jDTO\":\"10101000000\",\"jPM\":\"0,54\",\"jQT\":\"\",\"jXD\":\"0,600\",\"jND\":\"0,510\",\"jLA\":\"41733\",\"jNN\":\"39\",\"jIV\":\"-\",\"jVR\":\"-11,11\",\"jEP\":\"0\",\"jVA\":\"0,60\",\"jVF\":\"0,56\",\"jIO\":\"P\",\"jPE\":\"7,00\",\"jDE\":\"00000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"050006314\",\"jCN\":\"PETRD73\",\"jDN\":\"20160309\",\"jHN\":\"11:35:26\",\"jCC\":\"\",\"jCV\":\"\",\"jCCN\":null,\"jCVN\":null,\"jMPC\":\"0,00\",\"jMPV\":\"0,00\",\"jQMPC\":\"\",\"jQMPV\":\"\",\"jQAMC\":\"n/d\",\"jQAMV\":\"n/d\",\"jPC\":\"1,10\",\"jPTA\":\"0,00\",\"jVTO\":\"0,00\",\"jDTO\":\"10101000000\",\"jPM\":\"1,12\",\"jQT\":\"\",\"jXD\":\"1,140\",\"jND\":\"1,100\",\"jLA\":\"336\",\"jNN\":\"3\",\"jIV\":\"\",\"jVR\":\"+1,85\",\"jEP\":\"0\",\"jVA\":\"1,14\",\"jVF\":\"1,10\",\"jIO\":\"C\",\"jPE\":\"7,30\",\"jDE\":\"30000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"050006314\",\"jCN\":\"PETRP73\",\"jDN\":\"20160309\",\"jHN\":\"12:02:12\",\"jCC\":\"\",\"jCV\":\"\",\"jCCN\":null,\"jCVN\":null,\"jMPC\":\"0,00\",\"jMPV\":\"0,00\",\"jQMPC\":\"\",\"jQMPV\":\"\",\"jQAMC\":\"n/d\",\"jQAMV\":\"n/d\",\"jPC\":\"0,69\",\"jPTA\":\"0,00\",\"jVTO\":\"-,00\",\"jDTO\":\"10101000000\",\"jPM\":\"0,68\",\"jQT\":\"\",\"jXD\":\"0,690\",\"jND\":\"0,680\",\"jLA\":\"14310\",\"jNN\":\"2\",\"jIV\":\"-\",\"jVR\":\"-5,48\",\"jEP\":\"0\",\"jVA\":\"0,68\",\"jVF\":\"0,69\",\"jIO\":\"P\",\"jPE\":\"7,30\",\"jDE\":\"30000000\",\"jDC\":null,\"jDV\":\"\"}],\"MensagensParaAcompanhamentoDeOrdem\":[],\"MensagensParaAcompanhamentoDeStartStop\":[],\"Avisos\":null,\"Alertas\":[]}";
			
			
			LinkedHashMap<String, IAtivoMini> mapAtivos = new LinkedHashMap<>();
			mapAtivos.put("PETR4", new AcaoMini("PETR4"));
			int qtdDiasVenc1 = 39;
			mapAtivos.put("PETRD70", new OpcaoMini("PETRD70", new BigDecimal("7.00"), qtdDiasVenc1, true));
			mapAtivos.put("PETRP70", new OpcaoMini("PETRP70", new BigDecimal("7.00"), qtdDiasVenc1, false));
			mapAtivos.put("PETRD73", new OpcaoMini("PETRD73", new BigDecimal("7.30"), qtdDiasVenc1, true));
			mapAtivos.put("PETRP73", new OpcaoMini("PETRP73", new BigDecimal("7.30"), qtdDiasVenc1, false));
			
			BigDecimal taxaDeJuros = new BigDecimal("0.1425");
			BigDecimal percReferencia = new BigDecimal("0.01");
			Gson gson = new Gson();
			
			//LivroOferta[] lo2 = gson.fromJson(leituraAtual, LivroOferta[].class);
			LeituraLivroOferta llo =  gson.fromJson(leituraAtual, LeituraLivroOferta.class);
			
			/*
			LivroOferta loTemp = new LivroOferta();
			loTemp.Papel = "PETR4";
			loTemp.OfertasDeVenda = new ArrayList<Oferta>();
			loTemp.OfertasDeVenda.add(new Oferta());
			loTemp.OfertasDeVenda.get(0).jPC = "7,00";
			*/
			if (llo != null && llo.LivrosDeOferta30 != null && llo.LivrosDeOferta30.size() > 0) {
				//llo.LivrosDeOferta30.add(0, loTemp);
				BigDecimal precoAcao = new BigDecimal(llo.LivrosDeOferta30.get(0).OfertasDeVenda.get(0).jPC.replace(",", "."));
				for (int i = 1; i < llo.LivrosDeOferta30.size(); i = i + 2) {
					LivroOferta loCall = llo.LivrosDeOferta30.get(i);
					// recupera a put
					LivroOferta loPut = llo.LivrosDeOferta30.get(i + 1);
					// se nao tiver a oferta de compra da call e oferta de venda da put vai para o proximo par
					if (!(loCall.OfertasDeCompra.size() > 0 && loPut.OfertasDeVenda.size() > 0))
						continue;
					// recupera o melhor preco de compra da CALL
					BigDecimal precoMelhorCompraCall = new BigDecimal(loCall.OfertasDeCompra.get(0).jPC.replace(",", "."));
					// recupera a call
					OpcaoMini omCall = (OpcaoMini) mapAtivos.get(loCall.Papel);
					// recupera o preco exercicio
					BigDecimal precoExercicioOpcao = omCall.getPrecoExercicio();
					// recupera a qtdade dias para vencimento
					int qtdDiasParaVencimento = omCall.getQtdDiasParaVencimento();
					// recupera o melhor preco de venda da PUT 
					BigDecimal precoMelhorVendaPUT =  new BigDecimal(loPut.OfertasDeVenda.get(0).jPC.replace(",", "."));
					// verifica se tem a anomalia
					boolean temAnomalia = temAnomalia1(precoAcao, precoExercicioOpcao, qtdDiasParaVencimento, taxaDeJuros, precoMelhorCompraCall, precoMelhorVendaPUT, percReferencia);
					if (temAnomalia) {
						// notifica por email ou por aplicacao
					}
				}
				LogUtil.log("FIM");
			}
			long fim = System.currentTimeMillis();
			LogUtil.log("tempo proc: " + (fim - inicio));
		}

	}
	

	public static void main(String[] args) {
		ProcessarLivroOfertas plo = new MonitorLivroOfertas().new ProcessarLivroOfertas();
		plo.processarLivroOfertas();
	}
	
	public static boolean temAnomalia1(BigDecimal precoAcao, BigDecimal precoExercicioOpcao, 
			int qtdDiasParaVencimento, BigDecimal taxaDeJuros, BigDecimal precoMelhorCompraCall, BigDecimal precoMelhorVendaPUT, BigDecimal percReferencia) {
		// recupera a volatilidade da call de acordo ao melhor preco de compra da Call
		BigDecimal volatilidadeCall = BlackScholes.calculaVolatilidade(true, precoAcao, precoExercicioOpcao, qtdDiasParaVencimento, precoMelhorCompraCall, taxaDeJuros);
		// recupera o preco BS da PUT se tivesse a mesma volatilidade da CALL
		BigDecimal precoBSPut = BlackScholes.blackScholes(false, precoAcao, precoExercicioOpcao, qtdDiasParaVencimento, taxaDeJuros, volatilidadeCall);
		// calcula o percentual de diferenca
		BigDecimal percentualDif = precoBSPut.subtract(precoMelhorVendaPUT).divide(precoAcao, RoundingMode.HALF_EVEN); 
		if (percentualDif.compareTo(percReferencia) > 0) {
			LogUtil.log("encontrou anomalia: " + percentualDif);
			return true;
		}
		return false;
	}
		
	private interface IAtivoMini {
		public String getCodigoAtivo();
		public boolean ehOpcao();
	}
	
	private class AcaoMini implements IAtivoMini {

		private String codigoAtivo;
		
		public AcaoMini(String codigoAtivo) {
			this.codigoAtivo = codigoAtivo;
		}
		
		@Override
		public String getCodigoAtivo() {
			// TODO Auto-generated method stub
			return codigoAtivo;
		}

		@Override
		public boolean ehOpcao() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
	
	private class OpcaoMini implements IAtivoMini {

		private String codigoAtivo;
		private BigDecimal precoExercicio;
		private int qtdDiasParaVencimento;
		private boolean ehCall;
		
		public OpcaoMini(String codigoAtivo, BigDecimal precoExercicio, int qtdDiasParaVencimento, boolean ehCall) {
			this.codigoAtivo = codigoAtivo;
			this.precoExercicio = precoExercicio;
			this.qtdDiasParaVencimento = qtdDiasParaVencimento;
			this.ehCall = ehCall;
		}
		
		@Override
		public String getCodigoAtivo() {
			// TODO Auto-generated method stub
			return codigoAtivo;
		}

		public BigDecimal getPrecoExercicio() {
			return precoExercicio;
		}

		public void setPrecoExercicio(BigDecimal precoExercicio) {
			this.precoExercicio = precoExercicio;
		}

		public int getQtdDiasParaVencimento() {
			return qtdDiasParaVencimento;
		}

		public void setQtdDiasParaVencimento(int qtdDiasParaVencimento) {
			this.qtdDiasParaVencimento = qtdDiasParaVencimento;
		}
		public boolean isEhCall() {
			return ehCall;
		}
		public void setEhCall(boolean ehCall) {
			this.ehCall = ehCall;
		}

		@Override
		public boolean ehOpcao() {
			// TODO Auto-generated method stub
			return true;
		}
		
	}
	
	
	private class LeituraLivroOferta {
		protected List<LivroOferta> LivrosDeOferta30;
	}
	
	private class LivroOferta {
		protected String Papel;
		protected List<Oferta> OfertasDeCompra;
		protected List<Oferta> OfertasDeVenda;
		
		@Override
		public String toString() {
			String melhorOfertaCompra = (OfertasDeCompra != null && OfertasDeCompra.size() > 0) ? OfertasDeCompra.get(0).jQT + " - " + OfertasDeCompra.get(0).jPC : "";
			String melhorOfertaVenda = (OfertasDeVenda != null && OfertasDeVenda.size() > 0) ? OfertasDeVenda.get(0).jQT + " - " + OfertasDeVenda.get(0).jPC : "";
			return Papel + " - Compra: " + melhorOfertaCompra + " ; Venda: " + melhorOfertaVenda;
		}
	}
	
	private class Oferta {
		protected int jNUC;
		protected String jNOC;
		protected long jQT;
		protected String jQA;
		protected String jPC;
		protected String jQTO;
	}
	
}
