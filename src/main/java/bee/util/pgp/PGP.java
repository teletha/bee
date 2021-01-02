/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util.pgp;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;

/**
 * @version 2017/01/16 12:04:25
 */
public class PGP {

    static {
        // Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * <p>
     * A simple utility that generates a RSA PGPPublicKey/PGPSecretKey pair.
     * </p>
     */
    public static void generateRSAKeyPair(Path directory, String identity, String passPhrase)
            throws IOException, NoSuchProviderException, SignatureException, NoSuchAlgorithmException {
        // directory = Paths.createDirectory(directory);
        // Path secretFile = directory.resolve("secret.asc");
        // Path publicFile = directory.resolve("pub.asc");
        //
        // KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        // kpg.initialize(1024);
        // KeyPair pair = kpg.generateKeyPair();
        //
        // PGPDigestCalculator sha1Calc = new
        // JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);
        // PGPKeyPair keyPair = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, pair, new Date());
        // JcePBESecretKeyEncryptorBuilder encryptor = new
        // JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.CAST5, sha1Calc);
        // PGPContentSignerBuilder signer = new
        // JcaPGPContentSignerBuilder(keyPair.getPublicKey().getAlgorithm(),
        // HashAlgorithmTags.SHA1);
        // PGPSecretKey secretKey = new PGPSecretKey(PGPSignature.DEFAULT_CERTIFICATION, keyPair,
        // identity, sha1Calc, null, null, signer, encryptor
        // .setProvider("BC")
        // .build(passPhrase.toCharArray()));
        //
        // // generate secret key
        // try (OutputStream secretOut = new ArmoredOutputStream(Files.newOutputStream(secretFile)))
        // {
        // secretKey.encode(secretOut);
        // }
        //
        // // generate public key
        // try (OutputStream publicOut = new ArmoredOutputStream(Files.newOutputStream(publicFile)))
        // {
        // secretKey.getPublicKey().encode(publicOut);
        // }
    }
}