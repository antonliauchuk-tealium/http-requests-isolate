import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class HangedUpThreads {

	private static Instant start;

	public static void main(String[] args) throws IOException, InterruptedException {
		httpConnectionOutsideIsolate();
	}

	public static void httpConnectionOutsideIsolate() {
		System.out.println("------------ Infinite loop is started ------------");
		start = Instant.now();
		while (true) {
			try {
				executorServiceActionWithShutdown();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public static void executorServiceActionWithShutdown() {
		var taskExecutor = Executors.newSingleThreadExecutor();
		try {
			Future<Integer> futureResult = taskExecutor.submit(() -> {
				Thread.sleep(10);
				return 3 + 1;
			});

			futureResult.get();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			shutdown(taskExecutor, 3);
		}
	}

	public static void shutdown(ExecutorService service, int maxRetries) {
		service.shutdownNow();

		try {
			for (int i = 0; i < maxRetries; i++) {
				var retryCount = i + 1;
				var executorWasShutdown = service.awaitTermination(getWaitTime(retryCount), TimeUnit.MILLISECONDS);
				if (executorWasShutdown) {
					return;
				} else {
					System.out.println("Executor was not shutdown, retry number = " + retryCount);
				}
			}
			System.out.println("Executor was not shutdown after several attempts.");
			System.out.println("Hanged up thread was reproduced after " + Duration.between(start, Instant.now()).toSeconds() + " seconds");
			var threadInfo = getAllStackTraces();
			System.out.println("Thread info: " + threadInfo);

			System.out.println("Stop application");
			System.exit(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static long getWaitTime(int retryCount) {
		return (long) Math.pow(2, retryCount) * 1000;
	}

	private static String getAllStackTraces() {
		StringBuilder sb = new StringBuilder();
		var liveThreads = Thread.getAllStackTraces();
		for (Thread key : liveThreads.keySet()) {
			sb.append("Thread " + key.getName());
			sb.append("\n ");
			sb.append("State " + key.getState());
			sb.append("\n ");
			sb.append("Priority " + key.getPriority());
			sb.append("\n ");
			var trace = liveThreads.get(key);
			for (StackTraceElement stackTraceElement : trace) {
				sb.append("\tat " + stackTraceElement);
				sb.append("\n");
			}
		}

		return sb.toString();
	}
}
