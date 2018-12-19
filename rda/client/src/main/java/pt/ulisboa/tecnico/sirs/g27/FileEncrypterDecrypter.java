package pt.ulisboa.tecnico.sirs.g27;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileEncrypterDecrypter {

    private SecretKey fileKey;

    public FileEncrypterDecrypter(SecretKey fileKey) {
        this.fileKey = fileKey;
    }

    public void encryptFile(String fileName) {

        try{
            // GET IV
            IvParameterSpec ivspec = getIvParameterSpec("iv_" + fileName);

            Cipher ce = Cipher.getInstance("AES/CBC/PKCS5Padding");
            ce.init(Cipher.ENCRYPT_MODE, fileKey, ivspec);
            processFile(ce, fileName, "enc_"+fileName);
        } catch (Exception e){
            e.getMessage();
        }
    }

    public void decryptFile(String fileName, String username) throws IOException {

        System.out.println("INSIDE DECRYPTTTT");

        try{
            System.out.println("1");
            // GET IV
            IvParameterSpec ivspec = getIvParameterSpec("download-dir/" + username + "/ivs/" + fileName);
            System.out.println("2");
            Cipher cd = Cipher.getInstance("AES/CBC/PKCS5Padding");
            System.out.println("3");
            cd.init(Cipher.DECRYPT_MODE, fileKey, ivspec);
            System.out.println("HELLO!!!");
            processFile(cd, "download-dir/" + username + "/" + fileName,
                    "download-dir/" + username + "/decryptedfiles/" + fileName);
        } catch (Exception e){
            e.getMessage();
        }
    }

    private IvParameterSpec getIvParameterSpec(String fileName) throws IOException {
        FileInputStream in = new FileInputStream(fileName);
        byte[] iv = new byte[128 / 8];
        in.read(iv);
        return new IvParameterSpec(iv);
    }

    private static byte[] getUTF8Bytes(String input){
        return input.getBytes(StandardCharsets.UTF_8);
    }

    static private void processFile(Cipher ci,String inFile,String outFile)
            throws javax.crypto.IllegalBlockSizeException,
            javax.crypto.BadPaddingException,
            java.io.IOException
    {
        try (FileInputStream in = new FileInputStream(inFile);
             FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] ibuf = new byte[1024];
            int len;
            while ((len = in.read(ibuf)) != -1) {
                byte[] obuf = ci.update(ibuf, 0, len);
                if ( obuf != null ) out.write(obuf);
            }
            byte[] obuf = ci.doFinal();
            if ( obuf != null ) out.write(obuf);
        }
    }
}
