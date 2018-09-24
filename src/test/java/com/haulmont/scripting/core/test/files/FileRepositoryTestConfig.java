package com.haulmont.scripting.core.test.files;

import com.haulmont.scripting.repository.config.EnableScriptRepositories;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableScriptRepositories(basePackages = {"com.haulmont.scripting.core.test.files"})
public class FileRepositoryTestConfig {
}
