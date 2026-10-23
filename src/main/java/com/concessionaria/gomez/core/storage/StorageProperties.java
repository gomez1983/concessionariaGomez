package com.concessionaria.gomez.core.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Getter
@Setter
@Component
@ConfigurationProperties("concessionaria.storage")
public class StorageProperties {

    private Local local = new Local();

    @Getter
    @Setter
    public class Local {
        private Path diretorioFotos = Path.of("./catalogo-fotos");
    }
}
