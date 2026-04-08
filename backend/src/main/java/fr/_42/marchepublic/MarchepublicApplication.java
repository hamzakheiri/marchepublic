package fr._42.marchepublic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class MarchepublicApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarchepublicApplication.class, args);
    }

}
