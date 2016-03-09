package br.com.lle.sata.util.monitoracao;

import static br.com.lle.sata.util.StringUtil.concat;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import br.com.lle.sata.util.LogUtil;

public class MonitorLivroOfertas {
	
	private static Map<Long, String> ofertaPapeis = new LinkedHashMap<>(); 
	
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
			//conn.setRequestMethod("GET");
			//conn.setRequestProperty("charset", "UTF-8");
			
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
		return new URL(targetURL).openConnection();
	}
	
	private static URLConnection getConnectionBuscaRespostas() throws IOException  {
		return urlBuscarRespostas.openConnection();
	}
	
	private static String URL_BUSCAR_RESPOSTAS = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=BuscarRespostas";
	
	private static String URL_ASSINAR_PAPEL = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=AssinarPapel&Papel={0}&Finalidade=LivroDeOferta30";
	
	private static String URL_LIBERAR_PAPEL = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=LiberarPapel&Papel={0}&Finalidade=livrodeoferta30";
	
	private static String[] PAPEIS_MONITORADOS = new String[] {"PETR4","PETRD70","PETRP70"};
	 	
	private static URL urlBuscarRespostas = null;
	
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
	
	private static String conteudoRecente;
	
	public synchronized static String getConteudoRecente(){
		return conteudoRecente;
	}
	
	public synchronized static void setConteudoRecente(String conteudo){
		conteudoRecente = conteudo;
	}
	
	private static long MIN_TIMEOUT_LEITURA = 2000;
	
	private static long TIMEOUT_MONITORACAO = 10000;
	
	public static void main(String[] args) throws InterruptedException {
		//String urlCotacao = "https://www.gradualinvestimentos.com.br/Async/Login.aspx";
		
		//String url = "https://www.gradualinvestimentos.com.br";
		
		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=BuscarRespostas";
		
		
		String aspnetSessionID = "ASP.NET_SessionId=gp3jrhcvlzjb0g5sbwl3cc32";
		
		Hashtable<String, String> ht = new Hashtable<>();
		//ht.put("Cookie", "ASP.NET_SessionId=onfsl5hhbkhculkggfucgzra");
		ht.put("Cookie", aspnetSessionID);
		
		
		// assina os papeis para monitoracao
		for (String papel : PAPEIS_MONITORADOS) {
			String assinarPapel = MessageFormat.format(URL_ASSINAR_PAPEL, papel);
			String response = MonitorLivroOfertas.GET(assinarPapel, ht);
			LogUtil.log(response);
		}
		
		
		/*
		for (int i = 0; i < 100; i++) {
			// consulta dos dados
			MONITORAR("ASP.NET_SessionId=gofn44gbx1q3doiqks2rjeh2");
		}
		*/
		
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
			System.out.println(getConteudoRecente());
		}

	}
	
}
