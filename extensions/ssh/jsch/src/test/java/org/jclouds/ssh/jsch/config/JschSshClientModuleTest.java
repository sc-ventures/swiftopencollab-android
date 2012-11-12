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
package org.jclouds.ssh.jsch.config;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.jclouds.ssh.SshClient;
import org.jclouds.ssh.jsch.JschSshClient;
import org.testng.annotations.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import static org.testng.Assert.assertEquals;

/**
 * Tests the ability to configure a {@link JschSshClient}
 * 
 * @author Adrian Cole
 */
@Test
public class JschSshClientModuleTest {

   public void testConfigureBindsClient() throws UnknownHostException {

      Injector i = Guice.createInjector(new JschSshClientModule());
      SshClient.Factory factory = i.getInstance(SshClient.Factory.class);
      SshClient connection = factory.create(new InetSocketAddress(InetAddress.getLocalHost(), 22),
               "username", "password");
      assert connection instanceof JschSshClient;
      Map<String, String> keyPair = factory.generateRSAKeyPair("comment", "hola");
      assertEquals(keyPair.get("comment"), "comment");
      assertEquals(keyPair.get("passphrase"), "hola");
      assert keyPair.get("private").indexOf("-----BEGIN RSA PRIVATE KEY-----") == 0 : keyPair;
      assert keyPair.get("public").indexOf("ssh-rsa ") == 0 : keyPair;
   }
}