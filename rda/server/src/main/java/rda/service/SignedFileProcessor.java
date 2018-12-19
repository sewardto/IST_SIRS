package rda.service;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rda.persistence.dao.FileRepository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;

@Service
public class SignedFileProcessor
{
    /*
     * verify the passed in file as being correctly signed.
     */
    private final FileRepository fileRepository;

    @Autowired
    public SignedFileProcessor(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public boolean verifyFile(String filePath, String key)
            throws Exception {
        System.out.println(key);
        InputStream keyIn = new ByteArrayInputStream(key.getBytes(StandardCharsets.US_ASCII));
        Security.addProvider(new BouncyCastleProvider());
        byte[] in = Base64.decodeBase64(fileRepository.findByFilePath(filePath).getPgp_sig());
        System.out.println(Arrays.toString(in));
        ByteArrayInputStream bis = new ByteArrayInputStream(in);
        JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(PGPUtil.getDecoderStream(bis));
        PGPCompressedData c1 = (PGPCompressedData)pgpFact.nextObject();
        pgpFact = new JcaPGPObjectFactory(c1.getDataStream());
        PGPOnePassSignatureList p1 = (PGPOnePassSignatureList)pgpFact.nextObject();
        PGPOnePassSignature ops = p1.get(0);
        PGPLiteralData p2 = (PGPLiteralData)pgpFact.nextObject();
        InputStream dIn = p2.getInputStream();
        PGPPublicKeyRingCollection pgpRing = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(keyIn), new JcaKeyFingerprintCalculator());
        PGPPublicKey pubKey = pgpRing.getPublicKey(ops.getKeyID());
        FileOutputStream out = new FileOutputStream(p2.getFileName());
        ops.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), pubKey);
        int ch;
        while ((ch = dIn.read()) >= 0) {
            ops.update((byte)ch);
            out.write(ch);
        }
        out.close();
        PGPSignatureList p3 = (PGPSignatureList)pgpFact.nextObject();
        return ops.verify(p3.get(0));
    }

    public boolean isChanged(String fileName) {
        try {
            String s = hash256(fileName);
            String str = IOUtils.toString(Base64.decodeBase64(fileRepository.findByFilePath(fileName).getSha256()));
            System.out.println(str);
            return !s.equals(str);
        } catch (IOException | NoSuchAlgorithmException e){
            e.printStackTrace();
            return false;
        }
    }

    private String hash256(String fileName) throws IOException, NoSuchAlgorithmException {
        File file = new File(fileName);
        InputStream fileIS = new FileInputStream(file);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return bytesToHex(digest.digest(IOUtils.toByteArray(fileIS)));
    }
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte hash1 : hash) {
            String hex = Integer.toHexString(0xff & hash1);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
