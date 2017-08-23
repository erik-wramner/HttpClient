package name.wramner.httpclient;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.BitSet;

import name.wramner.util.HexDumpEncoder;

public class NtlmHelper {
    private static final Charset UTF_16_LITTLE_ENDIAN = Charset.forName("UTF-16LE");
    private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    private static final byte[] NTLM_SIGNATURE = new byte[] { 'N', 'T', 'L', 'M', 'S', 'S', 'P', 0x00 };

    public void parseNtlmChallengeMessage(String encodedMessage) {
        ByteBuffer buf = Base64.getDecoder().decode(ByteBuffer.wrap(encodedMessage.getBytes(ISO_8859_1)));
        buf.order(ByteOrder.LITTLE_ENDIAN);

        for (byte b : NTLM_SIGNATURE) {
            if (buf.get() != b) {
                throw new IllegalArgumentException("Invalid NTLM challenge, signature is missing");
            }
        }
        if (buf.getInt() != 2) {
            throw new IllegalArgumentException("Invalid NTLM challenge, expected message type 2");
        }
        int targetNameLength = buf.getShort();
        buf.getShort(); // Ignore max target name length
        int targetNameOffset = buf.getInt();
        int negotiateFlags = buf.getInt();
        dumpNegotiationFlags(negotiateFlags);

        byte[] serverChallenge = new byte[8];
        buf.get(serverChallenge);
        buf.getLong(); // Skip 8 reserved bytes
        int targetInfoLength = buf.getShort();
        buf.getShort(); // Ignore max target info length
        int targetInfoOffset = buf.getInt();
        long version = buf.getLong();
        System.out.println(String.format("Version: %X", version));

        if ((negotiateFlags | (1 << NegotiateFlagBits.NTLMSSP_NEGOTIATE_TARGET_INFO)) != 0 && targetInfoLength != 0) {
            buf.position(targetInfoOffset);
            int avId;
            do {
                avId = buf.getShort();
                int avLength = buf.getShort();
                byte[] avValue = new byte[avLength];
                if (avValue.length > 0) {
                    buf.get(avValue);
                }
                String avValueAsString = (avId == 7 || avId == 8 || avId >= 10) ? new BigInteger(avValue).toString(16)
                                : new String(avValue, UTF_16_LITTLE_ENDIAN);
                System.out.println("AvId: " + avId + " value " + avValueAsString);
            } while (avId != 0x0000);
        }

        if (targetNameLength != 0) {
            buf.position(targetNameOffset);
            byte[] targetName = new byte[targetNameLength];
            buf.get(targetName);
            System.out.println("Target name: " + new String(targetName, UTF_16_LITTLE_ENDIAN));
        }
    }

