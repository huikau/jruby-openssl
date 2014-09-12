/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006, 2007 Ola Bini <ola@ologix.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.openssl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;

import static org.jruby.ext.openssl.OpenSSL.*;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Cipher extends RubyObject {
    private static final long serialVersionUID = -5390983669951165103L;

    private static ObjectAllocator CIPHER_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Cipher(runtime, klass);
        }
    };

    public static void createCipher(final Ruby runtime, final RubyModule OpenSSL) {
        final RubyClass Cipher = OpenSSL.defineClassUnder("Cipher", runtime.getObject(), CIPHER_ALLOCATOR);
        Cipher.defineAnnotatedMethods(Cipher.class);
        final RubyClass OpenSSLError = OpenSSL.getClass("OpenSSLError");
        Cipher.defineClassUnder("CipherError", OpenSSLError, OpenSSLError.getAllocator());

        String cipherName;

        cipherName = "AES"; // OpenSSL::Cipher::AES
        Cipher.defineClassUnder(cipherName, Cipher, new NamedCipherAllocator(cipherName))
              .defineAnnotatedMethods(Named.class);

        cipherName = "CAST5"; // OpenSSL::Cipher::CAST5
        Cipher.defineClassUnder(cipherName, Cipher, new NamedCipherAllocator(cipherName))
              .defineAnnotatedMethods(Named.class);

        cipherName = "BF"; // OpenSSL::Cipher::BF
        Cipher.defineClassUnder(cipherName, Cipher, new NamedCipherAllocator(cipherName))
              .defineAnnotatedMethods(Named.class);

        cipherName = "DES"; // OpenSSL::Cipher::DES
        Cipher.defineClassUnder(cipherName, Cipher, new NamedCipherAllocator(cipherName))
              .defineAnnotatedMethods(Named.class);

        cipherName = "IDEA"; // OpenSSL::Cipher::IDEA
        Cipher.defineClassUnder(cipherName, Cipher, new NamedCipherAllocator(cipherName))
              .defineAnnotatedMethods(Named.class);

        cipherName = "RC2"; // OpenSSL::Cipher::RC2
        Cipher.defineClassUnder(cipherName, Cipher, new NamedCipherAllocator(cipherName))
              .defineAnnotatedMethods(Named.class);

        cipherName = "RC4"; // OpenSSL::Cipher::RC4
        Cipher.defineClassUnder(cipherName, Cipher, new NamedCipherAllocator(cipherName))
              .defineAnnotatedMethods(Named.class);

        cipherName = "RC5"; // OpenSSL::Cipher::RC5
        Cipher.defineClassUnder(cipherName, Cipher, new NamedCipherAllocator(cipherName))
              .defineAnnotatedMethods(Named.class);

        String keyLength;

        keyLength = "128"; // OpenSSL::Cipher::AES128
        Cipher.defineClassUnder("AES" + keyLength, Cipher, new AESCipherAllocator(keyLength))
              .defineAnnotatedMethods(AES.class);
        keyLength = "192"; // OpenSSL::Cipher::AES192
        Cipher.defineClassUnder("AES" + keyLength, Cipher, new AESCipherAllocator(keyLength))
              .defineAnnotatedMethods(AES.class);
        keyLength = "256"; // OpenSSL::Cipher::AES256
        Cipher.defineClassUnder("AES" + keyLength, Cipher, new AESCipherAllocator(keyLength))
              .defineAnnotatedMethods(AES.class);
    }

    static RubyClass _Cipher(final Ruby runtime) {
        return (RubyClass) runtime.getModule("OpenSSL").getConstant("Cipher");
    }

    public static boolean isSupportedCipher(final String name) {
        final Collection<String> ciphers = getSupportedCiphers();
        return ciphers.contains( name.toUpperCase() );
    }

    @JRubyMethod(meta = true)
    public static IRubyObject ciphers(final ThreadContext context, final IRubyObject self) {
        final Ruby runtime = context.runtime;

        final Collection<String> ciphers = getSupportedCiphers();
        final RubyArray result = runtime.newArray(ciphers.size() * 2);
        for ( final String cipher : ciphers ) {
            result.append( runtime.newString(cipher) );
            result.append( runtime.newString(cipher.toLowerCase()) );
        }
        return result;
    }

    private static boolean supportedCiphersInitialized = false;
    static final Collection<String> supportedCiphers = new LinkedHashSet<String>(120, 1);

    private static Collection<String> getSupportedCiphers() {
        if ( supportedCiphersInitialized ) return supportedCiphers;
        synchronized ( supportedCiphers ) {
            if ( supportedCiphersInitialized ) return supportedCiphers;

            final Collection<Provider.Service> services = availableCipherServices();

            final String[] bases = {
                "AES-128", "AES-192", "AES-256",
                "BF", "DES", "DES-EDE", "DES-EDE3",
                "RC2", "CAST5",
                "Camellia-128", "Camellia-192", "Camellia-256",
                "SEED",
            };
            final String[] suffixes = {
                "", "-CBC", "-CFB", "-CFB1", "-CFB8", "-ECB", "-OFB"
            };
            for ( int i = 0; i < bases.length; i++ ) {
                for ( int k = 0; k < suffixes.length; k++ ) {
                    final String cipher = bases[i] + suffixes[k];
                    if ( supportedCipher( cipher, services ) ) {
                        supportedCiphers.add( cipher.toUpperCase() );
                    }
                }
            }
            final String[] other = {
                "AES128", "AES192", "AES256",
                "BLOWFISH",
                "RC2-40-CBC", "RC2-64-CBC",
                "RC4", "RC4-40", // "RC4-HMAC-MD5",
                "CAST", "CAST-CBC"
            };
            for ( int i = 0; i < other.length; i++ ) {
                final String cipher = other[i];
                if ( supportedCipher( cipher, services ) ) {
                    supportedCiphers.add( cipher.toUpperCase() );
                }
            }
            supportedCiphersInitialized = true;
            return supportedCiphers;
        }
    }

    private static boolean supportedCipher(final String osslName,
        final Collection<Provider.Service> services) {
        final Algorithm alg = Algorithm.osslToJava(osslName);

        for ( final Provider.Service service : services ) {
            if ( alg.base.equalsIgnoreCase( service.getAlgorithm() ) ) {
                if ( alg.mode == null ) return true;
                final String supportedModes = service.getAttribute("SupportedModes");
                if ( supportedModes != null && supportedModes.contains(alg.mode) ) {
                    // NOTE: we can not verify version at this point
                    // e.g. for AES whether it supports 256 bit keys
                    return true;
                }
                // NOTE: do not stop as there migth be multiple
            }
        }

        if ( isDebug() ) debug("supportedCipher( "+ osslName +" ) tryGetCipher = " + alg.realName);
        return tryGetCipher( alg.realName ); // last (slow but reliable) resort
    }

    private static boolean tryGetCipher(final String realName) {
        try {
            return getCipher(realName, true) != null;
        }
        catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static Collection<Provider.Service> availableCipherServices() {
        final Collection<Provider.Service> services = new ArrayList<Provider.Service>();

        if ( SecurityHelper.securityProvider != null && ! SecurityHelper.isProviderRegistered() ) {
            final Provider provider = SecurityHelper.securityProvider;
            for ( Provider.Service service : provider.getServices() ) {
                if ( "Cipher".equals( service.getType() ) ) {
                    services.add(service);
                }
            }
        }

        final Provider[] providers = java.security.Security.getProviders();
        for ( int i = 0; i < providers.length; i++ ) {
            final Provider provider = providers[i];
            // skip those that are known to provide no Cipher impls :
            if ( provider.getName().indexOf("JGSS") >= 0 ) continue; // SunJGSS
            if ( provider.getName().indexOf("SASL") >= 0 ) continue; // SunSASL
            if ( provider.getName().indexOf("XMLD") >= 0 ) continue; // XMLDSig
            if ( provider.getName().indexOf("PCSC") >= 0 ) continue; // SunPCSC
            if ( provider.getName().indexOf("JSSE") >= 0 ) continue; // SunJSSE

            for ( Provider.Service service : provider.getServices() ) {
                if ( "Cipher".equals( service.getType() ) ) {
                    services.add(service);
                }
            }
        }
        return services;
    }

    public static final class Algorithm {

        final String base; // DES
        final String version; // EDE3
        final String mode; // CBC
        final String padding; // PKCS5Padding
        final String realName;

        private Algorithm(String cryptoBase, String cryptoVersion, String cryptoMode,
            String realName, String padding) {
            this.base = cryptoBase;
            this.version = cryptoVersion;
            this.mode = cryptoMode;
            this.realName = realName;
            this.padding = padding;
        }

        private static final Set<String> BLOCK_MODES;

        static {
            BLOCK_MODES = new HashSet<String>();
            BLOCK_MODES.add("CBC");
            BLOCK_MODES.add("CFB");
            BLOCK_MODES.add("CFB1");
            BLOCK_MODES.add("CFB8");
            BLOCK_MODES.add("ECB");
            BLOCK_MODES.add("OFB");
            BLOCK_MODES.add("CTR");
            BLOCK_MODES.add("CTS"); // not supported by OpenSSL
            BLOCK_MODES.add("PCBC"); // not supported by OpenSSL
            BLOCK_MODES.add("NONE"); // valid to pass into JCE
        }

        @Deprecated
        public static String jsseToOssl(final String cipherName, final int keyLen) {
            return javaToOssl(cipherName, keyLen);
        }

        public static String javaToOssl(final String cipherName, final int keyLen) {
            String cryptoBase;
            String cryptoVersion = null;
            String cryptoMode = null;
            final String[] parts = cipherName.split("/");
            if ( parts.length != 1 && parts.length != 3 ) {
                return null;
            }
            if ( parts.length > 2 ) {
                cryptoMode = parts[1];
                // padding: parts[2] is not used
            }
            cryptoBase = parts[0];
            if ( ! BLOCK_MODES.contains(cryptoMode) ) {
                cryptoVersion = cryptoMode;
                cryptoMode = "CBC";
            }
            if (cryptoMode == null) {
                cryptoMode = "CBC";
            }
            if ( "DESede".equals(cryptoBase) ) {
                cryptoBase = "DES";
                cryptoVersion = "EDE3";
            }
            else if ( "Blowfish".equals(cryptoBase) ) {
                cryptoBase = "BF";
            }
            if (cryptoVersion == null) {
                cryptoVersion = String.valueOf(keyLen);
            }
            return cryptoBase + '-' + cryptoVersion + '-' + cryptoMode;
        }

        public static String getAlgorithmBase(javax.crypto.Cipher cipher) {
            final String algorithm = cipher.getAlgorithm();
            final int idx = algorithm.indexOf('/');
            if ( idx != -1 ) return algorithm.substring(0, idx);
            return algorithm;
        }

        public static String getRealName(final String osslName) {
            return osslToJava(osslName).realName;
        }

        static Algorithm osslToJava(final String osslName) {
            return osslToJava(osslName, null); // assume PKCS5Padding
        }

        private static Algorithm osslToJava(final String osslName, final String padding) {
            String cryptoBase;
            String cryptoVersion = null;
            String cryptoMode = "CBC"; // default
            String realName;

            int s = osslName.indexOf('-'); int i = 0;
            if (s == -1) { cryptoBase = osslName; }
            else {
                cryptoBase = osslName.substring(i, s);

                s = osslName.indexOf('-', i = s + 1);
                if (s == -1) cryptoMode = osslName.substring(i); // "base-mode"
                else { // two separators :  "base-version-mode"
                    cryptoVersion = osslName.substring(i, s);
                    //cryptoMode = osslName.substring(s + 1);
                    s = osslName.indexOf('-', i = s + 1);
                    if (s == -1) {
                        cryptoMode = osslName.substring(i);
                    }
                    else {
                        cryptoMode = osslName.substring(i, s);
                    }
                }
            }

            String paddingType;
            if (padding == null || padding.equalsIgnoreCase("PKCS5Padding")) {
                paddingType = "PKCS5Padding";
            }
            else if (padding.equals("0") || padding.equalsIgnoreCase("NoPadding")) {
                paddingType = "NoPadding";
            }
            else if (padding.equalsIgnoreCase("ISO10126Padding")) {
                paddingType = "ISO10126Padding";
            }
            else if (padding.equalsIgnoreCase("PKCS1Padding")) {
                paddingType = "PKCS1Padding";
            }
            else if (padding.equalsIgnoreCase("SSL3Padding")) {
                paddingType = "SSL3Padding";
            }
            //else if (padding.equalsIgnoreCase("OAEPPadding")) {
            //    paddingType = "OAEPPadding";
            //}
            else {
                paddingType = "PKCS5Padding"; // default
            }

            if ( "BF".equalsIgnoreCase(cryptoBase) ) {
                cryptoBase = "Blowfish";
            }

            if ( "CAST".equalsIgnoreCase(cryptoBase) ) {
                realName = "CAST5";
            } else if ( "DES".equalsIgnoreCase(cryptoBase) && "EDE3".equalsIgnoreCase(cryptoVersion) ) {
                realName = "DESede";
            } else {
                realName = cryptoBase;
            }

            final String cryptoModeUpper = cryptoMode.toUpperCase();
            if ( ! BLOCK_MODES.contains(cryptoModeUpper) ) {
                if ( ! "XTS".equals(cryptoModeUpper) ) { // valid but likely not supported in JCE
                    cryptoVersion = cryptoMode; cryptoMode = "CBC";
                }
            }
            else if ( "CFB1".equals(cryptoModeUpper) ) {
                cryptoMode = "CFB"; // uglish SunJCE mode normalization
            }

            if ( "RC4".equalsIgnoreCase(realName) ) {
                realName = "RC4"; cryptoMode = "NONE"; paddingType = "NoPadding";
            }
            else {
                realName = realName + '/' + cryptoMode + '/' + paddingType;
            }

            return new Algorithm(cryptoBase, cryptoVersion, cryptoMode, realName, paddingType);
        }

        public static int[] osslKeyIvLength(final String cipherName) {
            final Algorithm alg = Algorithm.osslToJava(cipherName);
            final String cryptoBaseUpper = alg.base.toUpperCase();
            final String cryptoVersion = alg.version;
            //final String mode = name[2];
            //final String realName = name[3];

            int keyLen = -1; int ivLen = -1;

            final boolean hasLen =
                "AES".equals(cryptoBaseUpper) || "RC2".equals(cryptoBaseUpper) || "RC4".equals(cryptoBaseUpper);
            if ( hasLen && cryptoVersion != null ) {
                try {
                    keyLen = Integer.parseInt(cryptoVersion) / 8;
                } catch (NumberFormatException e) {
                    keyLen = -1;
                }
            }
            if ( keyLen == -1 ) {
                if ( "DES".equals(cryptoBaseUpper) ) {
                    ivLen = 8;
                    if ( "EDE3".equalsIgnoreCase(cryptoVersion) ) {
                        keyLen = 24;
                    } else {
                        keyLen = 8;
                    }
                } else if ( "RC4".equals(cryptoBaseUpper) ) {
                    ivLen = 0;
                    keyLen = 16;
                } else {
                    keyLen = 16;
                    try {
                        int maxLen = javax.crypto.Cipher.getMaxAllowedKeyLength(cipherName) / 8;
                        if (maxLen < keyLen) keyLen = maxLen;
                    }
                    catch (NoSuchAlgorithmException e) { }
                }
            }

            if ( ivLen == -1 ) {
                if ( "AES".equals(cryptoBaseUpper) ) {
                    ivLen = 16;
                } else {
                    ivLen = 8;
                }
            }
            return new int[] { keyLen, ivLen };
        }

    }

    private static javax.crypto.Cipher getCipher(final String transformation, boolean silent)
        throws NoSuchAlgorithmException, NoSuchPaddingException {
        try {
            return SecurityHelper.getCipher(transformation); // tries BC if it's available
        }
        catch (NoSuchAlgorithmException e) {
            if ( silent ) return null;
            throw e;
        }
        catch (NoSuchPaddingException e) {
            if ( silent ) return null;
            throw e;
        }
    }

    public Cipher(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    private javax.crypto.Cipher cipher;
    private String name;
    private String cryptoBase;
    private String cryptoVersion;
    private String cryptoMode;
    private String paddingType;
    private String realName;
    private int keyLen = -1;
    private int generateKeyLen = -1;
    private int ivLen = -1;
    private boolean encryptMode = true;
    //private IRubyObject[] modeParams;
    private boolean cipherInited = false;
    private byte[] key;
    private byte[] realIV;
    private byte[] orgIV;
    private String padding;

    private void dumpVars(final PrintStream out) {
        out.println("***** Cipher instance vars ****");
        out.println("name = " + name);
        out.println("cryptoBase = " + cryptoBase);
        out.println("cryptoVersion = " + cryptoVersion);
        out.println("cryptoMode = " + cryptoMode);
        out.println("padding_type = " + paddingType);
        out.println("realName = " + realName);
        out.println("keyLen = " + keyLen);
        out.println("ivLen = " + ivLen);
        out.println("cipher block size = " + cipher.getBlockSize());
        out.println("encryptMode = " + encryptMode);
        out.println("cipherInited = " + cipherInited);
        out.println("key.length = " + (key == null ? 0 : key.length));
        out.println("iv.length = " + (realIV == null ? 0 : realIV.length));
        out.println("padding = " + padding);
        out.println("cipher alg = " + cipher.getAlgorithm());
        out.println("*******************************");
    }

    @JRubyMethod(required = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(final ThreadContext context, final IRubyObject name) {
        initializeImpl(context.runtime, name.toString());
        return this;
    }

    final void initializeImpl(final Ruby runtime, final String name) {
        if ( ! isSupportedCipher(name) ) {
            throw newCipherError(runtime, "unsupported cipher algorithm ("+ name +")");
        }
        if ( cipher != null ) {
            throw runtime.newRuntimeError("Cipher already inititalized!");
        }
        updateCipher(name, padding);
    }


    @Override
    @JRubyMethod(required = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(final IRubyObject obj) {
        if (this == obj) return this;

        checkFrozen();

        final Cipher other = (Cipher) obj;
        cryptoBase = other.cryptoBase;
        cryptoVersion = other.cryptoVersion;
        cryptoMode = other.cryptoMode;
        paddingType = other.paddingType;
        realName = other.realName;
        name = other.name;
        keyLen = other.keyLen;
        ivLen = other.ivLen;
        encryptMode = other.encryptMode;
        cipherInited = false;
        if ( other.key != null ) {
            key = Arrays.copyOf(other.key, other.key.length);
        } else {
            key = null;
        }
        if (other.realIV != null) {
            realIV = Arrays.copyOf(other.realIV, other.realIV.length);
        } else {
            realIV = null;
        }
        this.orgIV = this.realIV;
        padding = other.padding;

        cipher = getCipher();

        return this;
    }

    @JRubyMethod
    public IRubyObject name() {
        return getRuntime().newString(name);
    }

    @JRubyMethod
    public IRubyObject key_len() {
        return getRuntime().newFixnum(keyLen);
    }

    @JRubyMethod
    public IRubyObject iv_len() {
        return getRuntime().newFixnum(ivLen);
    }

    @JRubyMethod(name = "key_len=", required = 1)
    public IRubyObject set_key_len(IRubyObject len) {
        this.keyLen = RubyNumeric.fix2int(len);
        return len;
    }

    @JRubyMethod(name = "key=", required = 1)
    public IRubyObject set_key(final ThreadContext context, final IRubyObject key) {
        byte[] keyBytes;
        try {
            keyBytes = key.convertToString().getBytes();
        }
        catch (Exception e) {
            final Ruby runtime = context.runtime;
            debugStackTrace(runtime, e);
            throw newCipherError(runtime, e);
        }
        if (keyBytes.length < keyLen) {
            throw newCipherError(context.runtime, "key length too short");
        }

        if (keyBytes.length > keyLen) {
            byte[] keys = new byte[keyLen];
            System.arraycopy(keyBytes, 0, keys, 0, keyLen);
            keyBytes = keys;
        }

        this.key = keyBytes;
        return key;
    }

    @JRubyMethod(name = "iv=", required = 1)
    public IRubyObject set_iv(final ThreadContext context, final IRubyObject iv) {
        final byte[] ivBytes;
        try {
            ivBytes = iv.convertToString().getBytes();
        }
        catch (Exception e) {
            final Ruby runtime = context.runtime;
            debugStackTrace(runtime, e);
            throw newCipherError(runtime, e);
        }
        if ( ivBytes.length < ivLen ) {
            throw newCipherError(context.runtime, "iv length to short");
        }
        else {
            // EVP_CipherInit_ex uses leading IV length of given sequence.
            byte[] iv2 = new byte[ivLen];
            System.arraycopy(ivBytes, 0, iv2, 0, ivLen);
            this.realIV = iv2;
        }
        this.orgIV = this.realIV;
        if ( ! isStreamCipher() ) {
            cipherInited = false;
        }
        return iv;
    }

    @JRubyMethod
    public IRubyObject block_size(final ThreadContext context) {
        checkInitialized();
        if ( isStreamCipher() ) {
            // getBlockSize() returns 0 for stream cipher in JCE
            // OpenSSL returns 1 for RC4.
            return context.runtime.newFixnum(1);
        }
        return context.runtime.newFixnum(cipher.getBlockSize());
    }

    private void init(final ThreadContext context, final IRubyObject[] args, final boolean encrypt) {
        final Ruby runtime = context.runtime;
        Arity.checkArgumentCount(runtime, args, 0, 2);

        encryptMode = encrypt;
        cipherInited = false;

        if ( args.length > 0 ) {
            /*
             * oops. this code mistakes salt for IV.
             * We deprecated the arguments for this method, but we decided
             * keeping this behaviour for backward compatibility.
             */
            byte[] pass = args[0].convertToString().getBytes();
            byte[] iv = null;
            try {
                iv = "OpenSSL for Ruby rulez!".getBytes("ISO8859-1");
                byte[] iv2 = new byte[this.ivLen];
                System.arraycopy(iv, 0, iv2, 0, this.ivLen);
                iv = iv2;
            } catch (Exception e) {
            }

            if ( args.length > 1 && ! args[1].isNil() ) {
                runtime.getWarnings().warning(ID.MISCELLANEOUS, "key derivation by " + getMetaClass().getRealClass().getName() + "#encrypt is deprecated; use " + getMetaClass().getRealClass().getName() + "::pkcs5_keyivgen instead");
                iv = args[1].convertToString().getBytes();
                if (iv.length > this.ivLen) {
                    byte[] iv2 = new byte[this.ivLen];
                    System.arraycopy(iv, 0, iv2, 0, this.ivLen);
                    iv = iv2;
                }
            }

            MessageDigest digest = Digest.getDigest("MD5", runtime);
            KeyAndIv result = evpBytesToKey(keyLen, ivLen, digest, iv, pass, 2048);
            this.key = result.key;
            this.realIV = iv;
            this.orgIV = this.realIV;
        }
    }

    @JRubyMethod(optional = 2)
    public IRubyObject encrypt(final ThreadContext context, IRubyObject[] args) {
        this.realIV = orgIV;
        init(context, args, true);
        return this;
    }

    @JRubyMethod(optional = 2)
    public IRubyObject decrypt(final ThreadContext context, IRubyObject[] args) {
        this.realIV = orgIV;
        init(context, args, false);
        return this;
    }

    @JRubyMethod
    public IRubyObject reset(final ThreadContext context) {
        checkInitialized();
        if ( ! isStreamCipher() ) {
            this.realIV = orgIV;
            doInitCipher(context.runtime);
        }
        return this;
    }

    private void updateCipher(String name, String padding) {
        // given 'rc4' must be 'RC4' here. OpenSSL checks it as a LN of object
        // ID and set SN. We don't check 'name' is allowed as a LN in ASN.1 for
        // the possibility of JCE specific algorithm so just do upperCase here
        // for OpenSSL compatibility.
        this.name = name.toUpperCase();
        this.padding = padding;

        final Algorithm alg = Algorithm.osslToJava(name, padding);
        cryptoBase = alg.base;
        cryptoVersion = alg.version;
        cryptoMode = alg.mode;
        realName = alg.realName;
        paddingType = alg.padding;

        int[] lengths = Algorithm.osslKeyIvLength(name);
        keyLen = lengths[0];
        ivLen = lengths[1];
        if ("DES".equalsIgnoreCase(cryptoBase)) {
            generateKeyLen = keyLen / 8 * 7;
        }

        cipher = getCipher();
    }

    javax.crypto.Cipher getCipher() {
        try {
            return getCipher(realName, false);
        }
        catch (NoSuchAlgorithmException e) {
            throw newCipherError(getRuntime(), "unsupported cipher algorithm (" + realName + ")");
        }
        catch (NoSuchPaddingException e) {
            throw newCipherError(getRuntime(), "unsupported cipher padding (" + realName + ")");
        }
    }

    @JRubyMethod(required = 1, optional = 3)
    public IRubyObject pkcs5_keyivgen(final ThreadContext context, final IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        Arity.checkArgumentCount(runtime, args, 1, 4);
        byte[] pass = args[0].convertToString().getBytes();
        byte[] salt = null;
        int iter = 2048;
        IRubyObject vdigest = runtime.getNil();
        if ( args.length > 1 ) {
            if ( ! args[1].isNil() ) {
                salt = args[1].convertToString().getBytes();
            }
            if ( args.length > 2 ) {
                if ( ! args[2].isNil() ) {
                    iter = RubyNumeric.fix2int(args[2]);
                }
                if ( args.length > 3 ) {
                    vdigest = args[3];
                }
            }
        }
        if ( salt != null && salt.length != 8 ) {
            throw newCipherError(runtime, "salt must be an 8-octet string");
        }

        final String algorithm = vdigest.isNil() ? "MD5" : ((Digest) vdigest).getAlgorithm();
        final MessageDigest digest = Digest.getDigest(algorithm, runtime);
        KeyAndIv result = evpBytesToKey(keyLen, ivLen, digest, salt, pass, iter);
        this.key = result.key;
        this.realIV = result.iv;
        this.orgIV = this.realIV;

        doInitCipher(runtime);

        return runtime.getNil();
    }

    private void doInitCipher(final Ruby runtime) {
        if ( isDebug(runtime) ) {
            runtime.getOut().println("*** doInitCipher");
            dumpVars( runtime.getOut() );
        }
        checkInitialized();
        if (key == null) {
            throw newCipherError(runtime, "key not specified");
        }
        try {
            if ( ! "ECB".equalsIgnoreCase(cryptoMode) ) {
                if ( this.realIV == null ) {
                    // no IV yet, start out with all zeros
                    this.realIV = new byte[ivLen];
                }
                if ( "RC2".equalsIgnoreCase(cryptoBase) ) {
                    this.cipher.init(encryptMode ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE, new SimpleSecretKey("RC2", this.key), new RC2ParameterSpec(this.key.length * 8, this.realIV));
                } else if ( "RC4".equalsIgnoreCase(cryptoBase) ) {
                    this.cipher.init(encryptMode ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE, new SimpleSecretKey("RC4", this.key));
                } else {
                    this.cipher.init(encryptMode ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE, new SimpleSecretKey(realName.split("/")[0], this.key), new IvParameterSpec(this.realIV));
                }
            } else {
                this.cipher.init(encryptMode ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE, new SimpleSecretKey(realName.split("/")[0], this.key));
            }
        }
        catch (InvalidKeyException e) {
            throw newCipherError(runtime, e.getMessage() + ": possibly you need to install Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files for your JRE");
        }
        catch (Exception e) {
            debugStackTrace(runtime, e);
            throw newCipherError(runtime, e);
        }
        cipherInited = true;
    }
    private byte[] lastIv = null;

    @JRubyMethod
    public IRubyObject update(final ThreadContext context, final IRubyObject data) {
        final Ruby runtime = context.runtime;
        if ( isDebug(runtime) ) debug("*** update [" + data + "]");

        checkInitialized();
        byte[] val = data.convertToString().getBytes();
        if (val.length == 0) {
            throw getRuntime().newArgumentError("data must not be empty");
        }

        if ( ! cipherInited ) {
            //if ( debug ) runtime.getOut().println("BEFORE INITING");
            doInitCipher(runtime);
            //if ( debug ) runtime.getOut().println("AFTER INITING");
        }

        byte[] str = new byte[0];
        try {
            byte[] out = cipher.update(val);
            if (out != null) {
                str = out;

                if (this.realIV != null) {
                    if (lastIv == null) {
                        lastIv = new byte[ivLen];
                    }
                    byte[] tmpIv = encryptMode ? out : val;
                    if (tmpIv.length >= ivLen) {
                        System.arraycopy(tmpIv, tmpIv.length - ivLen, lastIv, 0, ivLen);
                    }
                }
            }
        }
        catch (Exception e) {
            debugStackTrace( runtime, e );
            throw newCipherError(runtime, e.getMessage());
        }

        return getRuntime().newString(new ByteList(str, false));
    }

    @JRubyMethod(name = "<<")
    public IRubyObject update_deprecated(final ThreadContext context, final IRubyObject data) {
        context.runtime.getWarnings().warn(ID.DEPRECATED_METHOD, this.getMetaClass().getRealClass().getName() + "#<< is deprecated; use " + this.getMetaClass().getRealClass().getName() + "#update instead");
        return update(context, data);
    }

    @JRubyMethod(name = "final")
    public IRubyObject _final(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        checkInitialized();
        if ( ! cipherInited ) {
            doInitCipher(runtime);
        }
        // trying to allow update after final like cruby-openssl. Bad idea.
        if ( "RC4".equalsIgnoreCase(cryptoBase) ) {
            return runtime.newString("");
        }
        ByteList str = new ByteList(ByteList.NULL_ARRAY);
        try {
            byte[] out = cipher.doFinal();
            if (out != null) {
                str = new ByteList(out, false);
                // TODO: Modifying this line appears to fix the issue, but I do
                // not have a good reason for why. Best I can tell, lastIv needs
                // to be set regardless of encryptMode, so we'll go with this
                // for now. JRUBY-3335.
                //if(this.realIV != null && encryptMode) {
                if (this.realIV != null) {
                    if (lastIv == null) {
                        lastIv = new byte[ivLen];
                    }
                    byte[] tmpIv = out;
                    if (tmpIv.length >= ivLen) {
                        System.arraycopy(tmpIv, tmpIv.length - ivLen, lastIv, 0, ivLen);
                    }
                }
            }
            if (this.realIV != null) {
                this.realIV = lastIv;
                doInitCipher(runtime);
            }
        }
        catch (GeneralSecurityException e) { // cipher.doFinal
            throw newCipherError(runtime, e.getMessage());
        }
        catch (RuntimeException e) {
            debugStackTrace(runtime, e);
            throw newCipherError(runtime, e);
        }
        return runtime.newString(str);
    }

    @JRubyMethod(name = "padding=")
    public IRubyObject set_padding(IRubyObject padding) {
        updateCipher(name, padding.toString());
        return padding;
    }

    String getAlgorithm() {
        return this.cipher.getAlgorithm();
    }

    String getName() {
        return this.name;
    }

    String getCryptoBase() {
        return this.cryptoBase;
    }

    String getCryptoMode() {
        return this.cryptoMode;
    }

    int getKeyLen() {
        return keyLen;
    }

    int getGenerateKeyLen() {
        return (generateKeyLen == -1) ? keyLen : generateKeyLen;
    }

    private void checkInitialized() {
        if ( cipher == null ) {
            throw getRuntime().newRuntimeError("Cipher not inititalized!");
        }
    }

    private boolean isStreamCipher() {
        return cipher.getBlockSize() == 0;
    }

    private static RaiseException newCipherError(Ruby runtime, Exception e) {
        return Utils.newError(runtime, _Cipher(runtime).getClass("CipherError"), e);
    }

    private static RaiseException newCipherError(Ruby runtime, String message) {
        return Utils.newError(runtime, _Cipher(runtime).getClass("CipherError"), message);
    }

    private static class KeyAndIv {

        final byte[] key;
        final byte[] iv;

        KeyAndIv(byte[] key, byte[] iv) {
            this.key = key;
            this.iv = iv;
        }

    }

    private static KeyAndIv evpBytesToKey(
            final int key_len, final int iv_len,
            final MessageDigest md,
            final byte[] salt,
            final byte[] data,
            final int count) {

        final byte[] key = new byte[key_len]; final byte[] iv = new byte[iv_len];

        if ( data == null ) return new KeyAndIv(key, iv);

        int key_ix = 0; int iv_ix = 0;
        byte[] md_buf = null;
        int nkey = key_len; int niv = iv_len;
        int i; int addmd = 0;

        for(;;) {
            md.reset();
            if ( addmd++ > 0 ) md.update(md_buf);
            md.update(data);
            if ( salt != null ) md.update(salt,0,8);

            md_buf = md.digest();

            for ( i = 1; i < count; i++ ) {
                md.reset();
                md.update(md_buf);
                md_buf = md.digest();
            }

            i = 0;
            if ( nkey > 0 ) {
                for(;;) {
                    if ( nkey == 0) break;
                    if ( i == md_buf.length ) break;
                    key[ key_ix++ ] = md_buf[i];
                    nkey--; i++;
                }
            }
            if ( niv > 0 && i != md_buf.length ) {
                for(;;) {
                    if ( niv == 0 ) break;
                    if ( i == md_buf.length ) break;
                    iv[ iv_ix++ ] = md_buf[i];
                    niv--; i++;
                }
            }
            if ( nkey == 0 && niv == 0 ) break;
        }
        return new KeyAndIv(key,iv);
    }

    private static class NamedCipherAllocator implements ObjectAllocator {

        private final String cipherBase;

        NamedCipherAllocator(final String cipherBase) {
            this.cipherBase = cipherBase;
        }

        public Named allocate(Ruby runtime, RubyClass klass) {
            return new Named(runtime, klass, cipherBase);
        }
    };

    public static class Named extends Cipher {
        private static final long serialVersionUID = 5599069534014317221L;

        final String cipherBase;

        Named(Ruby runtime, RubyClass type, String cipherBase) {
            super(runtime, type);
            this.cipherBase = cipherBase; // e.g. "AES"
        }

        /*
        AES = Class.new(Cipher) do
          define_method(:initialize) do |*args|
            cipher_name = args.inject('AES'){|n, arg| "#{n}-#{arg}" }
            super(cipher_name)
          end
        end
         */
        @JRubyMethod(rest = true, visibility = Visibility.PRIVATE)
        public IRubyObject initialize(final ThreadContext context, final IRubyObject[] args) {
            final StringBuilder name = new StringBuilder();
            name.append(cipherBase);
            if ( args != null ) {
                for ( int i = 0; i < args.length; i++ ) {
                    name.append('-').append( args[i].asString() );
                }
            }
            initializeImpl(context.runtime, name.toString());
            return this;
        }

        //@Override
        //void initializeImpl(final Ruby runtime, final String name) {
        //    final String cipherName = name == null ? cipherBase : ( cipherBase + '-' + name );
        //    super.initializeImpl(runtime, cipherName);
        //}

    }

    private static class AESCipherAllocator implements ObjectAllocator {

        private final String keyLength;

        AESCipherAllocator(final String keyLength) {
            this.keyLength = keyLength;
        }

        public AES allocate(Ruby runtime, RubyClass klass) {
            return new AES(runtime, klass, keyLength);
        }
    };

    public static class AES extends Cipher {
        private static final long serialVersionUID = -3627749495034257750L;

        final String keyLength;

        AES(Ruby runtime, RubyClass type, String keyLength) {
            super(runtime, type);
            this.keyLength = keyLength; // e.g. "256"
        }

        /*
        AES256 = Class.new(Cipher) do
          define_method(:initialize) do |mode|
            mode ||= "CBC"
            cipher_name = "AES-256-#{mode}"
            super(cipher_name)
          end
        end
         */
        @JRubyMethod(rest = true, visibility = Visibility.PRIVATE)
        public IRubyObject initialize(final ThreadContext context, final IRubyObject[] args) {
            final String mode;
            if ( args != null && args.length > 0 ) {
                mode = args[0].toString();
            }
            else {
                mode = "CBC";
            }
            initializeImpl(context.runtime, "AES-" + keyLength + '-' + mode);
            return this;
        }

    }

}
