package android.util;
import android.annotation.Nullable;
import android.security.keystore.KeyProperties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MiInfoReader {
    private static final String AES_KEY = "winteam@tank@key";
    private static final String AES_IV = "winteam2020@tank";

    @Nullable  public String getInfo(@Nullable String str) {
        String str2 = "";
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("/system/vendor/Utils/" + str)));
            String readLine = bufferedReader.readLine();
            if (readLine == null || readLine == "") {
                str2 = "";
            } else {
                str2 = decrypt(readLine);
            }
            bufferedReader.close();
            if (str2 == null) {
                return "";
            }
            return str2;
        } catch (Exception e) {
            return "";
        }
    }

    @Nullable  public double getInfoDouble(@Nullable String str) {
        String info = getInfo(str);
        if (info == null || info == "") {
            return -999999.0d;
        }
        try {
            return Double.parseDouble(info);
        } catch (Exception e) {
            return -999999.0d;
        }
    }


    private String decrypt(String str) throws Exception {
        IvParameterSpec ivParameterSpec = new IvParameterSpec(AES_IV.getBytes("UTF-8"));
        SecretKeySpec secretKeySpec = new SecretKeySpec(AES_KEY.getBytes("UTF-8"), KeyProperties.KEY_ALGORITHM_AES);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(2, secretKeySpec, ivParameterSpec);
        return new String(cipher.doFinal(Base64.decode(str, 0)));
    }

}
