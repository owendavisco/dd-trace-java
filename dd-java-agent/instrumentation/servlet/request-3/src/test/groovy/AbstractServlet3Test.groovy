import datadog.trace.agent.test.asserts.TraceAssert
import datadog.trace.agent.test.base.HttpServerTest
import datadog.trace.api.DDSpanTypes
import datadog.trace.api.DDTags
import datadog.trace.bootstrap.instrumentation.api.Tags
import datadog.trace.core.DDSpan
import datadog.trace.instrumentation.servlet3.Servlet3Decorator
import okhttp3.Request
import okhttp3.RequestBody

import javax.servlet.Servlet

import static datadog.trace.agent.test.base.HttpServerTest.ServerEndpoint.AUTH_REQUIRED
import static datadog.trace.agent.test.base.HttpServerTest.ServerEndpoint.ERROR
import static datadog.trace.agent.test.base.HttpServerTest.ServerEndpoint.EXCEPTION
import static datadog.trace.agent.test.base.HttpServerTest.ServerEndpoint.QUERY_PARAM
import static datadog.trace.agent.test.base.HttpServerTest.ServerEndpoint.REDIRECT
import static datadog.trace.agent.test.base.HttpServerTest.ServerEndpoint.SUCCESS
import static datadog.trace.agent.test.base.HttpServerTest.ServerEndpoint.TIMEOUT
import static datadog.trace.agent.test.base.HttpServerTest.ServerEndpoint.TIMEOUT_ERROR

abstract class AbstractServlet3Test<SERVER, CONTEXT> extends HttpServerTest<SERVER> {
  @Override
  URI buildAddress() {
    if (dispatch) {
      return new URI("http://localhost:$port/$context/dispatch/")
    } else {
      return new URI("http://localhost:$port/$context/")
    }
  }

  @Override
  String component() {
    return Servlet3Decorator.DECORATE.component()
  }

  @Override
  String expectedServiceName() {
    context
  }

  @Override
  String expectedOperationName() {
    return "servlet.request"
  }

  boolean hasHandlerSpan() {
    return isDispatch()
  }

  boolean isDispatch() {
    return false
  }

  // FIXME: Add authentication tests back in...
//  @Shared
//  protected String user = "user"
//  @Shared
//  protected String pass = "password"

  abstract String getContext()

  Class<Servlet> servlet = servlet()

  abstract Class<Servlet> servlet()

  abstract void addServlet(CONTEXT context, String path, Class<Servlet> servlet)

  protected void setupServlets(CONTEXT context) {
    def servlet = servlet()

    addServlet(context, SUCCESS.path, servlet)
    addServlet(context, QUERY_PARAM.path, servlet)
    addServlet(context, ERROR.path, servlet)
    addServlet(context, EXCEPTION.path, servlet)
    addServlet(context, REDIRECT.path, servlet)
    addServlet(context, AUTH_REQUIRED.path, servlet)
    addServlet(context, TIMEOUT.path, servlet)
    addServlet(context, TIMEOUT_ERROR.path, servlet)
  }

  protected ServerEndpoint lastRequest

  @Override
  Request.Builder request(ServerEndpoint uri, String method, RequestBody body) {
    lastRequest = uri
    super.request(uri, method, body)
  }

  // Almost identical to serverSpan()
  @Override
  void handlerSpan(TraceAssert trace, Object parent, ServerEndpoint endpoint = SUCCESS) {
    if (!isDispatch()) {
      throw new UnsupportedOperationException("handlerSpan not applicable for nondispatch")
    }

    trace.span {
      serviceName expectedServiceName()
      operationName expectedOperationName()
      resourceName endpoint.status == 404 ? "404" : "GET ${endpoint.resolve(address).path.replace("/dispatch", "")}"
      spanType DDSpanTypes.HTTP_SERVER
      errored endpoint.errored
      childOf(parent as DDSpan)
      tags {
        "$Tags.COMPONENT" component
        "$Tags.SPAN_KIND" Tags.SPAN_KIND_SERVER
        "$Tags.PEER_HOST_IPV4" { it == null || it == "127.0.0.1" } // Optional
        "$Tags.PEER_PORT" Integer
        "$Tags.HTTP_URL" "${endpoint.resolve(address)}".replace("/dispatch", "")
        "$Tags.HTTP_METHOD" "GET"
        if (endpoint.status > 0) {
          "$Tags.HTTP_STATUS" endpoint.status
        } else {
          "timeout" 1_000
        }
        if (context) {
          "servlet.context" "/$context"
        }
        "servlet.path" endpoint.status == 404 ? endpoint.path : "$endpoint.path".replace("/dispatch", "")
        if (endpoint.errored) {
          "error.msg" { it == null || it == EXCEPTION.body }
          "error.type" { it == null || it == Exception.name }
          "error.stack" { it == null || it instanceof String }
        }
        if (endpoint.query) {
          "$DDTags.HTTP_QUERY" endpoint.query
        }
        defaultTags()
      }
    }
  }

  @Override
  void serverSpan(TraceAssert trace, BigInteger traceID = null, BigInteger parentID = null, String method = "GET", ServerEndpoint endpoint = SUCCESS) {
    def dispatch = isDispatch()
    trace.span {
      serviceName expectedServiceName()
      operationName expectedOperationName()
      resourceName endpoint.status == 404 ? "404" : "$method ${endpoint.resolve(address).path}"
      spanType DDSpanTypes.HTTP_SERVER
      errored endpoint.errored
      if (parentID != null) {
        traceId traceID
        parentId parentID
      } else {
        parent()
      }
      tags {
        "$Tags.COMPONENT" component
        "$Tags.SPAN_KIND" Tags.SPAN_KIND_SERVER
        "$Tags.PEER_HOST_IPV4" { it == null || it == "127.0.0.1" } // Optional
        "$Tags.PEER_PORT" Integer
        "$Tags.HTTP_URL" "${endpoint.resolve(address)}"
        "$Tags.HTTP_METHOD" method
        if (endpoint.status > 0) {
          "$Tags.HTTP_STATUS" endpoint.status
        } else {
          "timeout" 1_000
        }
        if (context) {
          "servlet.context" "/$context"
        }

        if (dispatch) {
          "servlet.path" endpoint.status == 404 ? endpoint.path : "/dispatch$endpoint.path"
          "servlet.dispatch" endpoint.path
        } else {
          "servlet.path" endpoint.path
        }

        if (endpoint.errored) {
          "error.msg" { it == null || it == EXCEPTION.body }
          "error.type" { it == null || it == Exception.name }
          "error.stack" { it == null || it instanceof String }
        }
        if (endpoint.query) {
          "$DDTags.HTTP_QUERY" endpoint.query
        }
        defaultTags(true)
      }
    }
  }
}
