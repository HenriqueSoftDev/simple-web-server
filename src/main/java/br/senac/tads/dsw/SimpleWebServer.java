package br.senac.tads.dsw;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleWebServer {

	private final int port;
	private static final int THREAD_POOL_SIZE = 10;

	public SimpleWebServer(int port) {
		this.port = port;
	}

	public void start() throws IOException {
		ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("Server started na porta " + port);
			while (true) {
				Socket clientSocket = serverSocket.accept();
				threadPool.execute(() -> handleClient(clientSocket));
			}
		}
	}

	private void handleClient(Socket clientSocket) {
		try (
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
		) {
			String requestLine = in.readLine(); // ex: GET /?nome=Maria&email=... HTTP/1.1
			String headerLine;
			String contentTypeHeader = "";

			// Lê os headers
			while (!(headerLine = in.readLine()).isEmpty()) {
				if (headerLine.toLowerCase().startsWith("content-type:")) {
					contentTypeHeader = headerLine.split(":", 2)[1].trim().toLowerCase();
				}
			}

			String nome = "Henrique";
			String email = "henrisilva2003@gmail.com";

			// Se NÃO for JSON, extrair dados da URL
			if (!"application/json".equals(contentTypeHeader)) {
				String[] parts = requestLine.split(" ");
				String path = parts.length > 1 ? parts[1] : "/";
				Map<String, String> queryParams = getQueryParams(path);
				nome = queryParams.getOrDefault("nome", "Não informado");
				email = queryParams.getOrDefault("email", "Não informado");
			}

			GeradorResposta gerador;
			String contentType;

			if ("application/json".equals(contentTypeHeader)) {
				gerador = new GeradorRespostaJson();
				contentType = "application/json";
			} else {
				gerador = new GeradorRespostaHtml();
				contentType = "text/html";
			}

			String responseBody = gerador.gerarResposta(nome, email);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
			ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

			out.write("HTTP/1.1 200 OK\r\n");
			out.write("Date: " + formatter.format(now) + "\r\n");
			out.write("Server: Custom Server\r\n");
			out.write("Content-Type: " + contentType + "\r\n");
			out.write("Content-Length: " + responseBody.getBytes().length + "\r\n");
			out.write("\r\n");
			out.write(responseBody);
			out.flush();

		} catch (IOException ex) {
			System.err.println("Erro ao lidar com cliente: " + ex.getMessage());
		} finally {
			try {
				clientSocket.close();
			} catch (IOException ex) {
				System.err.println("Erro ao fechar conexão: " + ex.getMessage());
			}
		}
	}


	private Map<String, String> getQueryParams(String path) throws UnsupportedEncodingException {
		Map<String, String> queryParams = new HashMap<>();
		if (path.contains("?")) {
			String query = path.split("\\?")[1];
			for (String param : query.split("&")) {
				String[] keyValue = param.split("=");
				if (keyValue.length > 1) {
					queryParams.put(keyValue[0], URLDecoder.decode(keyValue[1], "UTF-8"));
				}
			}
		}
		return queryParams;
	}

	public static void main(String[] args) throws IOException {
		int port = 8080;
		SimpleWebServer server = new SimpleWebServer(port);
		server.start();
	}
}
