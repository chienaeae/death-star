package com.deathstar.vader.board.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deathstar.vader.board.repository.BoardColumnRepository;
import com.deathstar.vader.loom.core.domain.Event;
import com.deathstar.vader.loom.core.engine.LoomEngine;
import com.deathstar.vader.loom.core.spi.EventStore;
import com.deathstar.vader.loom.domain.FieldConstants;
import com.deathstar.vader.loom.infrastructure.ScopedValueIdentityResolver;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class BoardTaskServiceTest {

    @Mock private LoomEngine loomEngine;
    @Mock private EventStore eventStore;
    @Mock private BoardColumnRepository columnRepository;
    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock private ScopedValueIdentityResolver identityResolver;

    @InjectMocks private BoardTaskService boardTaskService;

    @BeforeEach
    void setUp() {
        // Setup default mocks for current tenant context if needed
    }

    @Test
    void createTask_insertsRowAndUpdatesLoomProperties() {
        UUID boardId = UUID.randomUUID();
        UUID statusColumnId = UUID.randomUUID();
        String title = "Test Task";
        String tenantId = "tenant-123";

        when(identityResolver.currentTenantId()).thenReturn(tenantId);

        UUID taskId = boardTaskService.createTask(boardId, title, statusColumnId);

        assertNotNull(taskId);

        // Verify initial ITEM_CREATED event is sent to JetStream instead of direct row insert
        verify(eventStore).append(any(Event.class));

        // Verify 3 property updates via LoomEngine
        verify(loomEngine).updateProperty(eq(taskId), any(UUID.class), eq(title), eq(1L));
        verify(loomEngine)
                .updateProperty(
                        eq(taskId),
                        eq(FieldConstants.STATUS_ID),
                        eq(statusColumnId.toString()),
                        eq(2L));
        verify(loomEngine)
                .updateProperty(eq(taskId), eq(FieldConstants.LEXRANK_ID), anyString(), eq(3L));
    }

    @Test
    void moveTask_updatesStatusAndLexRankViaLoom() {
        UUID taskId = UUID.randomUUID();
        UUID newStatusId = UUID.randomUUID();
        String prevLexRank = "a";
        String nextLexRank = "c";
        long currentVersion = 5L;

        boardTaskService.moveTask(taskId, newStatusId, prevLexRank, nextLexRank, currentVersion);

        // Verify Status changed
        verify(loomEngine)
                .updateProperty(
                        eq(taskId),
                        eq(FieldConstants.STATUS_ID),
                        eq(newStatusId.toString()),
                        eq(currentVersion));

        // Verify LexRank changed (assuming 'b' is the midpoint between 'a' and 'c')
        verify(loomEngine)
                .updateProperty(
                        eq(taskId), eq(FieldConstants.LEXRANK_ID), eq("b"), eq(currentVersion + 1));
    }
}
