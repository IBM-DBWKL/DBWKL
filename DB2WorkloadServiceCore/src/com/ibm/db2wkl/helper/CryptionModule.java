/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.db2wkl.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.ibm.db2wkl.DB2WorkloadServiceDirectory;
import org.apache.commons.codec.binary.Base64;
import com.ibm.staf.STAFResult;

/**
 * This class uses public and private RSA encrypted BASE64 encoded security keys
 * to encrypt and decrypt incoming passwords or messages
 * 
 */
public class CryptionModule {

	/**
	 * CryptionModule Instance
	 */
	private static CryptionModule cryptionModule;

	/**
	 * Cipher Instance for crypting
	 */
	private Cipher cipher;

	/**
	 * Base64 encoded Private Key
	 */
	private final static String PRIVATE_KEY = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJQsTlFtMJUa3V6KVNypLnINBLH9bKLIu8bC9+BEuvZsn3JUoiKTc4TM6YfK6osKvurn3JJUixS5dxNfc+61slM0KWCIChcuBydh4kxUUqbWAFvy5EP1RhZ/XGoU93Kb+LI30QhWcXxVNUCmwe8q8DYxmUYyNDDMqW2ruQGnFZvlAgMBAAECgYAyf1MFnx/CgRBWivBW73V7uwIRotumMqEEISgXD3VkTCqe7UcAX54r8SXZeIcscbIVHoXmfNeVbYuuV4aMIPIvsFJt+X8l+WKkW14ZxzzTzF1us2gBCzQWrK3y3c+1wiYpWrOUIPCaP5+x1xA05TAxboW2zl82qtj24BVEz7UdgQJBAOcNeTGJLAsjZR3hj6IOia+K7r1kdyQRKBzZ57okB1QRWiK/aIBpQe0fVMHTCatWXy8v5qLsQHRyuULpXnxwBsUCQQCkK/VSeqS7SmQbSQOD34NRnB747URQ3fuu5VC9QOJtoCA96Hpz/Z+xM4iUjQNOXxFmNUaGrEZ31GNoeh8yAZKhAkA7wfpUW3vurYrbfZkeetAVfMNebHt78owDWkBanjLfBVXgosyuWYrZfz72mlRn4gDgPW1TOfM5qupLafwsBvVZAkAlVbncn/eGgExzyA78loAOtyp0AcFfgpwSEiRiZbcYpymt6oiuiCcg7U9KOSdfU09ppwP67IK6DZrtz5f4j1MBAkEA2wlNtN5qRU8+6o0dkinnfvj0wkndAnIjsTN7DBOVIsV9Eu5K5naYs+C155i2RXNPF7DGOIKcIMfydSJNdsSKxA==";

	/**
	 * Base64 encoded Public Key
	 */
	private final static String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCULE5RbTCVGt1eilTcqS5yDQSx/WyiyLvGwvfgRLr2bJ9yVKIik3OEzOmHyuqLCr7q59ySVIsUuXcTX3PutbJTNClgiAoXLgcnYeJMVFKm1gBb8uRD9UYWf1xqFPdym/iyN9EIVnF8VTVApsHvKvA2MZlGMjQwzKltq7kBpxWb5QIDAQAB";

	/**
	 * SecurityPrefix for encrypted messages
	 */
	public static final String SECURITY_PREFIX = "secured:";

	/**
	 * SecurityPrefix for encrypted messages
	 */
	public static final String PASSFILE_PREFIX = "file:";
	
	/**
	 * Default path to the password file
	 */
	private String passpath = DB2WorkloadServiceDirectory.getDB2WorkloadServiceDirectory() + "/pass";

	/**
	 * Default private constructor for singelton Instance
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 */
	private CryptionModule() throws NoSuchAlgorithmException, NoSuchPaddingException {
		this.cipher = Cipher.getInstance("RSA");
	}

