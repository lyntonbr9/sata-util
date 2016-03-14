package br.com.lle.sata.util.monitoracao;

import static br.com.lle.sata.util.StringUtil.concat;

import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.TimerTask;

import br.com.lle.sata.alert.domain.UsuarioAlert;
import br.com.lle.sata.alert.interfaces.INotificacao;
import br.com.lle.sata.alert.notificacao.Notificacao;
import br.com.lle.sata.mobile.core.util.BlackScholes;
import br.com.lle.sata.util.LogUtil;

import com.google.gson.Gson;

public class MonitorLivroOfertas {
	
	private static Map<Long, String> OFERTA_PAPEIS = new LinkedHashMap<>(); 
	
	private static String URL_BUSCAR_RESPOSTAS = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=BuscarRespostas";
	
	private static String URL_ASSINAR_PAPEL = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=AssinarPapel&Papel={0}&Finalidade=LivroDeOferta30";
	
	private static String URL_LIBERAR_PAPEL = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=LiberarPapel&Papel={0}&Finalidade=livrodeoferta30";
	 	
	private static URL urlBuscarRespostas = null;
	
	private static long MIN_TIMEOUT_LEITURA = 2000;
	
	private static long TIMEOUT_MONITORACAO = 4000;
	
	private static String conteudoRecente;
	
	private static Map<String, IAtivoMini> MAP_ATIVOS = new LinkedHashMap<>();
	
	private static List<UsuarioAlert> USUARIOS = new ArrayList<>();
	
	static {
		iniciarDadosStaticos();
	}
	
