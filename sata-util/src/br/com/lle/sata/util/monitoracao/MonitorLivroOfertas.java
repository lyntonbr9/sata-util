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
			String leituraAtual = getConteudoRecente();
			//LogUtil.log(leituraAtual);
			//leituraAtual = "{\"Papel\":\"PETRD70\"}";
			//leituraAtual = "{\"Papel\":\"PETRD70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":21700.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,14\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"1,05\",\"jQTO\":\"-\"},{\"jNUC\":37,\"jNOC\":\"UM\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"1,01\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"0,95\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2900.0,\"jQA\":null,\"jPC\":\"0,80\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,52\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,30\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,26\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,24\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,21\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":4700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,21\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,24\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":3500.0,\"jQA\":null,\"jPC\":\"1,37\",\"jQTO\":\"-\"},{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"2,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"4,00\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"30,00\",\"jQTO\":\"-\"}]}";
			//leituraAtual = "[{\"Papel\":\"PETRD70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":21700.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,14\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"1,05\",\"jQTO\":\"-\"},{\"jNUC\":37,\"jNOC\":\"UM\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"1,01\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"0,95\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2900.0,\"jQA\":null,\"jPC\":\"0,80\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,52\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,30\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,26\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,24\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,21\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":4700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,21\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,24\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":3500.0,\"jQA\":null,\"jPC\":\"1,37\",\"jQTO\":\"-\"},{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"2,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"4,00\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"30,00\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRP70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":37600.0,\"jQA\":null,\"jPC\":\"0,58\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":38400.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":37600.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46500.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46500.0,\"jQA\":null,\"jPC\":\"0,56\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":27600.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":7600.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46400.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":30800.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":10,\"jNOC\":\"SPINELLI\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"0,70\",\"jQTO\":\"-\"}]}]";
			leituraAtual = "{\"IdDaRequisicao\":null,\"LivrosDeOferta\":[],\"LivrosDeOferta30\":[{\"Papel\":\"PETRD70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":21700.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,14\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"1,05\",\"jQTO\":\"-\"},{\"jNUC\":37,\"jNOC\":\"UM\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"1,01\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"0,95\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2900.0,\"jQA\":null,\"jPC\":\"0,80\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,52\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,30\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,26\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,24\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,21\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":4700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,21\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,24\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":3500.0,\"jQA\":null,\"jPC\":\"1,37\",\"jQTO\":\"-\"},{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"2,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"4,00\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"30,00\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRP70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":37600.0,\"jQA\":null,\"jPC\":\"0,58\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":38400.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":37600.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46500.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46500.0,\"jQA\":null,\"jPC\":\"0,56\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":27600.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":7600.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46400.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":30800.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":10,\"jNOC\":\"SPINELLI\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"0,70\",\"jQTO\":\"-\"}]}],\"LivrosAgregados\":[],\"LivrosDeNegocios\":[],\"MensagensParaCotacao\":[],\"MensagensParaCotacaoOrdem\":[],\"MensagensParaCotacaoRapida\":[],\"MensagensParaAcompanhamentoDeOrdem\":[],\"MensagensParaAcompanhamentoDeStartStop\":[],\"Avisos\":null,\"Alertas\":[]}";
			
			
			LinkedHashMap<String, IAtivoMini> mapAtivos = new LinkedHashMap<>();
			mapAtivos.put("PETR4", new AcaoMini("PETR4"));
			mapAtivos.put("PETRD70", new OpcaoMini("PETRD70", new BigDecimal("7.00"), 40, true));
			mapAtivos.put("PETRP70", new OpcaoMini("PETRP70", new BigDecimal("7.00"), 40, false));
			
			
			Gson gson = new Gson();
			//LivroOferta[] lo2 = gson.fromJson(leituraAtual, LivroOferta[].class);
			LeituraLivroOferta llo =  gson.fromJson(leituraAtual, LeituraLivroOferta.class);
			if (llo != null && llo.LivrosDeOferta30 != null && llo.LivrosDeOferta30.size() > 0) {
				LogUtil.log(llo.LivrosDeOferta30.toString());
				// recupera o preco da acao
				//BigDecimal precoAcao = new BigDecimal(llo.LivrosDeOferta30.get(0).OfertasDeCompra.get(0).jPC.replace(",", "."));
				BigDecimal precoAcao = new BigDecimal("7.20");
				// recupera a call
				LivroOferta loCall = llo.LivrosDeOferta30.get(0);
				// recupera a put
				LivroOferta loPut = llo.LivrosDeOferta30.get(1);
				// recupera a volatilidade da call de acordo ao melhor preco de compra da Call
				//BigDecimal volatilidadeCall = new BigDecimal("0.5");
				// recupera o melhor preco de compra da CALL
				BigDecimal precoMelhorCompraCall = new BigDecimal(loCall.OfertasDeCompra.get(0).jPC.replace(",", "."));
				// recupera o melhor preco de venda da PUT 
				BigDecimal precoMelhorVendaPUT =  new BigDecimal(loPut.OfertasDeVenda.get(0).jPC.replace(",", "."));
				BigDecimal divisao = precoMelhorCompraCall.subtract(precoMelhorVendaPUT).divide(precoAcao, RoundingMode.HALF_EVEN); 
				if (divisao.compareTo(new BigDecimal("0.01")) > 0) {
					LogUtil.log("encontrou anomalia");
				}
				
				// recupera o valor de venda da put caso tenha a mesma volatilidade da call
				//BlackScholes.blackScholes(false, precoAcao, precoExercicioOpcao, diasParaVencimento, volatilidade, taxaJuros)
				// OpcaoMini om = (OpcaoMini) mapAtivos.get(loPut.Papel);
				
				//BigDecimal precoJustoPut = BlackScholes.blackScholes(false, precoAcao, om.getPrecoExercicio(), om.getQtdDiasParaVencimento(), volatilidadeCall, new BigDecimal("14.25"));
				
			}
			long fim = System.currentTimeMillis();
			LogUtil.log("tempo proc: " + (fim - inicio));
		}

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
	
	
	public static void main(String[] args) {
		ProcessarLivroOfertas plo = new MonitorLivroOfertas().new ProcessarLivroOfertas();
		plo.processarLivroOfertas();
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
			// TODO Auto-generated method stub
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
