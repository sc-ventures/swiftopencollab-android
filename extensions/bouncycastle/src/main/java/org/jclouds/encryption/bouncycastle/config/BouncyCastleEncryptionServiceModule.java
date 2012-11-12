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
package org.jclouds.encryption.bouncycastle.config;

import org.jclouds.encryption.EncryptionService;
import org.jclouds.encryption.bouncycastle.BouncyCastleEncryptionService;

import com.google.inject.AbstractModule;

/**
 * Configures EncryptionService of type {@link BouncyCastleEncryptionService}
 * 
 * @author Adrian Cole
 * 
 */
public class BouncyCastleEncryptionServiceModule extends AbstractModule {

   @Override
   protected void configure() {
      bind(EncryptionService.class).to(BouncyCastleEncryptionService.class);
   }

}
