package datadog.trace.instrumentation.http4s023_213;

import cats.effect.kernel.Resource;
import net.bytebuddy.asm.Advice;
import org.http4s.blaze.client.BlazeClientBuilder;
import org.http4s.client.Client;
import scala.concurrent.impl.Promise;

public class Http4sClientBuilderAdvice {
  @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
  public static <F> void exit(
      @Advice.This BlazeClientBuilder<F> zis,
      @Advice.Return(readOnly = false) Resource<F, Client<F>> retVal) {
    retVal = ClientWrapper$.MODULE$.resource(retVal, zis.F());
  }

  /** Promise.Transformation was introduced in scala 2.13 */
  private static void muzzleCheck(final Promise.Transformation<?, ?> callback) {
    callback.submitWithValue(null);
  }
}