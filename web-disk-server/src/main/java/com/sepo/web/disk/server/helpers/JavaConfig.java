package com.sepo.web.disk.server.helpers;

import com.sepo.web.disk.server.connection.Network;
import com.sepo.web.disk.server.database.DBRequestBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JavaConfig {

    @Bean
    public Network network(){
        return new Network();
    }

    @Bean
    public DBRequestBuilder dbRequestBuilder(){
        return new DBRequestBuilder();
    }
}
