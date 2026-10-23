@BDD
Feature: Employee API role boundaries

  @BDD-US1-001 @FR-002 @FR-003 @FR-008
  Scenario: A student cannot list tenant students
    Given an authenticated "STUDENT" session
    When the client requests "/v1/students"
    Then the response status is 403
    And the error code is "COMMON_006"

  @BDD-US1-001 @FR-002 @FR-009 @EC-002
  Scenario: A student can request a Q&A attachment upload
    Given an authenticated "STUDENT" session
    When the client requests a "QNA" upload
    Then the response status is 201
    And the upload service is called

  @BDD-US1-001 @FR-002 @FR-008 @EC-002
  Scenario: A student cannot request a textbook upload
    Given an authenticated "STUDENT" session
    When the client requests a "TEXTBOOK" upload
    Then the response status is 403
    And the error code is "COMMON_006"
    And the upload service is not called

  @BDD-US2-001 @FR-004 @FR-008
  Scenario: An instructor cannot read billing keys
    Given an authenticated "INSTRUCTOR" session
    When the client requests "/v1/billing-keys"
    Then the response status is 403
    And the error code is "COMMON_006"

  @BDD-US3-001 @FR-006 @SC-004 @EC-005
  Scenario: The server records the authenticated employee as announcement author
    Given an authenticated "OWNER" session
    When the client creates an announcement with forged author id 999
    Then the response status is 201
    And the announcement author id is 10

  @BDD-US1-001 @FR-002 @FR-008 @EC-002
  Scenario: A student cannot list staff exam authoring data
    Given an authenticated "STUDENT" session
    When the client requests "/v1/exams"
    Then the response status is 403
    And the error code is "COMMON_006"

  @BDD-US2-001 @FR-005 @FR-008 @EC-004
  Scenario: An instructor cannot list employee accounts
    Given an authenticated "INSTRUCTOR" session
    When the client requests "/v1/staff"
    Then the response status is 403
    And the error code is "COMMON_006"

  @BDD-US3-005 @FR-010 @EC-002
  Scenario: Student denial keeps the Problem JSON error contract
    Given an authenticated "STUDENT" session
    When the client requests "/v1/staff"
    Then the response status is 403
    And the error code is "COMMON_006"

  @BDD-US1-001 @FR-001 @FR-008
  Scenario: A student is denied by the existing staff-only messaging matcher
    Given an authenticated "STUDENT" session
    When the client requests "/v1/sms/history"
    Then the response status is 403
    And the error code is "COMMON_006"

  @BDD-US3-001 @FR-008 @EC-001
  Scenario: A missing session keeps the existing unauthorized response
    Given no authenticated session
    When the client requests "/v1/students"
    Then the response status is 401
    And the error code is "COMMON_007"

  @BDD-US3-003 @FR-008 @EC-005
  Scenario: An authenticated session without a role is forbidden
    Given an authenticated session without a role
    When the client requests "/v1/students"
    Then the response status is 403
    And the error code is "COMMON_006"

  @BDD-US3-004 @FR-009 @EC-006
  Scenario: A student keeps access to the current-student class route
    Given an authenticated "STUDENT" session
    When the client requests "/v1/classes/me"
    Then the response status is 200
    And the class query service is called

  @BDD-US3-004 @FR-009 @EC-006
  Scenario: An anonymous caller can access the existing public API documentation
    Given no authenticated session
    When the client requests "/v3/api-docs"
    Then the response status is 200
