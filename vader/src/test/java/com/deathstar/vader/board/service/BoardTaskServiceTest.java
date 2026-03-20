package com.deathstar.vader.board.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deathstar.vader.loom.api.ItemQueryService;
import com.deathstar.vader.loom.api.LoomClient;
import com.deathstar.vader.loom.core.domain.Item;
import com.deathstar.vader.loom.infrastructure.ScopedValueIdentityResolver;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class BoardTaskServiceTest {

    @Mock private LoomClient loomClient;
    @Mock private ItemQueryService itemQueryService;
    @Mock private ScopedValueIdentityResolver identityResolver;

    @InjectMocks private BoardTaskService boardTaskService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void createTask_usesLoomClientToCreateItem() {
        UUID boardId = UUID.randomUUID();
        UUID statusColumnId = UUID.randomUUID();
        String title = "Test Task";
        String tenantId = "tenant-123";

        UUID taskId = boardTaskService.createTask(boardId, title, statusColumnId);

        assertNotNull(taskId);

        verify(loomClient).createItemWithId(eq(taskId), eq("BoardTask"), any());
    }

    @Test
    void moveTask_updatesStatusAndLexRankViaLoomClient() {
        UUID taskId = UUID.randomUUID();
        UUID newStatusId = UUID.randomUUID();
        String prevLexRank = "a";
        String nextLexRank = "c";
        long currentVersion = 5L;

        boardTaskService.moveTask(taskId, newStatusId, prevLexRank, nextLexRank, currentVersion);

        verify(loomClient).updateItem(eq(taskId), any(), org.mockito.ArgumentMatchers.anyLong());
    }
}
