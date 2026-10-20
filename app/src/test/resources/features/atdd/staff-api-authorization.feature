@ATDD
Feature: Staff access boundary acceptance

  @ATDD-US3-001 @FR-006 @SC-004 @EC-005
  Scenario: A forged author id cannot replace the authenticated employee
    Given an authenticated "OWNER" session
    When the client creates an announcement with forged author id 999
    Then the response status is 201
    And the announcement author id is 10

  @ATDD-US1-001 @FR-002 @FR-003 @FR-007 @FR-008 @EC-002
  Scenario: A student cannot delete a tenant file
    Given an authenticated "STUDENT" session
    When the client sends "DELETE" to "/v1/files/00000000-0000-0000-0000-000000000001"
    Then the response status is 403
    And the error code is "COMMON_006"
    And the file deletion service is not called

  @ATDD-US1-001 @FR-007 @FR-008
  Scenario: An instructor can delete a file in the current Tenant
    Given an authenticated "INSTRUCTOR" session
    When the client sends "DELETE" to "/v1/files/00000000-0000-0000-0000-000000000001"
    Then the response status is 204
    And the file deletion service is called

  @ATDD-US1-001 @FR-003 @EC-003
  Scenario: An instructor can manage students
    Given an authenticated "INSTRUCTOR" session
    When the client requests "/v1/students"
    Then the response status is 200
    And the student query service is called

  @ATDD-US1-001 @FR-009 @EC-002
  Scenario: A student can request an attachment for a Q&A
    Given an authenticated "STUDENT" session
    When the client requests a "QNA" upload
    Then the response status is 201
    And the upload service is called

  @ATDD-US1-001 @FR-002 @FR-008 @EC-002
  Scenario: A student cannot request a textbook upload
    Given an authenticated "STUDENT" session
    When the client requests a "TEXTBOOK" upload
    Then the response status is 403
    And the error code is "COMMON_006"
    And the upload service is not called

  @ATDD-US2-001 @FR-004 @FR-005 @FR-008
  Scenario: A manager cannot manage billing keys
    Given an authenticated "MANAGER" session
    When the client requests "/v1/billing-keys"
    Then the response status is 403
    And the error code is "COMMON_006"

  @ATDD-US3-001 @FR-006 @FR-007 @FR-009
  Scenario: An anonymous caller cannot read staff data
    Given no authenticated session
    When the client requests "/v1/students"
    Then the response status is 401
    And the error code is "COMMON_007"

  @ATDD-US3-002 @FR-009 @EC-006
  Scenario: An internal service keeps access to an HMAC-protected schema route
    When an internal HMAC caller requests the schema for "test-tenant"
    Then the response status is 200
    And the schema service is called

  @ATDD-US3-001 @FR-009 @EC-006
  Scenario: A student keeps access to the current-student class route
    Given an authenticated "STUDENT" session
    When the client requests "/v1/classes/me"
    Then the response status is 200
    And the class query service is called

  @ATDD-US3-002 @FR-009 @EC-006
  Scenario: Existing public API documentation remains accessible
    Given no authenticated session
    When the client requests "/v3/api-docs"
    Then the response status is 200

  @ATDD-US3-002 @FR-009 @EC-006
  Scenario: Public API documentation remains accessible without a session
    Given no authenticated session
    When the client requests "/v3/api-docs"
    Then the response status is 200
