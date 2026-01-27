package io.inji.verify.config;

import com.nimbusds.jose.shaded.gson.Gson;
import io.mosip.pixelpass.PixelPass;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.PresentationVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public CredentialsVerifier credentialsVerifier() {
        return new CredentialsVerifier();
    }

    @Bean
    public PresentationVerifier presentationVerifier() {
        return new PresentationVerifier();
    }

    @Bean
    public PixelPass pixelPass() { return new PixelPass(); }

    @Bean
    public Gson Gson() {
        return new Gson();
    }
}
