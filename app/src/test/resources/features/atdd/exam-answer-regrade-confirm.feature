@BDD @ATDD @US1 @FR-005 @FR-008 @FR-009
Feature: Confirm an exam answer correction
  A staff member confirms a reviewed change and receives an auditable receipt.

  @BDD-US1-002 @ATDD-US1-002 @SC-002 @SC-004
  Scenario: Confirm the reviewed answer correction
    Given an authorized instructor is editing exam 5
    When the instructor confirms the correction with an idempotency key
    Then the official regrade receipt contains the affected record counts

  @BDD-US1-014 @ATDD-US1-014 @FR-014 @SC-002
  Scenario: Confirm changed name, answer, score, and type
    Given an authorized instructor is editing exam 5
    When the instructor confirms a name, answer, score, and type correction
    Then the correction request and receipt include the changed exam fields
