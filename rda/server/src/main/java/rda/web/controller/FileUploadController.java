package rda.web.controller;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import rda.persistence.dao.FileRepository;
import rda.persistence.model.File;
import rda.persistence.model.Key;
import rda.service.SignedFileProcessor;
import rda.service.UserService;
import rda.storage.StorageFileNotFoundException;
import rda.storage.StorageProperties;
import rda.storage.StorageService;

@Controller
public class FileUploadController {

    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final UserService userService;
    private final SignedFileProcessor signedFileProcessor;
    private final FileRepository fileRepository;

    @Autowired
    public FileUploadController(StorageService storageService, StorageProperties storageProperties, UserService userService, SignedFileProcessor signedFileProcessor, FileRepository fileRepository) {
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.userService = userService;
        this.signedFileProcessor = signedFileProcessor;
        this.fileRepository = fileRepository;
    }

    @GetMapping("/user")
    @PreAuthorize("principal.username.equals(#username)")
    @ResponseBody
    public List<Path> list(@RequestParam(value = "username") String username) {
        storageProperties.setLocation(storageProperties.getRoot() + username);
        storageService.setRootLocation(storageProperties);
        return storageService.loadAll().collect(Collectors.toList());
    }

    @GetMapping("/user/files")
    @PreAuthorize("principal.username.equals(#username)")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@RequestParam(value = "username") String username, @RequestParam(value = "filename") String filename) {
        storageProperties.setLocation(storageProperties.getRoot() + username);
        storageService.setRootLocation(storageProperties);

        ResponseEntity<String> responseEntity = new RestTemplate().exchange("https://ca.sirsrda.tk/key?name={name}", HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class, username);

        System.out.println("Verifying the file...");

        if (signedFileProcessor.isChanged(storageService.getRootLocation().resolve(filename).toString())) {
            System.out.println("file changed, fetch from backup");
            //get file from backup
        }

        System.out.println("Downloading...");

        Resource file = storageService.loadAsResource(filename);

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @GetMapping("/user/filekeys")
    @PreAuthorize("principal.username.equals(#username)")
    @ResponseBody
    public ResponseEntity<String> serveFileKey(@RequestParam(value = "username") String username,@RequestParam(value = "filename") String filename,
                                               @RequestParam(value = "dest_user") String dest_user) {

        List<Key> fileKeys = userService.getFileKeys(username, filename);
        for(Key key : fileKeys) {
            if(userService.findUserByUsername(dest_user).getId().equals(key.getId())){
                return ResponseEntity.ok().body(key.getFileKey());
            }
        }
        return ResponseEntity.ok().body("User not found");
    }

    @GetMapping("/user/fileiv")
    @PreAuthorize("principal.username.equals(#username)")
    @ResponseBody
    public ResponseEntity<String> serveFileIv(@RequestParam(value = "username") String username,@RequestParam(value = "filename") String filename) {
        String fileIv = userService.getFileIv(username, filename);
        return ResponseEntity.ok().body(fileIv);
    }

    @PostMapping(value = "/user")
    @PreAuthorize("principal.username.equals(#username)")
    @ResponseBody
    public String handleFileUpload(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "key") String key,
                                   @RequestParam(value = "username") String username, @RequestParam(value = "iv") String iv,
                                   @RequestParam(value = "pgp_sig") String pgp_sig, @RequestParam(value = "sha256") String hash) {
        try{
            userService.addNewFile(storageProperties.getRoot() + username + "\\" + file.getOriginalFilename(), key, username, iv, pgp_sig, hash);

            storageProperties.setLocation(storageProperties.getRoot() + username);
            storageService.setRootLocation(storageProperties);
            storageService.store(file);
        }catch (FileAlreadyExistsException e) {
            return "the file is already in the server";
        }
        return "upload success";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound() {
        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/share")
    @PreAuthorize("principal.username.equals(#username)")
    @ResponseBody
    public void shareFile(@RequestParam(value = "file") String filename, @RequestParam(value = "source_name") String username,
                          @RequestParam(value = "dest_user") String dest_user, @RequestParam(value = "fileKey") String fileKey) {
        Key key = new Key();
        key.setFileKey(fileKey);
        key.setUid(userService.findUserByUsername(dest_user).getId());
        File file = fileRepository.findByFilePath(filename);
        file.addKeys(key);
        file.addUser(userService.findUserByUsername(dest_user));
    }
}
