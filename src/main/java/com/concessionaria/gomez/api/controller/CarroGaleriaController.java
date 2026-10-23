package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.assembler.FotoGaleriaModelAssembler;
import com.concessionaria.gomez.api.model.FotoGaleriaModel;
import com.concessionaria.gomez.api.model.input.FotoCarroInput;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.service.FotoStorageService;
import com.concessionaria.gomez.domain.service.GaleriaCarroService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/carros/{carroId}/fotos")
@Tag(name = "Galeria de veículos")
@RequiredArgsConstructor
public class CarroGaleriaController {
    private final GaleriaCarroService galeria;
    private final FotoStorageService storage;
    private final FotoGaleriaModelAssembler assembler;

    @GetMapping
    public List<FotoGaleriaModel> listar(@PathVariable Long carroId) {
        return galeria.listar(carroId).stream().map(assembler::toModel).toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FotoGaleriaModel adicionar(@PathVariable Long carroId, @Valid @ModelAttribute FotoCarroInput input) {
        return salvar(carroId, null, input);
    }

    @PutMapping(value = "/{fotoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FotoGaleriaModel substituir(@PathVariable Long carroId, @PathVariable Long fotoId,
                                       @Valid @ModelAttribute FotoCarroInput input) {
        return salvar(carroId, fotoId, input);
    }

    @GetMapping("/{fotoId}/conteudo")
    public ResponseEntity<InputStreamResource> imagem(@PathVariable Long carroId, @PathVariable Long fotoId) {
        var foto = galeria.buscarOuFalhar(carroId, fotoId);
        var recuperada = storage.recuperar(foto.getNomeArquivo());
        if (recuperada == null || !recuperada.temInputStream()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(foto.getContentType()))
                .cacheControl(CacheControl.noCache())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(recuperada.getInputStream()));
    }

    @DeleteMapping("/{fotoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long carroId, @PathVariable Long fotoId) { galeria.excluir(carroId, fotoId); }

    private FotoGaleriaModel salvar(Long carroId, Long fotoId, FotoCarroInput input) {
        var arquivo = input.getArquivo();
        try (InputStream dados = arquivo.getInputStream()) {
            return assembler.toModel(galeria.salvar(carroId, fotoId, input.getDescricao(),
                    arquivo.getContentType(), arquivo.getSize(), dados));
        } catch (IOException ex) { throw new NegocioException("Não foi possível ler a imagem enviada."); }
    }
}
