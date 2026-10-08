package br.com.plataforma.contratos.api;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Formato de toda listagem paginada (Contrato §8.3). */
public record Pagina<T>(List<T> itens, int pagina, int tamanho, long total) {

    public static <E, T> Pagina<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new Pagina<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements());
    }
}
