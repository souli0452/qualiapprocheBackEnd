package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.common.dto.NonConformiteDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
 
/**
 * Le dossier tel qu'une action corrective a besoin de le montrer : son identité, son origine, ce
 * qui a été constaté, où il en est.
 *
 * <p>Une action se traite en connaissance de la non-conformité qui l'a motivée — son numéro, le
 * processus concerné, la justification du signalement. L'écran de traitement n'en disposait pas :
 * l'action ne porte que l'identifiant de son dossier, qu'aucun écran ne peut afficher, et le
 * responsable devait rouvrir la non-conformité dans une autre liste pour savoir ce qu'on lui
 * demandait de corriger.</p>
 *
 * <p><b>Sans les collections du dossier</b>, et pour deux raisons. La première tient à la boucle :
 * les actions du dossier portent à leur tour leur dossier, et la réponse ne finirait jamais de se
 * replier sur elle-même. La seconde au coût : elles sont chargées à la demande, et les rapatrier
 * pour chaque ligne d'une liste ferait payer à chaque page ce dont aucune n'a l'usage.</p>
 *
 * <p>Interface distincte de {@link NonConformiteMapper} : deux méthodes rendant le même DTO depuis
 * la même entité rendent indécidable, pour MapStruct, celle qu'il doit employer pour convertir une
 * collection.</p>
 */
@Mapper(componentModel = "spring", uses = { ParticipantsMapper.class })
public interface NonConformiteResumeMapper {

    @Mapping(target = "planActions", ignore = true)
    @Mapping(target = "fichiers", ignore = true)
    @Mapping(target = "docRejet", ignore = true)
    @Mapping(target = "workflowState", ignore = true)
    NonConformiteDto versResume(NonConformite entity);
}