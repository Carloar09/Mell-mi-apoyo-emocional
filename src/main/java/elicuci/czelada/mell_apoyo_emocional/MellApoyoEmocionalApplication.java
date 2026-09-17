package elicuci.czelada.mell_apoyo_emocional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MellApoyoEmocionalApplication {

    public static void main(String[] args) {
        SpringApplication.run(MellApoyoEmocionalApplication.class, args);
    }

}
