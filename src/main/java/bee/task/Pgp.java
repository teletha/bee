/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import bee.TaskFailure;
import bee.api.Command;
import bee.api.Library;
import bee.api.Repository;
import bee.api.Task;
import bee.task.pgp.PgpMojo;
import bee.util.pgp.PGP;

/**
 * @version 2017/01/11 16:20:19
 */
public class Pgp extends Task {

    static {
        Repository.require("org.bouncycastle", "bcprov-jdk15on", "1.56");
        Repository.require("org.bouncycastle", "bcpg-jdk15on", "1.56");

        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * How to retrieve the secret key?
     */
    protected String secretkey;

    /**
     * String that indicates where to retrieve the secret key pass phrase from.
     */
    protected String passphrase;

    @Command("Install GnuPG.")
    public void test() throws Exception {
        Library library = project.getLibrary();

        // capture the attached artifacts to sign before we start attaching our own stuff
        List<Path> attached = new ArrayList<>();
        attached.add(library.getLocalJar());
        attached.add(library.getLocalJavadocJar());
        attached.add(library.getLocalSourceJar());
        attached.add(library.getLocalPOM());

        PGP.generateRSAKeyPair(project.getRoot(), "test", "test");

        // PGPSecretKey secretKey = loadSecretKey();
        // Signer signer = new Signer(secretKey, loadPassPhrase(secretKey).toCharArray());
        //
        // for (Path file : attached) {
        // sign(signer, file);
        // }
    }

    /**
     * From {@link #secretkey}, load the key pair.
     */
    public PGPSecretKey loadSecretKey() {
        if (secretkey == null) {
            secretkey = System.getenv("PGP_SECRETKEY");
        }

        if (secretkey == null) {
            ui.talk("No PGP secret key is configured. Either do so in project definition, or the PGP_SECRETKEY environment variable.");
            secretkey = ui.ask("Input your PGP secret key.", this::validateSecretKey);
        }

        int index = secretkey.indexOf(':');
        String scheme = secretkey.substring(0, index);

        try {
            SecretKeyLoader keyLoader = new KeyFileLoader();
            return keyLoader.load(secretkey.substring(index + 1));
        } catch (IOException e) {
            throw new TaskFailure("Failed to load key from " + secretkey, e);
        }
    }

    /**
     * From {@link #passphrase}, load the passphrase.
     */
    public String loadPassPhrase(PGPSecretKey key) {
        if (passphrase == null) {
            passphrase = System.getenv("PGP_PASSPHRASE");
        }

        if (passphrase == null) {
            ui.talk("No PGP passphrase is configured. Either do so in project definition, or the PGP_PASSPHRASE environment variable.");
            passphrase = ui.ask("Input your PGP passphrase.", this::validatePassPhrase);
        }

        int index = passphrase.indexOf(':');
        String scheme = passphrase.substring(0, index);

        try {
            PassphraseLoader loader = new LiteralPassPhraseLoader();
            return loader.load(key, passphrase.substring(index + 1));
        } catch (IOException e) {
            throw new TaskFailure("Failed to load passphrase from " + passphrase, e);
        }
    }

    /**
     * <p>
     * Validate PGP secret key.
     * </p>
     * 
     * @param key
     * @return
     */
    private boolean validateSecretKey(String key) {
        if (key.indexOf(':') < 0) {
            ui.error("Invalid secret key string. It needs to start with a scheme like 'FOO:': " + key);
            return false;
        }
        return true;
    }

    /**
     * <p>
     * Validate PGP passphrase.
     * </p>
     * 
     * @param passphrase
     * @return
     */
    private boolean validatePassPhrase(String passphrase) {
        if (passphrase.indexOf(':') < 0) {
            ui.error("Invalid passphrase string. It needs to start with a scheme like 'FOO:': " + passphrase);
            return false;
        }
        return true;
    }

    /**
     * Sign and attach the signature to the build.
     */
    protected void sign(Signer signer, Path file) {
        Path asc = file.resolve(file.getFileName() + ".asc");

        try {
            signer.sign(file.toFile(), asc.toFile());
            ui.talk("Sign [" + asc + "].");
        } catch (Exception e) {
            throw new TaskFailure("Failed to sign " + file, e);
        }

        // projectHelper.attachArtifact(project, file.getArtifactHandler().getExtension() + ".asc",
        // file.getClassifier(), signature);
    }

    /**
     * Loads the secret key (a public key/private key pair) to generate a signature with.
     * <p>
     * Implementations should be plexus components, and its role hint is matched against the
     * passphrase loader configuration parameter's scheme portion.
     *
     * @author Kohsuke Kawaguchi
     */
    private abstract class SecretKeyLoader {
        /**
         * @param mojo Mojo that's driving the execution.
         * @param specifier The secretkey loader parameter specified to {@link PgpMojo}, except the
         *            first scheme part. If the loader needs to take additional parameters, it
         *            should do so from this string.
         */
        protected abstract PGPSecretKey load(String specifier) throws IOException;

        /**
         * Parses "a=b&c=d&..." into a map. Useful for creating a structure in the specifier
         * argument to the load method.
         */
        protected final Map<String, String> parseQueryParameters(String specifier) {
            Map<String, String> opts = new HashMap<String, String>();
            for (String token : specifier.split("&")) {
                int idx = token.indexOf('=');
                if (idx < 0)
                    opts.put(token, "");
                else
                    opts.put(token.substring(0, idx), token.substring(idx + 1));
            }
            return opts;
        }
    }

    /**
     * Loads PGP secret key from the exported key file, which normally ends with the ".asc"
     * extension and has a "-----BEGIN PGP PRIVATE KEY BLOCK-----" header.
     *
     * @author Kohsuke Kawaguchi
     */
    private class KeyFileLoader extends SecretKeyLoader {

        /**
         * {@inheritDoc}
         */
        @Override
        public PGPSecretKey load(String keyFile) throws IOException {
            FileInputStream in = new FileInputStream(new File(keyFile));
            try {
                BcKeyFingerprintCalculator calculator = new BcKeyFingerprintCalculator();
                PGPObjectFactory pgpF = new PGPObjectFactory(PGPUtil.getDecoderStream(in), calculator);
                Object o = pgpF.nextObject();
                if (!(o instanceof PGPSecretKeyRing)) {
                    throw new IOException(keyFile + " doesn't contain PGP private key");
                }
                PGPSecretKeyRing keyRing = (PGPSecretKeyRing) o;
                return keyRing.getSecretKey();
            } finally {
                in.close();
            }
        }
    }

    /**
     * Loads a key from a keyring.
     * 
     * @author Kohsuke Kawaguchi
     */
    private class KeyRingLoader extends SecretKeyLoader {

        /**
         * {@inheritDoc}
         */
        @Override
        public PGPSecretKey load(String specifier) throws IOException {
            Map<String, String> opts = parseQueryParameters(specifier);

            File keyFile;
            if (opts.containsKey("keyring")) {
                keyFile = new File(opts.get("keyring"));
            } else {
                keyFile = new File(new File(System.getProperty("user.home")), ".gnupg/secring.gpg");
            }
            if (!keyFile.exists()) throw new IOException("No such key ring file exists: " + keyFile);

            String id = opts.get("id");

            InputStream in = PGPUtil.getDecoderStream(new FileInputStream(keyFile));

            try {
                KeyFingerPrintCalculator calculator = new BcKeyFingerprintCalculator();
                PGPObjectFactory pgpFact = new PGPObjectFactory(in, calculator);

                Object obj;
                while ((obj = pgpFact.nextObject()) != null) {
                    if (!(obj instanceof PGPSecretKeyRing)) throw new IOException("Expecting a secret key but found " + obj);

                    PGPSecretKeyRing key = (PGPSecretKeyRing) obj;

                    if (id == null) return key.getSecretKey(); // pick up the first one if no key ID
                                                               // specifier is given

                    Iterator jtr = key.getSecretKeys();
                    while (jtr.hasNext()) {
                        PGPSecretKey skey = (PGPSecretKey) jtr.next();

                        if (id.equalsIgnoreCase(Long.toHexString(skey.getPublicKey().getKeyID() & 0xFFFFFFFF))) return skey;

                        for (Iterator ktr = skey.getUserIDs(); ktr.hasNext();) {
                            String s = (String) ktr.next();
                            if (s.contains(id)) return skey;
                        }

                    }
                }

                throw new IOException("No key that matches " + id + " was found in " + keyFile);
            } finally {
                in.close();
            }
        }
    }

    /**
     * Loads a pass-phrase for the specified key.
     * <p>
     * Implementations should be plexus components, and its role hint is matched against the
     * passphrase loader configuration parameter's scheme portion.
     *
     * @author Kohsuke Kawaguchi
     */
    private abstract class PassphraseLoader {
        /**
         * Obtains the pass-phrase.
         *
         * @param mojo Mojo that's driving the execution.
         * @param secretKey The key for which the pass-phrase is retrieved.
         * @param specifier The pass phrase loader parameter specified to {@link PgpMojo}, except
         *            the first scheme part. If the loader needs to take additional parameters, it
         *            should do so from this string.
         * @return the passphrase.
         */
        protected abstract String load(PGPSecretKey secretKey, String specifier) throws IOException;
    }

    /**
     * Specifies a pass phrase directly as literal.
     * 
     * @author Kohsuke Kawaguchi
     */
    private class LiteralPassPhraseLoader extends PassphraseLoader {

        /**
         * {@inheritDoc}
         */
        @Override
        public String load(PGPSecretKey secretKey, String specifier) throws IOException {
            return specifier;
        }
    }

    /**
     * Generates a PGP signature.
     *
     * @author Kohsuke Kawaguchi
     */
    private static class Signer {
        /* package */ static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

        private final PGPPrivateKey privateKey;

        private final PGPPublicKey publicKey;

        /**
         * @param privateKey
         * @param publicKey
         */
        Signer(PGPPrivateKey privateKey, PGPPublicKey publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }

        /**
         * @param secretKey
         * @param passphrase
         */
        Signer(PGPSecretKey secretKey, char[] passphrase) {
            try {
                JcePBESecretKeyDecryptorBuilder builder = new JcePBESecretKeyDecryptorBuilder();
                builder.setProvider(PROVIDER);
                PBESecretKeyDecryptor decryptor = builder.build(passphrase);

                this.privateKey = secretKey.extractPrivateKey(decryptor);
                if (this.privateKey == null) throw new IllegalArgumentException("Unsupported signing key" + (secretKey
                        .getKeyEncryptionAlgorithm() == PGPPublicKey.RSA_SIGN ? ": RSA (sign-only) is unsupported by BouncyCastle" : ""));
                this.publicKey = secretKey.getPublicKey();
            } catch (PGPException e) {
                throw new IllegalArgumentException("Passphrase is incorrect", e);
            }
        }

        /**
         * @param in
         * @return
         * @throws IOException
         * @throws PGPException
         * @throws GeneralSecurityException
         */
        PGPSignature sign(InputStream in) throws IOException, PGPException, GeneralSecurityException {
            PGPContentSignerBuilder builder = new BcPGPContentSignerBuilder(publicKey.getAlgorithm(), PGPUtil.SHA1);
            PGPSignatureGenerator generator = new PGPSignatureGenerator(builder);
            generator.init(PGPSignature.BINARY_DOCUMENT, privateKey);

            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) >= 0)
                generator.update(buf, 0, len);
            return generator.generate();
        }

        /**
         * Generates the signature of the given input stream as an ASCII file into the given output
         * stream.
         */
        void sign(InputStream in, OutputStream signatureOutput) throws PGPException, IOException, GeneralSecurityException {
            BCPGOutputStream bOut = new BCPGOutputStream(new ArmoredOutputStream(signatureOutput));
            sign(in).encode(bOut);
            bOut.close();
        }

        void sign(File in, File signature) throws PGPException, IOException, GeneralSecurityException {
            InputStream fin = new BufferedInputStream(new FileInputStream(in));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(signature));
            try {
                sign(fin, out);
            } finally {
                fin.close();
                out.close();
            }
        }
    }
}
