package co.edu.uniquindio.tallerapi2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TallerApi2Application {
    public static void main(String[] args) {
        SpringApplication.run(TallerApi2Application.class, args);
    }
}
