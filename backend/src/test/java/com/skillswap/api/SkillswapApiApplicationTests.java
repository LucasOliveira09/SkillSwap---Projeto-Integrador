package com.skillswap.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class SkillswapApiApplicationTests {
	@Autowired
	JdbcTemplate jdbc;

	@Test
	void contextLoads() {
	}

	@Test
	void aplicaMigacoesVersionadasAntesDeValidarAsEntidades() {
		assertEquals(23, jdbc.queryForObject(
				"select max(cast(\"version\" as integer)) from \"flyway_schema_history\" where \"success\" = true", Integer.class));
	}

}
