package br.com.plataforma.contratos.contrato;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.plataforma.contratos.api.Pagina;

/** Registro e consulta do histórico de mudanças do contrato. */
@Service
public class HistoricoServico {

    private static final Logger log = LoggerFactory.getLogger(HistoricoServico.class);

    private final HistoricoRepositorio historicos;

    public HistoricoServico(HistoricoRepositorio historicos) {
        this.historicos = historicos;
    }

    /**
     * Grava uma linha. Exige transação já aberta (MANDATORY): o registro faz parte da mesma transação da
     * operação, então se a operação falhar, o histórico some junto, e vice-versa.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(UUID contratoId, String campo, String anterior, String novo, UUID usuario) {
        if (usuario == null) {
            // usuario_id é obrigatório no histórico; token de serviço não tem usuário (Contrato §9.2).
            log.warn("Histórico do contrato {} ({}) não registrado: a operação veio sem usuário.", contratoId, campo);
            return;
        }
        historicos.save(HistoricoContrato.novo(contratoId, campo, anterior, novo, usuario));
    }

    /** Registra, em ordem, cada mudança encontrada pelo {@link ComparadorDeContrato}. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(UUID contratoId, List<ComparadorDeContrato.Alteracao> alteracoes, UUID usuario) {
        for (ComparadorDeContrato.Alteracao a : alteracoes) {
            registrar(contratoId, a.campo(), a.anterior(), a.novo(), usuario);
        }
    }

    /** Do mais recente para o mais antigo; o id desempata, para a paginação não repetir nem pular linhas. */
    @Transactional(readOnly = true)
    public Pagina<HistoricoDto> listar(UUID contratoId, int pagina, int tamanho) {
        Sort ordem = Sort.by(Sort.Order.desc("ocorridoEm"), Sort.Order.desc("id"));
        return Pagina.de(historicos.listar(contratoId, PageRequest.of(pagina, tamanho, ordem)), HistoricoDto::de);
    }
}
