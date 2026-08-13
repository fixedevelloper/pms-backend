package com.pms.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// EnableScheduling : sans ça, @Scheduled sur NightAuditService#autoRunIfEnabled
// serait silencieusement ignoré (aucune erreur, la tâche ne se déclenche juste jamais).
@SpringBootApplication
@EnableScheduling
public class PmsModulithApplication {

	public static void main(String[] args) {
		SpringApplication.run(PmsModulithApplication.class, args);
	}

}
