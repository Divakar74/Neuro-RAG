package com.skillmap.config;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class NLPConfig {

    @Bean
    public StanfordCoreNLP stanfordCoreNLP() {
        // Minimal pipeline to avoid external model dependencies at startup
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit");
        return new StanfordCoreNLP(props);
    }
}
