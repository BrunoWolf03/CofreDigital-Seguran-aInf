import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;

public class TOTP {

    private byte[] key = null;
    private long timeStepInSeconds = 30;

    public TOTP(String base32EncodedSecret, long timeStepInSeconds) throws Exception {
        Base32 base32 = new Base32(Base32.Alphabet.BASE32, false, false);
        byte[] decoded = base32.fromString(base32EncodedSecret);
        if (decoded == null) throw new Exception("Segredo BASE32 invalido");
        this.key = decoded;
        this.timeStepInSeconds = timeStepInSeconds;
    }

    private String getTOTPCodeFromHash(byte[] hash) {
        int offset = hash[hash.length - 1] & 0x0F;
        int binary =
            ((hash[offset]     & 0x7F) << 24) |
            ((hash[offset + 1] & 0xFF) << 16) |
            ((hash[offset + 2] & 0xFF) <<  8) |
            ( hash[offset + 3] & 0xFF);
        int otp = binary % 1_000_000;
        return String.format("%06d", otp);
    }

    private byte[] HMAC_SHA1(byte[] counter, byte[] keyByteArray) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(keyByteArray, "HmacSHA1"));
            return mac.doFinal(counter);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular HMAC-SHA1", e);
        }
    }

    private String TOTPCode(long timeInterval) {
        byte[] counter = new byte[8];
        for (int i = 7; i >= 0; i--) {
            counter[i] = (byte) (timeInterval & 0xFF);
            timeInterval >>= 8;
        }
        byte[] hash = HMAC_SHA1(counter, key);
        return getTOTPCodeFromHash(hash);
    }

    public String generateCode() {
        long timeInterval = (new Date().getTime() / 1000L) / timeStepInSeconds;
        return TOTPCode(timeInterval);
    }

    public boolean validateCode(String inputTOTP) {
        if (inputTOTP == null) return false;
        long t = (new Date().getTime() / 1000L) / timeStepInSeconds;
        return inputTOTP.equals(TOTPCode(t))
            || inputTOTP.equals(TOTPCode(t - 1))
            || inputTOTP.equals(TOTPCode(t + 1));
    }
}
