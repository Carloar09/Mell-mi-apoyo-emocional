package elicuci.czelada.mell_apoyo_emocional.repository;

import elicuci.czelada.mell_apoyo_emocional.model.VoiceProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VoiceProfileRepository extends JpaRepository<VoiceProfile, UUID> {
    List<VoiceProfile> findByUserId(UUID userId);
    List<VoiceProfile> findByUserIdAndIsActiveTrue(UUID userId);
}
