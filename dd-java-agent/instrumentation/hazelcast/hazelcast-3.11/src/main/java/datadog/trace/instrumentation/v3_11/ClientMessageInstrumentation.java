package datadog.trace.instrumentation.v3_11;

import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import com.hazelcast.client.impl.clientside.HazelcastClientInstanceImpl;
import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.proxy.ClientMapProxy;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.instrumentation.hazelcast.HazelcastConstants;
import java.util.Collections;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public class ClientMessageInstrumentation extends Instrumenter.Tracing {

  public ClientMessageInstrumentation() {
    super(HazelcastConstants.INSTRUMENTATION_NAME);
  }

  @Override
  protected boolean defaultEnabled() {
    return HazelcastConstants.DEFAULT_ENABLED;
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {"datadog.trace.instrumentation.hazelcast.HazelcastConstants"};
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return named("com.hazelcast.client.impl.protocol.ClientMessage");
  }

  @Override
  public Map<String, String> contextStore() {
    return Collections.singletonMap(
        "com.hazelcast.client.impl.protocol.ClientMessage", String.class.getName());
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return Collections.singletonMap(
        isMethod()
            .and(namedOneOf("setOperationName"))
            .and(takesArgument(0, named(String.class.getName()))),
        getClass().getName() + "$OperationCapturingAdvice");
  }

  public static class OperationCapturingAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.This ClientMessage that, @Advice.Argument(0) final String operationName) {

      InstrumentationContext.get(ClientMessage.class, String.class).put(that, operationName);
    }

    public static void muzzleCheck(
        // Moved in 4.0
        ClientMapProxy proxy,

        // Moved in 3.11
        HazelcastClientInstanceImpl client) {
      proxy.getServiceName();
      client.start();
    }
  }
}
