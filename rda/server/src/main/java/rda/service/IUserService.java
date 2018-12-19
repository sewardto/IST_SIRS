package rda.service;

import rda.persistence.model.Key;
import rda.persistence.model.User;
import rda.web.dto.UserDto;
import rda.web.error.UserAlreadyExistException;
import rda.web.error.UserNotExistException;

import java.nio.file.FileAlreadyExistsException;
import java.util.List;

public interface IUserService {
    User registerNewUserAccount(UserDto accountDto)
            throws UserAlreadyExistException;

    User findUserByUsername(String username)
        throws UserNotExistException;

    void addNewFile(String filePath, String key, String username, String iv, String pgp_sig, String sha256)
        throws FileAlreadyExistsException;

    List<Key> getFileKeys(String filename, String username);
    String getFileIv(String username, String filename);
}