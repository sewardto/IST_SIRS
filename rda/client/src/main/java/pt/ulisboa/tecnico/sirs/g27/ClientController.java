package pt.ulisboa.tecnico.sirs.g27;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.util.io.Streams;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.shell.standard.*;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import pt.ulisboa.tecnico.sirs.g27.dto.UserDto;
import pt.ulisboa.tecnico.sirs.g27.keygenerator.KeyBasedFileProcessor;
import pt.ulisboa.tecnico.sirs.g27.keygenerator.RSAKeyPairGenerator;
import pt.ulisboa.tecnico.sirs.g27.keygenerator.SignedFileProcessor;
import pt.ulisboa.tecnico.sirs.g27.storage.StorageProperties;
import pt.ulisboa.tecnico.sirs.g27.storage.StorageService;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.List;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

@ShellComponent
public class ClientController {

    private final StorageService storageService;
    private final StorageProperties storageProperties;

    public ClientController(StorageService storageService, StorageProperties storageProperties) {
        this.storageService = storageService;
        this.storageProperties = storageProperties;
    }

    private UserDto user = null;

    private static List<String> unexpectedExtensions = Arrays.asList(".ade", ".adp", ".ani", ".bas", ".bat", ".chm", ".cmd", ".com", ".cpl", ".crt", ".hlp", ".ht",
            "hta", ".inf", ".ins", ".isp", ".jar", ".job", ".js", ".jse", ".lnk", ".mda", ".mdb", ".mde", ".mdz", ".msc", ".msi",
            ".msp", ".mst", ".ocx", ".pcd", ".ps1", ".reg", ".scr", ".sct", ".shs", ".svg", ".url", ".vb", ".vbe", ".vbs", ".wbk", ".wsc",
            ".ws", ".wsf", ".wsh", ".exe", ".pif", ".pub");

    @ShellMethod("Login")
    public void login() throws Exception {
        if (this.user == null) {
            LineReader reader = LineReaderBuilder.builder().build();

            String username = reader.readLine("Input username: ");
            String password = reader.readLine("Input password: ", '*');

            String plainCreds = username + ":" + password;
            byte[] plainCredsBytes = plainCreds.getBytes();
            byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
            String base64Creds = new String(base64CredsBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Basic " + base64Creds);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> responseEntity = new RestTemplate().exchange("http://localhost:9002/login", HttpMethod.GET, request, String.class);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                System.out.println("Login Succeeded!");
                String publicKeyPath = reader.readLine("Input path to public key (empty for default): ", '*');
                if (publicKeyPath.equals("")) {
                    publicKeyPath = "pub.asc";
                }
                String privateKeyPath = reader.readLine("Input path to private key (empty for default): ", '*');
                if (privateKeyPath.equals("")) {
                    privateKeyPath = "secret.asc";
                }

                this.user = new UserDto();
                this.user.setUsername(username);
                this.user.setPassword(password);
                this.user.setPrivateKey(privateKeyPath);
                this.user.setPublicKey(publicKeyPath);
                this.user.setHttpHeaders(headers);

            } else {
                System.out.println("failed to login");
            }
        } else {
            System.out.println("You are already logged in.");
        }
    }

    @ShellMethod("Sign up")
    public String signup() throws Exception {

        LineReader reader = LineReaderBuilder.builder().build();

        String username = reader.readLine("Input username: ");
        String password = reader.readLine("Input password: ", '*');
        String passwordConfirmation = reader.readLine("Repeat password: ", '*');
        String publicKey;

        if (!password.equals(passwordConfirmation)) {
            return "Passwords do not match. Please try to create a new user again";
        }
        String email = reader.readLine("Input email: ");
        System.out.println("Creating a new user...");
        RSAKeyPairGenerator KPGen = new RSAKeyPairGenerator();
        KPGen.init(username, password);

        UserDto userDto = new UserDto();
        userDto.setUsername(username);
        userDto.setPassword(password);
        userDto.setMatchingPassword(passwordConfirmation);
        userDto.setPrivateKey(Paths.get("secret.asc").toString());
        userDto.setPublicKey(Paths.get("pub.asc").toString());
        userDto.setEmail(email);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = new RestTemplate().postForEntity("http://localhost:9002/registration", new HttpEntity<>(userDto, headers), String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            publicKey = new String(Files.readAllBytes(Paths.get("pub.asc")));

            ResponseEntity<String> responseCA = new RestTemplate().exchange(
                    "https://ca.sirsrda.tk/save?name={name}&password={password}&publicKey={publicKey}",
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    String.class,
                    username,
                    password,
                    publicKey);
            System.out.println(responseCA);
            return response.getStatusCode().toString();
        }

        return response.getStatusCode().toString();
    }

