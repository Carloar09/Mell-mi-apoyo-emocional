package elicuci.czelada.mell_apoyo_emocional.repository;

import elicuci.czelada.mell_apoyo_emocional.model.VoiceConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VoiceConsentRepository extends JpaRepository<VoiceConsent, UUID> {

    List<VoiceConsent> findByUserId(UUID userId);
}
