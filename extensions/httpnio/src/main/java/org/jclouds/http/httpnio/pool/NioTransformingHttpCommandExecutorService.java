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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.http.nio.NHttpConnection;
import org.jclouds.Constants;
import org.jclouds.http.HttpCommandRendezvous;
import org.jclouds.http.pool.ConnectionPoolTransformingHttpCommandExecutorService;

/**
 * // TODO: Adrian: Document this!
 * 
 * @author Adrian Cole
 */
@Singleton
public class NioTransformingHttpCommandExecutorService extends
         ConnectionPoolTransformingHttpCommandExecutorService<NHttpConnection> {

   @Override
   public String toString() {
      return String.format("NioTransformingHttpCommandExecutorService [ hashCode=%d ]", hashCode());
   }

   @Inject
   public NioTransformingHttpCommandExecutorService(
            @Named(Constants.PROPERTY_USER_THREADS) ExecutorService executor,
            NioHttpCommandConnectionPool.Factory poolFactory,
            BlockingQueue<HttpCommandRendezvous<?>> commandQueue) {
      super(executor, poolFactory, commandQueue);
   }

}