    @ShellMethod("Logout")
    public void logout() throws Exception {
        this.user = null;
        System.out.println("Log out completed.");
    }


    @ShellMethod("Share file")
    public void share() {
        LineReader reader = LineReaderBuilder.builder().build();

        String username = reader.readLine("Input user you want to share with: ");
    }

    @ShellMethod("Upload File")
    public void upload() throws NoSuchAlgorithmException, NoSuchPaddingException, IOException {

        LineReader reader = LineReaderBuilder.builder().build();

        String fileName = reader.readLine("Input path to the file: ");

        for(String extension:unexpectedExtensions) {
            if(fileName.endsWith(extension)){
                System.out.println("This file type is not accepted.");
                return;
            }
        }

        // GENERATING FILE KEY
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey fileKey = keyGen.generateKey();
        String fileKeyName = "key_" + fileName;

        //WRITING FILE KEY TO FILE
        FileOutputStream out = new FileOutputStream(fileKeyName);
        out.write(Base64.encodeBase64(fileKey.getEncoded()));
        out.close();


        // GENERATE IV
        SecureRandom srandom = new SecureRandom();
        byte[] iv = new byte[128 / 8];
        srandom.nextBytes(iv);
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        String ivFile = "iv_" + fileName;
        // WRITING IV TO FILE
        OutputStream ivOut = new FileOutputStream(ivFile);
        ivOut.write(iv);
        ivOut.close();


        FileEncrypterDecrypter fed = new FileEncrypterDecrypter(fileKey);

        //ENCRYPT FILE
        fed.encryptFile(fileName);

        //ADD SIGNATURE TO THE FILE
        FileInputStream keyIn = new FileInputStream("secret.asc");
        FileOutputStream signedOut = new FileOutputStream("sig_" + fileName);

        try {
            SignedFileProcessor.signFile("enc_" + fileName, keyIn, signedOut, this.user.getPassword().toCharArray(), true);
        } catch (NullPointerException e) {
            System.out.println("Please make sure you have logged in.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        keyIn.close();
        signedOut.close();

        //ENCRYPT FILE KEY
        try {
            KeyBasedFileProcessor.encryptFile(fileKeyName, "enc_" + fileKeyName, "pub.asc", true, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //SEND FILE + IV + Encrypted FILE KEY TO SERVER

        this.user.getHttpHeaders().setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();

        body.add("file", new FileSystemResource("enc_" + fileName));
        body.add("key", Files.readAllBytes(Paths.get("enc_" + fileKeyName)));
        body.add("username", this.user.getUsername());
        body.add("iv", Base64.encodeBase64(iv));
        body.add("pgp_sig", Files.readAllBytes(Paths.get("sig_" + fileName)));



        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<MultiValueMap<String,Object>> requestEntity = new HttpEntity<>(body, this.user.getHttpHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:9002/user",
                HttpMethod.POST,
                requestEntity,
                String.class
                );

        System.out.println(response);


        //DEBUG
        /*FileInputStream fileIn = new FileInputStream("signed_enc_" + fileName);
        keyIn = new FileInputStream("pub.asc");
        try {
            SignedFileProcessor.verifyFile(fileIn, keyIn);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        /*String encodedKey = Base64.encodeBase64String(fileKey.getEncoded());
        System.out.println(encodedKey);*/
    }


    @ShellMethod("Download file")
    public void download(){

        LineReader reader = LineReaderBuilder.builder().build();

        String filename = reader.readLine("Input filename: ");

        this.user.getHttpHeaders().setContentType(MediaType.TEXT_PLAIN);


        // ASK SV FOR FILE
        ResponseEntity<Resource> responseSV = new RestTemplate().exchange(
                "http://localhost:9002/user/files?username={username}&filename={filename}",
                HttpMethod.GET,
                new HttpEntity<>(this.user.getHttpHeaders()),
                Resource.class,
                this.user.getUsername(),
                filename);

        try {

            Files.createDirectories(Paths.get(storageProperties.getRoot() + this.user.getUsername()));
            Files.createDirectories(Paths.get(storageProperties.getRoot() + this.user.getUsername() + "/decryptedfiles"));
            storageProperties.setLocation(storageProperties.getRoot() + this.user.getUsername());
            storageService.setRootLocation(storageProperties);
            File dlFile = new File(storageProperties.getLocation() + "/" + filename);
            dlFile.createNewFile();
            Streams.pipeAll(responseSV.getBody().getInputStream(), new FileOutputStream(dlFile));

        } catch (IOException e) {

            e.printStackTrace();
        }


        // ASK SV FOR FILE KEY
        ResponseEntity<String> responseSVkey = new RestTemplate().exchange(
                "http://localhost:9002/user/filekeys?username={username}&filename={filename}",
                HttpMethod.GET,
                new HttpEntity<>(this.user.getHttpHeaders()),
                String.class,
                this.user.getUsername(),
                filename);

        try {
            // SAVE FILEKEY IN FOLDER
            String encFileKey = responseSVkey.getBody();
            new File(storageProperties.getRoot() + this.user.getUsername() + "/filekeys/").mkdir();
            File fkey = new File(storageProperties.getRoot() + this.user.getUsername() + "/filekeys/" + filename);
            FileOutputStream dlEncFileKey = new FileOutputStream(fkey);
            dlEncFileKey.write(encFileKey.getBytes());
            dlEncFileKey.close();

            // DECRYPT FILEKEY WITH SECRET KEY
            KeyBasedFileProcessor.decryptFile(
                    storageProperties.getRoot() + this.user.getUsername() + "/filekeys/" + filename,
                    "secret.asc",
                    this.user.getPassword().toCharArray(),
                    storageProperties.getRoot() + this.user.getUsername() + "/filekeys/" + "dec_" + filename);

        } catch (IOException | NoSuchProviderException e) {
            e.printStackTrace();
        }

        // ASK IV FROM SV

        ResponseEntity<String> responseSViv = new RestTemplate().exchange(
                "http://localhost:9002/user/fileiv?username={username}&filename={filename}",
                HttpMethod.GET,
                new HttpEntity<>(this.user.getHttpHeaders()),
                String.class,
                this.user.getUsername(),
                filename);

        try {
            // SAVE FILEIV IN FOLDER
            byte[] fileIv = Base64.decodeBase64(responseSViv.getBody());
            new File(storageProperties.getRoot() + this.user.getUsername() + "/ivs/").mkdir();
            File iv = new File(storageProperties.getRoot() + this.user.getUsername() + "/ivs/" + filename);
            FileOutputStream dlFileIv = new FileOutputStream(iv);
            dlFileIv.write(fileIv);
            dlFileIv.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            byte[] decodedKey = Base64.decodeBase64(Files.readAllBytes((Paths.get(storageProperties.getRoot() + this.user.getUsername() + "/filekeys/" + "dec_" + filename))));

            System.out.println("decodedKey : " + decodedKey);

            FileEncrypterDecrypter fed = new FileEncrypterDecrypter(
                    new SecretKeySpec(decodedKey,0, decodedKey.length, "AES"));

            System.out.println("Before decrypting file");
            System.out.println("File to be decrypted : " + storageProperties.getRoot() + this.user.getUsername() + "/" + filename);
            fed.decryptFile(filename, this.user.getUsername());
            System.out.println("After decrypting file");
        } catch (IOException e){
            e.printStackTrace();
        }

    }
}
