package datadog.trace.agent.test.utils;

import datadog.trace.agent.test.server.http.TestHttpServer;
import java.net.ProxySelector;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class was moved from groovy to java because groovy kept trying to introspect on the
 * OkHttpClient class which contains java 8 only classes, which caused the build to fail for java 7.
 */
public class OkHttpUtils {

  private static final Logger CLIENT_LOGGER = LoggerFactory.getLogger("http-client");

  static {
    ((ch.qos.logback.classic.Logger) CLIENT_LOGGER).setLevel(ch.qos.logback.classic.Level.DEBUG);
  }

  private static final HttpLoggingInterceptor LOGGING_INTERCEPTOR =
      new HttpLoggingInterceptor(
          new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(final String message) {
              CLIENT_LOGGER.debug(message);
            }
          });

  static {
    LOGGING_INTERCEPTOR.setLevel(Level.BASIC);
  }

  static OkHttpClient.Builder clientBuilder() {
    final TimeUnit unit = TimeUnit.MINUTES;
    return new OkHttpClient.Builder()
        .addInterceptor(LOGGING_INTERCEPTOR)
        .connectTimeout(1, unit)
        .writeTimeout(1, unit)
        .readTimeout(1, unit);
  }

  public static OkHttpClient client() {
    return client(false);
  }

  public static OkHttpClient client(long connectTimeout, long readTimeout, TimeUnit unit) {
    return client(
        clientBuilder().connectTimeout(connectTimeout, unit).readTimeout(readTimeout, unit), false);
  }

  public static OkHttpClient client(final boolean followRedirects) {
    return client(clientBuilder(), followRedirects);
  }

  static OkHttpClient client(final OkHttpClient.Builder builder, final boolean followRedirects) {
    return builder.followRedirects(followRedirects).build();
  }

  public static OkHttpClient client(
      final TestHttpServer server, final ProxySelector proxySelector) {
    return clientBuilder()
        .sslSocketFactory(server.sslContext.getSocketFactory(), server.getTrustManager())
        .hostnameVerifier(server.getHostnameVerifier())
        .proxySelector(proxySelector)
        .build();
  }
}
