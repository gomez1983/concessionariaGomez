package com.concessionaria.gomez.core.security;

import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Injeta {@link OperadorAutenticado} nos controllers a partir do JWT já validado pelo filtro de segurança. */
@Component
public class OperadorAutenticadoArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return OperadorAutenticado.class.equals(parameter.getParameterType());
    }

    @Override
    public OperadorAutenticado resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                               NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return atual();
    }

    public static OperadorAutenticado atual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new AuthenticationCredentialsNotFoundException("Operador não autenticado.");
        }
        Jwt jwt = token.getToken();
        String perfil = jwt.getClaimAsString(TokenService.CLAIM_PERFIL);
        try {
            return new OperadorAutenticado(
                    jwt.getSubject(),
                    jwt.getClaimAsString(TokenService.CLAIM_NOME),
                    PerfilOperadorComercial.valueOf(perfil));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadCredentialsException("Token sem perfil válido.");
        }
    }
}
