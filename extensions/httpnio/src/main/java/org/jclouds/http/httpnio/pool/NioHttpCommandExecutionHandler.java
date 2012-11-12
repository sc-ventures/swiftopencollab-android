/**
 *
 * Copyright (C) 2009 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */
package org.jclouds.http.httpnio.pool;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.HttpHeaders;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.protocol.HttpContext;
import org.jclouds.Constants;
import org.jclouds.http.HttpCommand;
import org.jclouds.http.HttpCommandRendezvous;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpRequestFilter;
import org.jclouds.http.HttpUtils;
import org.jclouds.http.Payloads;
import org.jclouds.http.handlers.DelegatingErrorHandler;
import org.jclouds.http.handlers.DelegatingRetryHandler;
import org.jclouds.http.httpnio.util.NioHttpUtils;
import org.jclouds.http.internal.HttpWire;
import org.jclouds.logging.Logger;

import com.google.common.io.Closeables;

/**
 * // TODO: Adrian: Document this!
 * 
 * @author Adrian Cole
 */
public class NioHttpCommandExecutionHandler implements NHttpRequestExecutionHandler {
   private final ConsumingNHttpEntityFactory EntityFactory;
   private final DelegatingRetryHandler retryHandler;
   private final DelegatingErrorHandler errorHandler;
   private final HttpWire wire;

   /**
    * inputOnly: nothing is taken from this queue.
    */
   private final BlockingQueue<HttpCommandRendezvous<?>> resubmitQueue;

   @Resource
   protected Logger logger = Logger.NULL;
   @Resource
   @Named(Constants.LOGGER_HTTP_HEADERS)
   protected Logger headerLog = Logger.NULL;

   @Inject
   public NioHttpCommandExecutionHandler(ConsumingNHttpEntityFactory EntityFactory,
            BlockingQueue<HttpCommandRendezvous<?>> resubmitQueue,
            DelegatingRetryHandler retryHandler, DelegatingErrorHandler errorHandler, HttpWire wire) {
      this.EntityFactory = EntityFactory;
      this.resubmitQueue = resubmitQueue;
      this.retryHandler = retryHandler;
      this.errorHandler = errorHandler;
      this.wire = wire;
   }

   public interface ConsumingNHttpEntityFactory {
      public ConsumingNHttpEntity create(HttpEntity httpEntity);
   }

   public void initalizeContext(HttpContext context, Object attachment) {
   }

   public HttpEntityEnclosingRequest submitRequest(HttpContext context) {
      HttpCommandRendezvous<?> rendezvous = (HttpCommandRendezvous<?>) context
               .removeAttribute("command");
      if (rendezvous != null) {
         HttpRequest request = rendezvous.getCommand().getRequest();
         for (HttpRequestFilter filter : request.getFilters()) {
            filter.filter(request);
         }
         logger.debug("Sending request %s: %s", request.hashCode(), request.getRequestLine());
         if (request.getPayload() != null && wire.enabled())
            request.setPayload(Payloads.newPayload(wire
                     .output(request.getPayload().getRawContent())));
         HttpEntityEnclosingRequest nativeRequest = NioHttpUtils.convertToApacheRequest(request);
         HttpUtils.logRequest(headerLog, request, ">>");
         return nativeRequest;
      }
      return null;
   }

   public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
            throws IOException {
      return EntityFactory.create(response.getEntity());
   }

   public void handleResponse(HttpResponse apacheResponse, HttpContext context) throws IOException {
      NioHttpCommandConnectionHandle handle = (NioHttpCommandConnectionHandle) context
               .removeAttribute("command-handle");
      if (handle != null) {
         try {
            HttpCommandRendezvous<?> rendezvous = handle.getCommandRendezvous();
            HttpCommand command = rendezvous.getCommand();
            org.jclouds.http.HttpResponse response = NioHttpUtils
                     .convertToJCloudsResponse(apacheResponse);
            logger
                     .debug("Receiving response %s: %s", response.hashCode(), response
                              .getStatusLine());
            HttpUtils.logResponse(headerLog, response, "<<");
            if (response.getContent() != null && wire.enabled())
               response.setContent(wire.input(response.getContent()));
            int statusCode = response.getStatusCode();
            if (statusCode >= 300) {
               if (retryHandler.shouldRetryRequest(command, response)) {
                  resubmitQueue.add(rendezvous);
               } else {
                  errorHandler.handleError(command, response);
                  Closeables.closeQuietly(response.getContent());
                  assert command.getException() != null : "errorHandler should have set an exception!";
                  rendezvous.setException(command.getException());
               }
            } else {
               // Close early, if there is no content.
               String header = response.getFirstHeaderOrNull(HttpHeaders.CONTENT_LENGTH);
               if (response.getStatusCode() == 204 || (header != null && header.equals("0"))) {
                  Closeables.closeQuietly(response.getContent());
               }
               // TODO, the connection should be released when the input stream is closed
               rendezvous.setResponse(response);
            }
         } catch (InterruptedException e) {
            logger.error(e, "interrupted processing response task");
         } finally {
            // This here is probably invalid, as the connection is really tied to the server
            // response.
            releaseConnectionToPool(handle);
         }
      } else {
         throw new IllegalStateException(String.format(
                  "No command-handle associated with command %1$s", context));
      }
   }

   protected void releaseConnectionToPool(NioHttpCommandConnectionHandle handle) {
      try {
         handle.release();
      } catch (InterruptedException e) {
         logger.error(e, "Interrupted releasing handle %1$s", handle);
      }
   }

   public void finalizeContext(HttpContext context) {
      NioHttpCommandConnectionHandle handle = (NioHttpCommandConnectionHandle) context
               .removeAttribute("command-handle");
      if (handle != null) {
         try {
            handle.cancel();
         } catch (Exception e) {
            logger.error(e, "Error cancelling handle %1$s", handle);
         }
      }
   }
}