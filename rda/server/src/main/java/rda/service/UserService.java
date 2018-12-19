package rda.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rda.persistence.dao.FileRepository;
import rda.persistence.dao.UserRepository;
import rda.persistence.model.File;
import rda.persistence.model.Key;
import rda.persistence.model.User;
import rda.web.dto.UserDto;
import rda.web.error.UserAlreadyExistException;
import rda.web.error.UserNotExistException;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class UserService implements IUserService {
    private final UserRepository repository;
    private final FileRepository fileRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public UserService(UserRepository repository, BCryptPasswordEncoder bCryptPasswordEncoder, FileRepository fileRepository) {
        this.repository = repository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.fileRepository = fileRepository;
    }

    @Transactional
    @Override
    public User registerNewUserAccount(UserDto accountDto)
            throws UserAlreadyExistException {

        if (emailExist(accountDto.getEmail())) {
            throw new UserAlreadyExistException(
                    "There is an account with that email address:"  + accountDto.getEmail());
        }
        if (userExist(accountDto.getUsername())) {
            throw new UserAlreadyExistException(
                    "There is an account with that username:"  + accountDto.getUsername());
        }
        String dirPath = "upload-dir/" + accountDto.getUsername();
        Path dirPathObj = Paths.get(dirPath);

        boolean dirExists = Files.exists(dirPathObj);
        if(dirExists) {
            System.out.println("! Directory Already Exists !");
        } else {
            try {
                // Creating The New Directory Structure
                Files.createDirectories(dirPathObj);
                System.out.println("! New Directory Successfully Created !");
            } catch (IOException ioExceptionObj) {
                System.out.println("Problem Occurred While Creating The Directory Structure= " + ioExceptionObj.getMessage());
            }
        }

        User user = new User();
        user.setUsername(accountDto.getUsername());
        user.setPassword(bCryptPasswordEncoder.encode(accountDto.getPassword()));
        user.setEmail(accountDto.getEmail());
        return repository.save(user);
    }

    @Transactional
    @Override
    public User findUserByUsername(String username)
            throws UserNotExistException{
        if(repository.findByUsername(username) == null){
            return null;
        }
        return repository.findByUsername(username);
    }

    @Transactional
    @Override
    public void addNewFile(String filePath, String keyString, String username, String iv, String pgp_sig, String sha256)
        throws FileAlreadyExistsException {
        if(fileExist(filePath))
            throw new FileAlreadyExistsException("File " + filePath + " already exists");

        Key key = new Key();
        key.setFileKey(keyString);
        key.setUid(findUserByUsername(username).getId());

        File file = new File();
        file.setFilePath(filePath);
        file.addKeys(key);
        file.addUser(findUserByUsername(username));
        file.setIV(iv);
        file.setPgp_sig(pgp_sig);
        file.setSha256(sha256);

        this.fileRepository.save(file);
    }

    @Transactional
    @Override
    public List<Key> getFileKeys(String username, String filename){
        return fileRepository.findByFilePath("upload-dir\\" + username + "\\" + filename).getKeys();
    }

    @Transactional
    @Override
    public String getFileIv(String username, String filename){
        return fileRepository.findByFilePath("upload-dir\\" + username + "\\" + filename).getIV();
    }

    private boolean userExist(String username) {
        User user = repository.findByUsername(username);
        return user != null;
    }
    private boolean emailExist(String email) {
        User user = repository.findByEmail(email);
        return user != null;
    }

    private boolean fileExist(String filePath) {
        File file = fileRepository.findByFilePath(filePath);
        return file != null;
    }
}