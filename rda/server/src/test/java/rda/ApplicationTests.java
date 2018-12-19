package rda;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests {

    private StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();

    @Test
    public void contextLoads() {
        stringEncryptor.setPassword("123");
        System.out.println(stringEncryptor.encrypt(""));
    }

}
