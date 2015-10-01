package io.paulbaker.mitm.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.log4j.Logger;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.MitmManager;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.mitm.HostNameMitmManager;
import org.littleshoot.proxy.mitm.RootCertificateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;
import java.util.Set;


/**
 * Created by paul on 9/28/15.
 */
@Configuration
public class ProxyConfig {

  private Logger logger = Logger.getLogger(this.getClass());

  @Autowired
  private Random random;

  @Value("#{'${application.whitelist}'.split(',')}")
  private Set<String> whitelist;

  @Bean
  public HttpFiltersSource httpFiltersSource() {
    return new HttpFiltersSourceAdapter() {
      @Override
      public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        String uri = originalRequest.getUri().toLowerCase();
        if (whitelist.stream().anyMatch(uri::contains)) {
          return new LoggingFilterAdapter(originalRequest, ctx);
        } else {
          return new DummyFilterAdapter();
        }
      }
    };
  }

  @Bean
  public MitmManager mitmManager() throws RootCertificateException {
    HostNameMitmManager hostNameMitmManager = new HostNameMitmManager();
    return hostNameMitmManager;
  }

  @Bean(destroyMethod = "stop")
  public HttpProxyServer httpProxyServer(HttpFiltersSource httpFiltersSource, MitmManager mitmManager, @Value("${application.proxy.port}") int portNumber) {
    HttpProxyServer proxyServer;
    proxyServer = DefaultHttpProxyServer.bootstrap()
      .withPort(portNumber)
      .withManInTheMiddle(mitmManager)
      .withFiltersSource(httpFiltersSource)
      .withTransparent(true)
      .start();
    return proxyServer;
  }

  public class LoggingFilterAdapter extends HttpFiltersAdapter {
    public LoggingFilterAdapter(HttpRequest originalRequest, ChannelHandlerContext clientCtx) {
      super(originalRequest, clientCtx);
    }

//    public LoggingFilterAdapter(HttpRequest originalRequest) {
//      super(originalRequest);
//    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
      if (httpObject instanceof HttpRequest) {
        HttpRequest request = (HttpRequest) httpObject;
        StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("line.separator")).append(request.getMethod()).append("->").append(request.getUri());
        sb.append(System.getProperty("line.separator")).append("\t").append("HEADERS->");
        request.headers().forEach(header -> sb.append(header.getKey()).append(": ").append(header.getValue()).append("; "));
        logger.info(sb.toString());
      } else {
        logger.info("REQUEST " + httpObject.getClass());
      }
//      if (httpObject instanceof DefaultLastHttpContent) {
//        DefaultLastHttpContent content = (DefaultLastHttpContent) httpObject;
//        StringBuilder sb = new StringBuilder("TRAILING -> ");
//        content.trailingHeaders().forEach(header -> sb.append(header.getKey()+": " + header.getValue()).append("; "));
//        logger.info(sb.toString());
//      }
      return super.clientToProxyRequest(httpObject);
    }

    @Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
      logger.info("RESPONSE -> " + httpObject.getClass());
//      if (httpObject instanceof HttpResponse) {
//        HttpResponse response = (HttpResponse) httpObject;
//        response.
//        String string = response.getDecoderResult().toString();
//        logger.info("DECODER -> " + string);
//      }
      return super.serverToProxyResponse(httpObject);
    }


  }

  /**
   * All requests are blocked. A simple 200 OK is returned back for all requests using the DummyFilter
   */
  private class DummyFilterAdapter extends HttpFiltersAdapter {

    private DefaultHttpResponse dummyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(200));

    public DummyFilterAdapter() {
      super(null, null);
    }

    /**
     * This is the only method that needs to be overwritten. It returns nothing.
     *
     * @param httpObject
     * @return
     */
    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
      if (httpObject instanceof HttpRequest) {
        HttpRequest request = (HttpRequest) httpObject;
        StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("line.separator")).append("[BLOCKED]").append(request.getMethod()).append("->").append(request.getUri());
        sb.append(System.getProperty("line.separator")).append("\t").append("HEADERS->");
        request.headers().forEach(header -> sb.append(header.getKey()).append(": ").append(header.getValue()).append("; "));
        logger.info(sb.toString());
      }
      return dummyResponse;
    }

//    @Override
//    public HttpResponse proxyToServerRequest(HttpObject httpObject) {
//      return dummyResponse;
//    }

    /**
     * Returns nothing to kill the request.
     *
     * @param httpObject
     * @return
     */
    @Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
//      return dummyResponse;
      return null;
    }

//    @Override
//    public HttpObject proxyToClientResponse(HttpObject httpObject) {
//      return dummyResponse;
//      return null;
//    }
  }
}
