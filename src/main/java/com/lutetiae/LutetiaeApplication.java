package com.lutetiae;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.net.InetAddress;

@SpringBootApplication
public class LutetiaeApplication {

	private final Environment environment;

	private static final Logger logger = LoggerFactory.getLogger(LutetiaeApplication.class);

	public LutetiaeApplication(Environment environment) {
		this.environment = environment;
	}

	@EventListener(ApplicationStartedEvent.class)
	public void onApplicationStarted() {
		try {
			String ip = InetAddress.getLocalHost().getHostAddress();
			String port = environment.getProperty("server.port");
			String url = "http://" + ip + ":" + port;
			logger.info("Use this URL: " + url);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(LutetiaeApplication.class, args);
	}
}