    private void dumpNegotiationFlags(int flags) {
        System.out.println(String.format("Flags (%X):", flags));
        try {
            for (Field f : NegotiateFlagBits.class.getDeclaredFields()) {
                int bit = f.getInt(null);
                if ((flags & (1 << bit)) != 0) {
                    System.out.println(" - " + f.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String createNtlmAuthenticateMessage(String domainName, String workstationName, String userName) {
        byte[] user = userName.toUpperCase().getBytes(UTF_16_LITTLE_ENDIAN);
        byte[] domain = domainName != null ? domainName.toUpperCase().getBytes(UTF_16_LITTLE_ENDIAN) : new byte[0];
        byte[] workstation = workstationName != null ? workstationName.toUpperCase().getBytes(UTF_16_LITTLE_ENDIAN)
                        : new byte[0];
        byte[] lmChallengeResponse = new byte[0];
        byte[] ntChallengeResponse = new byte[0];
        byte[] encryptedRandomSessionKey = new byte[0];

        ByteBuffer buf = ByteBuffer.allocate(1024); // TODO
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(NTLM_SIGNATURE); // Signature
        buf.putInt(3); // Message type
        buf.putShort((short) lmChallengeResponse.length); // LM challenge/response length
        buf.putShort((short) lmChallengeResponse.length); // LM challenge/response max length
        buf.putInt(0); // TODO: LM challenge/response buffer offset
        buf.putShort((short) ntChallengeResponse.length); // NT challenge/response length
        buf.putShort((short) ntChallengeResponse.length); // NT challenge/response max length
        buf.putInt(0); // TODO: NT challenge/response buffer offset

        buf.putShort((short) domain.length); // Domain name length
        buf.putShort((short) domain.length); // Domain name max length
        buf.putInt(0); // Domain name offset in buffer

        buf.putShort((short) user.length); // User name length
        buf.putShort((short) user.length); // User name max length
        buf.putInt(0); // User name offset in buffer

        buf.putShort((short) workstation.length); // Workstation name length
        buf.putShort((short) workstation.length); // Workstation name max length
        buf.putInt(0); // Workstation name offset in buffer

        buf.putShort((short) encryptedRandomSessionKey.length); // Encrypted random session key length
        buf.putShort((short) encryptedRandomSessionKey.length); // Encrypted random session max length
        buf.putInt(0); // Encrypted random session offset in buffer

        int negotiateFlags = 0;
        buf.putInt(negotiateFlags); // Flags
        buf.putLong(8L); // Ignore version
        byte[] mic = new byte[16];
        buf.put(mic);

        return null;
    }

    public String createNtlmNegotiateMessage(String domainName, String workstationName) {
        byte[] domain = domainName != null ? domainName.toUpperCase().getBytes(UTF_16_LITTLE_ENDIAN) : new byte[0];
        byte[] workstation = workstationName != null ? workstationName.toUpperCase().getBytes(UTF_16_LITTLE_ENDIAN)
                        : new byte[0];

        BitSet flagBits = new BitSet(32);

        flagBits.set(NegotiateFlagBits.NTLMSSP_NEGOTIATE_KEY_EXCH);
        flagBits.set(NegotiateFlagBits.NTLMSSP_NEGOTIATE_56);
        flagBits.set(NegotiateFlagBits.NTLMSSP_NEGOTIATE_128);
        flagBits.set(NegotiateFlagBits.NTLMSSP_NEGOTIATE_ALWAYS_SIGN);
        flagBits.set(NegotiateFlagBits.NTLMSSP_REQUEST_TARGET);
        flagBits.set(NegotiateFlagBits.NTLMSSP_NEGOTIATE_NTLM);
        if (domain.length > 0) {
            flagBits.set(NegotiateFlagBits.NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED);
        }
        if (workstation.length > 0) {
            flagBits.set(NegotiateFlagBits.NTLMSSP_NEGOTIATE_OEM_WORKSTATION_SUPPLIED);
        }
        flagBits.set(NegotiateFlagBits.NTLM_NEGOTIATE_UNICODE);
        int negotiateFlags = flagBits.stream().reduce(0, (value, bit) -> (value | (1 << bit)));

        ByteBuffer buf = ByteBuffer.allocate(32 + domain.length + workstation.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(NTLM_SIGNATURE); // Signature
        buf.putInt(1); // Message type
        buf.putInt(negotiateFlags); // Negotiate flags
        buf.putShort((short) domain.length); // Domain name length
        buf.putShort((short) domain.length); // Domain name max length
        buf.putInt((short) (32 + workstation.length)); // Domain name offset in buffer
        buf.putShort((short) workstation.length); // Workstation name length
        buf.putShort((short) workstation.length); // Workstation name max length
        buf.putInt((short) 32); // Workstation name offset in buffer
        buf.put(workstation); // Workstation in OEM encoding
        buf.put(domain); // Domain in OEM encoding
        buf.flip();

        return toBase64EncodedString(buf);
    }

    // Negotiate flag bits
    public static interface NegotiateFlagBits {
        /**
         * Request 56-bit session key encryption.
         */
        static final int NTLMSSP_NEGOTIATE_56 = 0;

        /**
         * Request explicit key exchange. This option should be used as it improves security.
         */
        static final int NTLMSSP_NEGOTIATE_KEY_EXCH = 1;

        /**
         * Request 128-bit session key encryption.
         */
        static final int NTLMSSP_NEGOTIATE_128 = 2;

        /**
         * Request protocol version number.
         */
        static final int NTLMSSP_NEGOTIATE_VERSION = 6;

        /**
         * Indicates that target info fields in challenge message are present.
         */
        static final int NTLMSSP_NEGOTIATE_TARGET_INFO = 8;

        /**
         * Request usage of LMOWF, a one-way function applied to the user's password.
         */
        static final int NTLMSSP_REQUEST_NON_NT_SESSION_KEY = 9;

        /**
         * Request identity-level token (not valid for impersonation).
         */
        static final int NTLMSSP_NEGOTIATE_IDENTIFY = 11;

        /**
         * Request usage of NTLM v2 session security. NTLM v2 session security is a misnomer because it is not NTLM v2.
         * It is NTLM v1 using the extended session security that is also in NTLM v2. This option is mutually exclusive
         * with NTLMSSP_NEGOTIATE_LM_KEY.
         */
        static final int NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY = 12;

        /**
         * When set, target name must be a server name. Mutually exclusive with NTLMSSP_TARGET_TYPE_DOMAIN.
         */
        static final int NTLMSSP_TARGET_TYPE_SERVER = 14;

        /**
         * When set, target name must be a domain. Mutually exclusive with NTLMSSP_TARGET_TYPE_SERVER.
         */
        static final int NTLMSSP_TARGET_TYPE_DOMAIN = 15;

        /**
         * Request the presence of a signature block for all messages. This option is overridden by
         * NTLMSSP_NEGOTIATE_SIGN and NTLMSSP_NEGOTIATE_SEAL if they are supported.
         */
        static final int NTLMSSP_NEGOTIATE_ALWAYS_SIGN = 16;

        /**
         * Indicate that the workstation field is present.
         */
        static final int NTLMSSP_NEGOTIATE_OEM_WORKSTATION_SUPPLIED = 18;

        /**
         * Indicate that the domain field is present.
         */
        static final int NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED = 19;

        /**
         * Request that the connection should be anonymous.
         */
        static final int ANONYMOUS_CONNECTION = 20;

        /**
         * Request usage of the NTLM v1 session security protocol.
         */
        static final int NTLMSSP_NEGOTIATE_NTLM = 22;

        /**
         * Requests LAN Manager (LM) session key computation.
         */
        static final int NTLMSSP_NEGOTIATE_LM_KEY = 24;

        /**
         * Requests connection-less authentication.
         */
        static final int NTLMSSP_NEGOTIATE_DATAGRAM = 25;

        /**
         * Requests session key negotiation for message confidentiality.
         */
        static final int NTLMSSP_NEGOTIATE_SEAL = 26;

        /**
         * Requests session key negotiation for message signatures.
         */
        static final int NTLMSSP_NEGOTIATE_SIGN = 27;

        /**
         * Target field of challenge must be supplied.
         */
        static final int NTLMSSP_REQUEST_TARGET = 29;

        /**
         * Request OEM character encoding. This is probably a very bad idea, as the client may have a different code
         * page than the server.
         */
        static final int NTLM_NEGOTIATE_OEM = 30;

        /**
         * Request Unicode character encoding, more specifically UTF-16 little-endian.
         */
        static final int NTLM_NEGOTIATE_UNICODE = 31;
    }

    private String toBase64EncodedString(ByteBuffer buf) {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);

        System.out.println(new HexDumpEncoder().encode(bytes));

        return Base64.getEncoder().encodeToString(bytes);
    }

}
