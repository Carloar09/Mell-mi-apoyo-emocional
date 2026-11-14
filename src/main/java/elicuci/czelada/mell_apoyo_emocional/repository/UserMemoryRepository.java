package elicuci.czelada.mell_apoyo_emocional.repository;

import elicuci.czelada.mell_apoyo_emocional.model.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserMemoryRepository extends JpaRepository<UserMemory, UUID> {
}