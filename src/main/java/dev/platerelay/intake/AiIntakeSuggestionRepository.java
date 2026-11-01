package dev.platerelay.intake;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiIntakeSuggestionRepository extends JpaRepository<AiIntakeSuggestion, UUID> {}
