package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.assembler.FotoCarroModelAssembler;
import com.concessionaria.gomez.api.model.FotoCarroModel;
import com.concessionaria.gomez.api.model.input.FotoCarroInput;
import com.concessionaria.gomez.domain.exception.FotoCarroNaoEncontradaException;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.model.FotoCarro;
import com.concessionaria.gomez.domain.service.CadastroCarroService;
import com.concessionaria.gomez.domain.service.CatalogoFotoCarroService;
import com.concessionaria.gomez.domain.service.FotoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@RestController
@RequestMapping("/carros/{carroId}/foto")
@Tag(name = "Fotos de Veículos", description = "Upload, download e gerenciamento de fotos de veículos")
@RequiredArgsConstructor
public class CarroFotoController {

    static final String MSG_FALHA_UPLOAD = "Falha ao processar o upload da imagem. Tente novamente.";

    private final CadastroCarroService cadastroCarroService;
    private final CatalogoFotoCarroService catalogoFotoCarroService;
    private final FotoStorageService fotoStorageService;
    private final FotoCarroModelAssembler fotoCarroModelAssembler;

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Atualiza ou adiciona a foto de um veículo (upload multipart)")
    public FotoCarroModel atualizarFoto(@PathVariable Long carroId,
                                        @Valid @ModelAttribute FotoCarroInput fotoCarroInput) {
        Carro carro = cadastroCarroService.buscarOuFalhar(carroId);
        MultipartFile arquivo = fotoCarroInput.getArquivo();

        FotoCarro foto = new FotoCarro();
        foto.setCarro(carro);
        foto.setDescricao(fotoCarroInput.getDescricao());
        foto.setContentType(arquivo.getContentType());
        foto.setTamanho(arquivo.getSize());
        foto.setNomeArquivo(arquivo.getOriginalFilename());

        try (InputStream dados = arquivo.getInputStream()) {
            FotoCarro fotoSalva = catalogoFotoCarroService.salvar(foto, dados);
            return fotoCarroModelAssembler.toModel(fotoSalva);
        } catch (IOException e) {
            // A mensagem da IOException pode trazer o caminho do arquivo temporário no servidor (SEC-09)
            log.error("Falha ao ler o upload da foto do carro {}", carroId, e);
            throw new NegocioException(MSG_FALHA_UPLOAD);
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Busca as informações e metadados da foto do veículo")
    public FotoCarroModel buscar(@PathVariable Long carroId) {
        FotoCarro foto = catalogoFotoCarroService.buscarOuFalhar(carroId);
        return fotoCarroModelAssembler.toModel(foto);
    }

    @GetMapping(produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp", MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @Operation(summary = "Serve diretamente o arquivo de imagem do veículo para exibição")
    public ResponseEntity<InputStreamResource> servirFoto(@PathVariable Long carroId) {
        try {
            FotoCarro foto = catalogoFotoCarroService.buscarOuFalhar(carroId);
            MediaType mediaTypeFoto = MediaType.parseMediaType(foto.getContentType());

            FotoStorageService.FotoRecuperada fotoRecuperada = fotoStorageService.recuperar(foto.getNomeArquivo());

            if (fotoRecuperada == null || !fotoRecuperada.temInputStream()) {
                throw new FotoCarroNaoEncontradaException(carroId);
            }

            return ResponseEntity.ok()
                    .contentType(mediaTypeFoto)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline().filename(foto.getNomeArquivo()).build().toString())
                    // Explícito, como na galeria: não depende dos cabeçalhos padrão do Spring Security (SEC-10)
                    .header("X-Content-Type-Options", "nosniff")
                    .body(new InputStreamResource(fotoRecuperada.getInputStream()));
        } catch (FotoCarroNaoEncontradaException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui a foto do veículo")
    public void excluir(@PathVariable Long carroId) {
        catalogoFotoCarroService.excluir(carroId);
    }
}