	/**
	 * Returns a singelton instance of CryptionModule
	 * 
	 * @return cryptionModule as singelton instance
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static CryptionModule getInstance() throws NoSuchAlgorithmException, NoSuchPaddingException {
		if (cryptionModule == null) {
			cryptionModule = new CryptionModule();
		}
		return cryptionModule;
	}

	/**
	 * @param plaintextedPassword
	 * @return encrpytedPassword
	 * @throws IOException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	public STAFResult getEncryptedPassword(String plaintextedPassword) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		String encryptedPassword = null;
		
		RSAPublicKey publicKey = getPublicKey();

		this.cipher.init(Cipher.ENCRYPT_MODE, publicKey);

		byte[] raw = plaintextedPassword.getBytes();

			// encrypt the message
		byte[] passBytes = this.cipher.doFinal(raw);

			// encode the encrypted message as base64
		encryptedPassword = Base64.encodeBase64String(passBytes);
		
		//omits CRLF
		encryptedPassword = encryptedPassword.replace("\r\n", "");
		
		return new STAFResult(STAFResult.Ok, encryptedPassword);
	}

	/**
	 * @return PublicKey Instance
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws InvalidKeySpecException
	 */
	private RSAPublicKey getPublicKey() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		KeySpec keySpec = new X509EncodedKeySpec(Base64.decodeBase64(PUBLIC_KEY));
		RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);

		return publicKey;
	}

	/**
	 * @param encryptedPass
	 * @return decryptedPassword
	 * @throws IOException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws IllegalArgumentException 
	 */
	public String getDecryptedPassword(String encryptedPass) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, IllegalArgumentException {
		String encryptedPassword = encryptedPass;
		String decryptedPassword = null;
		byte[] passbytes;
		
		if (encryptedPassword.startsWith(SECURITY_PREFIX)) {
			encryptedPassword = encryptedPassword.replace(SECURITY_PREFIX,"");
		} else if(encryptedPassword.startsWith(PASSFILE_PREFIX)){
			encryptedPassword = encryptedPassword.replace(PASSFILE_PREFIX,"");
			encryptedPassword = readPassfile(encryptedPassword);
		} else {
			throw new IllegalArgumentException("The encrpted password is not of valid form. It must start with " + SECURITY_PREFIX + " or " + PASSFILE_PREFIX);
		}
		passbytes = Base64.decodeBase64(encryptedPassword);

		this.cipher.init(Cipher.DECRYPT_MODE, getPrivateKey());
			// decode the message
		byte[] decryptedBytes = this.cipher.doFinal(passbytes);

		decryptedPassword = new String(decryptedBytes, "UTF8");
		
		return decryptedPassword;
	}

	/**
	 * @return PrivateKey Instance
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	private RSAPrivateKey getPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] privateKeyBytes = null;
		RSAPrivateKey privateKey = null;

		privateKeyBytes = getPrivateKeyBytes();
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		KeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
		privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

		return privateKey;
	}

	/**
	 * @return PrivateKeyBytes of the privateKeyFile
	 * @throws IOException
	 */
	private byte[] getPrivateKeyBytes() throws IOException {
		byte[] privateKeyBytes = Base64.decodeBase64(PRIVATE_KEY);

		return privateKeyBytes;
	}

	/**
	 * @param path
	 * @return byte Array with the encrypted password
	 * @throws IOException
	 */
	public String readPassfile(String path) throws IOException {
		String encryptedPassword;
		byte[] passbytes;

		if (path != null) {
			if (path.length() != 0) {
				this.passpath = path;
			}
		}

		File passfile = new File(this.passpath);
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(passfile));
		passbytes = new byte[(int) passfile.length()];
		bis.read(passbytes);
		bis.close();

		encryptedPassword = new String(passbytes, "UTF8");

		return encryptedPassword;
	}

	/**
	 * @param result
	 *            as encryptedPassword
	 * @param path
	 * @return STAFResult
	 */
	public STAFResult writePassfile(String result, String path) {
		byte[] passbytes;

		if (path != null) {
			if (path.length() != 0) {
				this.passpath = path;
			}
		}

		try {
			File passfile = new File(this.passpath);
			BufferedOutputStream bos = new BufferedOutputStream(
					new FileOutputStream(passfile));
			passbytes = result.getBytes();
			bos.write(passbytes);
			bos.close();
		} catch (IOException e) {
			return new STAFResult(STAFResult.InvalidRequestString, "Error while wrting to path");
		}

		return new STAFResult(STAFResult.Ok, "Encrypted Password saved in " + this.passpath);
	}
}