package datadog.trace.instrumentation.spymemcached;

import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import java.util.concurrent.ExecutionException;
import net.spy.memcached.MemcachedConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncCompletionListener extends CompletionListener<Void> {
  private static final Logger log = LoggerFactory.getLogger(SyncCompletionListener.class);

  public SyncCompletionListener(final MemcachedConnection connection, final String methodName) {
    super(connection, methodName);
  }

  @Override
  protected void processResult(final AgentSpan span, final Void future)
      throws ExecutionException, InterruptedException {
    log.error("processResult was called on SyncCompletionListener. This should never happen. ");
  }

  public void done(final Throwable thrown) {
    closeSyncSpan(thrown);
  }
}
