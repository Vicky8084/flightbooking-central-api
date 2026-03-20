package central_api.central_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CentralApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CentralApiApplication.class, args);
    }
}