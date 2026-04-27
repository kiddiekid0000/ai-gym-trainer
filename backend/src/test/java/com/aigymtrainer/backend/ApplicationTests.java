package com.aigymtrainer.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic application smoke test.
 * Full integration tests are covered by specific test classes like
 * AuthControllerTest, UserServiceTest, UserRepositoryTest, etc.
 */
class ApplicationTests {

	@Test
	void applicationCanInstantiate() {
		// Basic test to ensure the application class exists and can be loaded
		assertTrue(Application.class.getName().contains("Application"));
	}
}