	private static void iniciarDadosStaticos() {
		try {
			urlBuscarRespostas = new URL(URL_BUSCAR_RESPOSTAS);
			
			int qtdDiasVenc1 = 38;
			MAP_ATIVOS.put("PETR4", new MonitorLivroOfertas().new AcaoMini("PETR4"));
			MAP_ATIVOS.put("PETRD78", new MonitorLivroOfertas().new OpcaoMini("PETRD78", new BigDecimal("7.80"), qtdDiasVenc1, true));
			MAP_ATIVOS.put("PETRP78", new MonitorLivroOfertas().new OpcaoMini("PETRP78", new BigDecimal("7.80"), qtdDiasVenc1, false));
			MAP_ATIVOS.put("PETRD80", new MonitorLivroOfertas().new OpcaoMini("PETRD80", new BigDecimal("8.00"), qtdDiasVenc1, true));
			MAP_ATIVOS.put("PETRP80", new MonitorLivroOfertas().new OpcaoMini("PETRP80", new BigDecimal("8.00"), qtdDiasVenc1, false));
			MAP_ATIVOS.put("PETRD35", new MonitorLivroOfertas().new OpcaoMini("PETRD35", new BigDecimal("8.40"), qtdDiasVenc1, true));
			MAP_ATIVOS.put("PETRP35", new MonitorLivroOfertas().new OpcaoMini("PETRP35", new BigDecimal("8.40"), qtdDiasVenc1, false));
			
			USUARIOS.add(new UsuarioAlert("lyntonbr", "APA91bEStrVI65Qf8awLjXdAX1VLcmaEpVNAorWF_ZyWC-28txG-EG-bAiLANV4K2oPPyZIrHWBh0-eoL9e7n3bMMzKdE2oTOs7Dco_zDTeExCmEawzE368CRVBqriEaAFBLdxkht9fiSphe-EENecK6B9teVM4Z_Q"));
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
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
	
	private static Gson gson = new Gson();
	
	
	public static void main(String[] args) throws InterruptedException, IOException {

		//String urlCotacao = "https://www.gradualinvestimentos.com.br/Async/Login.aspx";
		
		//String url = "https://www.gradualinvestimentos.com.br";
		
		//String url = "https://homebroker.gradualinvestimentos.com.br/Backend/CentralizadorDeRespostas.aspx?Acao=BuscarRespostas";

		String aspnetSessionID = "ASP.NET_SessionId=u2wh5yv20biopq5uxwbr4c12;";
		
		Hashtable<String, String> ht = new Hashtable<>();
		
		ht.put("Cookie", aspnetSessionID);
		
		/*
		for (IAtivoMini ativoMini : MAP_ATIVOS.values()) {
			String assinarPapel = MessageFormat.format(URL_ASSINAR_PAPEL, ativoMini.getCodigoAtivo());
			String response = MonitorLivroOfertas.GET(assinarPapel, ht);
			LogUtil.log(response);
		}
		*/
		// cria o objeto que processa os dados lidos
		MonitorLivroOfertas.ProcessarLivroOfertas mp = new MonitorLivroOfertas().new ProcessarLivroOfertas();
		// cria uma thread para processar o livro de ofertas
		Thread t = new Thread(mp);
		// inicia o processamento
		t.start();
		
		/*
		// cria um timer para fazer a leitura
		Timer timer = new Timer(true);
		// instancia a task que ira fazer a leitura
		TimerTask leituraLivroOfertaTask = new MonitorLivroOfertas().new LivroOfertasTask(aspnetSessionID);
		// vai rodar a tarefa de leitura a cada tempo definido
		timer.scheduleAtFixedRate(leituraLivroOfertaTask, 0, MIN_TIMEOUT_LEITURA);
		// para a thread principal por um tempo limitado
		Thread.sleep(TIMEOUT_MONITORACAO);
		
		// para o timer
		timer.cancel();
		timer.purge();
		*/
		
		Thread.sleep(TIMEOUT_MONITORACAO);
		
		// para a execucao da thread
		mp.terminate();
		t.join();
		
		System.out.println("FIM PROCESSAMENTO!");
		
		/*
		// Salva em arquivo a leitura do livro de ofertas
		FileUtils.writeLines(new File("LivroOfertas_" + System.currentTimeMillis()), OFERTA_PAPEIS.values());
		
		// libera os papeis monitorados
		for (IAtivoMini ativoMini : MAP_ATIVOS.values()) {
			String liberarPapel = MessageFormat.format(URL_LIBERAR_PAPEL, ativoMini.getCodigoAtivo());
			String response = MonitorLivroOfertas.GET(liberarPapel, ht);
			LogUtil.log(response);
		}
		*/
		
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
			setConteudoRecente(sc.nextLine());
			OFERTA_PAPEIS.put(tempoLeitura, getConteudoRecente());
			LogUtil.log(tempoLeitura + ": " + getConteudoRecente());
			
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
		
		private boolean isRunning = true;
		
		public void terminate() {
			this.isRunning = false;
		}
		
		@Override
		public void run() {

			while (this.isRunning) {
			
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
						Thread.currentThread().interrupt();
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		}
		
		
		public void processarLivroOfertas() {
			long inicio = System.currentTimeMillis();
			// recupera a leitura mais recente
			String leituraAtual = getConteudoRecente();
			
			/*
			String arquivo = "LivroOfertas_1457726659704";
			List<String> linhas = null;
			try {
				linhas = FileUtils.readLines(new File(arquivo));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			String leituraAtual = linhas.get(0);
			//LogUtil.log(leituraAtual);
			*/
			//leituraAtual = "{\"Papel\":\"PETRD70\"}";
			//leituraAtual = "{\"Papel\":\"PETRD70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":21700.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,14\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"1,05\",\"jQTO\":\"-\"},{\"jNUC\":37,\"jNOC\":\"UM\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"1,01\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"0,95\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2900.0,\"jQA\":null,\"jPC\":\"0,80\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,52\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,30\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,26\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,24\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,21\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":4700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,21\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,24\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":3500.0,\"jQA\":null,\"jPC\":\"1,37\",\"jQTO\":\"-\"},{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"2,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"4,00\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"30,00\",\"jQTO\":\"-\"}]}";
			//leituraAtual = "[{\"Papel\":\"PETRD70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":21700.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,14\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"1,05\",\"jQTO\":\"-\"},{\"jNUC\":37,\"jNOC\":\"UM\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"1,01\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"0,95\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2900.0,\"jQA\":null,\"jPC\":\"0,80\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,52\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,30\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,26\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,24\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,21\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":4700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,21\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,24\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":3500.0,\"jQA\":null,\"jPC\":\"1,37\",\"jQTO\":\"-\"},{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"2,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"4,00\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"30,00\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRP70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":37600.0,\"jQA\":null,\"jPC\":\"0,58\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":38400.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":37600.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46500.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46500.0,\"jQA\":null,\"jPC\":\"0,56\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":27600.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":7600.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46400.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":30800.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":10,\"jNOC\":\"SPINELLI\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"0,70\",\"jQTO\":\"-\"}]}]";
			//leituraAtual = "{\"IdDaRequisicao\":null,\"LivrosDeOferta\":[],\"LivrosDeOferta30\":[{\"Papel\":\"PETRD70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":21700.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,14\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"1,05\",\"jQTO\":\"-\"},{\"jNUC\":37,\"jNOC\":\"UM\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"1,01\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"0,95\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2900.0,\"jQA\":null,\"jPC\":\"0,80\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,52\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,30\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,26\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,24\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,21\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":4700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":25700.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":31800.0,\"jQA\":null,\"jPC\":\"1,21\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,24\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":3500.0,\"jQA\":null,\"jPC\":\"1,37\",\"jQTO\":\"-\"},{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"2,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"4,00\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":8200.0,\"jQA\":null,\"jPC\":\"30,00\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRP70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":37600.0,\"jQA\":null,\"jPC\":\"0,58\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":38400.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":37600.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46500.0,\"jQA\":null,\"jPC\":\"0,57\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46500.0,\"jQA\":null,\"jPC\":\"0,56\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":27600.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":7600.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":46400.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":30800.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":10,\"jNOC\":\"SPINELLI\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"0,70\",\"jQTO\":\"-\"}]}],\"LivrosAgregados\":[],\"LivrosDeNegocios\":[],\"MensagensParaCotacao\":[],\"MensagensParaCotacaoOrdem\":[],\"MensagensParaCotacaoRapida\":[],\"MensagensParaAcompanhamentoDeOrdem\":[],\"MensagensParaAcompanhamentoDeStartStop\":[],\"Avisos\":null,\"Alertas\":[]}";
			//leituraAtual = "{\"IdDaRequisicao\":\"1457609495334\",\"LivrosDeOferta\":[],\"LivrosDeOferta30\":[{\"Papel\":\"PETR4\",\"OfertasDeCompra\":[{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":90,\"jNOC\":\"EASYINVEST\",\"jQT\":600.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":800.0,\"jQA\":null,\"jPC\":\"7,50\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":12500.0,\"jQA\":null,\"jPC\":\"7,50\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,50\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":5300.0,\"jQA\":null,\"jPC\":\"7,49\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":262,\"jNOC\":\"MIRAE ASSET\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,45\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,45\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"7,45\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,45\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,45\",\"jQTO\":\"-\"},{\"jNUC\":262,\"jNOC\":\"MIRAE ASSET\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,44\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":1300.0,\"jQA\":null,\"jPC\":\"7,44\",\"jQTO\":\"-\"},{\"jNUC\":23,\"jNOC\":\"CONCORDIA\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,44\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,42\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,41\",\"jQTO\":\"-\"},{\"jNUC\":10,\"jNOC\":\"SPINELLI\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"7,41\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,41\",\"jQTO\":\"-\"},{\"jNUC\":131,\"jNOC\":\"FATOR\",\"jQT\":22300.0,\"jQA\":null,\"jPC\":\"7,40\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"7,40\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"7,40\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,40\",\"jQTO\":\"-\"},{\"jNUC\":129,\"jNOC\":\"PLANNER\",\"jQT\":11500.0,\"jQA\":null,\"jPC\":\"7,40\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"7,40\",\"jQTO\":\"-\"},{\"jNUC\":23,\"jNOC\":\"CONCORDIA\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,39\",\"jQTO\":\"-\"},{\"jNUC\":23,\"jNOC\":\"CONCORDIA\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,37\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":1800.0,\"jQA\":null,\"jPC\":\"7,37\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,36\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":147,\"jNOC\":\"ATIVA\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,65\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,66\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":3100.0,\"jQA\":null,\"jPC\":\"7,68\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,68\",\"jQTO\":\"-\"},{\"jNUC\":59,\"jNOC\":\"J SAFRA\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":4300.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":70,\"jNOC\":\"HSBC\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":5500.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":23,\"jNOC\":\"CONCORDIA\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":4000.0,\"jQA\":null,\"jPC\":\"7,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,72\",\"jQTO\":\"-\"},{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,72\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":2700.0,\"jQA\":null,\"jPC\":\"7,72\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":6700.0,\"jQA\":null,\"jPC\":\"7,72\",\"jQTO\":\"-\"},{\"jNUC\":308,\"jNOC\":\"CLEAR\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,73\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,75\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":600.0,\"jQA\":null,\"jPC\":\"7,75\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":3300.0,\"jQA\":null,\"jPC\":\"7,75\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,75\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,75\",\"jQTO\":\"-\"},{\"jNUC\":308,\"jNOC\":\"CLEAR\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,75\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":8600.0,\"jQA\":null,\"jPC\":\"7,77\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"},{\"jNUC\":308,\"jNOC\":\"CLEAR\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":2300.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"7,78\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRD70\",\"OfertasDeCompra\":[{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,51\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"0,49\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,30\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,26\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,24\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,21\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,15\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,14\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,14\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,14\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,13\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"0,13\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,13\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"2,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"3,00\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"4,00\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRP70\",\"OfertasDeCompra\":[],\"OfertasDeVenda\":[]},{\"Papel\":\"PETRD73\",\"OfertasDeCompra\":[],\"OfertasDeVenda\":[]},{\"Papel\":\"PETRP73\",\"OfertasDeCompra\":[],\"OfertasDeVenda\":[]}],\"LivrosAgregados\":[],\"LivrosDeNegocios\":[],\"MensagensParaCotacao\":[],\"MensagensParaCotacaoOrdem\":[],\"MensagensParaCotacaoRapida\":[{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"050000870\",\"jCN\":\"PETR4\",\"jDN\":\"20160309\",\"jHN\":\"18:08:06\",\"jCC\":\"58\",\"jCV\":\"147\",\"jCCN\":\"SOCOPA\",\"jCVN\":\"ATIVA\",\"jMPC\":\"7,55\",\"jMPV\":\"7,65\",\"jQMPC\":\"200\",\"jQMPV\":\"300\",\"jQAMC\":\"900\",\"jQAMV\":\"300\",\"jPC\":\"7,60\",\"jPTA\":\"0,00\",\"jVTO\":\"0,00\",\"jDTO\":\"10101000000\",\"jPM\":\"7,61\",\"jQT\":\"\",\"jXD\":\"7,780\",\"jND\":\"7,360\",\"jLA\":\"646121853\",\"jNN\":\"57560\",\"jIV\":\"\",\"jVR\":\"+1,74\",\"jEP\":\"0\",\"jVA\":\"7,70\",\"jVF\":\"7,60\",\"jIO\":\"X\",\"jPE\":\"0,00\",\"jDE\":\"00000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"050006688\",\"jCN\":\"PETRD70\",\"jDN\":\"20160309\",\"jHN\":\"18:15:00\",\"jCC\":\"39\",\"jCV\":\"58\",\"jCCN\":\"AGORA\",\"jCVN\":\"SOCOPA\",\"jMPC\":\"0,90\",\"jMPV\":\"2,70\",\"jQMPC\":\"1200\",\"jQMPV\":\"500\",\"jQAMC\":\"1200\",\"jQAMV\":\"500\",\"jPC\":\"1,15\",\"jPTA\":\"0,00\",\"jVTO\":\"-,00\",\"jDTO\":\"10101000000\",\"jPM\":\"1,26\",\"jQT\":\"\",\"jXD\":\"1,360\",\"jND\":\"1,090\",\"jLA\":\"1021574\",\"jNN\":\"348\",\"jIV\":\"-\",\"jVR\":\"-0,86\",\"jEP\":\"0\",\"jVA\":\"1,19\",\"jVF\":\"1,15\",\"jIO\":\"C\",\"jPE\":\"7,00\",\"jDE\":\"00000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"050006688\",\"jCN\":\"PETRP70\",\"jDN\":\"20160309\",\"jHN\":\"16:13:51\",\"jCC\":\"\",\"jCV\":\"\",\"jCCN\":null,\"jCVN\":null,\"jMPC\":\"0,00\",\"jMPV\":\"0,00\",\"jQMPC\":\"\",\"jQMPV\":\"\",\"jQAMC\":\"n/d\",\"jQAMV\":\"n/d\",\"jPC\":\"0,56\",\"jPTA\":\"0,00\",\"jVTO\":\"-,00\",\"jDTO\":\"10101000000\",\"jPM\":\"0,54\",\"jQT\":\"\",\"jXD\":\"0,600\",\"jND\":\"0,510\",\"jLA\":\"41733\",\"jNN\":\"39\",\"jIV\":\"-\",\"jVR\":\"-11,11\",\"jEP\":\"0\",\"jVA\":\"0,60\",\"jVF\":\"0,56\",\"jIO\":\"P\",\"jPE\":\"7,00\",\"jDE\":\"00000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"050006314\",\"jCN\":\"PETRD73\",\"jDN\":\"20160309\",\"jHN\":\"11:35:26\",\"jCC\":\"\",\"jCV\":\"\",\"jCCN\":null,\"jCVN\":null,\"jMPC\":\"0,00\",\"jMPV\":\"0,00\",\"jQMPC\":\"\",\"jQMPV\":\"\",\"jQAMC\":\"n/d\",\"jQAMV\":\"n/d\",\"jPC\":\"1,10\",\"jPTA\":\"0,00\",\"jVTO\":\"0,00\",\"jDTO\":\"10101000000\",\"jPM\":\"1,12\",\"jQT\":\"\",\"jXD\":\"1,140\",\"jND\":\"1,100\",\"jLA\":\"336\",\"jNN\":\"3\",\"jIV\":\"\",\"jVR\":\"+1,85\",\"jEP\":\"0\",\"jVA\":\"1,14\",\"jVF\":\"1,10\",\"jIO\":\"C\",\"jPE\":\"7,30\",\"jDE\":\"30000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"050006314\",\"jCN\":\"PETRP73\",\"jDN\":\"20160309\",\"jHN\":\"12:02:12\",\"jCC\":\"\",\"jCV\":\"\",\"jCCN\":null,\"jCVN\":null,\"jMPC\":\"0,00\",\"jMPV\":\"0,00\",\"jQMPC\":\"\",\"jQMPV\":\"\",\"jQAMC\":\"n/d\",\"jQAMV\":\"n/d\",\"jPC\":\"0,69\",\"jPTA\":\"0,00\",\"jVTO\":\"-,00\",\"jDTO\":\"10101000000\",\"jPM\":\"0,68\",\"jQT\":\"\",\"jXD\":\"0,690\",\"jND\":\"0,680\",\"jLA\":\"14310\",\"jNN\":\"2\",\"jIV\":\"-\",\"jVR\":\"-5,48\",\"jEP\":\"0\",\"jVA\":\"0,68\",\"jVF\":\"0,69\",\"jIO\":\"P\",\"jPE\":\"7,30\",\"jDE\":\"30000000\",\"jDC\":null,\"jDV\":\"\"}],\"MensagensParaAcompanhamentoDeOrdem\":[],\"MensagensParaAcompanhamentoDeStartStop\":[],\"Avisos\":null,\"Alertas\":[]}";
			
			//leituraAtual = "{\"IdDaRequisicao\":\"1457621201877\",\"LivrosDeOferta\":[],\"LivrosDeOferta30\":[{\"Papel\":\"PETRD70\",\"OfertasDeCompra\":[{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":1500.0,\"jQA\":null,\"jPC\":\"1,12\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":20600.0,\"jQA\":null,\"jPC\":\"1,11\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":16800.0,\"jQA\":null,\"jPC\":\"1,11\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":4900.0,\"jQA\":null,\"jPC\":\"1,10\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":20500.0,\"jQA\":null,\"jPC\":\"1,10\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":16700.0,\"jQA\":null,\"jPC\":\"1,10\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":11400.0,\"jQA\":null,\"jPC\":\"1,10\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"1,00\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"0,91\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"0,70\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,51\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"0,49\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,30\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,26\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,24\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,21\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,16\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":10400.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":16700.0,\"jQA\":null,\"jPC\":\"1,16\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":20600.0,\"jQA\":null,\"jPC\":\"1,16\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"1,25\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"1,29\",\"jQTO\":\"-\"},{\"jNUC\":37,\"jNOC\":\"UM\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"1,31\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":7000.0,\"jQA\":null,\"jPC\":\"1,33\",\"jQTO\":\"-\"},{\"jNUC\":147,\"jNOC\":\"ATIVA\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"1,34\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5100.0,\"jQA\":null,\"jPC\":\"1,35\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":3000.0,\"jQA\":null,\"jPC\":\"1,35\",\"jQTO\":\"-\"},{\"jNUC\":27,\"jNOC\":\"SANTANDER\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,39\",\"jQTO\":\"-\"},{\"jNUC\":37,\"jNOC\":\"UM\",\"jQT\":3000.0,\"jQA\":null,\"jPC\":\"1,35\",\"jQTO\":\"-\"},{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"2,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"3,00\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"4,00\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRP70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":24400.0,\"jQA\":null,\"jPC\":\"0,56\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":30000.0,\"jQA\":null,\"jPC\":\"0,56\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":31200.0,\"jQA\":null,\"jPC\":\"0,56\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":24400.0,\"jQA\":null,\"jPC\":\"0,55\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"0,48\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,47\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,45\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":308,\"jNOC\":\"CLEAR\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"0,59\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":24400.0,\"jQA\":null,\"jPC\":\"0,60\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":30000.0,\"jQA\":null,\"jPC\":\"0,60\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":24400.0,\"jQA\":null,\"jPC\":\"0,61\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":30000.0,\"jQA\":null,\"jPC\":\"0,61\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":15700.0,\"jQA\":null,\"jPC\":\"0,61\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":262,\"jNOC\":\"MIRAE ASSET\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":308,\"jNOC\":\"CLEAR\",\"jQT\":3700.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETR4\",\"OfertasDeCompra\":[{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":1700.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":40000.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":30000.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":131,\"jNOC\":\"FATOR\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":33,\"jNOC\":\"LEROSA\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"7,47\",\"jQTO\":\"-\"},{\"jNUC\":131,\"jNOC\":\"FATOR\",\"jQT\":3300.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"},{\"jNUC\":131,\"jNOC\":\"FATOR\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"},{\"jNUC\":262,\"jNOC\":\"MIRAE ASSET\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"},{\"jNUC\":106,\"jNOC\":\"MERCANTIL\",\"jQT\":7700.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":3000.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":2500.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"},{\"jNUC\":131,\"jNOC\":\"FATOR\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":1800.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"},{\"jNUC\":23,\"jNOC\":\"CONCORDIA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"7,46\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":1600.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":600.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":93,\"jNOC\":\"FUTURA\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":800.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":238,\"jNOC\":\"GOLDMAN SACHS\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":600.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":600.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":1800.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":2600.0,\"jQA\":null,\"jPC\":\"7,48\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,49\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,49\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,49\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,49\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,49\",\"jQTO\":\"-\"},{\"jNUC\":59,\"jNOC\":\"J SAFRA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"7,49\",\"jQTO\":\"-\"},{\"jNUC\":85,\"jNOC\":\"BTG PACTUAL\",\"jQT\":600.0,\"jQA\":null,\"jPC\":\"7,49\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,49\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,49\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRD73\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":18300.0,\"jQA\":null,\"jPC\":\"0,95\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":22400.0,\"jQA\":null,\"jPC\":\"0,95\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":18300.0,\"jQA\":null,\"jPC\":\"0,94\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":22400.0,\"jQA\":null,\"jPC\":\"0,94\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":27000.0,\"jQA\":null,\"jPC\":\"0,98\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":22400.0,\"jQA\":null,\"jPC\":\"1,00\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":18200.0,\"jQA\":null,\"jPC\":\"1,00\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":18200.0,\"jQA\":null,\"jPC\":\"1,01\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRP73\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":23300.0,\"jQA\":null,\"jPC\":\"0,69\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":30000.0,\"jQA\":null,\"jPC\":\"0,69\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":28600.0,\"jQA\":null,\"jPC\":\"0,69\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":23300.0,\"jQA\":null,\"jPC\":\"0,74\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":28600.0,\"jQA\":null,\"jPC\":\"0,74\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":29800.0,\"jQA\":null,\"jPC\":\"0,75\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,95\",\"jQTO\":\"-\"}]}],\"LivrosAgregados\":[],\"LivrosDeNegocios\":[],\"MensagensParaCotacao\":[],\"MensagensParaCotacaoOrdem\":[],\"MensagensParaCotacaoRapida\":[{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"114640975\",\"jCN\":\"PETR4\",\"jDN\":\"20160310\",\"jHN\":\"11:46:40\",\"jCC\":\"3\",\"jCV\":\"8\",\"jCCN\":\"XP\",\"jCVN\":\"UBS\",\"jMPC\":\"7,47\",\"jMPV\":\"7,48\",\"jQMPC\":\"1700\",\"jQMPV\":\"200\",\"jQAMC\":\"6000\",\"jQAMV\":\"44600\",\"jPC\":\"7,48\",\"jPTA\":\"7,80\",\"jVTO\":\"-2,63\",\"jDTO\":\"10101000000\",\"jPM\":\"7,63\",\"jQT\":\"2600\",\"jXD\":\"7,830\",\"jND\":\"7,460\",\"jLA\":\"176988456\",\"jNN\":\"12576\",\"jIV\":\"-\",\"jVR\":\"-1,58\",\"jEP\":\"2\",\"jVA\":\"7,80\",\"jVF\":\"7,60\",\"jIO\":\"X\",\"jPE\":\"0,00\",\"jDE\":\"00000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"114630523\",\"jCN\":\"PETRP70\",\"jDN\":\"20160310\",\"jHN\":\"11:31:45\",\"jCC\":\"45\",\"jCV\":\"308\",\"jCCN\":\"CREDIT SUISSE\",\"jCVN\":\"CLEAR\",\"jMPC\":\"0,56\",\"jMPV\":\"0,59\",\"jQMPC\":\"24400\",\"jQMPV\":\"500\",\"jQAMC\":\"85600\",\"jQAMV\":\"500\",\"jPC\":\"0,59\",\"jPTA\":\"0,00\",\"jVTO\":\"0,00\",\"jDTO\":\"10101000000\",\"jPM\":\"0,53\",\"jQT\":\"\",\"jXD\":\"0,590\",\"jND\":\"0,480\",\"jLA\":\"21033\",\"jNN\":\"11\",\"jIV\":\"\",\"jVR\":\"+5,36\",\"jEP\":\"2\",\"jVA\":\"0,48\",\"jVF\":\"0,56\",\"jIO\":\"P\",\"jPE\":\"7,00\",\"jDE\":\"00000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"114633768\",\"jCN\":\"PETRP73\",\"jDN\":\"20160310\",\"jHN\":\"10:18:38\",\"jCC\":\"45\",\"jCV\":\"45\",\"jCCN\":\"CREDIT SUISSE\",\"jCVN\":\"CREDIT SUISSE\",\"jMPC\":\"0,69\",\"jMPV\":\"0,74\",\"jQMPC\":\"23300\",\"jQMPV\":\"23300\",\"jQAMC\":\"81900\",\"jQAMV\":\"51900\",\"jPC\":\"0,62\",\"jPTA\":\"0,00\",\"jVTO\":\"-,00\",\"jDTO\":\"10101000000\",\"jPM\":\"0,62\",\"jQT\":\"\",\"jXD\":\"0,620\",\"jND\":\"0,620\",\"jLA\":\"1240\",\"jNN\":\"1\",\"jIV\":\"-\",\"jVR\":\"-10,14\",\"jEP\":\"2\",\"jVA\":\"0,62\",\"jVF\":\"0,69\",\"jIO\":\"P\",\"jPE\":\"7,30\",\"jDE\":\"30000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"114633768\",\"jCN\":\"PETRD73\",\"jDN\":\"20160310\",\"jHN\":\"11:33:02\",\"jCC\":\"45\",\"jCV\":\"3\",\"jCCN\":\"CREDIT SUISSE\",\"jCVN\":\"XP\",\"jMPC\":\"0,95\",\"jMPV\":\"0,98\",\"jQMPC\":\"18300\",\"jQMPV\":\"27000\",\"jQAMC\":\"40700\",\"jQAMV\":\"27000\",\"jPC\":\"1,01\",\"jPTA\":\"0,00\",\"jVTO\":\"-,00\",\"jDTO\":\"10101000000\",\"jPM\":\"1,02\",\"jQT\":\"\",\"jXD\":\"1,160\",\"jND\":\"1,010\",\"jLA\":\"19995\",\"jNN\":\"2\",\"jIV\":\"-\",\"jVR\":\"-8,18\",\"jEP\":\"2\",\"jVA\":\"1,16\",\"jVF\":\"1,10\",\"jIO\":\"C\",\"jPE\":\"7,30\",\"jDE\":\"30000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"114633768\",\"jCN\":\"PETRD70\",\"jDN\":\"20160310\",\"jHN\":\"11:41:34\",\"jCC\":\"386\",\"jCV\":\"77\",\"jCCN\":\"OCTO\",\"jCVN\":\"CITIGROUP\",\"jMPC\":\"1,12\",\"jMPV\":\"1,15\",\"jQMPC\":\"1500\",\"jQMPV\":\"1000\",\"jQAMC\":\"1500\",\"jQAMV\":\"11400\",\"jPC\":\"1,12\",\"jPTA\":\"0,00\",\"jVTO\":\"-,00\",\"jDTO\":\"10101000000\",\"jPM\":\"1,25\",\"jQT\":\"6000\",\"jXD\":\"1,360\",\"jND\":\"1,120\",\"jLA\":\"154023\",\"jNN\":\"34\",\"jIV\":\"-\",\"jVR\":\"-2,61\",\"jEP\":\"2\",\"jVA\":\"1,36\",\"jVF\":\"1,15\",\"jIO\":\"C\",\"jPE\":\"7,00\",\"jDE\":\"00000000\",\"jDC\":null,\"jDV\":\"\"}],\"MensagensParaAcompanhamentoDeOrdem\":[],\"MensagensParaAcompanhamentoDeStartStop\":[],\"Avisos\":null,\"Alertas\":[]}";
			
			//leituraAtual = "{\"IdDaRequisicao\":\"1457628538494\",\"LivrosDeOferta\":[],\"LivrosDeOferta30\":[{\"Papel\":\"PETR4\",\"OfertasDeCompra\":[{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,53\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"7,53\",\"jQTO\":\"-\"},{\"jNUC\":40,\"jNOC\":\"MORGAN STANLEY\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,53\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,53\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,53\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,53\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,53\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"7,53\",\"jQTO\":\"-\"},{\"jNUC\":262,\"jNOC\":\"MIRAE ASSET\",\"jQT\":1400.0,\"jQA\":null,\"jPC\":\"7,53\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,53\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,52\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,52\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,52\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,52\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"7,52\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"7,52\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"7,52\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":1500.0,\"jQA\":null,\"jPC\":\"7,52\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":6500.0,\"jQA\":null,\"jPC\":\"7,52\",\"jQTO\":\"-\"},{\"jNUC\":131,\"jNOC\":\"FATOR\",\"jQT\":17000.0,\"jQA\":null,\"jPC\":\"7,52\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":30000.0,\"jQA\":null,\"jPC\":\"7,52\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,52\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,52\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":4400.0,\"jQA\":null,\"jPC\":\"7,51\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,51\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,51\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,51\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":2500.0,\"jQA\":null,\"jPC\":\"7,51\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,51\",\"jQTO\":\"-\"},{\"jNUC\":85,\"jNOC\":\"BTG PACTUAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,51\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,54\",\"jQTO\":\"-\"},{\"jNUC\":16,\"jNOC\":\"JP MORGAN\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,54\",\"jQTO\":\"-\"},{\"jNUC\":238,\"jNOC\":\"GOLDMAN SACHS\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"7,54\",\"jQTO\":\"-\"},{\"jNUC\":9,\"jNOC\":\"DEUTSCHE BANK\",\"jQT\":900.0,\"jQA\":null,\"jPC\":\"7,54\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,54\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,54\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"7,54\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,54\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":16,\"jNOC\":\"JP MORGAN\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":30000.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":1500.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":59,\"jNOC\":\"J SAFRA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":85,\"jNOC\":\"BTG PACTUAL\",\"jQT\":600.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":3500.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":4000.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":6000.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":8,\"jNOC\":\"UBS\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":20000.0,\"jQA\":null,\"jPC\":\"7,55\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRP73\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":23300.0,\"jQA\":null,\"jPC\":\"0,68\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":28600.0,\"jQA\":null,\"jPC\":\"0,68\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":29800.0,\"jQA\":null,\"jPC\":\"0,66\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":28600.0,\"jQA\":null,\"jPC\":\"0,72\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":23300.0,\"jQA\":null,\"jPC\":\"0,72\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":29800.0,\"jQA\":null,\"jPC\":\"0,72\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,95\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRP70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":24500.0,\"jQA\":null,\"jPC\":\"0,55\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":30400.0,\"jQA\":null,\"jPC\":\"0,55\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":30100.0,\"jQA\":null,\"jPC\":\"0,54\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":31500.0,\"jQA\":null,\"jPC\":\"0,53\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"0,48\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,47\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"0,45\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":30100.0,\"jQA\":null,\"jPC\":\"0,58\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":24800.0,\"jQA\":null,\"jPC\":\"0,58\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":30100.0,\"jQA\":null,\"jPC\":\"0,59\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":24800.0,\"jQA\":null,\"jPC\":\"0,59\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"0,62\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,65\",\"jQTO\":\"-\"},{\"jNUC\":262,\"jNOC\":\"MIRAE ASSET\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":308,\"jNOC\":\"CLEAR\",\"jQT\":3700.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRD70\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":20400.0,\"jQA\":null,\"jPC\":\"1,16\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":20200.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":16600.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":15,\"jNOC\":\"GUIDE\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"1,15\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":16600.0,\"jQA\":null,\"jPC\":\"1,14\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,12\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":17500.0,\"jQA\":null,\"jPC\":\"1,12\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":4900.0,\"jQA\":null,\"jPC\":\"1,10\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"1,00\",\"jQTO\":\"-\"},{\"jNUC\":114,\"jNOC\":\"ITAU\",\"jQT\":400.0,\"jQA\":null,\"jPC\":\"0,91\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":1200.0,\"jQA\":null,\"jPC\":\"0,90\",\"jQTO\":\"-\"},{\"jNUC\":1982,\"jNOC\":\"1982\",\"jQT\":300.0,\"jQA\":null,\"jPC\":\"0,85\",\"jQTO\":\"-\"},{\"jNUC\":39,\"jNOC\":\"AGORA\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"0,70\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"0,51\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"0,49\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,30\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,26\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,24\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,22\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,21\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,20\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,19\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":100.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,18\",\"jQTO\":\"-\"},{\"jNUC\":86,\"jNOC\":\"WALPIRES\",\"jQT\":51000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"},{\"jNUC\":120,\"jNOC\":\"BRASIL PLURAL\",\"jQT\":50000.0,\"jQA\":null,\"jPC\":\"0,17\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":20200.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":16500.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":4600.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":17500.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":12700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":77,\"jNOC\":\"CITIGROUP\",\"jQT\":12700.0,\"jQA\":null,\"jPC\":\"1,19\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":20200.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":16500.0,\"jQA\":null,\"jPC\":\"1,20\",\"jQTO\":\"-\"},{\"jNUC\":47,\"jNOC\":\"SOLIDEZ\",\"jQT\":10000.0,\"jQA\":null,\"jPC\":\"1,21\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":2900.0,\"jQA\":null,\"jPC\":\"1,25\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"1,27\",\"jQTO\":\"-\"},{\"jNUC\":37,\"jNOC\":\"UM\",\"jQT\":2000.0,\"jQA\":null,\"jPC\":\"1,31\",\"jQTO\":\"-\"},{\"jNUC\":735,\"jNOC\":\"ICAP\",\"jQT\":7000.0,\"jQA\":null,\"jPC\":\"1,33\",\"jQTO\":\"-\"},{\"jNUC\":147,\"jNOC\":\"ATIVA\",\"jQT\":5000.0,\"jQA\":null,\"jPC\":\"1,34\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":5100.0,\"jQA\":null,\"jPC\":\"1,35\",\"jQTO\":\"-\"},{\"jNUC\":386,\"jNOC\":\"OCTO\",\"jQT\":3000.0,\"jQA\":null,\"jPC\":\"1,35\",\"jQTO\":\"-\"},{\"jNUC\":27,\"jNOC\":\"SANTANDER\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,39\",\"jQTO\":\"-\"},{\"jNUC\":3,\"jNOC\":\"XP\",\"jQT\":1000.0,\"jQA\":null,\"jPC\":\"1,35\",\"jQTO\":\"-\"},{\"jNUC\":37,\"jNOC\":\"UM\",\"jQT\":3000.0,\"jQA\":null,\"jPC\":\"1,35\",\"jQTO\":\"-\"},{\"jNUC\":58,\"jNOC\":\"SOCOPA\",\"jQT\":500.0,\"jQA\":null,\"jPC\":\"2,70\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"3,00\",\"jQTO\":\"-\"},{\"jNUC\":72,\"jNOC\":\"BRADESCO\",\"jQT\":200.0,\"jQA\":null,\"jPC\":\"4,00\",\"jQTO\":\"-\"}]},{\"Papel\":\"PETRD73\",\"OfertasDeCompra\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":22100.0,\"jQA\":null,\"jPC\":\"1,00\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":17900.0,\"jQA\":null,\"jPC\":\"0,99\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":21900.0,\"jQA\":null,\"jPC\":\"0,99\",\"jQTO\":\"-\"}],\"OfertasDeVenda\":[{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":21900.0,\"jQA\":null,\"jPC\":\"1,03\",\"jQTO\":\"-\"},{\"jNUC\":45,\"jNOC\":\"CREDIT SUISSE\",\"jQT\":17900.0,\"jQA\":null,\"jPC\":\"1,03\",\"jQTO\":\"-\"}]}],\"LivrosAgregados\":[],\"LivrosDeNegocios\":[],\"MensagensParaCotacao\":[],\"MensagensParaCotacaoOrdem\":[],\"MensagensParaCotacaoRapida\":[{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"134856828\",\"jCN\":\"PETRP70\",\"jDN\":\"20160310\",\"jHN\":\"13:02:59\",\"jCC\":\"45\",\"jCV\":\"45\",\"jCCN\":\"CREDIT SUISSE\",\"jCVN\":\"CREDIT SUISSE\",\"jMPC\":\"0,55\",\"jMPV\":\"0,58\",\"jQMPC\":\"24500\",\"jQMPV\":\"30100\",\"jQAMC\":\"54900\",\"jQAMV\":\"54900\",\"jPC\":\"0,59\",\"jPTA\":\"0,00\",\"jVTO\":\"0,00\",\"jDTO\":\"10101000000\",\"jPM\":\"0,53\",\"jQT\":\"500\",\"jXD\":\"0,590\",\"jND\":\"0,480\",\"jLA\":\"21328\",\"jNN\":\"12\",\"jIV\":\"\",\"jVR\":\"+5,36\",\"jEP\":\"2\",\"jVA\":\"0,48\",\"jVF\":\"0,56\",\"jIO\":\"P\",\"jPE\":\"7,00\",\"jDE\":\"00000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"134842273\",\"jCN\":\"PETRP73\",\"jDN\":\"20160310\",\"jHN\":\"10:18:38\",\"jCC\":\"45\",\"jCV\":\"45\",\"jCCN\":\"CREDIT SUISSE\",\"jCVN\":\"CREDIT SUISSE\",\"jMPC\":\"0,68\",\"jMPV\":\"0,72\",\"jQMPC\":\"23300\",\"jQMPV\":\"28600\",\"jQAMC\":\"51900\",\"jQAMV\":\"81700\",\"jPC\":\"0,62\",\"jPTA\":\"0,00\",\"jVTO\":\"-,00\",\"jDTO\":\"10101000000\",\"jPM\":\"0,62\",\"jQT\":\"\",\"jXD\":\"0,620\",\"jND\":\"0,620\",\"jLA\":\"1240\",\"jNN\":\"1\",\"jIV\":\"-\",\"jVR\":\"-10,14\",\"jEP\":\"2\",\"jVA\":\"0,62\",\"jVF\":\"0,69\",\"jIO\":\"P\",\"jPE\":\"7,30\",\"jDE\":\"30000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"134857702\",\"jCN\":\"PETR4\",\"jDN\":\"20160310\",\"jHN\":\"13:48:57\",\"jCC\":\"8\",\"jCV\":\"8\",\"jCCN\":\"UBS\",\"jCVN\":\"UBS\",\"jMPC\":\"7,53\",\"jMPV\":\"7,54\",\"jQMPC\":\"300\",\"jQMPV\":\"400\",\"jQAMC\":\"11400\",\"jQAMV\":\"10500\",\"jPC\":\"7,54\",\"jPTA\":\"7,80\",\"jVTO\":\"-2,63\",\"jDTO\":\"10101000000\",\"jPM\":\"7,58\",\"jQT\":\"400\",\"jXD\":\"7,830\",\"jND\":\"7,440\",\"jLA\":\"343405212\",\"jNN\":\"29047\",\"jIV\":\"-\",\"jVR\":\"-0,79\",\"jEP\":\"2\",\"jVA\":\"7,80\",\"jVF\":\"7,60\",\"jIO\":\"X\",\"jPE\":\"0,00\",\"jDE\":\"00000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"134856812\",\"jCN\":\"PETRD70\",\"jDN\":\"20160310\",\"jHN\":\"13:43:50\",\"jCC\":\"45\",\"jCV\":\"45\",\"jCCN\":\"CREDIT SUISSE\",\"jCVN\":\"CREDIT SUISSE\",\"jMPC\":\"1,16\",\"jMPV\":\"1,19\",\"jQMPC\":\"20400\",\"jQMPV\":\"20200\",\"jQAMC\":\"20400\",\"jQAMV\":\"54200\",\"jPC\":\"1,17\",\"jPTA\":\"0,00\",\"jVTO\":\"0,00\",\"jDTO\":\"10101000000\",\"jPM\":\"1,20\",\"jQT\":\"1000\",\"jXD\":\"1,360\",\"jND\":\"1,110\",\"jLA\":\"279373\",\"jNN\":\"62\",\"jIV\":\"\",\"jVR\":\"+1,74\",\"jEP\":\"2\",\"jVA\":\"1,36\",\"jVF\":\"1,15\",\"jIO\":\"C\",\"jPE\":\"7,00\",\"jDE\":\"00000000\",\"jDC\":null,\"jDV\":\"\"},{\"jHTM\":\"NE\",\"jHTB\":\"BV\",\"jHDT\":\"20160310\",\"jHHR\":\"134849870\",\"jCN\":\"PETRD73\",\"jDN\":\"20160310\",\"jHN\":\"13:04:28\",\"jCC\":\"45\",\"jCV\":\"45\",\"jCCN\":\"CREDIT SUISSE\",\"jCVN\":\"CREDIT SUISSE\",\"jMPC\":\"1,00\",\"jMPV\":\"1,03\",\"jQMPC\":\"22100\",\"jQMPV\":\"21900\",\"jQAMC\":\"22100\",\"jQAMV\":\"1000\",\"jPC\":\"0,98\",\"jPTA\":\"0,00\",\"jVTO\":\"-,00\",\"jDTO\":\"10101000000\",\"jPM\":\"0,99\",\"jQT\":\"10000\",\"jXD\":\"1,160\",\"jND\":\"0,980\",\"jLA\":\"56255\",\"jNN\":\"6\",\"jIV\":\"-\",\"jVR\":\"-10,91\",\"jEP\":\"2\",\"jVA\":\"1,16\",\"jVF\":\"1,10\",\"jIO\":\"C\",\"jPE\":\"7,30\",\"jDE\":\"30000000\",\"jDC\":null,\"jDV\":\"\"}],\"MensagensParaAcompanhamentoDeOrdem\":[],\"MensagensParaAcompanhamentoDeStartStop\":[],\"Avisos\":null,\"Alertas\":[]}";
			
			BigDecimal taxaDeJuros = new BigDecimal("0.1425");
			BigDecimal percReferencia = new BigDecimal("0.01");
			
			//LivroOferta[] lo2 = gson.fromJson(leituraAtual, LivroOferta[].class);
			LeituraLivroOferta llo =  gson.fromJson(leituraAtual, LeituraLivroOferta.class);
			
			if (llo != null && llo.LivrosDeOferta30 != null && llo.LivrosDeOferta30.size() > 0) {
				// joga a acao para o inicio da lista
				/*
				LivroOferta loTemp = new LivroOferta();
				Map.Entry<String, IAtivoMini> primeiroElemento =  MAP_ATIVOS.entrySet().iterator().next();
				loTemp.Papel =  primeiroElemento.getValue().getCodigoAtivo();
				int indiceAcao = llo.LivrosDeOferta30.indexOf(loTemp);
				loTemp = llo.LivrosDeOferta30.get(indiceAcao);
				llo.LivrosDeOferta30.remove(indiceAcao);
				llo.LivrosDeOferta30.add(0, loTemp);
				*/
				List<IAtivoMini> ativos = new ArrayList<>(MAP_ATIVOS.values());
				//BigDecimal precoAcao = new BigDecimal(llo.LivrosDeOferta30.get(0).OfertasDeVenda.get(0).jPC.replace(",", "."));
				
				LivroOferta loTemp = new LivroOferta();
				loTemp.Papel = ativos.get(0).getCodigoAtivo();
				LivroOferta loAcao = llo.LivrosDeOferta30.get(llo.LivrosDeOferta30.indexOf(loTemp));
				BigDecimal precoAcao = new BigDecimal(loAcao.OfertasDeVenda.get(0).jPC.replace(",", "."));
				for (int i = 1; i < llo.LivrosDeOferta30.size(); i = i + 2) {
					loTemp.Papel = ativos.get(i).getCodigoAtivo();
					LivroOferta loCall = llo.LivrosDeOferta30.get(llo.LivrosDeOferta30.indexOf(loTemp));
					loTemp.Papel = ativos.get(i+1).getCodigoAtivo();
					// recupera a put
					LivroOferta loPut = llo.LivrosDeOferta30.get(llo.LivrosDeOferta30.indexOf(loTemp));
					// se nao tiver a oferta de compra da call e oferta de venda da put vai para o proximo par
					if (!(loCall.OfertasDeCompra.size() > 0 && loPut.OfertasDeVenda.size() > 0)) {
						LogUtil.log("Nao tem oferta de compra e venda no par da opcao");
						continue;
					}
					// recupera o melhor preco de compra da CALL
					BigDecimal precoMelhorCompraCall = new BigDecimal(loCall.OfertasDeCompra.get(0).jPC.replace(",", "."));
					// recupera a call
					OpcaoMini omCall = (OpcaoMini) MAP_ATIVOS.get(loCall.Papel);
					// recupera o preco exercicio
					BigDecimal precoExercicioOpcao = omCall.getPrecoExercicio();
					// recupera a qtdade dias para vencimento
					int qtdDiasParaVencimento = omCall.getQtdDiasParaVencimento();
					// recupera o melhor preco de venda da PUT 
					BigDecimal precoMelhorVendaPUT =  new BigDecimal(loPut.OfertasDeVenda.get(0).jPC.replace(",", "."));
					// verifica se tem a anomalia
					boolean temAnomalia = temAnomalia1(precoAcao, precoExercicioOpcao, qtdDiasParaVencimento, taxaDeJuros, precoMelhorCompraCall, precoMelhorVendaPUT, percReferencia);
					if (temAnomalia) {
						// notifica por email/pela aplicacao
						notificarUsuarios("Encontrou anomalia na " + loCall.Papel);
					}
				}
				LogUtil.log("FIM");
			}
			long fim = System.currentTimeMillis();
			LogUtil.log("tempo proc: " + (fim - inicio));
		}

	}
	
	private void notificarUsuarios(String msg) {
		INotificacao notificacao = new Notificacao();
		for (UsuarioAlert usuario : USUARIOS) {
			notificacao.notificar(usuario, msg);
		}
	}

	/*
	public static void main(String[] args) {
		ProcessarLivroOfertas plo = new MonitorLivroOfertas().new ProcessarLivroOfertas();
		plo.processarLivroOfertas();
	}
	*/
	
	public static boolean temAnomalia1(BigDecimal precoAcao, BigDecimal precoExercicioOpcao, 
			int qtdDiasParaVencimento, BigDecimal taxaDeJuros, BigDecimal precoMelhorCompraCall, BigDecimal precoMelhorVendaPUT, BigDecimal percReferencia) {
		// recupera a volatilidade da call de acordo ao melhor preco de compra da Call
		BigDecimal volatilidadeCall = BlackScholes.calculaVolatilidade(true, precoAcao, precoExercicioOpcao, qtdDiasParaVencimento, precoMelhorCompraCall, taxaDeJuros);
		// recupera o preco BS da PUT se tivesse a mesma volatilidade da CALL
		BigDecimal precoBSPut = BlackScholes.blackScholes(false, precoAcao, precoExercicioOpcao, qtdDiasParaVencimento, volatilidadeCall, taxaDeJuros);
		// calcula o percentual de diferenca
		BigDecimal percentualDif = precoBSPut.subtract(precoMelhorVendaPUT).divide(precoAcao, RoundingMode.HALF_EVEN); 
		if (percentualDif.compareTo(percReferencia) > 0) {
			LogUtil.log("encontrou anomalia: " + percentualDif);
			return true;
		}
		LogUtil.log("percentualDif: " + percentualDif);
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
		public boolean equals(Object obj) {
			// TODO Auto-generated method stub
			LivroOferta obj2 = (LivroOferta) obj;
			return this.Papel.equalsIgnoreCase(obj2.Papel);
		}
		
		@Override
		public int hashCode() {
			return Objects.hashCode(this.Papel);
		}
		
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
