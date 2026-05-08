package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.common.dto.NcDashboardDto;
import com.qualiapproche.common.enumeration.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class NonConformiteServiceTest {

    @Mock
    private NonConformiteRepository nonConformiteRepository;

    @InjectMocks
    private NonConformiteServiceImpl nonConformiteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetDashboardRQ() {
        // Prepare mock data
        NonConformite nc1 = NonConformite.builder()
                .status(Status.DRAFT)
                .niveauNonConformiteLibelle("Critique")
                .build();
        NonConformite nc2 = NonConformite.builder()
                .status(Status.DRAFT)
                .niveauNonConformiteLibelle("Majeur")
                .build();
        NonConformite nc3 = NonConformite.builder()
                .status(Status.PUBLISHED)
                .niveauNonConformiteLibelle("Critique")
                .build();

        List<NonConformite> mockNcs = Arrays.asList(nc1, nc2, nc3);
        when(nonConformiteRepository.findAll()).thenReturn(mockNcs);

        // Execute
        NcDashboardDto result = nonConformiteService.getDashboardRQ();

        // Verify
        assertEquals(3, result.getTotalNC());
        assertEquals(2, result.getStatsByStatus().get(Status.DRAFT));
        assertEquals(1, result.getStatsByStatus().get(Status.PUBLISHED));

        // Verify gravity breakdown
        Map<String, Long> draftGravity = result.getStatsByStatusAndGravity().get(Status.DRAFT);
        assertEquals(1, draftGravity.get("Critique"));
        assertEquals(1, draftGravity.get("Majeur"));

        Map<String, Long> publishedGravity = result.getStatsByStatusAndGravity().get(Status.PUBLISHED);
        assertEquals(1, publishedGravity.get("Critique"));
    }
}
