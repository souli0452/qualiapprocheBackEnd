package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.evaluation.dto.SignatureDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Signature;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutSignature;
import com.qualiapproche.evaluation.repository.AuditRepository;
import com.qualiapproche.evaluation.repository.SignatureRepository;
import com.qualiapproche.evaluation.service.referentiel.CodificationDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Le circuit de signature du rapport d'audit.
 *
 * <p>Posé sur un audit en cours — c'est l'« envoi en signature », qui attribue au rapport sa
 * référence. Seul le signataire désigné donne son visa, et <b>dans l'ordre qu'il veut</b> : la
 * maquette n'impose aucun enchaînement, et attendre un absent bloquerait les autres sans rien
 * garantir de plus. L'ordre posé ne sert qu'à l'affichage.</p>
 *
 * <p>Un refus arrête le circuit. Le reposer efface les visas déjà donnés — c'est ce qu'on fait
 * une fois le rapport repris.</p>
 */
@Service
@RequiredArgsConstructor
public class SignatureService {

    private final SignatureRepository depot;
    private final AuditService audits;
    private final AuditRepository depotDesAudits;
    private final CodificationDocumentService codification;

    @Transactional(readOnly = true)
    public List<SignatureDto> circuit(UUID auditId) {
        return depot.findAllByAuditIdOrderByOrdreAsc(audits.sienOuRien(auditId).getId())
                .stream().map(this::versDto).toList();
    }

    @Transactional
    public List<SignatureDto> poserLeCircuit(UUID auditId, List<SignatureDto> signataires) {
        Audit audit = audits.sienOuRien(auditId);
        if (audit.getStatut() != StatutAudit.EN_COURS) {
            throw new BusinessException("Le circuit se pose sur le rapport d'un audit en cours ; "
                    + audit.getReference() + " est " + audit.getStatut() + ".", HttpStatus.CONFLICT);
        }
        if (audit.getType() != null && audit.getType().isCircuitExterne()) {
            throw new BusinessException("Ce type d'audit fait signer son rapport hors de l'application :"
                    + " chargez le rapport signé.", HttpStatus.CONFLICT);
        }
        if (signataires == null || signataires.isEmpty()) {
            throw new BusinessException("Un circuit compte au moins un signataire.", HttpStatus.BAD_REQUEST);
        }
        if (audit.getReferenceRapport() == null) {
            audit.setReferenceRapport(codification.genererReference(CodificationDocumentService.RAPPORT_AUDIT));
            depotDesAudits.save(audit);
        }
        depot.deleteAllByAuditId(audit.getId());
        depot.flush();
        List<Signature> circuit = new ArrayList<>();
        for (int i = 0; i < signataires.size(); i++) {
            SignatureDto s = signataires.get(i);
            circuit.add(Signature.builder()
                    .audit(audit)
                    .signataireId(s.getSignataireId().strip())
                    .fonctionSignataire(s.getFonctionSignataire())
                    .ordre(i + 1)
                    .statut(StatutSignature.EN_ATTENTE)
                    .build());
        }
        return depot.saveAll(circuit).stream().map(this::versDto).toList();
    }

    @Transactional
    public SignatureDto signer(UUID auditId) {
        Signature visa = monTour(auditId);
        visa.signer();
        return versDto(depot.save(visa));
    }

    @Transactional
    public SignatureDto refuser(UUID auditId, String motif) {
        Signature visa = monTour(auditId);
        visa.refuser(motif);
        return versDto(depot.save(visa));
    }

    /** Le visa que l'appelant a encore à donner, ou un refus qui dit pourquoi. */
    private Signature monTour(UUID auditId) {
        Audit audit = audits.sienOuRien(auditId);
        if (audit.getStatut() != StatutAudit.EN_COURS) {
            throw new BusinessException("L'audit " + audit.getReference() + " n'est pas en cours.", HttpStatus.CONFLICT);
        }
        List<Signature> circuit = depot.findAllByAuditIdOrderByOrdreAsc(audit.getId());
        if (circuit.stream().anyMatch(v -> v.getStatut() == StatutSignature.REFUSEE)) {
            throw new BusinessException("Le circuit est arrêté par un refus : il doit être reposé"
                    + " une fois le rapport repris.", HttpStatus.CONFLICT);
        }
        String moi = SecurityUtils.getCurrentUserId();
        return circuit.stream()
                .filter(v -> v.getStatut() == StatutSignature.EN_ATTENTE)
                .filter(v -> moi != null && moi.equals(v.getSignataireId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Vous n'avez aucun visa en attente sur le rapport de "
                        + audit.getReference() + ".", HttpStatus.FORBIDDEN));
    }

    private SignatureDto versDto(Signature s) {
        return SignatureDto.builder()
                .id(s.getId())
                .auditId(s.getAudit().getId())
                .signataireId(s.getSignataireId())
                .fonctionSignataire(s.getFonctionSignataire())
                .ordre(s.getOrdre())
                .statut(s.getStatut())
                .dateSignature(s.getDateSignature())
                .motifRefus(s.getMotifRefus())
                .build();
    }
}
