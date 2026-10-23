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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/carros/{carroId}/foto")
@Tag(name = "Fotos de Veículos", description = "Upload, download e gerenciamento de fotos de veículos")
@CrossOrigin(origins = "*")
public class CarroFotoController {

    @Autowired
    private CadastroCarroService cadastroCarroService;

    @Autowired
    private CatalogoFotoCarroService catalogoFotoCarroService;

    @Autowired
    private FotoStorageService fotoStorageService;

    @Autowired
    private FotoCarroModelAssembler fotoCarroModelAssembler;

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

        try {
            FotoCarro fotoSalva = catalogoFotoCarroService.salvar(foto, arquivo.getInputStream());
            return fotoCarroModelAssembler.toModel(fotoSalva);
        } catch (IOException e) {
            throw new NegocioException("Falha ao processar o upload da imagem: " + e.getMessage());
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Busca as informações e metadados da foto do veículo")
    public FotoCarroModel buscar(@PathVariable Long carroId) {
        FotoCarro foto = catalogoFotoCarroService.buscarOuFalhar(carroId);
        return fotoCarroModelAssembler.toModel(foto);
    }

    @GetMapping(produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
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
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + foto.getNomeArquivo() + "\"")
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
