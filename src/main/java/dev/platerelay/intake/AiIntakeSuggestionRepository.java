package dev.platerelay.intake;

import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiIntakeSuggestionRepository extends JpaRepository<AiIntakeSuggestion, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select suggestion from AiIntakeSuggestion suggestion where suggestion.id = :id")
    Optional<AiIntakeSuggestion> findByIdForUpdate(@Param("id") UUID id);
}
