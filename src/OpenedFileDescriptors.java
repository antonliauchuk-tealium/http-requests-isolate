import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.Isolates;
import org.graalvm.nativeimage.Isolates.CreateIsolateParameters;
import org.graalvm.nativeimage.c.function.CEntryPoint;

//net.inet.tcp.keepidle: 7200000
//net.inet.tcp.keepintvl: 75000
//net.inet.tcp.keepcnt: 8
// echo "Main-Class: OpenedFileDescriptors" > manifest.txt && javac OpenedFileDescriptors.java &&  jar -cvfm OpenedFileDescriptors.jar manifest.txt OpenedFileDescriptors.class && native-image -H:EnableURLProtocols=http -H:ReflectionConfigurationFiles=reflection-config.json -jar OpenedFileDescriptors.jar
public class OpenedFileDescriptors {

	private static final String URL = "http://localhost:8085";

	public static void main(String[] args) throws IOException, InterruptedException {
		//no opened file descriptors
//		httpConnectionOutsideIsolate();
//		printFromCommand("lsof -a -p OpenedFileDescriptors");

		//a lot of file descriptors
		printFromCommand("cat /proc/sys/net/ipv4/tcp_keepalive_time");
		printFromCommand("cat /proc/sys/net/ipv4/tcp_keepalive_intvl");
		printFromCommand("cat /proc/sys/net/ipv4/tcp_keepalive_probes");
		httpConnectionInsideIsolate();
		printFromCommand("lsof -a -p OpenedFileDescriptors");
		printFromCommand("ls -la /proc/$$/fd");
		var pidof = resultFromCommand("pidof OpenedFileDescriptors");
		printFromCommand("cd /proc/" + pidof + "/fd && ls -l | less");
		printFromCommand("netstat -a -W -p");
//		printFromCommand("lsof -n | grep OpenedFil");
	}

	private static void printFromCommand(String command) throws IOException, InterruptedException {
		String[] cmd = { "/bin/sh", "-c", command };
		Process pr = Runtime.getRuntime().exec(cmd);
		String line;

		try (var inputStream = pr.getInputStream(); var in = new InputStreamReader(inputStream); BufferedReader input = new BufferedReader(in)) {

			System.out.println("result from " + command);
			while ((line = input.readLine()) != null)
				System.out.println(line);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int exitVal = pr.waitFor();
	}

	private static String resultFromCommand(String command) throws IOException, InterruptedException {
		String[] cmd = { "/bin/sh", "-c", command };
		Process pr = Runtime.getRuntime().exec(cmd);
		String line = null;
		try (var inputStream = pr.getInputStream(); var in = new InputStreamReader(inputStream); BufferedReader input = new BufferedReader(in)) {

			System.out.println("result from " + command);
			line = input.readLine();
			System.out.println(line);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int exitVal = pr.waitFor();
		return line;
	}

	public static void httpConnectionOutsideIsolate() {
		System.out.println("------------ HTTP Requests Without Isolate ------------");
		var iterations = 5000;
		while (iterations > 0) {
			iterations--;
			try {
				httpRequestsInsideLoop();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public static void httpConnectionInsideIsolate() throws InterruptedException {
		System.out.println("------------ HTTP Requests Inside Isolate ------------");
		var iterations = 100;
		while (iterations > 0) {
			iterations--;
			var context = Isolates.createIsolate(CreateIsolateParameters.getDefault());
			nativeWrapper(context);
			Isolates.tearDownIsolate(context);
		}
		System.out.println("sleep for 190 minutes");
		TimeUnit.MINUTES.sleep(190);
	}

	@CEntryPoint
	public static void nativeWrapper(@CEntryPoint.IsolateThreadContext IsolateThread context) {
		try {
			httpRequestsInsideLoop();
//			System.out.println("sleep for 10 seconds");
//			TimeUnit.SECONDS.sleep(10);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void httpRequestsInsideLoop() {
		HttpURLConnection connection = null;
		try {
//			System.setProperty("http.keepAlive", "true");
//			System.setProperty("sun.net.httpserver.debug", "true");
			//open connection
			connection = (HttpURLConnection) new URL(URL).openConnection();
			//set method
			connection.setRequestMethod("POST");
//			connection
			//ensure connection will be closed
			connection.setRequestProperty("Connection", "close");
			connection.setDoOutput(true);
			try (var out = connection.getOutputStream()) {
				out.write("{'param':'value1'}".getBytes(StandardCharsets.UTF_8));
				out.flush();
			}


			//get response code
			int responseCode = connection.getResponseCode();
			var responseMessage = connection.getResponseMessage();
			//process response body
			String response = null;
			try(var inputStream = is2xx(responseCode) ? connection.getInputStream() : connection.getErrorStream()) {
				if (inputStream != null) {
					try (var in = new BufferedReader(new InputStreamReader(inputStream))){
						response = in.lines().collect(Collectors.joining("\n"));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean is2xx(int code) {
		return code >= 200 && code <= 299;
	}
}
