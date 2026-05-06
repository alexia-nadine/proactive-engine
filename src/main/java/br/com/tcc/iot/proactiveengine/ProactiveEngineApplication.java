package br.com.tcc.iot.proactiveengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProactiveEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProactiveEngineApplication.class, args);
	}

}
