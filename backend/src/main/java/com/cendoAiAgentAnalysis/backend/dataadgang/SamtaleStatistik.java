package com.cendoAiAgentAnalysis.backend.dataadgang;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Custom interface til statestikfunctioner som SamtaleReposetory skal kunne udføre, og som implementeres i SamtaleStatistikImpl. 
// Dette interface er adskilt fra SamtaleRepository for at holde repositoryet rent og fokuseret på CRUD-operationer, mens statistiklogikken håndteres separat.
public interface SamtaleStatistik {
    OverblikTal hentOverblikstal(String companyId, LocalDateTime fra, LocalDateTime til, String search);

    List<CauseCount> hentTopAarsager(String companyId, LocalDateTime fra, LocalDateTime til, String search);

    List<DayCount> hentSamtalerPrDag(String companyId, LocalDateTime fra, LocalDateTime til, String search);

    record OverblikTal(long totalConversations, long causesFound) {
    }

    record CauseCount(String id, String title, long count) {
    }

    record DayCount(LocalDate date, long conversations) {
    }
}
