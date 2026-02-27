package com.deathstar.vader.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.deathstar.vader.domain.Todo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 針對資料庫邊界的整合測試。 拒絕使用 H2！透過 Testcontainers 啟動真實的 PostgreSQL 16 確保方言與索引行為 100% 一致。 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TodoRepositoryTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private TodoRepository repository;

    @Test
    void shouldReturnTodosOrderedByCreatedAtDescending() throws InterruptedException {
        // 1. 準備測試資料
        var todo1 = repository.save(new Todo("First Objective"));
        // 確保時間戳有微小的差異
        Thread.sleep(10);
        var todo2 = repository.save(new Todo("Second Objective"));

        // 2. 執行查詢
        List<Todo> results = repository.findAllByOrderByCreatedAtDesc();

        // 3. 驗證結果 (確保最新建立的排在最前面)
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTitle()).isEqualTo("Second Objective");
        assertThat(results.get(1).getTitle()).isEqualTo("First Objective");

        // 驗證 UUID 是否由 Hibernate 成功生成
        assertThat(results.get(0).getId()).isNotNull();
    }
}
